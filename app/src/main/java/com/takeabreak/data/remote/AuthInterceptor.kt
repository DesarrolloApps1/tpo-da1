package com.takeabreak.data.remote

import com.takeabreak.data.local.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

// Agrega el token a cada request; si el backend lo rechaza, borra la sesión y la app vuelve al acceso
class AuthInterceptor(private val session: SessionStore) : Interceptor {

    // OkHttp corre en su propio hilo, por eso runBlocking no traba la UI
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { session.token.first() }
        val request = chain.request().let { original ->
            if (token == null) original else original.newBuilder().header("Authorization", "Bearer $token").build()
        }
        val response = chain.proceed(request)
        if (response.code == 401 && token != null) runBlocking { session.clear() }
        return response
    }
}
