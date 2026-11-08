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

    override suspend fun closedPausesSince(checkpoint: Instant): List<Pause> = withContext(Dispatchers.IO) {
        PauseAssembler.closedPauses(screenEventsBetween(checkpoint, Instant.now()), ZoneId.systemDefault())
    }

    // Android guarda estos eventos aunque la app no corra, por unos 10 días
    fun screenEventsBetween(from: Instant, to: Instant): List<ScreenEvent> {
        // Sin el permiso de acceso a datos de uso no hay eventos
        val events = usageStats.queryEvents(from.toEpochMilli(), to.toEpochMilli()) ?: return emptyList()
        val result = mutableListOf<ScreenEvent>()
        val event = UsageEvents.Event()
        while (events.getNextEvent(event)) {
            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_SHOWN -> result += ScreenEvent(Instant.ofEpochMilli(event.timeStamp), locked = true)
                UsageEvents.Event.KEYGUARD_HIDDEN -> result += ScreenEvent(Instant.ofEpochMilli(event.timeStamp), locked = false)
            }
        }
        return result
    }

    companion object {
        // Solo estos cuatro eventos, nunca nombres de apps (RNF03). Las pausas usan los de bloqueo;
        // los de pantalla se cuentan en el diagnóstico de la prueba técnica
        val SCREEN_EVENTS = setOf(
            UsageEvents.Event.SCREEN_INTERACTIVE,
            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
            UsageEvents.Event.KEYGUARD_SHOWN,
            UsageEvents.Event.KEYGUARD_HIDDEN,
        )
    }
}
