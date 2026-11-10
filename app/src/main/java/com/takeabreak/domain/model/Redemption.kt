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
) {
    // Un código puede haber vencido sin que el servidor lo haya informado todavía
    fun statusAt(now: Instant): RedemptionStatus =
        if (status == RedemptionStatus.ACTIVE && expiresAt != null && !now.isBefore(expiresAt)) RedemptionStatus.EXPIRED
        else status
}

enum class RedemptionStatus { ACTIVE, USED, EXPIRED, REFUNDED }
