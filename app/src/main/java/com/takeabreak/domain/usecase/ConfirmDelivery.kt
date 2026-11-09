package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.DeliveryResult
import com.takeabreak.domain.repository.RedemptionRepository

// El cajero escribe el PIN del local en el teléfono del usuario
class ConfirmDelivery(private val redemptions: RedemptionRepository) {
    suspend operator fun invoke(id: String, pin: String): DeliveryResult = redemptions.confirmDelivery(id, pin)
}
