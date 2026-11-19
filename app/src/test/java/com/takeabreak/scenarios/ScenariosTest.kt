package com.takeabreak.scenarios

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.usecase.SyncBreaks
import com.takeabreak.platform.usage.PauseAssembler
import com.takeabreak.platform.usage.ScreenEvent
import com.takeabreak.platform.usage.ScreenEvent.Kind
import com.takeabreak.testing.FakeBreakRepository
import com.takeabreak.testing.FakeSettingsRepository
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Situaciones de uso real, de punta a punta: los eventos que Android registraría, las pausas que arma
// PauseAssembler y los puntos y el motivo que asigna SyncBreaks con las reglas por defecto.
// Reemplazan a la semana de registro manual (US-13): cada regla queda justificada con el caso que
// la motiva. Lo que estos tests no prueban es que Android entregue los eventos, eso va en el emulador.
class ScenariosTest {

    private val zone = ZoneId.systemDefault()
    private val breaks = FakeBreakRepository()
    private val syncBreaks = SyncBreaks(breaks, FakeSettingsRepository())

    // 27/10/2026 es martes y 31/10/2026 es sábado
    private fun event(time: String, kind: Kind, service: Int? = null) =
        ScreenEvent(LocalDateTime.parse(time).atZone(zone).toInstant(), kind, service)

    private fun lock(time: String) = event(time, Kind.LOCK)
    private fun unlock(time: String) = event(time, Kind.UNLOCK)
    private fun serviceStart(time: String, service: Int = 1) = event(time, Kind.SERVICE_START, service)
    private fun serviceStop(time: String, service: Int = 1) = event(time, Kind.SERVICE_STOP, service)

    // Lo que queda en el historial: minutos válidos, puntos y motivo de cada pausa
    private suspend fun day(vararg events: ScreenEvent): List<Triple<Int, Int, BreakStatus>> =
        syncBreaks(PauseAssembler.closedPauses(events.toList(), zone)).map { it.summary() }

    private fun BreakSession.summary() = Triple(validMinutes, points, status)

    @Test
    fun `deja el telefono en la mesa para cenar una hora`() = runTest {
        val result = day(lock("2026-10-27T20:00"), unlock("2026-10-27T21:00"))

        assertEquals(listOf(Triple(60, 60, BreakStatus.VALID)), result)
    }

    @Test
    fun `una notificacion prende la pantalla pero no lo desbloquea`() = runTest {
        // El sistema vuelve a mostrar el bloqueo; la pausa sigue siendo una sola
        val result = day(lock("2026-10-27T20:00"), lock("2026-10-27T20:25"), unlock("2026-10-27T21:00"))

        assertEquals(listOf(Triple(60, 60, BreakStatus.VALID)), result)
    }

