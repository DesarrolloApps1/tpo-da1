package com.takeabreak.domain.usecase

import com.takeabreak.domain.repository.AuthRepository

// Borra el token; las pausas y el historial locales quedan
class SignOut(private val auth: AuthRepository) {
    suspend operator fun invoke() = auth.signOut()
}
