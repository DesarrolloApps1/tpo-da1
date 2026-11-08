package com.takeabreak.domain.usecase

import com.takeabreak.domain.repository.SettingsRepository
import com.takeabreak.domain.rules.BreakRules

class SetGoal(
    private val settings: SettingsRepository,
    private val rules: BreakRules = BreakRules(),
) {
    suspend operator fun invoke(rewardId: String) = settings.setGoal(rewardId)

    // Meta diaria en minutos (1 punto por minuto): entre la pausa mínima y el tope del día
    suspend fun dailyGoal(minutes: Int) = settings.setDailyGoal(minutes.coerceIn(MIN_DAILY_GOAL, rules.config.dailyCap))

    companion object {
        const val MIN_DAILY_GOAL = 30
    }
}
