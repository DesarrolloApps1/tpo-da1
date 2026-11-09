package com.takeabreak.domain.usecase

import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.PointsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// Informa al servidor el total de cada día que cambió y actualiza el saldo. Sin cambios igual
// consulta, porque el saldo puede haber subido por un canje vencido o devuelto
class SyncPoints(
    private val breaks: BreakRepository,
    private val points: PointsRepository,
    private val today: () -> LocalDate = LocalDate::now,
) {
    suspend operator fun invoke() {
        val since = today().minusDays(REPORT_WINDOW_DAYS)
        val totals = breaks.observeSince(since).first()
            .groupBy { it.start.toLocalDate() }
            .mapValues { (_, sessions) -> sessions.sumOf { it.points }.coerceAtMost(MAX_DAILY_POINTS) }
        val reported = points.reportedDays.first()
        points.report(totals.filter { (day, total) -> total > (reported[day] ?: 0) })
    }

    companion object {
        // El servidor rechaza todo el envío si un día tiene más de 10 días o más de 240 puntos.
        // Se manda uno menos para no quedar justo en el límite si la hora del teléfono difiere
        const val REPORT_WINDOW_DAYS = 9L
        const val MAX_DAILY_POINTS = 240
    }
}
