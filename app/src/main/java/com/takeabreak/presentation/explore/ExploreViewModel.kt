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
import com.takeabreak.presentation.UiState
import com.takeabreak.presentation.toErrorCause
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

// TODO: ordenar por distancia con LocationProvider y filtro "me alcanza" (RF03)
class ExploreViewModel(private val getRewards: GetRewards, network: NetworkMonitor) : ViewModel() {

    // Lo que se muestra mientras no haya nada guardado: cargando, vacío o el error con Reintentar
    private val withoutCatalog = MutableStateFlow<UiState<Nothing>>(UiState.Loading)

    val state: StateFlow<ScreenState<List<Reward>>> =
        combine(getRewards(), network.isOnline, withoutCatalog) { rewards, online, fallback ->
            ScreenState(if (rewards.isEmpty()) fallback else UiState.Content(rewards), isOffline = !online)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreenState())

    val lastSync: StateFlow<Instant?> =
        getRewards.lastSync.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        update { getRewards.refreshIfStale() }
    }

    fun refresh() = update { getRewards.refresh() }

    // Si falla y ya hay catálogo, se sigue mostrando lo guardado en Room
    private fun update(call: suspend () -> Unit) {
        withoutCatalog.value = UiState.Loading
        viewModelScope.launch {
            withoutCatalog.value = runCatching { call() }.fold(
                onSuccess = { UiState.Empty },
                onFailure = { UiState.Error(it.toErrorCause()) },
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer.run { ExploreViewModel(getRewards, networkMonitor) } }
        }
    }
}
