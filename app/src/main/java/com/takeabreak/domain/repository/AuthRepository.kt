package com.takeabreak.domain.repository

import com.takeabreak.domain.model.AuthResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    // true mientras haya un token de sesión guardado
    val isSignedIn: Flow<Boolean>

    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun signInWithEmail(email: String, password: String, register: Boolean): AuthResult
    suspend fun signOut()
}
