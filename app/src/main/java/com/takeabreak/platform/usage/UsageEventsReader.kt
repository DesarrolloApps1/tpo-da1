package com.takeabreak.platform.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.takeabreak.domain.model.Pause
import java.time.Instant

// Lee los eventos de pantalla del sistema y arma las pausas cerradas
interface UsageEventsReader {
    suspend fun closedPausesSince(checkpoint: Instant): List<Pause>
}

class AndroidUsageEventsReader(context: Context) : UsageEventsReader {

    private val usageStats = context.getSystemService(UsageStatsManager::class.java)

    override suspend fun closedPausesSince(checkpoint: Instant): List<Pause> =
        TODO("consultar usageStats.queryEvents desde el checkpoint y armar pausas de KEYGUARD_SHOWN a KEYGUARD_HIDDEN")

    private companion object {
        // Solo estos cuatro eventos, nunca nombres de apps (RNF03)
        val SCREEN_EVENTS = setOf(
            UsageEvents.Event.SCREEN_INTERACTIVE,
            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
            UsageEvents.Event.KEYGUARD_SHOWN,
            UsageEvents.Event.KEYGUARD_HIDDEN,
        )
    }
}
