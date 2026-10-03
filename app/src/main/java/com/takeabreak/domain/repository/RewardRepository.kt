package com.takeabreak.domain.repository

import com.takeabreak.domain.model.Reward
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun observeRewards(): Flow<List<Reward>>

    // Trae de la API lo que cambió y lo guarda en Room
    suspend fun refresh()
}
