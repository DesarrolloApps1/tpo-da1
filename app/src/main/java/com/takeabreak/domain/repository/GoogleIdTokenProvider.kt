package com.takeabreak.domain.repository

import com.takeabreak.domain.model.Account

// Pide el ID token de Google al sistema; lo implementa la capa de plataforma
fun interface GoogleIdTokenProvider {
    suspend fun requestIdToken(): GoogleIdTokenResult
}

sealed interface GoogleIdTokenResult {
    // account trae email, nombre y foto de la cuenta elegida, para Perfil
    data class Token(val idToken: String, val account: Account? = null) : GoogleIdTokenResult
    data object Cancelled : GoogleIdTokenResult
    data object Failed : GoogleIdTokenResult
}
