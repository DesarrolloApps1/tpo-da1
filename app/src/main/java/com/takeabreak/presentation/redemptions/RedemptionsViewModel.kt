package com.takeabreak.presentation.redemptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.usecase.GetRedemptions
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.toScreenState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// title es null si el premio ya salió del catálogo
data class RedemptionRow(val redemption: Redemption, val title: String?)

class RedemptionsViewModel(
    getRedemptions: GetRedemptions,
    getRewards: GetRewards,
    network: NetworkMonitor,
) : ViewModel() {

    // Los activos arriba, que son los que se pueden mostrar en el comercio
    val state: StateFlow<ScreenState<List<RedemptionRow>>> =
        combine(getRedemptions(), getRewards()) { redemptions, rewards ->
            val titles = rewards.associate { it.id to it.title }
            redemptions
                .sortedByDescending { it.status == RedemptionStatus.ACTIVE }
                .map { RedemptionRow(it, titles[it.rewardId]) }
        }.toScreenState(network.isOnline, viewModelScope) { it.isEmpty() }

    // Al abrir la pantalla y cada vez que vuelve la red
    init {
        network.isOnline.filter { it }.onEach { getRedemptions.refreshActive() }.launchIn(viewModelScope)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer.run { RedemptionsViewModel(getRedemptions, getRewards, networkMonitor) } }
        }
    }
}
