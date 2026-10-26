package com.takeabreak.domain.usecase

import com.takeabreak.domain.repository.AuthRepository

// Borra el token y los datos de la cuenta. Las pausas y los canjes locales quedan: el saldo sale de restarlos
class SignOut(private val auth: AuthRepository) {
    suspend operator fun invoke() = auth.signOut()
}
