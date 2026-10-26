package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.Profile
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.domain.repository.AuthRepository
import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.RedemptionRepository
import com.takeabreak.domain.repository.RewardRepository
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetProfile(
    private val auth: AuthRepository,
    private val breaks: BreakRepository,
    private val redemptions: RedemptionRepository,
    private val rewards: RewardRepository,
    private val settings: SettingsRepository,
) {
    operator fun invoke(): Flow<Profile> {
        val preferences = combine(settings.goalRewardId, settings.dailyGoal, settings.notificationsEnabled, ::Triple)
        // Total histórico: todo lo ganado en pausas, sin restar los canjes (eso es el saldo de Inicio)
        return combine(
            auth.account, breaks.observeTotalPoints(), redemptions.observeRedemptions(), rewards.observeRewards(), preferences,
        ) { account, earned, history, catalog, (goalId, dailyGoal, notifications) ->
            Profile(
                account = account,
                totalEarned = earned,
                redemptionCount = history.count { it.status != RedemptionStatus.REFUNDED },
                activeRedemptions = history.count { it.status == RedemptionStatus.ACTIVE },
                goal = catalog.firstOrNull { it.id == goalId },
                dailyGoal = dailyGoal,
                notificationsEnabled = notifications,
                catalog = catalog,
            )
        }
    }
}
