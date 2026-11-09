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

    @Test
    fun `las pausas de menos de 1 minuto no se guardan pero mueven el checkpoint`() = runTest {
        val saved = syncBreaks(
            listOf(
                pause("2026-10-27T10:00", "2026-10-27T11:00"),
                Pause(LocalDateTime.parse("2026-10-27T11:30:00"), LocalDateTime.parse("2026-10-27T11:30:40")),
            ),
        )

        assertEquals(listOf(Triple(60, 60, BreakStatus.VALID)), saved.summary())
        val expected = LocalDateTime.parse("2026-10-27T11:30:40").atZone(ZoneId.systemDefault()).toInstant()
        assertEquals(listOf(expected), settings.savedCheckpoints)
    }

    @Test
    fun `una pausa con cambio de hora no suma y queda con su motivo`() = runTest {
        val changed = Pause(LocalDateTime.parse("2026-10-27T10:00"), LocalDateTime.parse("2026-10-27T11:00"), clockChanged = true)

        assertEquals(listOf(Triple(60, 0, BreakStatus.CLOCK_CHANGED)), syncBreaks(listOf(changed)).summary())
    }

    @Test
    fun `una pausa de mas de 3 horas suma el tope y queda con su motivo`() = runTest {
        val saved = syncBreaks(listOf(pause("2026-10-27T10:00", "2026-10-27T15:00")))

        assertEquals(listOf(Triple(300, 180, BreakStatus.PAUSE_CAP)), saved.summary())
    }

    private fun declareAt(time: String) {
        settings.declaredBreakAt.value = LocalDateTime.parse(time).atZone(ZoneId.systemDefault()).toInstant()
    }

    @Test
    fun `tocar el tile y bloquear una hora un martes suma 60 mas el bonus`() = runTest {
        declareAt("2026-10-27T09:59")

        val saved = syncBreaks(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")))

        assertEquals(listOf(Triple(60, 66, BreakStatus.VALID)), saved.summary())
        assertEquals(6, saved.single().bonusPoints)
        assertEquals(null, settings.declaredBreakAt.value)
    }

    @Test
    fun `tocar el tile sin bloquear no cambia nada`() = runTest {
        declareAt("2026-10-27T09:59")

        val saved = syncBreaks(listOf(pause("2026-10-26T14:00", "2026-10-26T14:45")))

        assertEquals(listOf(Triple(45, 45, BreakStatus.VALID)), saved.summary())
        assertEquals(0, saved.single().bonusPoints)
        // La declaración sigue esperando el bloqueo
        assertTrue(settings.declaredBreakAt.value != null)
    }

    @Test
    fun `una pausa que empieza fuera de la ventana no recibe el bonus y consume la declaracion`() = runTest {
        declareAt("2026-10-27T09:50")

        val saved = syncBreaks(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")))

        assertEquals(listOf(Triple(60, 60, BreakStatus.VALID)), saved.summary())
        assertEquals(null, settings.declaredBreakAt.value)
    }

    @Test
    fun `el bonus es solo para la primera pausa despues de tocar el tile`() = runTest {
        declareAt("2026-10-27T09:59")

        val saved = syncBreaks(
            listOf(
                pause("2026-10-27T10:00", "2026-10-27T11:00"),
                pause("2026-10-27T11:01", "2026-10-27T12:01"),
            ),
        )

        assertEquals(listOf(66, 60), saved.map { it.points })
    }

    @Test
    fun `una pausa corta que no suma tampoco recibe el bonus`() = runTest {
        declareAt("2026-10-27T09:59")

        val saved = syncBreaks(listOf(pause("2026-10-27T10:00", "2026-10-27T10:20")))

        assertEquals(listOf(Triple(20, 0, BreakStatus.TOO_SHORT)), saved.summary())
        assertEquals(0, saved.single().bonusPoints)
    }

    @Test
    fun `el bonus no supera el tope por pausa`() = runTest {
        declareAt("2026-10-27T09:59")

        val saved = syncBreaks(listOf(pause("2026-10-27T10:00", "2026-10-27T13:00")))

        assertEquals(listOf(Triple(180, 180, BreakStatus.VALID)), saved.summary())
        assertEquals(0, saved.single().bonusPoints)
    }

    @Test
    fun `el bonus no supera el tope diario`() = runTest {
        breaks.sessions.value = listOf(
            BreakSession(
                LocalDateTime.parse("2026-10-27T07:00"), LocalDateTime.parse("2026-10-27T09:00"),
                120, 120, BreakStatus.VALID,
            ),
        )
        declareAt("2026-10-27T09:59")

        // 120 ya sumados: quedan 120. 100 min + 10 % = 110, entra completo
        val first = syncBreaks(listOf(pause("2026-10-27T10:00", "2026-10-27T11:40")))
        assertEquals(110, first.single().points)

        // 230 sumados: quedan 10 de tope, aunque 60 + 6 = 66
        declareAt("2026-10-27T12:59")
        val second = syncBreaks(listOf(pause("2026-10-27T13:00", "2026-10-27T14:00")))
        assertEquals(10, second.single().points)
        assertEquals(0, second.single().bonusPoints)
    }

    @Test
    fun `sin declaracion no hay bonus`() = runTest {
        val saved = syncBreaks(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")))

        assertEquals(0, saved.single().bonusPoints)
        assertEquals(60, saved.single().points)
    }
}
