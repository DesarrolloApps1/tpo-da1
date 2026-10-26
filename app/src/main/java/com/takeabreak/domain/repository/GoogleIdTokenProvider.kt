package com.takeabreak.domain.repository

// Pide el ID token de Google al sistema; lo implementa la capa de plataforma
fun interface GoogleIdTokenProvider {
    suspend fun requestIdToken(): GoogleIdTokenResult
}

sealed interface GoogleIdTokenResult {
    data class Token(val idToken: String) : GoogleIdTokenResult
    data object Cancelled : GoogleIdTokenResult
    data object Failed : GoogleIdTokenResult
}
