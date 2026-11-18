package com.takeabreak.presentation.reward

import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.domain.usecase.SetGoal
import com.takeabreak.presentation.UiState
import com.takeabreak.testing.FakeNetworkMonitor
import com.takeabreak.testing.FakeRewardRepository
import com.takeabreak.testing.FakeSettingsRepository
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

class RewardDetailViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val rewards = FakeRewardRepository()
    private val settings = FakeSettingsRepository()

    private fun TestScope.subscribed(id: String): RewardDetailViewModel {
        val vm = RewardDetailViewModel(id, GetRewards(rewards), SetGoal(settings), FakeNetworkMonitor())
        vm.state.launchIn(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))
        return vm
    }

    @Test
    fun `muestra el premio pedido`() = runTest {
        rewards.rewards.value = listOf(reward("cafe"), reward("yoga"))

        val vm = subscribed("yoga")

        assertEquals(UiState.Content(reward("yoga")), vm.state.value.content)
    }

    @Test
    fun `un premio dado de baja muestra el estado vacio`() = runTest {
        rewards.rewards.value = listOf(reward("cafe"))

        val vm = subscribed("yoga")

        assertEquals(UiState.Empty, vm.state.value.content)
    }

    @Test
    fun `fijar como meta guarda el premio`() = runTest {
        rewards.rewards.value = listOf(reward("yoga"))
        val vm = subscribed("yoga")

        vm.setAsGoal()
        testScheduler.advanceUntilIdle()

        assertEquals("yoga", settings.goalRewardId.value)
    }
}
