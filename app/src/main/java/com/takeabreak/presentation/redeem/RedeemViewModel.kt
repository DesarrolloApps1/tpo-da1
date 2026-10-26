package com.takeabreak.presentation.redeem

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.usecase.GetRedemptions
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.domain.usecase.RedeemReward
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.UiState
import com.takeabreak.presentation.toErrorCause
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
            _state.value = redeemReward(reward).fold(
                onSuccess = {
                    handle[REDEMPTION_ID] = it.id
                    ScreenState(UiState.Content(it))
                },
                onFailure = { ScreenState(UiState.Error(it.toErrorCause())) },
            )
        }
    }

    private fun restore(redemptionId: String) {
        viewModelScope.launch {
            getRedemptions().first().firstOrNull { it.id == redemptionId }?.let { _state.value = ScreenState(UiState.Content(it)) }
        }
    }

    companion object {
        private const val REDEMPTION_ID = "redemptionId"

        val Factory = viewModelFactory {
            initializer {
                appContainer.run { RedeemViewModel(createSavedStateHandle(), getRewards, redeemReward, getRedemptions) }
            }
        }
    }
}
