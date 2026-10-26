package com.takeabreak.presentation.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.AuthError
import com.takeabreak.domain.model.AuthResult
import com.takeabreak.domain.repository.GoogleIdTokenProvider
import com.takeabreak.domain.repository.GoogleIdTokenResult
import com.takeabreak.domain.usecase.SignInWithEmail
import com.takeabreak.domain.usecase.SignInWithGoogle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Formulario con carga y error en línea, por eso no usa UiState. Al entrar, la navegación reacciona a ObserveSession
data class AccessState(
    val googleEnabled: Boolean,
    val showEmailForm: Boolean = false,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
)

class AccessViewModel(
    private val signInWithGoogle: SignInWithGoogle,
    private val signInWithEmail: SignInWithEmail,
    googleEnabled: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(AccessState(googleEnabled = googleEnabled))
    val state: StateFlow<AccessState> = _state.asStateFlow()

    fun onEmailChange(email: String) = _state.update { it.copy(email = email, error = null) }

    fun onPasswordChange(password: String) = _state.update { it.copy(password = password, error = null) }

    fun showEmailForm() = _state.update { it.copy(showEmailForm = true, error = null) }

    fun hideEmailForm() = _state.update { it.copy(showEmailForm = false, error = null) }

    // El proveedor llega desde la pantalla porque necesita la Activity
    fun continueWithGoogle(google: GoogleIdTokenProvider) {
        if (!_state.value.googleEnabled) return
        attempt {
            when (val result = google.requestIdToken()) {
                is GoogleIdTokenResult.Token -> signInWithGoogle(result.idToken, result.account)
                GoogleIdTokenResult.Cancelled -> null
                GoogleIdTokenResult.Failed -> AuthResult.Failure(AuthError.GOOGLE_FAILED)
            }
        }
    }

    fun submitEmail(register: Boolean) {
        val (email, password) = _state.value.let { it.email to it.password }
        attempt { signInWithEmail(email, password, register) }
    }

    // Ignora toques mientras hay un intento en curso; null = el usuario canceló
    private fun attempt(block: suspend () -> AuthResult?) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = block()
            _state.update {
                it.copy(
                    isLoading = false,
                    error = (result as? AuthResult.Failure)?.error,
                    password = if (result == AuthResult.Success) "" else it.password,
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { appContainer.run { AccessViewModel(signInWithGoogle, signInWithEmail, isGoogleSignInEnabled) } }
        }
    }
}
