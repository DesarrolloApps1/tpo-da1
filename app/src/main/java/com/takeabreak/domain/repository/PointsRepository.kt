package com.takeabreak.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PointsRepository {
    // Último saldo que confirmó el servidor; null si todavía no hubo ninguna sincronización
    val lastKnownBalance: Flow<Int?>

    // Total que el servidor ya conoce de cada día
    val reportedDays: Flow<Map<LocalDate, Int>>

    // Manda solo días y totales, guarda el saldo que devuelve y marca esos días como informados
    suspend fun report(days: Map<LocalDate, Int>)
}
