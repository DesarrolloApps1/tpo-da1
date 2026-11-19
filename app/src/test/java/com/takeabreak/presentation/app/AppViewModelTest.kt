package com.takeabreak.presentation.app

import app.cash.turbine.test
import com.takeabreak.domain.model.Account
import com.takeabreak.domain.model.AppPermission
import com.takeabreak.domain.usecase.GetAccount
import com.takeabreak.domain.usecase.ObserveSession
import com.takeabreak.domain.usecase.SignOut
import com.takeabreak.testing.FakeAuthRepository
import com.takeabreak.testing.FakeNetworkMonitor
import com.takeabreak.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val network = FakeNetworkMonitor()
    private val granted = mutableSetOf<AppPermission>()

    private var pointsSyncs = 0

    private fun viewModel() = AppViewModel(ObserveSession(auth), GetAccount(auth), network, { it in granted }, { pointsSyncs++ })

    @Test
    fun `informa los puntos solo cuando hay sesion`() = runTest {
        viewModel()
        assertEquals(0, pointsSyncs)

        auth.token.value = "session-token"

        assertEquals(1, pointsSyncs)
    }

    @Test
    fun `junta sesion, cuenta y conexion para el scaffold`() = runTest {
        auth.token.value = "session-token"
        auth.savedAccount.value = Account("ana@mail.com")
        network.isOnline.value = false

        val state = viewModel().state.first { it.isSignedIn != null }

        assertEquals(AppState(isSignedIn = true, account = Account("ana@mail.com"), isOnline = false, onboardingDone = true), state)
    }

    @Test
    fun `cerrar sesion se refleja para volver al acceso`() = runTest {
        auth.token.value = "session-token"
        val vm = viewModel()

        vm.state.test {
            assertEquals(true, expectMostRecentItem().isSignedIn)
            SignOut(auth)()
            assertEquals(false, awaitItem().isSignedIn)
        }
    }

    @Test
    fun `pide permisos solo si falta el acceso a uso`() {
        val vm = viewModel()
        assertTrue(vm.needsPermissions())

        granted += AppPermission.USAGE_ACCESS

        assertFalse(vm.needsPermissions())
    }

    @Test
    fun `el onboarding queda marcado como visto`() = runTest {
        val seen = MutableStateFlow(false)
        val vm = AppViewModel(
            ObserveSession(auth), GetAccount(auth), network, { true }, {},
            onboardingDone = seen, markOnboardingDone = { seen.value = true },
        )
        vm.state.launchIn(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))
        assertEquals(false, vm.state.value.onboardingDone)

        vm.finishOnboarding()
        testScheduler.advanceUntilIdle()

        assertEquals(true, vm.state.value.onboardingDone)
    }
}