    @Test
    fun `mira la hora unos segundos y lo vuelve a bloquear`() = runTest {
        val result = day(lock("2026-10-27T10:00:00"), unlock("2026-10-27T10:00:20"))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `lo deja 20 minutos para hacer un mandado`() = runTest {
        val result = day(lock("2026-10-27T10:00"), unlock("2026-10-27T10:20"))

        assertEquals(listOf(Triple(20, 0, BreakStatus.TOO_SHORT)), result)
    }

    @Test
    fun `una siesta de 40 minutos un sabado`() = runTest {
        // El fin de semana el mínimo es 45 min
        val result = day(lock("2026-10-31T15:00"), unlock("2026-10-31T15:40"))

        assertEquals(listOf(Triple(40, 0, BreakStatus.TOO_SHORT)), result)
    }

    @Test
    fun `duerme toda la noche`() = runTest {
        val result = day(lock("2026-10-27T23:30"), unlock("2026-10-28T07:30"))

        assertEquals(listOf(Triple(0, 0, BreakStatus.REST_TIME)), result)
    }

    @Test
    fun `lee un libro desde las 22 30 hasta pasadas las 23`() = runTest {
        // Solo cuenta hasta las 23; con 30 min alcanza el mínimo
        val result = day(lock("2026-10-27T22:30"), unlock("2026-10-27T23:45"))

        assertEquals(listOf(Triple(30, 30, BreakStatus.VALID)), result)
    }

    @Test
    fun `se olvida el telefono en un cajon 5 horas`() = runTest {
        val result = day(lock("2026-10-27T10:00"), unlock("2026-10-27T15:00"))

        assertEquals(listOf(Triple(300, 180, BreakStatus.PAUSE_CAP)), result)
    }

    @Test
    fun `tres pausas largas en el mismo dia`() = runTest {
        val result = day(
            lock("2026-10-27T09:00"), unlock("2026-10-27T11:00"),
            lock("2026-10-27T13:00"), unlock("2026-10-27T15:00"),
            lock("2026-10-27T17:00"), unlock("2026-10-27T19:00"),
        )

        assertEquals(
            listOf(Triple(120, 120, BreakStatus.VALID), Triple(120, 120, BreakStatus.VALID), Triple(120, 0, BreakStatus.DAILY_CAP)),
            result,
        )
    }

    @Test
    fun `pone musica y bloquea el telefono`() = runTest {
        val result = day(serviceStart("2026-10-27T19:55"), lock("2026-10-27T20:00"), unlock("2026-10-27T21:00"))

        assertEquals(listOf(Triple(60, 0, BreakStatus.SERVICE_ACTIVE)), result)
    }

    @Test
    fun `una llamada larga con la pantalla apagada`() = runTest {
        val result = day(
            lock("2026-10-27T10:00"), serviceStart("2026-10-27T10:05"), serviceStop("2026-10-27T10:55"), unlock("2026-10-27T11:00"),
        )

        assertEquals(listOf(Triple(60, 0, BreakStatus.SERVICE_ACTIVE)), result)
    }

    @Test
    fun `la musica se corta enseguida y la pausa sigue`() = runTest {
        // 10 de 60 minutos con un servicio activo: no alcanza la mitad
        val result = day(
            serviceStart("2026-10-27T19:55"), lock("2026-10-27T20:00"), serviceStop("2026-10-27T20:10"), unlock("2026-10-27T21:00"),
        )

        assertEquals(listOf(Triple(60, 60, BreakStatus.VALID)), result)
    }

    @Test
    fun `tiene una VPN o un contador de pasos prendido todo el dia`() = runTest {
        val result = day(serviceStart("2026-10-27T06:00"), lock("2026-10-27T20:00"), unlock("2026-10-27T21:00"))

        assertEquals(listOf(Triple(60, 60, BreakStatus.VALID)), result)
    }

    @Test
    fun `el telefono se apaga por bateria en medio de la pausa`() = runTest {
        // El tiempo apagado no cuenta
        val result = day(
            lock("2026-10-27T10:00"), event("2026-10-27T10:40", Kind.SHUTDOWN),
            event("2026-10-27T12:00", Kind.STARTUP), unlock("2026-10-27T12:05"),
        )

        assertEquals(listOf(Triple(40, 40, BreakStatus.VALID)), result)
    }

    @Test
    fun `cambia la hora del telefono para sumar mas`() = runTest {
        val result = day(lock("2026-10-27T10:00"), event("2026-10-27T10:10", Kind.CLOCK_CHANGE), unlock("2026-10-27T13:00"))

        assertEquals(listOf(Triple(180, 0, BreakStatus.CLOCK_CHANGED)), result)
    }

    @Test
    fun `la app se cierra justo despues de guardar y la tanda se vuelve a leer`() = runTest {
        val events = arrayOf(lock("2026-10-27T09:00"), unlock("2026-10-27T12:20"), lock("2026-10-27T15:00"), unlock("2026-10-27T16:00"))
        val first = day(*events)

        val again = day(*events)

        assertEquals(first, again)
        assertEquals(240, breaks.sessions.value.sumOf { it.points })
    }
}
