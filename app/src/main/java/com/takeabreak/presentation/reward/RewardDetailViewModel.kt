package com.takeabreak.presentation.reward

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.domain.usecase.SetGoal
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.toScreenState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RewardDetailViewModel(
    private val rewardId: String,
    getRewards: GetRewards,
    private val setGoal: SetGoal,
    network: NetworkMonitor,
) : ViewModel() {

    val state: StateFlow<ScreenState<Reward?>> = getRewards()
        .map { rewards -> rewards.firstOrNull { it.id == rewardId } }
        .toScreenState(network.isOnline, viewModelScope) { it == null }

    fun setAsGoal() {
        viewModelScope.launch { setGoal(rewardId) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val id = requireNotNull(createSavedStateHandle().get<String>("id"))
                appContainer.run { RewardDetailViewModel(id, getRewards, setGoal, networkMonitor) }
            }
        }
    }
}
