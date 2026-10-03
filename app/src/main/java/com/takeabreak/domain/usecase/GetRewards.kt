package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.RewardRepository
import kotlinx.coroutines.flow.Flow

class GetRewards(private val rewards: RewardRepository) {
    operator fun invoke(): Flow<List<Reward>> = rewards.observeRewards()

    suspend fun refresh() = rewards.refresh()
}
