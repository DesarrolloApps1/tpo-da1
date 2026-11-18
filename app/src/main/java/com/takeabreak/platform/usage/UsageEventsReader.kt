package com.takeabreak.platform.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.takeabreak.domain.model.Pause
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

// Lee los eventos de pantalla del sistema y arma las pausas cerradas
interface UsageEventsReader {
    suspend fun closedPausesSince(checkpoint: Instant): List<Pause>
}

// serviceLookback: cuánto antes de bloquear se miran los servicios de otras apps (BreakRulesConfig)
class AndroidUsageEventsReader(
    context: Context,
    private val serviceLookback: Duration = Duration.ofMinutes(30),
) : UsageEventsReader {

    private val usageStats = context.getSystemService(UsageStatsManager::class.java)
    private val clockChanges = ClockChangeLog(context)

    // El checkpoint es el final de la última pausa cerrada, así que una pausa abierta
    // se vuelve a leer entera en la próxima lectura
    override suspend fun closedPausesSince(checkpoint: Instant): List<Pause> = withContext(Dispatchers.IO) {
        // Los servicios se leen desde un poco antes, por si uno arrancó justo antes de bloquear.
        // El resto de los eventos, solo desde el checkpoint, para no volver a armar pausas ya leídas
        val events = screenEventsBetween(checkpoint.minus(serviceLookback), Instant.now())
            .filter { it.serviceKey != null || !it.at.isBefore(checkpoint) }
        PauseAssembler.closedPauses(events, ZoneId.systemDefault(), serviceLookback)
    }

    // Android guarda estos eventos aunque la app no corra, por unos 10 días
    fun screenEventsBetween(from: Instant, to: Instant): List<ScreenEvent> {
        val result = clockChanges.changesBetween(from, to).map { ScreenEvent(it, ScreenEvent.Kind.CLOCK_CHANGE) }.toMutableList()
        // Sin el permiso de acceso a datos de uso no hay eventos
        val events = usageStats.queryEvents(from.toEpochMilli(), to.toEpochMilli()) ?: return result
        val event = UsageEvents.Event()
        while (events.getNextEvent(event)) {
            val kind = KINDS[event.eventType] ?: continue
            // Para emparejar inicio y fin de un servicio alcanza con un número; el nombre no se guarda
            val key = if (kind in SERVICE_KINDS) (event.packageName + "/" + event.className).hashCode() else null
            result += ScreenEvent(Instant.ofEpochMilli(event.timeStamp), kind, key)
        }
        return result
    }

    companion object {
        // Solo estos eventos (RNF03). Los de pantalla no arman pausas; se cuentan en el diagnóstico
        // de la prueba técnica. Los de servicios existen desde Android 10: en Android 9 la regla del
        // servicio activo no se aplica
        val SCREEN_EVENTS = setOf(
            UsageEvents.Event.SCREEN_INTERACTIVE,
            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
            UsageEvents.Event.KEYGUARD_SHOWN,
            UsageEvents.Event.KEYGUARD_HIDDEN,
            UsageEvents.Event.DEVICE_SHUTDOWN,
            UsageEvents.Event.DEVICE_STARTUP,
            UsageEvents.Event.FOREGROUND_SERVICE_START,
            UsageEvents.Event.FOREGROUND_SERVICE_STOP,
        )

        private val SERVICE_KINDS = setOf(ScreenEvent.Kind.SERVICE_START, ScreenEvent.Kind.SERVICE_STOP)

        private val KINDS = mapOf(
            UsageEvents.Event.KEYGUARD_SHOWN to ScreenEvent.Kind.LOCK,
            UsageEvents.Event.KEYGUARD_HIDDEN to ScreenEvent.Kind.UNLOCK,
            UsageEvents.Event.DEVICE_SHUTDOWN to ScreenEvent.Kind.SHUTDOWN,
            UsageEvents.Event.DEVICE_STARTUP to ScreenEvent.Kind.STARTUP,
            UsageEvents.Event.FOREGROUND_SERVICE_START to ScreenEvent.Kind.SERVICE_START,
            UsageEvents.Event.FOREGROUND_SERVICE_STOP to ScreenEvent.Kind.SERVICE_STOP,
        )
    }
}
