package com.takeabreak.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SettingsRepository {
    val goalRewardId: Flow<String?>
    val dailyGoal: Flow<Int>
    val lastCatalogSync: Flow<Instant?>

    // Interruptor de Perfil: apagado no se avisa, pero las pausas se siguen sumando
    val notificationsEnabled: Flow<Boolean>

    // Hora en que se tocó el tile "Me tomo un break" (US-14); null si no hay declaración
    val declaredBreakAt: Flow<Instant?>

    suspend fun setGoal(rewardId: String)
    suspend fun setDailyGoal(minutes: Int)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun saveCatalogSync(at: Instant)
    suspend fun declareBreak(at: Instant)
    suspend fun clearDeclaredBreak()

    // Hasta dónde ya se leyeron los eventos de pantalla
    suspend fun checkpoint(): Instant
    suspend fun saveCheckpoint(at: Instant)
}
