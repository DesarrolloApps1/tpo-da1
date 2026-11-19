package com.takeabreak.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.Coordinates
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.usecase.GetRewards
import com.takeabreak.domain.usecase.OrderRewards
import com.takeabreak.domain.usecase.RewardListItem
import com.takeabreak.platform.location.LocationProvider
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.UiState
import com.takeabreak.presentation.toErrorCause
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

// Lo que dibuja Explorar sobre el catálogo: la lista ordenada, el mapa y el filtro
data class ExploreListing(
    val items: List<RewardListItem> = emptyList(),
    val here: Coordinates? = null,
    val onlyAffordable: Boolean = false,
)

// Sin proveedor (tests de solo catálogo): nunca hay ubicación
private object NoLocation : LocationProvider {
    override suspend fun current(): Coordinates? = null
}

class ExploreViewModel(
    private val getRewards: GetRewards,
    network: NetworkMonitor,
    private val location: LocationProvider = NoLocation,
    // Saldo que se puede gastar, para el filtro "Me alcanza"
    spendable: Flow<Int> = flowOf(0),
) : ViewModel() {

    // Lo que se muestra mientras no haya nada guardado: cargando, vacío o el error con Reintentar
    private val withoutCatalog = MutableStateFlow<UiState<Nothing>>(UiState.Loading)
    private val here = MutableStateFlow<Coordinates?>(null)
    private val onlyAffordable = MutableStateFlow(false)

    val state: StateFlow<ScreenState<List<Reward>>> =
        combine(getRewards(), network.isOnline, withoutCatalog) { rewards, online, fallback ->
            ScreenState(if (rewards.isEmpty()) fallback else UiState.Content(rewards), isOffline = !online)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreenState())

    // Por distancia si hay ubicación, de la A a la Z si no (RF03)
    val listing: StateFlow<ExploreListing> =
        combine(getRewards(), here, spendable, onlyAffordable) { rewards, from, balance, filter ->
            ExploreListing(OrderRewards(rewards, from, balance, filter), from, filter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreListing())

    val lastSync: StateFlow<Instant?> =
        getRewards.lastSync.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        update { getRewards.refreshIfStale() }
        locate()
    }

    fun refresh() = update { getRewards.refresh() }

    // Una sola lectura, en primer plano. Sin permiso queda null y la lista sigue de la A a la Z
    fun locate() {
        viewModelScope.launch { here.value = location.current() }
    }

    fun toggleAffordable() {
        onlyAffordable.value = !onlyAffordable.value
    }

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
            initializer {
                appContainer.run {
                    ExploreViewModel(getRewards, networkMonitor, locationProvider, getHomeSummary().map { it.spendable })
                }
            }
        }
    }
}
