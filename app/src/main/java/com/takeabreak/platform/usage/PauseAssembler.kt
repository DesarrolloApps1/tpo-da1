package com.takeabreak.platform.usage

import com.takeabreak.domain.model.Pause
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

// Evento del sistema que importa para las pausas, sin ningún otro dato (RNF03).
// serviceKey: identifica al servicio para emparejar su inicio con su fin. Es un número opaco
// calculado al leer; el nombre de la app no sale del lector ni se guarda
data class ScreenEvent(val at: Instant, val kind: Kind, val serviceKey: Int? = null) {
    enum class Kind { LOCK, UNLOCK, SHUTDOWN, STARTUP, CLOCK_CHANGE, SERVICE_START, SERVICE_STOP }
}

// Arma las pausas cerradas de una secuencia de eventos. No usa Android, así se prueba en la JVM
object PauseAssembler {

    // serviceLookback: un servicio de otra app (música, llamada, navegación) cuenta si arrancó hasta
    // este tiempo antes de bloquear o durante la pausa. Los que corren desde hace más (VPN, contador
    // de pasos) se ignoran: si no, casi ninguna pausa sumaría
    fun closedPauses(
        events: List<ScreenEvent>,
        zone: ZoneId,
        serviceLookback: Duration = Duration.ofMinutes(30),
    ): List<Pause> {
        val pauses = mutableListOf<Pause>()
        var lockedAt: Instant? = null
        var clockChanged = false
        // Servicios corriendo (clave → inicio) y los que ya pararon, como intervalos
        val running = mutableMapOf<Int, Instant>()
        val stopped = mutableListOf<Pair<Instant, Instant>>()

        fun close(at: Instant) {
            val start = lockedAt ?: return
            // Si la hora retrocedió, el cierre puede quedar antes del inicio: esa pausa no se inventa
            if (at > start) {
                val recent = (stopped + running.values.map { it to at })
                    .filter { (from, _) -> !from.isBefore(start.minus(serviceLookback)) }
                pauses += Pause(
                    start = start.atZone(zone).toLocalDateTime(),
                    end = at.atZone(zone).toLocalDateTime(),
                    clockChanged = clockChanged,
                    serviceMinutes = overlapMinutes(recent, start, at),
                )
            }
            lockedAt = null
            stopped.removeAll { (_, end) -> !end.isAfter(at) }
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
                ScreenEvent.Kind.STARTUP -> {
                    lockedAt = null
                    running.clear()
                }
                ScreenEvent.Kind.CLOCK_CHANGE -> if (lockedAt != null) clockChanged = true
                ScreenEvent.Kind.SERVICE_START -> event.serviceKey?.let { running.putIfAbsent(it, event.at) }
                ScreenEvent.Kind.SERVICE_STOP -> event.serviceKey?.let { key ->
                    running.remove(key)?.let { stopped += it to event.at }
                }
            }
        }
        return pauses
    }

    // Minutos de [start, end] en los que corría al menos uno de los servicios, sin contar dos veces
    // los que se superponen
    private fun overlapMinutes(intervals: List<Pair<Instant, Instant>>, start: Instant, end: Instant): Int {
        var total = Duration.ZERO
        var coveredUntil = start
        for ((from, to) in intervals.sortedBy { it.first }) {
            val clippedFrom = maxOf(from, coveredUntil)
            val clippedTo = minOf(to, end)
            if (clippedTo > clippedFrom) {
                total += Duration.between(clippedFrom, clippedTo)
                coveredUntil = clippedTo
            }
        }
        return total.toMinutes().toInt()
    }
}
