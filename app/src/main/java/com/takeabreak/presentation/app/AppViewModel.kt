package com.takeabreak.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.Account
import com.takeabreak.domain.model.AppPermission
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.repository.PermissionChecker
import com.takeabreak.domain.usecase.GetAccount
import com.takeabreak.domain.usecase.ObserveSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// isSignedIn y onboardingDone son null mientras se leen: la app muestra el splash
data class AppState(
    val isSignedIn: Boolean? = null,
    val account: Account? = null,
    val isOnline: Boolean = true,
    val onboardingDone: Boolean? = null,
)

// Estado global que usa el scaffold: sesión, avatar y aviso offline
class AppViewModel(
    observeSession: ObserveSession,
    getAccount: GetAccount,
    network: NetworkMonitor,
    private val permissionChecker: PermissionChecker,
    syncPoints: () -> Unit = {},
    onboardingDone: Flow<Boolean> = flowOf(true),
    private val markOnboardingDone: suspend () -> Unit = {},
) : ViewModel() {

    val state: StateFlow<AppState> = combine(observeSession(), getAccount(), network.isOnline, onboardingDone, ::AppState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState())

    // Al abrir la app y justo después de entrar, que es cuando ya hay token para informar los puntos
    init {
        observeSession().filter { it }.onEach { syncPoints() }.launchIn(viewModelScope)
    }

    // Sin acceso a uso no se pueden contar pausas: después de entrar se pasa por Permisos
    fun needsPermissions(): Boolean = !permissionChecker.isGranted(AppPermission.USAGE_ACCESS)

    // El onboarding se muestra una sola vez
    fun finishOnboarding() {
        viewModelScope.launch { markOnboardingDone() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                appContainer.run {
                    AppViewModel(
                        observeSession, getAccount, networkMonitor, permissionChecker, ::schedulePointsSync,
                        settings.onboardingDone, settings::setOnboardingDone,
                    )
                }
            }
        }
    }
}
