package com.takeabreak.domain.model

import java.time.Instant

// El id es un UUID que genera el teléfono, así un reintento no duplica el canje
data class Redemption(
    val id: String,
    val rewardId: String,
    val points: Int,
    val code: String?,
    val status: RedemptionStatus,
    val createdAt: Instant,
    val expiresAt: Instant?,
)

enum class RedemptionStatus { PENDING, ACTIVE, USED, EXPIRED, REFUNDED }
