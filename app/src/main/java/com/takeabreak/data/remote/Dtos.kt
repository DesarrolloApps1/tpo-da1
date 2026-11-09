package com.takeabreak.data.remote

import kotlinx.serialization.Serializable

// Mismos nombres de campo que en el backend. Las fechas viajan en epoch millis

@Serializable
data class GoogleAuthRequest(val idToken: String)

// register = true crea la cuenta; false ingresa con una existente
@Serializable
data class EmailAuthRequest(val email: String, val password: String, val register: Boolean)

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
    // true si se dio de baja: hay que borrarlo de Room
    val deleted: Boolean = false,
)

@Serializable
data class RedemptionRequest(val id: String, val rewardId: String, val points: Int)

// Total de puntos de un día (yyyy-MM-dd). Nunca se mandan pausas ni horarios
@Serializable
data class DailyPointsDto(val day: String, val points: Int)

@Serializable
data class BalanceDto(val balance: Int)

@Serializable
data class UseRedemptionRequest(val pin: String)

// Cuerpo de las respuestas de error del backend
@Serializable
data class ErrorDto(val error: String, val attemptsLeft: Int? = null)

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
