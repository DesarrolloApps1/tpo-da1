package com.takeabreak.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Los endpoints del backend del equipo. Los de canjes exigen el token (lo agrega AuthInterceptor)
interface TakeABreakApi {
    @POST("auth/google")
    suspend fun signInWithGoogle(@Body body: GoogleAuthRequest): SessionDto

    @POST("auth/email")
    suspend fun signInWithEmail(@Body body: EmailAuthRequest): SessionDto

    @GET("rewards")
    suspend fun rewards(@Query("since") since: Long?): CatalogDto

    @POST("redemptions")
    suspend fun createRedemption(@Body body: RedemptionRequest): RedemptionDto

    @GET("redemptions/{id}")
    suspend fun redemption(@Path("id") id: String): RedemptionDto
}
