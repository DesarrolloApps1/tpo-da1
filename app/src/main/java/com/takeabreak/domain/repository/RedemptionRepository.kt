package com.takeabreak.domain.repository

import com.takeabreak.domain.model.DeliveryResult
import com.takeabreak.domain.model.RedeemResult
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.Reward
import kotlinx.coroutines.flow.Flow

interface RedemptionRepository {
    fun observeRedemptions(): Flow<List<Redemption>>

    // Puntos de los canjes que no fueron devueltos ni vencieron
    fun observeSpentPoints(): Flow<Int>

    // Quien llama genera el id y lo repite en los reintentos, así el servidor no duplica el canje
    suspend fun redeem(id: String, reward: Reward): RedeemResult
    suspend fun confirmDelivery(id: String, pin: String): DeliveryResult

    // Consulta el estado de los canjes activos. Sin red no hace nada: queda lo guardado
    suspend fun refreshActive()
}
