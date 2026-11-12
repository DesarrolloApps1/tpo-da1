package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.testing.FakeBreakRepository
import com.takeabreak.testing.FakePointsRepository
import com.takeabreak.testing.FakeRedemptionRepository
import com.takeabreak.testing.FakeRewardRepository
import com.takeabreak.testing.FakeSettingsRepository
import com.takeabreak.testing.reward
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetHomeSummaryTest {

    private val today = LocalDate.parse("2026-11-12")
    private val breaks = FakeBreakRepository()
    private val points = FakePointsRepository()
    private val rewards = FakeRewardRepository()
    private val getHomeSummary = GetHomeSummary(
        breaks, FakeRedemptionRepository(), rewards, FakeSettingsRepository(), points, today = { today },
    )

    private fun session(daysAgo: Long, points: Int) = today.minusDays(daysAgo).atTime(15, 0).let {
        BreakSession(it, it.plusMinutes(points.toLong()), points, points, if (points > 0) BreakStatus.VALID else BreakStatus.TOO_SHORT)
    }

    @Test
    fun `sin sincronizar el saldo se calcula en el telefono y todo queda por confirmar`() = runTest {
        breaks.sessions.value = listOf(session(0, 60), session(1, 40))

        val summary = getHomeSummary().first()

        assertEquals(100, summary.balance)
        assertEquals(100, summary.pendingPoints)
        assertEquals(0, summary.spendable)
    }

    @Test
    fun `el saldo es el del servidor mas lo que todavia no se informo`() = runTest {
        breaks.sessions.value = listOf(session(1, 40), session(0, 60))
        points.lastKnownBalance.value = 300
        points.reportedDays.value = mapOf(today.minusDays(1) to 40, today to 20)

        val summary = getHomeSummary().first()

        assertEquals(40, summary.pendingPoints) // hoy: 60 ganados, 20 informados
        assertEquals(300, summary.spendable)
        assertEquals(340, summary.balance)
    }

    @Test
    fun `lo que alcanza se calcula con el saldo confirmado`() = runTest {
        rewards.rewards.value = listOf(reward("cafe", points = 100), reward("yoga", points = 300))
        breaks.sessions.value = listOf(session(0, 200))
        points.lastKnownBalance.value = 150

        val summary = getHomeSummary().first()

        assertEquals(listOf("cafe"), summary.affordable.map { it.id })
    }

    @Test
    fun `puntos de hoy y racha`() = runTest {
        breaks.sessions.value = listOf(session(0, 60), session(0, 0), session(1, 30), session(2, 45), session(4, 50))

        val summary = getHomeSummary().first()

        assertEquals(60, summary.pointsToday)
        assertEquals(3, summary.streakDays)
    }
}
