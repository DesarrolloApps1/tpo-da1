package com.takeabreak.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.toScreenState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// TODO: ordenar por distancia con LocationProvider y filtro "me alcanza" (RF03)
class ExploreViewModel(private val getRewards: GetRewards, network: NetworkMonitor) : ViewModel() {

    val state: StateFlow<ScreenState<List<Reward>>> =
        getRewards().toScreenState(network.isOnline, viewModelScope) { it.isEmpty() }

    init {
        refresh()
    }

    // Si falla, se sigue mostrando lo guardado en Room
    fun refresh() {
        viewModelScope.launch { runCatching { getRewards.refresh() } }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer.run { ExploreViewModel(getRewards, networkMonitor) } }
        }
    }
}
