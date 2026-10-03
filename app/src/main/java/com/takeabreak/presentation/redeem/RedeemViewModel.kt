package com.takeabreak.presentation.redeem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.domain.usecase.RedeemReward
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Empty = esperando que mantenga presionado; Content = código o canje pendiente
class RedeemViewModel(
    private val rewardId: String,
    private val getRewards: GetRewards,
    private val redeemReward: RedeemReward,
) : ViewModel() {

    private val _state = MutableStateFlow(ScreenState<Redemption>(UiState.Empty))
    val state: StateFlow<ScreenState<Redemption>> = _state.asStateFlow()

    fun confirm() {
        _state.value = ScreenState(UiState.Loading)
        viewModelScope.launch {
            val reward = getRewards().first().first { it.id == rewardId }
            _state.value = redeemReward(reward).fold(
                onSuccess = { ScreenState(UiState.Content(it)) },
                onFailure = { ScreenState(UiState.Error(it.message)) },
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val id = requireNotNull(createSavedStateHandle().get<String>("id"))
                appContainer.run { RedeemViewModel(id, getRewards, redeemReward) }
            }
        }
    }
}
