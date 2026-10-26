package com.takeabreak.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.AppPermission
import com.takeabreak.domain.model.Profile
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.repository.PermissionChecker
import com.takeabreak.domain.usecase.GetProfile
import com.takeabreak.domain.usecase.SetGoal
import com.takeabreak.domain.usecase.SetNotificationsEnabled
import com.takeabreak.domain.usecase.SignOut
import com.takeabreak.presentation.ScreenState
import com.takeabreak.presentation.toScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Las preferencias van a DataStore y vuelven por el Flow de GetProfile, así no hay estado duplicado
class ProfileViewModel(
    getProfile: GetProfile,
    private val setGoal: SetGoal,
    private val setNotificationsEnabled: SetNotificationsEnabled,
    private val signOut: SignOut,
    private val permissionChecker: PermissionChecker,
    network: NetworkMonitor,
) : ViewModel() {

    val state: StateFlow<ScreenState<Profile>> = getProfile().toScreenState(network.isOnline, viewModelScope)

    private val _permissions = MutableStateFlow(checkPermissions())
    val permissions: StateFlow<Map<AppPermission, Boolean>> = _permissions.asStateFlow()

    // Se llama al volver a la pantalla, por si el usuario cambió algo en Ajustes
    fun refreshPermissions() {
        _permissions.value = checkPermissions()
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { setNotificationsEnabled(enabled) }
    }

    fun chooseGoal(rewardId: String) {
        viewModelScope.launch { setGoal(rewardId) }
    }

    fun chooseDailyGoal(minutes: Int) {
        viewModelScope.launch { setGoal.dailyGoal(minutes) }
    }

    // La navegación vuelve sola al acceso porque observa la sesión
    fun signOut() {
        viewModelScope.launch { signOut.invoke() }
    }

    private fun checkPermissions() = AppPermission.entries.associateWith(permissionChecker::isGranted)

    companion object {
        // Opciones de meta diaria en minutos, hasta el tope de 240 puntos por día
        val DAILY_GOAL_OPTIONS = listOf(30, 60, 90, 120, 180, 240)

        val Factory = viewModelFactory {
            initializer {
                appContainer.run {
                    ProfileViewModel(getProfile, setGoal, setNotificationsEnabled, signOut, permissionChecker, networkMonitor)
                }
            }
        }
    }
}
