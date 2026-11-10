package com.takeabreak.presentation.redemptions

import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.domain.usecase.GetRedemptions
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.presentation.UiState
import com.takeabreak.testing.FakeNetworkMonitor
import com.takeabreak.testing.FakeRedemptionRepository
import com.takeabreak.testing.FakeRewardRepository
import com.takeabreak.testing.MainDispatcherRule
import com.takeabreak.testing.redemption
import com.takeabreak.testing.reward
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RedemptionsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val redemptions = FakeRedemptionRepository()
    private val rewards = FakeRewardRepository()
    private val network = FakeNetworkMonitor()

    private fun viewModel() = RedemptionsViewModel(GetRedemptions(redemptions), GetRewards(rewards), network)

    @Test
    fun `pone los activos arriba y le pone titulo a cada canje`() = runTest {
        rewards.rewards.value = listOf(reward("a"))
        val used = redemption("u", status = RedemptionStatus.USED)
        val active = redemption("a").copy(rewardId = "a")
        redemptions.redemptions.value = listOf(used, active)
        val vm = viewModel()
        vm.state.launchIn(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)))

        assertEquals(
            UiState.Content(listOf(RedemptionRow(active, "Premio a"), RedemptionRow(used, null))),
            vm.state.value.content,
        )
    }

    @Test
    fun `consulta los activos al abrir y cuando vuelve la red, no sin red`() = runTest {
        network.isOnline.value = false
        viewModel()
        assertEquals(0, redemptions.refreshCalls)

        network.isOnline.value = true

        assertEquals(1, redemptions.refreshCalls)
    }
}
