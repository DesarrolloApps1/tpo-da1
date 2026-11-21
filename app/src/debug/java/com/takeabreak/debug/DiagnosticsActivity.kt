package com.takeabreak.debug

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.Pause
import com.takeabreak.domain.rules.BreakRules
import com.takeabreak.platform.usage.AndroidUsageEventsReader
import com.takeabreak.platform.usage.PauseAssembler
import com.takeabreak.ui.theme.TakeABreakTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

// Solo en debug: informe para la prueba técnica de US-01 en cada teléfono (sección 7 del documento)
class DiagnosticsActivity : ComponentActivity() {

    private var report by mutableStateOf("")
    private var demoStatus by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TakeABreakTheme {
                Column(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DemoPauseSection(demoStatus, ::simulate)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::refresh) { Text("Actualizar") }
                        OutlinedButton(onClick = ::copyReport) { Text("Copiar informe") }
                        OutlinedButton(onClick = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) { Text("Acceso") }
                    }
                    Text(
                        text = report,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }

    // Guarda la pausa para la próxima lectura y corre la misma sincronización que el worker: reglas,
    // Room, aviso y puntos. afternoon: de 14 h en adelante, para que sume aunque se ensaye de noche.
    // declared: como si se hubiera tocado el tile justo antes, para mostrar el bonus
    private fun simulate(minutes: Long, afternoon: Boolean, declared: Boolean) {
        val now = LocalDateTime.now().withNano(0)
        val end = if (afternoon) {
            LocalDate.now().atTime(14, 0).plusMinutes(minutes).let { if (it.isAfter(now)) it.minusDays(1) else it }
        } else {
            now
        }
        val start = end.minusMinutes(minutes)
        lifecycleScope.launch {
            val container = applicationContext.appContainer
            if (declared) container.settings.declareBreak(start.minusMinutes(1).atZone(ZoneId.systemDefault()).toInstant())
            PrefsDemoPauseStore(this@DiagnosticsActivity).put(Pause(start, end))
            demoStatus = runCatching { container.breakSync() }.fold(
                onSuccess = { "Pausa simulada de ${FORMAT.format(start)} a ${FORMAT.format(end)}. Abrí Take a Break para verla." },
                onFailure = { "No se pudo simular: ${it.message}" },
            )
            refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val to = Instant.now()
        val from = to.minus(Duration.ofHours(HOURS))
        report = buildString {
            appendLine("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Informe: ${FORMAT.format(to.atZone(ZoneId.systemDefault()))}")
            appendLine()
            appendLine("Acceso a datos de uso: ${yesNo(hasUsageAccess())}")
            appendLine("Exclusión de batería: ${yesNo(getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName))}")
            appendLine("Bucket de standby: ${bucketName(getSystemService(UsageStatsManager::class.java).appStandbyBucket)}")
            appendLine("Worker break-sync: ${workerState()}")
            appendLine()
            appendLine("Eventos de las últimas $HOURS h:")
            countEvents(from, to).forEach { (name, count) -> appendLine("  $name: $count") }
            appendLine()
            val pauses = PauseAssembler.closedPauses(
                AndroidUsageEventsReader(this@DiagnosticsActivity).screenEventsBetween(from, to),
                ZoneId.systemDefault(),
            )
            val long = pauses.filter { Duration.between(it.start, it.end).toMinutes() >= 30 }
            appendLine("Pausas armadas: ${pauses.size}. De 30 min o más: ${long.size}")
            long.forEach {
                appendLine("  ${FORMAT.format(it.start)} → ${FORMAT.format(it.end)} · ${Duration.between(it.start, it.end).toMinutes()} min · ${BreakRules().validMinutes(it)} válidos · ${it.serviceMinutes} con servicio")
            }
        }
    }

    private fun countEvents(from: Instant, to: Instant): List<Pair<String, Int>> {
        val counts = AndroidUsageEventsReader.SCREEN_EVENTS.associateWith { 0 }.toMutableMap()
        val events = getSystemService(UsageStatsManager::class.java).queryEvents(from.toEpochMilli(), to.toEpochMilli())
        val event = UsageEvents.Event()
        while (events != null && events.getNextEvent(event)) {
            counts.computeIfPresent(event.eventType) { _, n -> n + 1 }
        }
        return counts.map { (type, n) -> EVENT_NAMES.getValue(type) to n }
    }

    private fun workerState(): String {
        val infos = WorkManager.getInstance(this).getWorkInfosForUniqueWork("break-sync").get()
        val info = infos.firstOrNull() ?: return "no está programado"
        val next = Instant.ofEpochMilli(info.nextScheduleTimeMillis).atZone(ZoneId.systemDefault())
        return "${info.state}, intentos ${info.runAttemptCount}, próxima ${FORMAT.format(next)}"
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(AppOpsManager::class.java)
        return appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    private fun copyReport() {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Diagnóstico", report))
    }

    private fun yesNo(value: Boolean) = if (value) "sí" else "no"

    private fun bucketName(bucket: Int) = when (bucket) {
        UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "active"
        UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "working_set"
        UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "frequent"
        UsageStatsManager.STANDBY_BUCKET_RARE -> "rare"
        UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "restricted"
        else -> "otro ($bucket)"
    }

    private companion object {
        const val HOURS = 48L
        val FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
        val EVENT_NAMES = mapOf(
            UsageEvents.Event.SCREEN_INTERACTIVE to "SCREEN_INTERACTIVE",
            UsageEvents.Event.SCREEN_NON_INTERACTIVE to "SCREEN_NON_INTERACTIVE",
            UsageEvents.Event.KEYGUARD_SHOWN to "KEYGUARD_SHOWN",
            UsageEvents.Event.KEYGUARD_HIDDEN to "KEYGUARD_HIDDEN",
            UsageEvents.Event.DEVICE_SHUTDOWN to "DEVICE_SHUTDOWN",
            UsageEvents.Event.DEVICE_STARTUP to "DEVICE_STARTUP",
            UsageEvents.Event.FOREGROUND_SERVICE_START to "FOREGROUND_SERVICE_START",
            UsageEvents.Event.FOREGROUND_SERVICE_STOP to "FOREGROUND_SERVICE_STOP",
        )
    }
}
