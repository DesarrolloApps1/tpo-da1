package com.takeabreak.domain.rules

import com.takeabreak.domain.model.Pause

// Reglas de validación de la tabla de reglas del documento
object BreakRules {
    const val DAILY_CAP = 240

    fun points(pause: Pause, pointsToday: Int): Int {
        val minMinutes = if (pause.isWeekend) 45 else 30
        val countable = pause.minutesOutside(restFrom = 23, restTo = 9)
        if (countable < minMinutes) return 0
        return countable.coerceAtMost(DAILY_CAP - pointsToday)
    }
}
