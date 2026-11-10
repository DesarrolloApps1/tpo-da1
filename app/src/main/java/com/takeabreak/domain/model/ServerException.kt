package com.takeabreak.domain.model

// El servidor respondió con un error. data/ traduce a esto las excepciones de Retrofit
class ServerException(val code: Int) : Exception("El servidor respondió $code") {
    // Un error 5xx puede pasar solo; uno 4xx va a volver a fallar igual
    val isRetryable: Boolean get() = code >= 500
}
