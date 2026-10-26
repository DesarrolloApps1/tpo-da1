package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.AuthResult
import com.takeabreak.domain.repository.AuthRepository

class SignInWithGoogle(private val auth: AuthRepository) {
    suspend operator fun invoke(idToken: String): AuthResult = auth.signInWithGoogle(idToken)
}
