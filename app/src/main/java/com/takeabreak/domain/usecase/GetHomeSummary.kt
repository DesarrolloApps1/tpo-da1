package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.HomeSummary
import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.PointsRepository
import com.takeabreak.domain.repository.RedemptionRepository
import com.takeabreak.domain.repository.RewardRepository
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class GetHomeSummary(
    private val breaks: BreakRepository,
    private val redemptions: RedemptionRepository,
    private val rewards: RewardRepository,
    private val settings: SettingsRepository,
    private val points: PointsRepository,
    private val today: () -> LocalDate = LocalDate::now,
) {
    operator fun invoke(): Flow<HomeSummary> {
        val day = today()
        val balances = combine(
            breaks.observeTotalPoints(), redemptions.observeSpentPoints(), points.lastKnownBalance,
            breaks.observeSince(day.minusDays(SyncPoints.REPORT_WINDOW_DAYS)), points.reportedDays,
        ) { earned, spent, confirmed, recent, reported ->
            // Lo ganado en el teléfono que el servidor todavía no conoce, con el mismo tope y la
            // misma ventana con que lo informa SyncPoints
            val pending = recent.groupBy { it.start.toLocalDate() }
                .map { (date, sessions) ->
                    val total = sessions.sumOf { it.points }.coerceAtMost(SyncPoints.MAX_DAILY_POINTS)
                    (total - (reported[date] ?: 0)).coerceAtLeast(0)
                }
                .sum()
            // Sin ninguna sincronización todavía, el saldo se calcula en el teléfono
            val spendable = confirmed ?: (earned - spent - pending)
            Balances(spendable + pending, pending, spendable)
        }
        val history = breaks.observeSince(day.minusDays(Streaks.HISTORY_DAYS))

        return combine(
            balances, history, settings.dailyGoal, settings.goalRewardId, rewards.observeRewards(),
        ) { balances, sessions, dailyGoal, goalId, catalog ->
            HomeSummary(
                balance = balances.total,
                pointsToday = sessions.filter { it.start.toLocalDate() == day }.sumOf { it.points },
                dailyGoal = dailyGoal,
                goal = catalog.firstOrNull { it.id == goalId },
                streakDays = Streaks.current(Streaks.validDays(sessions), day),
                affordable = catalog.filter { it.points <= balances.spendable },
                pendingPoints = balances.pending,
                spendable = balances.spendable,
            )
        }
    }

    private data class Balances(val total: Int, val pending: Int, val spendable: Int)
}
