package com.takeabreak.platform.notifications

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

// Respeta el interruptor de Perfil. Solo corta el aviso: el worker ya guardó la pausa antes de llamar
class PreferenceGatedNotifier(
    private val delegate: Notifier,
    private val settings: SettingsRepository,
) : Notifier {

    override suspend fun breaksValidated(sessions: List<BreakSession>, reachedGoal: Reward?) {
        if (enabled()) delegate.breaksValidated(sessions, reachedGoal)
    }

    private suspend fun enabled() = settings.notificationsEnabled.first()
}
