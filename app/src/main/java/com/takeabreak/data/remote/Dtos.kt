package com.takeabreak.data.remote

import kotlinx.serialization.Serializable

// Mismos nombres de campo que en el backend. Las fechas viajan en epoch millis

@Serializable
data class GoogleAuthRequest(val idToken: String)

@Serializable
data class SessionDto(val token: String)

@Serializable
data class CatalogDto(val businesses: List<BusinessDto>, val rewards: List<RewardDto>)

@Serializable
data class BusinessDto(
    val id: String,
    val name: String,
    val address: String,
    val neighborhood: String,
    val lat: Double,
    val lng: Double,
    val openingHours: String,
)

@Serializable
data class RewardDto(
    val id: String,
    val businessId: String,
    val title: String,
    val points: Int,
    val dailyStock: Int,
    val updatedAt: Long,
)

@Serializable
data class RedemptionRequest(val id: String, val rewardId: String, val points: Int)

// status: ACTIVE, USED, EXPIRED o REJECTED
@Serializable
data class RedemptionDto(
    val id: String,
    val rewardId: String,
    val points: Int,
    val code: String?,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long?,
)
