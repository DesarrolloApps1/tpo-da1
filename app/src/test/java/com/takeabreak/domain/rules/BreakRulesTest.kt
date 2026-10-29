package com.takeabreak.domain.rules

import com.takeabreak.domain.model.Pause
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class BreakRulesTest {

    // 27/10/2026 es martes y 31/10/2026 es sábado
    private fun pause(start: String, end: String) =
        Pause(LocalDateTime.parse(start), LocalDateTime.parse(end))

    @Test
    fun `una hora un martes suma 60 puntos`() {
        assertEquals(60, BreakRules.points(pause("2026-10-27T10:00", "2026-10-27T11:00"), pointsToday = 0))
    }

    @Test
    fun `entre semana 30 minutos alcanzan y 29 no`() {
        assertEquals(30, BreakRules.points(pause("2026-10-27T10:00", "2026-10-27T10:30"), pointsToday = 0))
        assertEquals(0, BreakRules.points(pause("2026-10-27T10:00", "2026-10-27T10:29"), pointsToday = 0))
    }

    @Test
    fun `el sabado 40 minutos no suman y 50 si`() {
        assertEquals(0, BreakRules.points(pause("2026-10-31T10:00", "2026-10-31T10:40"), pointsToday = 0))
        assertEquals(50, BreakRules.points(pause("2026-10-31T10:00", "2026-10-31T10:50"), pointsToday = 0))
    }

    @Test
    fun `el minimo se mide sin el horario de descanso`() {
        // 75 min de pausa pero solo 30 antes de las 23
        assertEquals(30, BreakRules.points(pause("2026-10-27T22:30", "2026-10-27T23:45"), pointsToday = 0))
        assertEquals(0, BreakRules.points(pause("2026-10-27T22:45", "2026-10-27T23:45"), pointsToday = 0))
    }

    @Test
    fun `pausa en horario de descanso no suma`() {
        assertEquals(0, BreakRules.points(pause("2026-10-27T23:30", "2026-10-28T08:00"), pointsToday = 0))
    }

    @Test
    fun `nunca se pasa del tope diario`() {
        val pause = pause("2026-10-27T10:00", "2026-10-27T11:00")
        assertEquals(40, BreakRules.points(pause, pointsToday = 200))
        assertEquals(0, BreakRules.points(pause, pointsToday = BreakRules.DAILY_CAP))
    }
}
