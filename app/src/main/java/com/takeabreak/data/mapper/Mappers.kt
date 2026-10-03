package com.takeabreak.data.mapper

import com.takeabreak.data.local.BreakSessionEntity
import com.takeabreak.data.local.BusinessEntity
import com.takeabreak.data.local.RedemptionEntity
import com.takeabreak.data.local.RewardEntity
import com.takeabreak.data.local.RewardWithBusiness
import com.takeabreak.data.remote.BusinessDto
import com.takeabreak.data.remote.RedemptionDto
import com.takeabreak.data.remote.RewardDto
import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.Business
import com.takeabreak.domain.model.Coordinates
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.domain.model.Reward
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

fun BreakSessionEntity.toDomain() = BreakSession(
    start = startedAt.toLocalDateTime(),
    end = endedAt.toLocalDateTime(),
    validMinutes = validMinutes,
    points = points,
    status = status,
)

fun BreakSession.toEntity() = BreakSessionEntity(
    startedAt = start.toEpochMillis(),
    endedAt = end.toEpochMillis(),
    validMinutes = validMinutes,
    points = points,
    status = status,
)

fun BusinessEntity.toDomain() = Business(id, name, address, neighborhood, Coordinates(lat, lng), openingHours)

fun RewardWithBusiness.toDomain() = Reward(
    id = reward.id,
    business = business.toDomain(),
    title = reward.title,
    points = reward.points,
    dailyStock = reward.dailyStock,
)

fun RedemptionEntity.toDomain() = Redemption(
    id = id,
    rewardId = rewardId,
    points = points,
    code = code,
    status = status,
    createdAt = Instant.ofEpochMilli(createdAt),
    expiresAt = expiresAt?.let(Instant::ofEpochMilli),
)

fun BusinessDto.toEntity() = BusinessEntity(id, name, address, neighborhood, lat, lng, openingHours)

fun RewardDto.toEntity() = RewardEntity(id, businessId, title, points, dailyStock, updatedAt)

// Si el servidor rechaza el canje, queda devuelto y los puntos vuelven al saldo
fun RedemptionDto.toEntity() = RedemptionEntity(
    id = id,
    rewardId = rewardId,
    points = points,
    code = code,
    status = when (status) {
        "ACTIVE" -> RedemptionStatus.ACTIVE
        "USED" -> RedemptionStatus.USED
        "EXPIRED" -> RedemptionStatus.EXPIRED
        else -> RedemptionStatus.REFUNDED
    },
    createdAt = createdAt,
    expiresAt = expiresAt,
)
