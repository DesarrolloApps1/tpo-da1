package com.takeabreak.platform.notifications

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.testing.FakeNotifier
import com.takeabreak.testing.FakeSettingsRepository
import com.takeabreak.testing.redemption
import com.takeabreak.testing.reward
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class PreferenceGatedNotifierTest {

    private val settings = FakeSettingsRepository()
    private val delegate = FakeNotifier()
    private val notifier = PreferenceGatedNotifier(delegate, settings)

    private val session = BreakSession(LocalDateTime.of(2026, 10, 20, 15, 0), LocalDateTime.of(2026, 10, 20, 16, 0), 60, 60, BreakStatus.VALID)

    @Test
    fun `con notificaciones activas avisa y pasa el premio meta`() = runTest {
        val goal = reward("yoga")

        notifier.breakValidated(session, goal)
        notifier.redemptionConfirmed(redemption("a"))

        assertEquals(listOf(session to goal), delegate.breaks)
        assertEquals(1, delegate.redemptions.size)
    }

    @Test
    fun `con notificaciones apagadas no avisa nada`() = runTest {
        settings.setNotificationsEnabled(false)

        notifier.breakValidated(session)
        notifier.redemptionConfirmed(redemption("a"))

        assertTrue(delegate.breaks.isEmpty())
        assertTrue(delegate.redemptions.isEmpty())
    }

    @Test
    fun `volver a activarlas avisa de nuevo`() = runTest {
        settings.setNotificationsEnabled(false)
        notifier.breakValidated(session)

        settings.setNotificationsEnabled(true)
        notifier.breakValidated(session)

        assertEquals(1, delegate.breaks.size)
    }
}
