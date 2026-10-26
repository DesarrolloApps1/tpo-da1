package com.takeabreak.testing

import com.takeabreak.domain.model.AuthResult
import com.takeabreak.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// Repositorio en memoria: guarda los pedidos y responde lo que diga el test
class FakeAuthRepository : AuthRepository {

    val token = MutableStateFlow<String?>(null)
    var result: AuthResult = AuthResult.Success
    val emailCalls = mutableListOf<Triple<String, String, Boolean>>()
    val googleTokens = mutableListOf<String>()

    // Si no es null, el acceso queda suspendido hasta completarlo (para ver el estado de carga)
    var gate: CompletableDeferred<Unit>? = null

    override val isSignedIn: Flow<Boolean> = token.map { it != null }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        googleTokens += idToken
        return respond()
    }

    override suspend fun signInWithEmail(email: String, password: String, register: Boolean): AuthResult {
        emailCalls += Triple(email, password, register)
        return respond()
    }

    override suspend fun signOut() {
        token.value = null
    }

    private suspend fun respond(): AuthResult {
        gate?.await()
        if (result == AuthResult.Success) token.value = "session-token"
        return result
    }
}
