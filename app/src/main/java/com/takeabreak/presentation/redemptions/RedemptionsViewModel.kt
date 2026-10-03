package com.takeabreak.presentation.redemptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.usecase.GetRedemptions
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.toScreenState
import kotlinx.coroutines.flow.StateFlow

class RedemptionsViewModel(getRedemptions: GetRedemptions, network: NetworkMonitor) : ViewModel() {

    val state: StateFlow<ScreenState<List<Redemption>>> =
        getRedemptions().toScreenState(network.isOnline, viewModelScope) { it.isEmpty() }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer.run { RedemptionsViewModel(getRedemptions, networkMonitor) } }
        }
    }
}
