package com.takeabreak.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

// Intervalo cerrado entre que se bloquea y se desbloquea el teléfono
data class Pause(val start: LocalDateTime, val end: LocalDateTime) {

    val isWeekend: Boolean
        get() = start.dayOfWeek == DayOfWeek.SATURDAY || start.dayOfWeek == DayOfWeek.SUNDAY

    // Minutos de la pausa que quedan fuera del horario de descanso
    fun minutesOutside(restFrom: Int, restTo: Int): Int {
        val crossesMidnight = restFrom > restTo
        var restSeconds = 0L
        // Arranca el día anterior para contar el descanso que ya venía corriendo al bloquear
        var day = start.toLocalDate().minusDays(1)
        while (!day.isAfter(end.toLocalDate())) {
            val restStart = day.atTime(restFrom, 0)
            val restEnd = (if (crossesMidnight) day.plusDays(1) else day).atTime(restTo, 0)
            restSeconds += overlapSeconds(restStart, restEnd)
            day = day.plusDays(1)
        }
        val totalSeconds = Duration.between(start, end).seconds
        return ((totalSeconds - restSeconds) / 60).toInt()
    }

    private fun overlapSeconds(from: LocalDateTime, to: LocalDateTime): Long {
        val overlapStart = maxOf(start, from)
        val overlapEnd = minOf(end, to)
        return if (overlapEnd > overlapStart) Duration.between(overlapStart, overlapEnd).seconds else 0
    }
}
