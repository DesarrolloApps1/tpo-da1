package com.takeabreak.platform.usage

import com.takeabreak.domain.model.Pause
import java.time.Instant
import java.time.ZoneId

// Bloqueo o desbloqueo del teléfono, sin ningún otro dato del sistema
data class ScreenEvent(val at: Instant, val locked: Boolean)

// Arma las pausas cerradas de una secuencia de eventos. No usa Android, así se prueba en la JVM
object PauseAssembler {

    fun closedPauses(events: List<ScreenEvent>, zone: ZoneId): List<Pause> {
        val pauses = mutableListOf<Pause>()
        var lockedAt: Instant? = null
        for (event in events.sortedBy { it.at }) {
            when {
                // Un segundo bloqueo seguido es la pantalla que se encendió por una notificación: la pausa sigue
                event.locked -> if (lockedAt == null) lockedAt = event.at
                lockedAt != null -> {
                    pauses += Pause(lockedAt.atZone(zone).toLocalDateTime(), event.at.atZone(zone).toLocalDateTime())
                    lockedAt = null
                }
                // Un desbloqueo sin bloqueo antes es el cierre de una pausa ya leída: se ignora
            }
        }
        return pauses
    }
}
