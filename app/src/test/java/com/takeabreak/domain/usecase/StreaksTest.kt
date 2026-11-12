package com.takeabreak.domain.usecase

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreaksTest {

    private val today = LocalDate.parse("2026-11-12")

    private fun days(vararg offsets: Long) = offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun `cuenta hoy si ya hubo una pausa valida`() {
        assertEquals(3, Streaks.current(days(0, 1, 2), today))
    }

    @Test
    fun `sin pausa hoy la racha sigue desde ayer`() {
        assertEquals(2, Streaks.current(days(1, 2), today))
    }

    @Test
    fun `un dia entero sin pausas corta la racha`() {
        assertEquals(0, Streaks.current(days(2, 3, 4), today))
        assertEquals(1, Streaks.current(days(0, 2, 3), today))
    }

    @Test
    fun `el record es la racha mas larga del historial`() {
        assertEquals(4, Streaks.record(days(0, 1, 5, 6, 7, 8, 12)))
        assertEquals(0, Streaks.record(emptySet()))
    }
}
