package com.takeabreak.domain.rules

import com.takeabreak.domain.model.Pause
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class BreakRulesTest {

    private val rules = BreakRules()

    // 27/10/2026 es martes y 31/10/2026 es sábado
    private fun pause(start: String, end: String) =
        Pause(LocalDateTime.parse(start), LocalDateTime.parse(end))

    @Test
    fun `una hora un martes suma 60 puntos`() {
        assertEquals(60, rules.points(pause("2026-10-27T10:00", "2026-10-27T11:00"), pointsToday = 0))
    }

    @Test
    fun `entre semana 30 minutos alcanzan y 29 no`() {
        assertEquals(30, rules.points(pause("2026-10-27T10:00", "2026-10-27T10:30"), pointsToday = 0))
        assertEquals(0, rules.points(pause("2026-10-27T10:00", "2026-10-27T10:29"), pointsToday = 0))
    }

    @Test
    fun `el sabado 40 minutos no suman y 50 si`() {
        assertEquals(0, rules.points(pause("2026-10-31T10:00", "2026-10-31T10:40"), pointsToday = 0))
        assertEquals(50, rules.points(pause("2026-10-31T10:00", "2026-10-31T10:50"), pointsToday = 0))
    }

    @Test
    fun `el minimo se mide sin el horario de descanso`() {
        // 75 min de pausa pero solo 30 antes de las 23
        assertEquals(30, rules.points(pause("2026-10-27T22:30", "2026-10-27T23:45"), pointsToday = 0))
        assertEquals(0, rules.points(pause("2026-10-27T22:45", "2026-10-27T23:45"), pointsToday = 0))
    }

    @Test
    fun `pausa en horario de descanso no suma`() {
        assertEquals(0, rules.points(pause("2026-10-27T23:30", "2026-10-28T08:00"), pointsToday = 0))
    }

    @Test
    fun `nunca se pasa del tope diario`() {
        val pause = pause("2026-10-27T10:00", "2026-10-27T11:00")
        assertEquals(40, rules.points(pause, pointsToday = 200))
        assertEquals(0, rules.points(pause, pointsToday = rules.config.dailyCap))
    }

    @Test
    fun `una pausa de 5 horas suma como maximo 3 horas`() {
        assertEquals(180, rules.points(pause("2026-10-27T10:00", "2026-10-27T15:00"), pointsToday = 0))
        assertEquals(true, rules.isOverPauseCap(rules.validMinutes(pause("2026-10-27T10:00", "2026-10-27T15:00"))))
    }

    @Test
    fun `el tope diario se aplica despues del tope por pausa`() {
        assertEquals(60, rules.points(pause("2026-10-27T10:00", "2026-10-27T15:00"), pointsToday = 180))
    }

    @Test
    fun `cambiar los parametros cambia el resultado`() {
        val custom = BreakRules(BreakRulesConfig(weekdayMinMinutes = 20, pauseCapMinutes = 60, restFrom = 22))
        val pause = pause("2026-10-27T21:00", "2026-10-27T23:00")

        // 21 a 22 h: 60 min válidos, alcanza el mínimo de 20 y llega justo al tope de 60
        assertEquals(60, custom.points(pause, pointsToday = 0))
        assertEquals(20, custom.points(pause("2026-10-27T10:00", "2026-10-27T10:20"), pointsToday = 0))
        assertEquals(0, rules.points(pause("2026-10-27T10:00", "2026-10-27T10:20"), pointsToday = 0))
    }
}
