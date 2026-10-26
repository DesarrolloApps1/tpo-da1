package com.takeabreak.domain.usecase

import com.takeabreak.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

// Emite false al cerrar sesión o cuando el backend rechaza el token
class ObserveSession(private val auth: AuthRepository) {
    operator fun invoke(): Flow<Boolean> = auth.isSignedIn.distinctUntilChanged()
}
