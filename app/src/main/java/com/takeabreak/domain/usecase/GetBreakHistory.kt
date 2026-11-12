package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.ActivityPeriod
import com.takeabreak.domain.model.ActivitySummary
import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.model.DayHistory
import com.takeabreak.domain.model.DayMinutes
import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// Resumen de Actividad: gráfico de la semana o del mes, racha e historial de los últimos 30 días
class GetBreakHistory(
    private val breaks: BreakRepository,
    private val settings: SettingsRepository,
    private val today: () -> LocalDate = LocalDate::now,
) {
    operator fun invoke(period: ActivityPeriod = ActivityPeriod.WEEK): Flow<ActivitySummary> {
        val day = today()
        val periodDays = when (period) {
            ActivityPeriod.WEEK -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { monday ->
                (0L until 7L).map { monday.plusDays(it) }
            }
            ActivityPeriod.MONTH -> (1..day.lengthOfMonth()).map { day.withDayOfMonth(it) }
        }
        return combine(breaks.observeSince(day.minusDays(Streaks.HISTORY_DAYS)), settings.dailyGoal) { sessions, dailyGoal ->
            val byDay = sessions.groupBy { it.start.toLocalDate() }
            val chart = periodDays.map { DayMinutes(it, byDay[it].orEmpty().sumOf(::minutesAway)) }
            val validDays = Streaks.validDays(sessions)
            ActivitySummary(
                period = period,
                today = day,
                chart = chart,
                minutes = chart.sumOf { it.minutes },
                points = periodDays.sumOf { date -> byDay[date].orEmpty().sumOf { it.points } },
                dailyGoalMinutes = dailyGoal,
                streakDays = Streaks.current(validDays, day),
                recordStreakDays = Streaks.record(validDays),
                days = byDay.filterKeys { !it.isBefore(day.minusDays(HISTORY_LIST_DAYS)) }
                    .toSortedMap(reverseOrder())
                    .map { (date, list) -> DayHistory(date, list.sortedByDescending { it.start }) },
            )
        }
    }

    // Tiempo lejos del celu: los minutos fuera del horario de descanso de las pausas que alcanzaron el
    // mínimo, aunque el tope no las deje sumar todos. Las cortas, de noche o con la hora cambiada no cuentan
    private fun minutesAway(session: BreakSession): Int = when (session.status) {
        BreakStatus.VALID, BreakStatus.PAUSE_CAP, BreakStatus.DAILY_CAP -> session.validMinutes
        else -> 0
    }

    private companion object {
        const val HISTORY_LIST_DAYS = 30L
    }
}
