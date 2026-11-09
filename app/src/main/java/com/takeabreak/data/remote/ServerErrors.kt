package com.takeabreak.data.remote

import com.takeabreak.domain.model.ServerException
import retrofit2.HttpException

// Las excepciones de Retrofit no salen de data/
internal suspend fun <T> withServerErrors(call: suspend () -> T): T = try {
    call()
} catch (e: HttpException) {
    throw ServerException(e.code())
}
