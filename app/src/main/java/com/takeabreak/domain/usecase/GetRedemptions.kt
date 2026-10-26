package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.repository.RedemptionRepository
import kotlinx.coroutines.flow.Flow

class GetRedemptions(private val redemptions: RedemptionRepository) {
    operator fun invoke(): Flow<List<Redemption>> = redemptions.observeRedemptions()
}
