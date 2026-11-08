package com.takeabreak.domain.rules

import com.takeabreak.domain.model.Pause
import java.time.Duration

// Reglas de validación de la tabla de reglas del documento
object BreakRules {
    const val DAILY_CAP = 240

    // Debajo de esto es mirar la hora y volver a bloquear: no se guarda en el historial
    const val MIN_RECORDED_MINUTES = 1

    fun isRecorded(pause: Pause): Boolean = Duration.between(pause.start, pause.end).toMinutes() >= MIN_RECORDED_MINUTES

    fun minMinutes(pause: Pause): Int = if (pause.isWeekend) 45 else 30

    fun validMinutes(pause: Pause): Int = pause.minutesOutside(restFrom = 23, restTo = 9)

    fun points(pause: Pause, pointsToday: Int): Int {
        if (pause.clockChanged) return 0
        val countable = validMinutes(pause)
        if (countable < minMinutes(pause)) return 0
        return countable.coerceAtMost(DAILY_CAP - pointsToday).coerceAtLeast(0)
    }
}
