package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.repository.BreakRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Historial de los últimos 30 días para Actividad
class GetBreakHistory(private val breaks: BreakRepository) {
    operator fun invoke(): Flow<List<BreakSession>> = breaks.observeSince(LocalDate.now().minusDays(30))
}
