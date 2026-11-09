package com.takeabreak.domain.repository

import com.takeabreak.domain.model.Reward
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface RewardRepository {
    fun observeRewards(): Flow<List<Reward>>

    // Para el aviso "Actualizado hace X"; null si nunca se sincronizó
    val lastSync: Flow<Instant?>

    // Trae de la API lo que cambió y lo aplica en Room, incluidas las bajas
    suspend fun refresh()

    // Lo mismo, pero solo si pasó más de una hora desde la última sincronización
    suspend fun refreshIfStale()
}
