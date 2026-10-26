package com.takeabreak.domain.model

// Resultado de un intento de acceso; la UI elige el mensaje según el error
sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

enum class AuthError {
    INVALID_EMAIL,
    SHORT_PASSWORD,
    EMAIL_TAKEN,
    WRONG_CREDENTIALS,
    GOOGLE_FAILED,
    NO_CONNECTION,
    UNKNOWN,
}
