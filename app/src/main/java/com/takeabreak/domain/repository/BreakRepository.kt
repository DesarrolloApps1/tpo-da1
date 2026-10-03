package com.takeabreak.domain.repository

import com.takeabreak.domain.model.BreakSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface BreakRepository {
    fun observeSince(day: LocalDate): Flow<List<BreakSession>>
    fun observeTotalPoints(): Flow<Int>
    suspend fun pointsOn(day: LocalDate): Int

    // Reemplaza si ya existe una pausa con el mismo inicio, así nunca se cuenta dos veces
    suspend fun save(sessions: List<BreakSession>)
}
