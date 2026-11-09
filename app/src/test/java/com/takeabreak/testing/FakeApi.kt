package com.takeabreak.testing

import com.takeabreak.data.remote.BalanceDto
import com.takeabreak.data.remote.CatalogDto
import com.takeabreak.data.remote.DailyPointsDto
import com.takeabreak.data.remote.EmailAuthRequest
import com.takeabreak.data.remote.GoogleAuthRequest
import com.takeabreak.data.remote.RedemptionDto
import com.takeabreak.data.remote.RedemptionRequest
import com.takeabreak.data.remote.SessionDto
import com.takeabreak.data.remote.TakeABreakApi
import com.takeabreak.data.remote.UseRedemptionRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

fun httpError(code: Int, body: String = "{}") =
    HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

// Cada test define solo los endpoints que usa; el resto falla
open class FakeApi : TakeABreakApi {
    override suspend fun signInWithGoogle(body: GoogleAuthRequest): SessionDto = error("no usado")
    override suspend fun signInWithEmail(body: EmailAuthRequest): SessionDto = error("no usado")
    override suspend fun rewards(since: Long?): CatalogDto = error("no usado")
    override suspend fun createRedemption(body: RedemptionRequest): RedemptionDto = error("no usado")
    override suspend fun redemption(id: String): RedemptionDto = error("no usado")
    override suspend fun useRedemption(id: String, body: UseRedemptionRequest): RedemptionDto = error("no usado")
    override suspend fun reportDailyPoints(days: List<DailyPointsDto>): BalanceDto = error("no usado")
}
