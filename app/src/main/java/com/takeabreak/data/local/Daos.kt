package com.takeabreak.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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

    // Comercios primero por la foreign key de los premios
    @Transaction
    open suspend fun upsertCatalog(businesses: List<BusinessEntity>, rewards: List<RewardEntity>) {
        upsertBusinesses(businesses)
        upsertRewards(rewards)
    }
}

@Dao
interface RedemptionDao {
    @Query("SELECT * FROM redemptions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RedemptionEntity>>

    @Query("SELECT COALESCE(SUM(points), 0) FROM redemptions WHERE status != 'REFUNDED'")
    fun observeSpentPoints(): Flow<Int>

    @Query("SELECT * FROM redemptions WHERE status = 'PENDING'")
    suspend fun pending(): List<RedemptionEntity>

    @Upsert
    suspend fun upsert(redemption: RedemptionEntity)
}
