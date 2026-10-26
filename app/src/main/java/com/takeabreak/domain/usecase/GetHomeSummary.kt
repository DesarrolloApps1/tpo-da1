package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.HomeSummary
import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.RedemptionRepository
import com.takeabreak.domain.repository.RewardRepository
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class GetHomeSummary(
    private val breaks: BreakRepository,
    private val redemptions: RedemptionRepository,
    private val rewards: RewardRepository,
    private val settings: SettingsRepository,
) {
    operator fun invoke(): Flow<HomeSummary> {
        // El saldo no se guarda: puntos ganados menos puntos canjeados
        val balance = combine(breaks.observeTotalPoints(), redemptions.observeSpentPoints()) { earned, spent ->
            earned - spent
        }
        val pointsToday = breaks.observeSince(LocalDate.now()).map { list -> list.sumOf { it.points } }

        return combine(
            balance, pointsToday, settings.dailyGoal, settings.goalRewardId, rewards.observeRewards(),
        ) { balance, today, dailyGoal, goalId, catalog ->
            HomeSummary(
                balance = balance,
                pointsToday = today,
                dailyGoal = dailyGoal,
                goal = catalog.firstOrNull { it.id == goalId },
                streakDays = 0, // TODO: días seguidos con al menos una pausa válida
                affordable = catalog.filter { it.points <= balance },
            )
        }
    }
}
