package com.takeabreak.presentation.profile

import app.cash.turbine.test
import com.takeabreak.domain.model.Account
import com.takeabreak.domain.model.AppPermission
import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.model.Profile
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.domain.usecase.GetProfile
import com.takeabreak.domain.usecase.ObserveSession
import com.takeabreak.domain.usecase.SetGoal
import com.takeabreak.domain.usecase.SetNotificationsEnabled
import com.takeabreak.domain.usecase.SignOut
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.UiState
import com.takeabreak.testing.FakeAuthRepository
import com.takeabreak.testing.FakeBreakRepository
import com.takeabreak.testing.FakeNetworkMonitor
import com.takeabreak.testing.FakeRedemptionRepository
import com.takeabreak.testing.FakeRewardRepository
import com.takeabreak.testing.FakeSettingsRepository
import com.takeabreak.testing.MainDispatcherRule
import com.takeabreak.testing.redemption
import com.takeabreak.testing.reward
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository().apply {
        token.value = "session-token"
        savedAccount.value = Account("martina@mail.com", "Martina")
    }
    private val breaks = FakeBreakRepository()
    private val redemptions = FakeRedemptionRepository()
    private val rewards = FakeRewardRepository()
    private val settings = FakeSettingsRepository()
    private val granted = mutableSetOf(AppPermission.USAGE_ACCESS)

    private fun viewModel() = ProfileViewModel(
        getProfile = GetProfile(auth, breaks, redemptions, rewards, settings),
        setGoal = SetGoal(settings),
        setNotificationsEnabled = SetNotificationsEnabled(settings),
        signOut = SignOut(auth),
        permissionChecker = { it in granted },
        network = FakeNetworkMonitor(),
    )

    private fun pause(points: Int, day: Int) =
        BreakSession(LocalDateTime.of(2026, 10, day, 15, 0), LocalDateTime.of(2026, 10, day, 16, 0), points, points, BreakStatus.VALID)

    // Espera el primer Content y lo devuelve
    private suspend fun StateFlow<ScreenState<Profile>>.profile(): Profile =
        first { it.content is UiState.Content }.content.let { (it as UiState.Content).data }

    @Test
    fun `muestra la cuenta, el total historico y los canjes`() = runTest {
        breaks.sessions.value = listOf(pause(60, 20), pause(90, 21))
        redemptions.redemptions.value = listOf(
            redemption("a", points = 100),
            redemption("b", points = 50, status = RedemptionStatus.USED),
            redemption("c", points = 120, status = RedemptionStatus.REFUNDED),
        )

        val profile = viewModel().state.profile()

        assertEquals("Martina", profile.account?.displayName)
        // Lo ganado, sin restar canjes: el saldo de Inicio sería 150 - 150
        assertEquals(150, profile.totalEarned)
        assertEquals(2, profile.redemptionCount)
        assertEquals(1, profile.activeRedemptions)
    }

    @Test
    fun `apagar las notificaciones guarda la preferencia y se ve en pantalla`() = runTest {
        val vm = viewModel()
        assertTrue(vm.state.profile().notificationsEnabled)

        vm.setNotifications(false)

        assertFalse(settings.notificationsEnabled.value)
        vm.state.test {
            // Puede llegar primero el valor anterior; lo que importa es que la pantalla termina mostrando el cambio
            while (true) {
                val profile = (awaitItem().content as? UiState.Content)?.data ?: continue
                if (!profile.notificationsEnabled) break
            }
        }
    }

    @Test
    fun `elegir meta guarda el premio y lo muestra`() = runTest {
        rewards.rewards.value = listOf(reward("yoga"), reward("cafe"))
        val vm = viewModel()

        vm.chooseGoal("yoga")

        assertEquals("yoga", settings.goalRewardId.value)
        assertEquals("Premio yoga", vm.state.profile().goal?.title)
    }

    @Test
    fun `la meta diaria queda entre la pausa minima y el tope del dia`() = runTest {
        val vm = viewModel()

        vm.chooseDailyGoal(90)
        assertEquals(90, settings.dailyGoal.value)

        vm.chooseDailyGoal(500)
        assertEquals(240, settings.dailyGoal.value)

        vm.chooseDailyGoal(5)
        assertEquals(30, settings.dailyGoal.value)
    }

    @Test
    fun `cerrar sesion borra la sesion pero no las pausas ni los canjes`() = runTest {
        breaks.sessions.value = listOf(pause(60, 20))
        redemptions.redemptions.value = listOf(redemption("a"))
        val vm = viewModel()

        ObserveSession(auth)().test {
            assertTrue(awaitItem())
            vm.signOut()
            assertFalse(awaitItem())
        }

        assertNull(auth.savedAccount.value)
        assertEquals(1, breaks.sessions.value.size)
        assertEquals(1, redemptions.redemptions.value.size)
    }

    @Test
    fun `los permisos se vuelven a leer al volver de Ajustes`() = runTest {
        val vm = viewModel()
        assertEquals(false, vm.permissions.value[AppPermission.NOTIFICATIONS])

        granted += AppPermission.NOTIFICATIONS
        vm.refreshPermissions()

        assertEquals(true, vm.permissions.value[AppPermission.NOTIFICATIONS])
        assertEquals(true, vm.permissions.value[AppPermission.USAGE_ACCESS])
        assertEquals(false, vm.permissions.value[AppPermission.LOCATION])
    }
}
