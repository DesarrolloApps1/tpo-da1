package com.takeabreak.presentation.redeem

import androidx.lifecycle.SavedStateHandle
import com.takeabreak.domain.usecase.GetRedemptions
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.domain.usecase.RedeemReward
import com.takeabreak.presentation.ErrorCause
import com.takeabreak.presentation.UiState
import com.takeabreak.testing.FakeRedemptionRepository
import com.takeabreak.testing.FakeRewardRepository
import com.takeabreak.testing.MainDispatcherRule
import com.takeabreak.testing.redemption
import com.takeabreak.testing.reward
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RedeemViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val rewards = FakeRewardRepository().apply { rewards.value = listOf(reward("cafe")) }
    private val redemptions = FakeRedemptionRepository()

    private fun viewModel(handle: SavedStateHandle) =
        RedeemViewModel(handle, GetRewards(rewards), RedeemReward(redemptions), GetRedemptions(redemptions))

    @Test
    fun `tras process death vuelve a mostrar el codigo sin canjear de nuevo`() = runTest {
        val handle = SavedStateHandle(mapOf("id" to "cafe"))
        redemptions.nextResult = redemption("a")
        viewModel(handle).confirm()

        // El sistema recrea el ViewModel con el mismo SavedStateHandle
        val restored = viewModel(SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }))

        assertEquals(UiState.Content(redemption("a")), restored.state.value.content)
        assertEquals(1, redemptions.redemptions.value.size)
    }

    @Test
    fun `si el canje falla muestra el error unificado`() = runTest {
        val vm = viewModel(SavedStateHandle(mapOf("id" to "cafe")))

        vm.confirm()

        assertEquals(UiState.Error(ErrorCause.UNKNOWN), vm.state.value.content)
    }

    @Test
    fun `sin canje previo espera la confirmacion`() = runTest {
        assertEquals(UiState.Empty, viewModel(SavedStateHandle(mapOf("id" to "cafe"))).state.value.content)
    }
}
