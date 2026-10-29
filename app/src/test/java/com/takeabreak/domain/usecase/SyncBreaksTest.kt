package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.model.Pause
import com.takeabreak.testing.FakeBreakRepository
import com.takeabreak.testing.FakeSettingsRepository
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncBreaksTest {

    private val breaks = FakeBreakRepository()
    private val settings = FakeSettingsRepository()
    private val syncBreaks = SyncBreaks(breaks, settings)

    // 27/10/2026 es martes y 31/10/2026 es sábado
    private fun pause(start: String, end: String) =
        Pause(LocalDateTime.parse(start), LocalDateTime.parse(end))

    private fun List<BreakSession>.summary() = map { Triple(it.validMinutes, it.points, it.status) }

    @Test
    fun `sin pausas no guarda ni mueve el checkpoint`() = runTest {
        assertEquals(emptyList<BreakSession>(), syncBreaks(emptyList()))
        assertTrue(breaks.sessions.value.isEmpty())
        assertTrue(settings.savedCheckpoints.isEmpty())
    }

    @Test
    fun `una hora un martes queda valida con 60 puntos`() = runTest {
        val saved = syncBreaks(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")))

        assertEquals(listOf(Triple(60, 60, BreakStatus.VALID)), saved.summary())
        assertEquals(saved, breaks.sessions.value)
    }

    @Test
    fun `cada pausa que no suma queda con su motivo`() = runTest {
        val saved = syncBreaks(
            listOf(
                pause("2026-10-27T10:00", "2026-10-27T10:20"), // corta
                pause("2026-10-27T23:30", "2026-10-28T08:00"), // toda en descanso
                pause("2026-10-31T10:00", "2026-10-31T10:40"), // sábado, menos de 45
            ),
        )

        assertEquals(
            listOf(
                Triple(20, 0, BreakStatus.TOO_SHORT),
                Triple(0, 0, BreakStatus.REST_TIME),
                Triple(40, 0, BreakStatus.TOO_SHORT),
            ),
            saved.summary(),
        )
    }

    @Test
    fun `si el descanso la deja por debajo del minimo el motivo es el descanso`() = runTest {
        // 60 min de pausa pero solo 15 antes de las 23
        val saved = syncBreaks(listOf(pause("2026-10-27T22:45", "2026-10-27T23:45")))

        assertEquals(listOf(Triple(15, 0, BreakStatus.REST_TIME)), saved.summary())
    }

    @Test
    fun `el tope diario se reparte entre las pausas de la tanda`() = runTest {
        val saved = syncBreaks(
            listOf(
                // Desordenadas a propósito: el tope se aplica por hora de inicio
                pause("2026-10-27T18:00", "2026-10-27T19:00"),
                pause("2026-10-27T09:00", "2026-10-27T12:00"),
                pause("2026-10-27T13:00", "2026-10-27T14:00"),
            ),
        )

        assertEquals(
            listOf(
                Triple(180, 180, BreakStatus.VALID),
                Triple(60, 60, BreakStatus.VALID),
                Triple(60, 0, BreakStatus.DAILY_CAP),
            ),
            saved.summary(),
        )
    }

    @Test
    fun `el tope cuenta los puntos ya guardados de ese dia`() = runTest {
        breaks.sessions.value = listOf(
            BreakSession(
                LocalDateTime.parse("2026-10-27T09:00"), LocalDateTime.parse("2026-10-27T12:20"),
                200, 200, BreakStatus.VALID,
            ),
        )

        val saved = syncBreaks(
            listOf(
                pause("2026-10-27T15:00", "2026-10-27T16:00"),
                pause("2026-10-28T15:00", "2026-10-28T16:00"), // otro día, arranca de cero
            ),
        )

        assertEquals(
            listOf(Triple(60, 40, BreakStatus.VALID), Triple(60, 60, BreakStatus.VALID)),
            saved.summary(),
        )
    }

    @Test
    fun `el checkpoint queda en el final de la ultima pausa`() = runTest {
        syncBreaks(
            listOf(
                pause("2026-10-27T10:00", "2026-10-27T11:00"),
                pause("2026-10-27T09:00", "2026-10-27T09:30"),
            ),
        )

        val expected = LocalDateTime.parse("2026-10-27T11:00").atZone(ZoneId.systemDefault()).toInstant()
        assertEquals(listOf(expected), settings.savedCheckpoints)
    }
}
