package com.takeabreak.platform.workers

import com.takeabreak.domain.repository.SettingsRepository
import com.takeabreak.domain.usecase.GetHomeSummary
import com.takeabreak.domain.usecase.SyncBreaks
import com.takeabreak.platform.notifications.Notifier
import com.takeabreak.platform.usage.UsageEventsReader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// 1. lee los eventos, 2. valida y guarda, 3. avisa si sumó.
// La usan el worker periódico y el que corre al abrir la app. Si corrieran a la vez, los dos leerían
// desde el mismo checkpoint y el segundo recalcularía el tope del día con los puntos del primero ya
// guardados: el Mutex hace que el segundo espere y lea desde el checkpoint nuevo
class BreakSync(
    private val reader: UsageEventsReader,
    private val settings: SettingsRepository,
    private val syncBreaks: SyncBreaks,
    private val getHomeSummary: GetHomeSummary,
    private val notifier: Notifier,
) {
    private val mutex = Mutex()

    suspend operator fun invoke() = mutex.withLock {
        val pauses = reader.closedPausesSince(settings.checkpoint())
        val earned = syncBreaks(pauses).filter { it.points > 0 }
        if (earned.isEmpty()) return@withLock
        // Con las pausas ya guardadas, el saldo dice si ya alcanza para la meta
        val summary = getHomeSummary().first()
        val reachedGoal = summary.goal?.takeIf { summary.balance >= it.points }
        notifier.breaksValidated(earned, reachedGoal)
    }
}
