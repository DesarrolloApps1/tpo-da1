package com.takeabreak.presentation.explore

import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.presentation.ErrorCause
import com.takeabreak.presentation.UiState
import com.takeabreak.testing.FakeNetworkMonitor
import com.takeabreak.testing.FakeRewardRepository
import com.takeabreak.testing.MainDispatcherRule
import com.takeabreak.testing.reward
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ExploreViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val rewards = FakeRewardRepository()
    private val network = FakeNetworkMonitor()

    private fun TestScope.subscribed(): ExploreViewModel {
        val vm = ExploreViewModel(GetRewards(rewards), network)
        vm.state.launchIn(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))
        return vm
    }

    @Test
    fun `la primera vez sin red y sin catalogo muestra el error`() = runTest {
        rewards.refreshError = IOException()

        val vm = subscribed()

        assertEquals(UiState.Error(ErrorCause.NO_CONNECTION), vm.state.value.content)
    }

    @Test
    fun `sin red pero con catalogo guardado muestra lo guardado`() = runTest {
        rewards.rewards.value = listOf(reward("cafe"))
        rewards.refreshError = IOException()
        network.isOnline.value = false

        val vm = subscribed()

        assertEquals(UiState.Content(listOf(reward("cafe"))), vm.state.value.content)
        assertEquals(true, vm.state.value.isOffline)
    }

    @Test
    fun `reintentar con red saca el error`() = runTest {
        rewards.refreshError = IOException()
        val vm = subscribed()

        rewards.refreshError = null
        vm.refresh()

        assertEquals(UiState.Empty, vm.state.value.content)
        assertEquals(2, rewards.refreshCalls)
    }
}
