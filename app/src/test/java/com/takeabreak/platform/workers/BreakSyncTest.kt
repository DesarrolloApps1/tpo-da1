package com.takeabreak.platform.workers

import com.takeabreak.domain.model.Pause
import com.takeabreak.domain.usecase.GetHomeSummary
import com.takeabreak.domain.usecase.SyncBreaks
import com.takeabreak.platform.usage.UsageEventsReader
import com.takeabreak.testing.FakeBreakRepository
import com.takeabreak.testing.FakeNotifier
import com.takeabreak.testing.FakeRedemptionRepository
import com.takeabreak.testing.FakeRewardRepository
import com.takeabreak.testing.FakeSettingsRepository
import com.takeabreak.testing.reward
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakSyncTest {

    private val breaks = FakeBreakRepository()
    private val settings = FakeSettingsRepository()
    private val rewards = FakeRewardRepository()
    private val notifier = FakeNotifier()

    // Devuelve las pausas que empiezan desde el checkpoint, tardando un poco como la lectura real
    private class FakeReader(private val pauses: List<Pause>) : UsageEventsReader {
        override suspend fun closedPausesSince(checkpoint: Instant): List<Pause> {
            delay(100)
            return pauses.filter { !it.start.atZone(ZoneId.systemDefault()).toInstant().isBefore(checkpoint) }
        }
    }

    private var pointsSyncs = 0

    private fun breakSync(vararg pauses: Pause) = BreakSync(
        reader = FakeReader(pauses.toList()),
        settings = settings,
        syncBreaks = SyncBreaks(breaks, settings),
        getHomeSummary = GetHomeSummary(breaks, FakeRedemptionRepository(), rewards, settings),
        notifier = notifier,
        syncPoints = { pointsSyncs++ },
    )

    // 27/10/2026 es martes
    private fun pause(start: String, end: String) = Pause(LocalDateTime.parse(start), LocalDateTime.parse(end))

    @Test
    fun `si sumo puntos pide informarlos y si no, no`() = runTest {
        breakSync().invoke()
        assertEquals(0, pointsSyncs)

        breakSync(pause("2026-10-27T10:00", "2026-10-27T11:00")).invoke()
        assertEquals(1, pointsSyncs)
    }

    @Test
    fun `dos sincronizaciones a la vez guardan y avisan cada pausa una sola vez`() = runTest {
        val sync = breakSync(pause("2026-10-27T10:00", "2026-10-27T11:00"))

        launch { sync() }
        launch { sync() }
        testScheduler.advanceUntilIdle()

        assertEquals(1, breaks.sessions.value.size)
        assertEquals(1, notifier.breaks.size)
    }

    @Test
    fun `varias pausas que suman van en un solo aviso`() = runTest {
        breakSync(
            pause("2026-10-27T10:00", "2026-10-27T11:00"),
            pause("2026-10-27T13:00", "2026-10-27T14:00"),
            pause("2026-10-27T15:00", "2026-10-27T15:10"), // corta, no suma
        )()

        assertEquals(1, notifier.breaks.size)
        assertEquals(listOf(60, 60), notifier.breaks.single().first.map { it.points })
    }

    @Test
    fun `sin pausas que sumen no avisa`() = runTest {
        breakSync(pause("2026-10-27T10:00", "2026-10-27T10:10"))()

        assertTrue(notifier.breaks.isEmpty())
    }

    @Test
    fun `avisa la meta cuando el saldo ya alcanza`() = runTest {
        val goal = reward("yoga", points = 100)
        rewards.rewards.value = listOf(goal)
        settings.setGoal("yoga")

        breakSync(
            pause("2026-10-27T10:00", "2026-10-27T11:00"),
            pause("2026-10-27T13:00", "2026-10-27T14:00"),
        )()

        assertEquals(goal, notifier.breaks.single().second)
    }
}
