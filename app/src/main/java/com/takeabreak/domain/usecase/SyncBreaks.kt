package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.Pause
import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.SettingsRepository
import java.time.ZoneId

// Valida las pausas nuevas, las guarda y mueve el checkpoint. Devuelve las guardadas
class SyncBreaks(
    private val breaks: BreakRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(pauses: List<Pause>): List<BreakSession> {
        if (pauses.isEmpty()) return emptyList()
        val sessions: List<BreakSession> =
            TODO("aplicar BreakRules a cada pausa, acumulando pointsOn(día) para el tope, y asignar el BreakStatus")
        // Primero Room y después el checkpoint: si el proceso muere en el medio, el upsert evita duplicados
        breaks.save(sessions)
        val lastEnd = pauses.maxOf { it.end }
        settings.saveCheckpoint(lastEnd.atZone(ZoneId.systemDefault()).toInstant())
        return sessions
    }
}
