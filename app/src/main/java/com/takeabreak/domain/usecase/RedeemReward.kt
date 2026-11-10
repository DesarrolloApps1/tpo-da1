package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.RedeemResult
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.RedemptionRepository

// Sin conexión no se canjea: el servidor genera el código y valida stock y saldo
class RedeemReward(private val redemptions: RedemptionRepository) {
    suspend operator fun invoke(id: String, reward: Reward): RedeemResult = redemptions.redeem(id, reward)
}
