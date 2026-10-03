package com.takeabreak.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.model.RedemptionStatus

// La hora de inicio es la clave: guardar dos veces la misma pausa la reemplaza
@Entity(tableName = "break_sessions")
data class BreakSessionEntity(
    @PrimaryKey val startedAt: Long,
    val endedAt: Long,
    val validMinutes: Int,
    val points: Int,
    val status: BreakStatus,
)

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val neighborhood: String,
    val lat: Double,
    val lng: Double,
    val openingHours: String,
)

@Entity(
    tableName = "rewards",
    foreignKeys = [ForeignKey(entity = BusinessEntity::class, parentColumns = ["id"], childColumns = ["businessId"])],
    indices = [Index("businessId")],
)
data class RewardEntity(
    @PrimaryKey val id: String,
    val businessId: String,
    val title: String,
    val points: Int,
    val dailyStock: Int,
    val updatedAt: Long,
)

// Sin foreign key a rewards: el canje tiene que sobrevivir aunque el premio salga del catálogo
@Entity(tableName = "redemptions", indices = [Index("rewardId")])
data class RedemptionEntity(
    @PrimaryKey val id: String,
    val rewardId: String,
    val points: Int,
    val code: String?,
    val status: RedemptionStatus,
    val createdAt: Long,
    val expiresAt: Long?,
)

data class RewardWithBusiness(
    @Embedded val reward: RewardEntity,
    @Relation(parentColumn = "businessId", entityColumn = "id") val business: BusinessEntity,
)
