package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.RedemptionRepository

// Devuelve el canje activo, o pendiente si no hay red
class RedeemReward(private val redemptions: RedemptionRepository) {
    suspend operator fun invoke(reward: Reward): Result<Redemption> = runCatching { redemptions.redeem(reward) }

    // Reenvía los canjes que quedaron pendientes sin conexión
    suspend fun syncPending() = redemptions.syncPending()
}
