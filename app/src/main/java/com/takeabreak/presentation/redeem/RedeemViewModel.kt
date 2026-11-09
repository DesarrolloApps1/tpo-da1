package com.takeabreak.presentation.redeem

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.RedeemError
import com.takeabreak.domain.model.RedeemResult
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.usecase.GetRedemptions
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.domain.usecase.RedeemReward
import com.takeabreak.presentation.ErrorCause
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

// Empty = esperando que mantenga presionado; Content = código o canje pendiente
class RedeemViewModel(
    private val handle: SavedStateHandle,
    private val getRewards: GetRewards,
    private val redeemReward: RedeemReward,
    private val getRedemptions: GetRedemptions,
) : ViewModel() {

    private val rewardId: String = requireNotNull(handle["id"])

    private val _state = MutableStateFlow(ScreenState<Redemption>(UiState.Empty))
    val state: StateFlow<ScreenState<Redemption>> = _state.asStateFlow()

    init {
        // Tras process death vuelve a mostrar el código en vez de ofrecer canjear de nuevo
        handle.get<String>(REDEMPTION_ID)?.let(::restore)
    }

    fun confirm() {
        _state.value = ScreenState(UiState.Loading)
        viewModelScope.launch {
            val reward = getRewards().first().firstOrNull { it.id == rewardId }
            if (reward == null) {
                _state.value = ScreenState(UiState.Error())
                return@launch
            }
            _state.value = when (val result = redeemReward(attemptId(), reward)) {
                is RedeemResult.Success -> {
                    handle[REDEMPTION_ID] = result.redemption.id
                    ScreenState(UiState.Content(result.redemption))
                }
                is RedeemResult.Failure -> {
                    // Si falló la red, el servidor puede haberlo creado igual: el reintento repite el id.
                    // Si lo rechazó, no hay nada que repetir
                    if (result.error.toCause().canRetry.not()) handle.remove<String>(ATTEMPT_ID)
                    ScreenState(UiState.Error(result.error.toCause()))
                }
            }
        }
    }

    private fun attemptId(): String =
        handle.get<String>(ATTEMPT_ID) ?: UUID.randomUUID().toString().also { handle[ATTEMPT_ID] = it }

    private fun RedeemError.toCause(): ErrorCause = when (this) {
        RedeemError.NO_CONNECTION -> ErrorCause.NO_CONNECTION
        RedeemError.OUT_OF_STOCK -> ErrorCause.OUT_OF_STOCK
        RedeemError.INSUFFICIENT_BALANCE -> ErrorCause.INSUFFICIENT_BALANCE
        RedeemError.REWARD_UNAVAILABLE -> ErrorCause.REWARD_UNAVAILABLE
        RedeemError.UNKNOWN -> ErrorCause.UNKNOWN
    }

    private fun restore(redemptionId: String) {
        viewModelScope.launch {
            getRedemptions().first().firstOrNull { it.id == redemptionId }?.let { _state.value = ScreenState(UiState.Content(it)) }
        }
    }

    companion object {
        private const val REDEMPTION_ID = "redemptionId"
        private const val ATTEMPT_ID = "attemptId"

        val Factory = viewModelFactory {
            initializer {
                appContainer.run { RedeemViewModel(createSavedStateHandle(), getRewards, redeemReward, getRedemptions) }
            }
        }
    }
}
