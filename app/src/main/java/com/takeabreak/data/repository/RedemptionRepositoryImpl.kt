package com.takeabreak.data.repository

import com.takeabreak.data.local.RedemptionDao
import com.takeabreak.data.mapper.toDomain
import com.takeabreak.data.remote.TakeABreakApi
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.RedemptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RedemptionRepositoryImpl(
    private val api: TakeABreakApi,
    private val dao: RedemptionDao,
    private val schedulePendingSync: () -> Unit,
) : RedemptionRepository {

    override fun observeRedemptions(): Flow<List<Redemption>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSpentPoints(): Flow<Int> = dao.observeSpentPoints()

    override suspend fun redeem(reward: Reward): Redemption =
        TODO("guardar PENDING con UUID propio, mandar a la API y actualizar; sin red dejarlo pendiente y llamar a schedulePendingSync")

    override suspend fun syncPending(): Unit =
        TODO("reenviar dao.pending(); si el servidor lo rechaza queda REFUNDED y los puntos vuelven al saldo")
}
