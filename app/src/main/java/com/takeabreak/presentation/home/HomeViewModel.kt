package com.takeabreak.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.HomeSummary
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.usecase.GetHomeSummary
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.toScreenState
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(getHomeSummary: GetHomeSummary, network: NetworkMonitor) : ViewModel() {

    // Vacío mientras no haya puntos todavía (frame 10)
    val state: StateFlow<ScreenState<HomeSummary>> =
        getHomeSummary().toScreenState(network.isOnline, viewModelScope) { it.balance == 0 && it.pointsToday == 0 }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer.run { HomeViewModel(getHomeSummary, networkMonitor) } }
        }
    }
}
