package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.Account
import com.takeabreak.domain.model.AuthResult
import com.takeabreak.domain.repository.AuthRepository

class SignInWithGoogle(private val auth: AuthRepository) {
    suspend operator fun invoke(idToken: String, account: Account? = null): AuthResult = auth.signInWithGoogle(idToken, account)
}
