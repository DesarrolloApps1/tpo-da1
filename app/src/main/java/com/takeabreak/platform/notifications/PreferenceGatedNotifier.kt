package com.takeabreak.platform.notifications

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

// Respeta el interruptor de Perfil. Solo corta el aviso: el worker ya guardó la pausa antes de llamar
class PreferenceGatedNotifier(
    private val delegate: Notifier,
    private val settings: SettingsRepository,
) : Notifier {

    override suspend fun breakValidated(session: BreakSession, reachedGoal: Reward?) {
        if (enabled()) delegate.breakValidated(session, reachedGoal)
    }

    override suspend fun redemptionConfirmed(redemption: Redemption) {
        if (enabled()) delegate.redemptionConfirmed(redemption)
    }

    private suspend fun enabled() = settings.notificationsEnabled.first()
}
