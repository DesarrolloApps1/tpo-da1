package com.takeabreak.platform.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.takeabreak.domain.model.Pause
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

// Lee los eventos de pantalla del sistema y arma las pausas cerradas
interface UsageEventsReader {
    suspend fun closedPausesSince(checkpoint: Instant): List<Pause>
}

class AndroidUsageEventsReader(context: Context) : UsageEventsReader {

    private val usageStats = context.getSystemService(UsageStatsManager::class.java)
    private val clockChanges = ClockChangeLog(context)

    // El checkpoint es el final de la última pausa cerrada, así que una pausa abierta
    // se vuelve a leer entera en la próxima lectura
    override suspend fun closedPausesSince(checkpoint: Instant): List<Pause> = withContext(Dispatchers.IO) {
        PauseAssembler.closedPauses(screenEventsBetween(checkpoint, Instant.now()), ZoneId.systemDefault())
    }

    // Android guarda estos eventos aunque la app no corra, por unos 10 días
    fun screenEventsBetween(from: Instant, to: Instant): List<ScreenEvent> {
        val result = clockChanges.changesBetween(from, to).map { ScreenEvent(it, ScreenEvent.Kind.CLOCK_CHANGE) }.toMutableList()
        // Sin el permiso de acceso a datos de uso no hay eventos
        val events = usageStats.queryEvents(from.toEpochMilli(), to.toEpochMilli()) ?: return result
        val event = UsageEvents.Event()
        while (events.getNextEvent(event)) {
            val kind = KINDS[event.eventType] ?: continue
            result += ScreenEvent(Instant.ofEpochMilli(event.timeStamp), kind)
        }
        return result
    }

    companion object {
        // Solo estos eventos, nunca nombres de apps (RNF03). Los de pantalla no arman pausas;
        // se cuentan en el diagnóstico de la prueba técnica
        val SCREEN_EVENTS = setOf(
            UsageEvents.Event.SCREEN_INTERACTIVE,
            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
            UsageEvents.Event.KEYGUARD_SHOWN,
            UsageEvents.Event.KEYGUARD_HIDDEN,
            UsageEvents.Event.DEVICE_SHUTDOWN,
            UsageEvents.Event.DEVICE_STARTUP,
        )

        private val KINDS = mapOf(
            UsageEvents.Event.KEYGUARD_SHOWN to ScreenEvent.Kind.LOCK,
            UsageEvents.Event.KEYGUARD_HIDDEN to ScreenEvent.Kind.UNLOCK,
            UsageEvents.Event.DEVICE_SHUTDOWN to ScreenEvent.Kind.SHUTDOWN,
            UsageEvents.Event.DEVICE_STARTUP to ScreenEvent.Kind.STARTUP,
        )
    }
}
