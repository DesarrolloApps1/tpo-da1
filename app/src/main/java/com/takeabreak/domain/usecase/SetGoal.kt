package com.takeabreak.domain.usecase

import com.takeabreak.domain.repository.SettingsRepository

class SetGoal(private val settings: SettingsRepository) {
    suspend operator fun invoke(rewardId: String) = settings.setGoal(rewardId)
}
