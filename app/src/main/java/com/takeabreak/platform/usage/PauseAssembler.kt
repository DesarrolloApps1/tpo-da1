package com.takeabreak.platform.usage

import com.takeabreak.domain.model.Pause
import java.time.Instant
import java.time.ZoneId

// Evento del sistema que importa para las pausas, sin ningún otro dato (RNF03)
data class ScreenEvent(val at: Instant, val kind: Kind) {
    enum class Kind { LOCK, UNLOCK, SHUTDOWN, STARTUP, CLOCK_CHANGE }
}

// Arma las pausas cerradas de una secuencia de eventos. No usa Android, así se prueba en la JVM
object PauseAssembler {

    fun closedPauses(events: List<ScreenEvent>, zone: ZoneId): List<Pause> {
        val pauses = mutableListOf<Pause>()
        var lockedAt: Instant? = null
        var clockChanged = false

        fun close(at: Instant) {
            val start = lockedAt ?: return
            // Si la hora retrocedió, el cierre puede quedar antes del inicio: esa pausa no se inventa
            if (at > start) {
                pauses += Pause(start.atZone(zone).toLocalDateTime(), at.atZone(zone).toLocalDateTime(), clockChanged)
            }
            lockedAt = null
        }

        for (event in events.sortedBy { it.at }) {
            when (event.kind) {
                // Un segundo bloqueo seguido es la pantalla que se encendió por una notificación: la pausa sigue
                ScreenEvent.Kind.LOCK -> if (lockedAt == null) {
                    lockedAt = event.at
                    clockChanged = false
                }
                // Un desbloqueo sin bloqueo antes es el cierre de una pausa ya leída: se ignora
                ScreenEvent.Kind.UNLOCK -> close(event.at)
                // El tiempo apagado no cuenta: la pausa termina al apagar
                ScreenEvent.Kind.SHUTDOWN -> close(event.at)
                // Si faltó el apagado, al encender no queda ninguna pausa abierta de antes
                ScreenEvent.Kind.STARTUP -> lockedAt = null
                ScreenEvent.Kind.CLOCK_CHANGE -> if (lockedAt != null) clockChanged = true
            }
        }
        return pauses
    }
}
