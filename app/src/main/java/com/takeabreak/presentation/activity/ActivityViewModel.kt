package com.takeabreak.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.usecase.GetBreakHistory
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.toScreenState
import kotlinx.coroutines.flow.StateFlow

class ActivityViewModel(getBreakHistory: GetBreakHistory, network: NetworkMonitor) : ViewModel() {

    val state: StateFlow<ScreenState<List<BreakSession>>> =
        getBreakHistory().toScreenState(network.isOnline, viewModelScope) { it.isEmpty() }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer.run { ActivityViewModel(getBreakHistory, networkMonitor) } }
        }
    }
}
