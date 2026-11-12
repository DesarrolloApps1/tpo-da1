package com.takeabreak.domain.model

import java.time.LocalDate

enum class ActivityPeriod { WEEK, MONTH }

// Lo que muestra Actividad (Figma 23 y 24)
data class ActivitySummary(
    val period: ActivityPeriod,
    // El día que se resalta en el gráfico y desde donde se cuentan "Hoy" y "Ayer"
    val today: LocalDate,
    // Un elemento por día del período (lunes a domingo, o del 1 al último del mes), para el gráfico
    val chart: List<DayMinutes>,
    // Minutos lejos del celu y puntos del período
    val minutes: Int,
    val points: Int,
    val dailyGoalMinutes: Int,
    val streakDays: Int,
    val recordStreakDays: Int,
    // Pausas de los últimos 30 días agrupadas por día, de la más nueva a la más vieja
    val days: List<DayHistory>,
)

data class DayMinutes(val day: LocalDate, val minutes: Int)

data class DayHistory(val day: LocalDate, val sessions: List<BreakSession>) {
    val points: Int get() = sessions.sumOf { it.points }
}
