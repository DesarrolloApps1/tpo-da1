package com.takeabreak.domain.repository

import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.Reward
import kotlinx.coroutines.flow.Flow

interface RedemptionRepository {
    fun observeRedemptions(): Flow<List<Redemption>>

    // Puntos de los canjes que no fueron devueltos
    fun observeSpentPoints(): Flow<Int>

    suspend fun redeem(reward: Reward): Redemption
    suspend fun syncPending()
}
