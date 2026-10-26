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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// isSignedIn es null mientras se lee el token guardado: la app muestra el splash
data class AppState(
    val isSignedIn: Boolean? = null,
    val account: Account? = null,
    val isOnline: Boolean = true,
)

// Estado global que usa el scaffold: sesión, avatar y aviso offline
class AppViewModel(
    observeSession: ObserveSession,
    getAccount: GetAccount,
    network: NetworkMonitor,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    val state: StateFlow<AppState> = combine(observeSession(), getAccount(), network.isOnline, ::AppState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState())

    // Sin acceso a uso no se pueden contar pausas: después de entrar se pasa por Permisos
    fun needsPermissions(): Boolean = !permissionChecker.isGranted(AppPermission.USAGE_ACCESS)

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer.run { AppViewModel(observeSession, getAccount, networkMonitor, permissionChecker) } }
        }
    }
}
