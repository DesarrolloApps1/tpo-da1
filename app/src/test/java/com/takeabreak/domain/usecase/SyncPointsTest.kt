package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.testing.FakeBreakRepository
import com.takeabreak.testing.FakePointsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SyncPointsTest {

    private val today = LocalDate.of(2026, 11, 9)
    private val breaks = FakeBreakRepository()
    private val points = FakePointsRepository()
    private val sync = SyncPoints(breaks, points) { today }

    private fun session(day: LocalDate, hour: Int, points: Int) = BreakSession(
        day.atTime(hour, 0), day.atTime(hour, 30), 30, points, BreakStatus.VALID,
    )

    @Test
    fun `manda el total de cada dia, sin pausas ni horarios`() = runTest {
        breaks.sessions.value = listOf(session(today, 10, 30), session(today, 12, 45), session(today.minusDays(1), 10, 60))

        sync()

        assertEquals(listOf(mapOf(today to 75, today.minusDays(1) to 60)), points.reports)
    }

    @Test
    fun `solo manda los dias que cambiaron`() = runTest {
        breaks.sessions.value = listOf(session(today.minusDays(1), 10, 60))
        sync()

        breaks.sessions.value += session(today, 10, 30)
        sync()

        assertEquals(mapOf(today to 30), points.reports.last())
    }

    @Test
    fun `sin cambios igual consulta el saldo`() = runTest {
        breaks.sessions.value = listOf(session(today, 10, 30))
        sync()

        sync()

        assertEquals(2, points.reports.size)
        assertEquals(emptyMap<LocalDate, Int>(), points.reports.last())
    }

    @Test
    fun `no manda dias que el servidor rechazaria`() = runTest {
        breaks.sessions.value = listOf(session(today.minusDays(10), 10, 30), session(today.minusDays(9), 10, 40))

        sync()

        assertEquals(mapOf(today.minusDays(9) to 40), points.reports.single())
    }

    @Test
    fun `nunca manda mas de 240 puntos en un dia`() = runTest {
        breaks.sessions.value = listOf(session(today, 10, 200), session(today, 12, 100))

        sync()

        assertEquals(mapOf(today to 240), points.reports.single())
    }
}
