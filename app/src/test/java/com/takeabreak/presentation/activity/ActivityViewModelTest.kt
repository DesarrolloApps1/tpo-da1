package com.takeabreak.presentation.activity

import com.takeabreak.domain.model.ActivityPeriod
import com.takeabreak.domain.model.ActivitySummary
import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.rules.BreakRulesConfig
import com.takeabreak.domain.usecase.GetBreakHistory
import com.takeabreak.presentation.UiState
import com.takeabreak.testing.FakeBreakRepository
import com.takeabreak.testing.FakeNetworkMonitor
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

class ActivityViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    // 18/11/2026 es miércoles
    private val today = LocalDate.parse("2026-11-18")
    private val breaks = FakeBreakRepository()
    private val network = FakeNetworkMonitor()

    private fun TestScope.subscribed(): ActivityViewModel {
        val vm = ActivityViewModel(GetBreakHistory(breaks, FakeSettingsRepository(), today = { today }), network, BreakRulesConfig())
        vm.state.launchIn(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))
        return vm
    }

    private fun ActivityViewModel.summary() = (state.value.content as UiState.Content<ActivitySummary>).data

    @Test
    fun `sin pausas muestra el contenido vacio de la pantalla y no el estado vacio generico`() = runTest {
        val vm = subscribed()

        assertTrue(vm.summary().days.isEmpty())
        assertEquals(7, vm.summary().chart.size)
    }

    @Test
    fun `arranca en la semana y se puede pasar al mes`() = runTest {
        val vm = subscribed()
        assertEquals(ActivityPeriod.WEEK, vm.summary().period)

        vm.selectPeriod(ActivityPeriod.MONTH)

        assertEquals(ActivityPeriod.MONTH, vm.summary().period)
        assertEquals(30, vm.summary().chart.size)
    }

    @Test
    fun `una pausa nueva aparece en el historial`() = runTest {
        val vm = subscribed()

        val start = today.atTime(10, 0)
        breaks.sessions.value = listOf(BreakSession(start, start.plusHours(1), 60, 60, BreakStatus.VALID))

        assertEquals(listOf(today), vm.summary().days.map { it.day })
        assertEquals(60, vm.summary().minutes)
    }

    @Test
    fun `sin red marca la pantalla offline`() = runTest {
        network.isOnline.value = false

        val vm = subscribed()

        assertTrue(vm.state.value.isOffline)
    }
}
