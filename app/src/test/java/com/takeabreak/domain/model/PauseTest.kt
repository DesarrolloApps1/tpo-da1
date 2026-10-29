package com.takeabreak.domain.model

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseTest {

    // 27/10/2026 es martes y 31/10/2026 es sábado
    private fun pause(start: String, end: String) =
        Pause(LocalDateTime.parse(start), LocalDateTime.parse(end))

    private fun Pause.outside() = minutesOutside(restFrom = 23, restTo = 9)

    @Test
    fun `pausa de dia cuenta entera`() {
        assertEquals(60, pause("2026-10-27T10:00", "2026-10-27T11:00").outside())
    }

    @Test
    fun `pausa que entra al descanso cuenta solo hasta las 23`() {
        assertEquals(30, pause("2026-10-27T22:30", "2026-10-27T23:45").outside())
    }

    @Test
    fun `pausa dentro del descanso cruzando la medianoche no cuenta`() {
        assertEquals(0, pause("2026-10-27T23:30", "2026-10-28T08:00").outside())
    }

    @Test
    fun `pausa que empieza en el descanso de la madrugada cuenta desde las 9`() {
        assertEquals(60, pause("2026-10-28T08:00", "2026-10-28T10:00").outside())
    }

    @Test
    fun `pausa de varios dias descuenta cada noche`() {
        // 38 h en total menos dos noches de 10 h
        assertEquals(18 * 60, pause("2026-10-26T20:00", "2026-10-28T10:00").outside())
    }

    @Test
    fun `segundos sueltos se redondean para abajo`() {
        assertEquals(59, pause("2026-10-27T10:00:30", "2026-10-27T11:00:00").outside())
    }

    @Test
    fun `fin de semana segun el dia en que empieza`() {
        assertTrue(pause("2026-10-31T10:00", "2026-10-31T11:00").isWeekend)
        assertFalse(pause("2026-10-27T10:00", "2026-10-27T11:00").isWeekend)
    }
}
