package com.takeabreak.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.ActivityPeriod
import com.takeabreak.domain.model.ActivitySummary
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.rules.BreakRulesConfig
import com.takeabreak.domain.usecase.GetBreakHistory
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.toScreenState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest

class ActivityViewModel(
    getBreakHistory: GetBreakHistory,
    network: NetworkMonitor,
    // Para que los textos de "Cómo sumás" y de cada motivo usen los mismos valores que las reglas
    val rules: BreakRulesConfig,
) : ViewModel() {

    private val period = MutableStateFlow(ActivityPeriod.WEEK)

    // Nunca Empty: sin pausas, la pantalla dibuja su propio estado vacío (Figma 24) con el gráfico en cero
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ScreenState<ActivitySummary>> =
        period.flatMapLatest { getBreakHistory(it) }.toScreenState(network.isOnline, viewModelScope)

    fun selectPeriod(value: ActivityPeriod) {
        period.value = value
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer.run { ActivityViewModel(getBreakHistory, networkMonitor, breakRules.config) } }
        }
    }
}
