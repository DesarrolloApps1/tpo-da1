package com.takeabreak.presentation.home

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.model.HomeSummary
import com.takeabreak.domain.usecase.GetHomeSummary
import com.takeabreak.presentation.UiState
import com.takeabreak.testing.FakeBreakRepository
import com.takeabreak.testing.FakeNetworkMonitor
import com.takeabreak.testing.FakePointsRepository
import com.takeabreak.testing.FakeRedemptionRepository
import com.takeabreak.testing.FakeRewardRepository
import com.takeabreak.testing.FakeSettingsRepository
import com.takeabreak.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class HomeViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.parse("2026-11-18")
    private val breaks = FakeBreakRepository()
    private val points = FakePointsRepository()
    private val network = FakeNetworkMonitor()

    private fun TestScope.subscribed(): HomeViewModel {
        val getHomeSummary = GetHomeSummary(
            breaks, FakeRedemptionRepository(), FakeRewardRepository(), FakeSettingsRepository(), points, today = { today },
        )
        val vm = HomeViewModel(getHomeSummary, network)
        vm.state.launchIn(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))
        return vm
    }

    private fun session(points: Int) = today.atTime(15, 0).let {
        BreakSession(it, it.plusMinutes(points.toLong()), points, points, BreakStatus.VALID)
    }

    @Test
    fun `sin puntos muestra el estado vacio`() = runTest {
        val vm = subscribed()

        assertEquals(UiState.Empty, vm.state.value.content)
    }

    @Test
    fun `con pausas muestra el saldo y los puntos por confirmar`() = runTest {
        breaks.sessions.value = listOf(session(60))
        points.lastKnownBalance.value = 100

        val vm = subscribed()

        val summary = (vm.state.value.content as UiState.Content<HomeSummary>).data
        assertEquals(160, summary.balance)
        assertEquals(60, summary.pendingPoints)
        assertEquals(60, summary.pointsToday)
    }

    @Test
    fun `una pausa nueva actualiza el saldo sin reabrir la pantalla`() = runTest {
        val vm = subscribed()

        breaks.sessions.value = listOf(session(45))

        assertEquals(45, (vm.state.value.content as UiState.Content<HomeSummary>).data.balance)
    }

    @Test
    fun `sin red marca la pantalla offline y muestra lo guardado`() = runTest {
        breaks.sessions.value = listOf(session(60))
        network.isOnline.value = false

        val vm = subscribed()

        assertTrue(vm.state.value.isOffline)
        assertTrue(vm.state.value.content is UiState.Content)
    }
}
