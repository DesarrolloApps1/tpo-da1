package com.takeabreak.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.takeabreak.domain.model.RedemptionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BreakDao {
    @Query("SELECT * FROM break_sessions WHERE startedAt >= :from ORDER BY startedAt DESC")
    fun observeFrom(from: Long): Flow<List<BreakSessionEntity>>

    @Query("SELECT COALESCE(SUM(points), 0) FROM break_sessions")
    fun observeTotalPoints(): Flow<Int>

    @Query("SELECT COALESCE(SUM(points), 0) FROM break_sessions WHERE startedAt >= :from AND startedAt < :to")
    suspend fun pointsBetween(from: Long, to: Long): Int

    @Upsert
    suspend fun upsert(sessions: List<BreakSessionEntity>)
}

@Dao
abstract class CatalogDao {
    @Transaction
    @Query("SELECT * FROM rewards ORDER BY title")
    abstract fun observeRewards(): Flow<List<RewardWithBusiness>>

    @Query("SELECT MAX(updatedAt) FROM rewards")
    abstract suspend fun lastUpdate(): Long?

    @Upsert
    protected abstract suspend fun upsertBusinesses(businesses: List<BusinessEntity>)

    @Upsert
    protected abstract suspend fun upsertRewards(rewards: List<RewardEntity>)

    @Query("DELETE FROM rewards WHERE id IN (:ids)")
    protected abstract suspend fun deleteRewards(ids: List<String>)

    // Altas, cambios y bajas juntos. Comercios primero por la foreign key de los premios
    @Transaction
    open suspend fun applyCatalog(businesses: List<BusinessEntity>, rewards: List<RewardEntity>, deletedIds: List<String>) {
        upsertBusinesses(businesses)
        upsertRewards(rewards)
        deleteRewards(deletedIds)
    }
}

@Dao
interface RedemptionDao {
    @Query("SELECT * FROM redemptions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RedemptionEntity>>

    // El servidor devuelve los puntos de los canjes rechazados y de los vencidos
    @Query("SELECT COALESCE(SUM(points), 0) FROM redemptions WHERE status NOT IN ('REFUNDED', 'EXPIRED')")
    fun observeSpentPoints(): Flow<Int>

    @Query("SELECT * FROM redemptions WHERE status = 'ACTIVE'")
    suspend fun active(): List<RedemptionEntity>

    @Query("UPDATE redemptions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: RedemptionStatus)

    @Upsert
    suspend fun upsert(redemption: RedemptionEntity)
}
