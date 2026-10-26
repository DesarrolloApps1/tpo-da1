package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.AuthError
import com.takeabreak.domain.model.AuthResult
import com.takeabreak.domain.repository.AuthRepository

// Valida antes de ir a la red, con las mismas reglas que el backend
class SignInWithEmail(private val auth: AuthRepository) {

    suspend operator fun invoke(email: String, password: String, register: Boolean): AuthResult {
        val normalized = email.trim().lowercase()
        return when {
            !EMAIL_REGEX.matches(normalized) -> AuthResult.Failure(AuthError.INVALID_EMAIL)
            password.length < MIN_PASSWORD_LENGTH -> AuthResult.Failure(AuthError.SHORT_PASSWORD)
            else -> auth.signInWithEmail(normalized, password, register)
        }
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        private val EMAIL_REGEX = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
    }
}
