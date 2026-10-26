package com.takeabreak.domain.model

import java.time.DayOfWeek
import java.time.LocalDateTime

// Intervalo cerrado entre que se bloquea y se desbloquea el teléfono
data class Pause(val start: LocalDateTime, val end: LocalDateTime) {

    val isWeekend: Boolean
        get() = start.dayOfWeek == DayOfWeek.SATURDAY || start.dayOfWeek == DayOfWeek.SUNDAY

    // Minutos de la pausa que quedan fuera del horario de descanso
    fun minutesOutside(restFrom: Int, restTo: Int): Int =
        TODO("descontar los minutos entre restFrom y restTo, incluso si la pausa cruza la medianoche")
}
