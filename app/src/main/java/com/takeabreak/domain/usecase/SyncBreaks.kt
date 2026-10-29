package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.model.Pause
import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.SettingsRepository
import com.takeabreak.domain.rules.BreakRules
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

// Valida las pausas nuevas, las guarda y mueve el checkpoint. Devuelve las guardadas
class SyncBreaks(
    private val breaks: BreakRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(pauses: List<Pause>): List<BreakSession> {
        if (pauses.isEmpty()) return emptyList()
        // Los puntos de cada día arrancan de lo ya guardado y suman las pausas de esta tanda, en orden
        val pointsByDay = mutableMapOf<LocalDate, Int>()
        val sessions = pauses.sortedBy { it.start }.map { pause ->
            val day = pause.start.toLocalDate()
            val pointsToday = pointsByDay.getOrPut(day) { breaks.pointsOn(day) }
            val validMinutes = BreakRules.validMinutes(pause)
            val points = BreakRules.points(pause, pointsToday)
            pointsByDay[day] = pointsToday + points
            BreakSession(pause.start, pause.end, validMinutes, points, statusOf(pause, validMinutes, points))
        }
        // Primero Room y después el checkpoint: si el proceso muere en el medio, el upsert evita duplicados
        breaks.save(sessions)
        val lastEnd = pauses.maxOf { it.end }
        settings.saveCheckpoint(lastEnd.atZone(ZoneId.systemDefault()).toInstant())
        return sessions
    }

    // Motivo por el que la pausa sumó o no, en el orden en que se aplican las reglas
    private fun statusOf(pause: Pause, validMinutes: Int, points: Int): BreakStatus {
        val minMinutes = BreakRules.minMinutes(pause)
        return when {
            points > 0 -> BreakStatus.VALID
            validMinutes >= minMinutes -> BreakStatus.DAILY_CAP
            Duration.between(pause.start, pause.end).toMinutes() >= minMinutes -> BreakStatus.REST_TIME
            else -> BreakStatus.TOO_SHORT
        }
    }
}
