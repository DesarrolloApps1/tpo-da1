package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.RewardRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class GetRewards(private val rewards: RewardRepository) {
    operator fun invoke(): Flow<List<Reward>> = rewards.observeRewards()

    val lastSync: Flow<Instant?> = rewards.lastSync

    suspend fun refresh() = rewards.refresh()

    suspend fun refreshIfStale() = rewards.refreshIfStale()
}
