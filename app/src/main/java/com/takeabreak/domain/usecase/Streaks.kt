package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.BreakSession
import java.time.LocalDate

// Racha: días seguidos con al menos una pausa que sumó puntos
internal object Streaks {

    fun validDays(sessions: List<BreakSession>): Set<LocalDate> =
        sessions.filter { it.points > 0 }.map { it.start.toLocalDate() }.toSet()

    // Hoy cuenta solo si ya hubo una pausa válida; si no, la racha sigue viva desde ayer
    fun current(validDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in validDays) today else today.minusDays(1)
        var count = 0
        while (day in validDays) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun record(validDays: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in validDays.sorted()) {
            run = if (previous != null && day == previous.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }

    // Hasta dónde se mira para la racha y el récord
    const val HISTORY_DAYS = 365L
}
