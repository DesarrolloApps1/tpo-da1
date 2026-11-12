package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.ActivityPeriod
import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.testing.FakeBreakRepository
import com.takeabreak.testing.FakeSettingsRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBreakHistoryTest {

    // 12/11/2026 es jueves: la semana va del lunes 9 al domingo 15
    private val today = LocalDate.parse("2026-11-12")
    private val breaks = FakeBreakRepository()
    private val getBreakHistory = GetBreakHistory(breaks, FakeSettingsRepository(), today = { today })

    private fun session(start: String, validMinutes: Int, points: Int, status: BreakStatus) =
        LocalDateTime.parse(start).let { BreakSession(it, it.plusMinutes(validMinutes.toLong()), validMinutes, points, status) }

    @Test
    fun `la semana tiene un dia por barra de lunes a domingo`() = runTest {
        val summary = getBreakHistory(ActivityPeriod.WEEK).first()

        assertEquals((9..15).map { LocalDate.parse("2026-11-%02d".format(it)) }, summary.chart.map { it.day })
        assertEquals(today, summary.today)
    }

    @Test
    fun `el mes tiene un dia por barra del 1 al 30`() = runTest {
        val summary = getBreakHistory(ActivityPeriod.MONTH).first()

        assertEquals(30, summary.chart.size)
        assertEquals(LocalDate.parse("2026-11-01"), summary.chart.first().day)
    }

    @Test
    fun `los minutos cuentan solo las pausas que alcanzaron el minimo`() = runTest {
        breaks.sessions.value = listOf(
            session("2026-11-10T10:00", 60, 60, BreakStatus.VALID),
            session("2026-11-10T14:00", 300, 180, BreakStatus.PAUSE_CAP),
            session("2026-11-10T18:00", 20, 0, BreakStatus.TOO_SHORT),
            session("2026-11-11T10:00", 50, 0, BreakStatus.DAILY_CAP),
            session("2026-11-08T10:00", 60, 60, BreakStatus.VALID), // domingo de la semana anterior
        )

        val summary = getBreakHistory(ActivityPeriod.WEEK).first()

        assertEquals(360, summary.chart.single { it.day == LocalDate.parse("2026-11-10") }.minutes)
        assertEquals(410, summary.minutes)
        assertEquals(240, summary.points)
    }

    @Test
    fun `el historial va por dia del mas nuevo al mas viejo y solo 30 dias`() = runTest {
        breaks.sessions.value = listOf(
            session("2026-11-11T09:00", 60, 60, BreakStatus.VALID),
            session("2026-11-12T08:00", 30, 0, BreakStatus.REST_TIME),
            session("2026-11-12T15:00", 60, 60, BreakStatus.VALID),
            session("2026-10-01T15:00", 60, 60, BreakStatus.VALID),
        )

        val summary = getBreakHistory().first()

        assertEquals(listOf(today, today.minusDays(1)), summary.days.map { it.day })
        assertEquals(listOf(15, 8), summary.days.first().sessions.map { it.start.hour })
        assertEquals(60, summary.days.first().points)
    }

    @Test
    fun `racha y record`() = runTest {
        breaks.sessions.value = listOf(
            session("2026-11-12T10:00", 60, 60, BreakStatus.VALID),
            session("2026-11-11T10:00", 60, 60, BreakStatus.VALID),
            session("2026-11-01T10:00", 60, 60, BreakStatus.VALID),
            session("2026-11-02T10:00", 60, 60, BreakStatus.VALID),
            session("2026-11-03T10:00", 60, 60, BreakStatus.VALID),
        )

        val summary = getBreakHistory().first()

        assertEquals(2, summary.streakDays)
        assertEquals(3, summary.recordStreakDays)
    }

    @Test
    fun `sin pausas no hay historial`() = runTest {
        assertTrue(getBreakHistory().first().days.isEmpty())
    }
}
