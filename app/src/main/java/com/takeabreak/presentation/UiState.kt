package com.takeabreak.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

// Los estados que dibuja cada pantalla
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val cause: ErrorCause = ErrorCause.UNKNOWN) : UiState<Nothing>
}

// Criterio único de errores (consigna 7.3): según qué pasó, la UI dice qué puede hacer el usuario
enum class ErrorCause {
    NO_CONNECTION, // Reintentar o seguir sin conexión
    MISSING_PERMISSION, // Ir a Ajustes
    UNKNOWN, // Reintentar
}

fun Throwable.toErrorCause(): ErrorCause = when (this) {
    is IOException -> ErrorCause.NO_CONNECTION
    is SecurityException -> ErrorCause.MISSING_PERMISSION
    else -> ErrorCause.UNKNOWN
}

// El estado más la marca de offline, que puede aparecer junto a cualquiera de los otros. El aviso lo dibuja el scaffold
data class ScreenState<out T>(
    val content: UiState<T> = UiState.Loading,
    val isOffline: Boolean = false,
)

// Arma el estado de pantalla a partir de los datos locales y la conexión
fun <T> Flow<T>.toScreenState(
    isOnline: Flow<Boolean>,
    scope: CoroutineScope,
    isEmpty: (T) -> Boolean = { false },
): StateFlow<ScreenState<T>> =
    combine(this, isOnline) { data, online ->
        ScreenState(if (isEmpty(data)) UiState.Empty else UiState.Content(data), isOffline = !online)
    }
        .catch { emit(ScreenState(UiState.Error(it.toErrorCause()))) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), ScreenState())
