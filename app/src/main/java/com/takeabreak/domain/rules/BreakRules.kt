package com.takeabreak.domain.rules

import com.takeabreak.domain.model.Pause

// Reglas de validación de la tabla de reglas del documento
object BreakRules {
    const val DAILY_CAP = 240

    fun minMinutes(pause: Pause): Int = if (pause.isWeekend) 45 else 30

    fun validMinutes(pause: Pause): Int = pause.minutesOutside(restFrom = 23, restTo = 9)

    fun points(pause: Pause, pointsToday: Int): Int {
        val countable = validMinutes(pause)
        if (countable < minMinutes(pause)) return 0
        return countable.coerceAtMost(DAILY_CAP - pointsToday).coerceAtLeast(0)
    }
}
