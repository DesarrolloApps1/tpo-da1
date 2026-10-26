package com.takeabreak.domain.usecase

import com.takeabreak.domain.repository.SettingsRepository

class SetNotificationsEnabled(private val settings: SettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = settings.setNotificationsEnabled(enabled)
}
