package com.takeabreak.platform.usage

import com.takeabreak.domain.model.Pause
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class PauseAssemblerTest {

    private val zone = ZoneOffset.UTC

    private fun event(at: String, kind: ScreenEvent.Kind) = ScreenEvent(LocalDateTime.parse(at).toInstant(zone), kind)
    private fun lock(at: String) = event(at, ScreenEvent.Kind.LOCK)
    private fun unlock(at: String) = event(at, ScreenEvent.Kind.UNLOCK)
    private fun shutdown(at: String) = event(at, ScreenEvent.Kind.SHUTDOWN)
    private fun startup(at: String) = event(at, ScreenEvent.Kind.STARTUP)
    private fun clockChange(at: String) = event(at, ScreenEvent.Kind.CLOCK_CHANGE)
    private fun pause(start: String, end: String, clockChanged: Boolean = false) =
        Pause(LocalDateTime.parse(start), LocalDateTime.parse(end), clockChanged)

    @Test
    fun `el apagado cierra la pausa y el tiempo apagado no cuenta`() {
        val events = listOf(
            lock("2026-10-27T10:00"), shutdown("2026-10-27T10:40"),
            startup("2026-10-27T12:40"), unlock("2026-10-27T12:45"),
        )

        assertEquals(listOf(pause("2026-10-27T10:00", "2026-10-27T10:40")), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `si falta el apagado el encendido descarta la pausa abierta`() {
        val events = listOf(lock("2026-10-27T10:00"), startup("2026-10-27T12:40"), unlock("2026-10-27T12:45"))

        assertEquals(emptyList<Pause>(), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `despues de encender un bloqueo nuevo arma su pausa`() {
        val events = listOf(
            startup("2026-10-27T08:00"), unlock("2026-10-27T08:01"),
            lock("2026-10-27T10:00"), unlock("2026-10-27T11:00"),
        )

        assertEquals(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `un cambio de hora durante la pausa la marca`() {
        val events = listOf(lock("2026-10-27T10:00"), clockChange("2026-10-27T10:30"), unlock("2026-10-27T11:00"))

        assertEquals(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00", clockChanged = true)), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `un cambio de hora fuera de la pausa no la marca`() {
        val events = listOf(
            clockChange("2026-10-27T09:00"), lock("2026-10-27T10:00"), unlock("2026-10-27T11:00"),
            lock("2026-10-27T12:00"), unlock("2026-10-27T13:00"),
        )

        assertEquals(
            listOf(pause("2026-10-27T10:00", "2026-10-27T11:00"), pause("2026-10-27T12:00", "2026-10-27T13:00")),
            PauseAssembler.closedPauses(events, zone),
        )
    }

    @Test
    fun `un cierre en el mismo instante que el bloqueo no arma pausa`() {
        val events = listOf(lock("2026-10-27T10:00"), unlock("2026-10-27T10:00"))

        assertEquals(emptyList<Pause>(), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `bloqueo y desbloqueo arman una pausa`() {
        val pauses = PauseAssembler.closedPauses(listOf(lock("2026-10-27T10:00"), unlock("2026-10-27T11:00")), zone)

        assertEquals(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")), pauses)
    }

    @Test
    fun `la pantalla encendida por una notificacion no corta la pausa`() {
        // El sistema vuelve a mostrar el bloqueo al encender la pantalla sin desbloquear
        val events = listOf(lock("2026-10-27T10:00"), lock("2026-10-27T10:20"), unlock("2026-10-27T11:00"))

        assertEquals(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `un desbloqueo sin bloqueo antes se ignora`() {
        val events = listOf(unlock("2026-10-27T09:00"), lock("2026-10-27T10:00"), unlock("2026-10-27T11:00"))

        assertEquals(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `una pausa todavia abierta no se devuelve`() {
        val events = listOf(lock("2026-10-27T10:00"), unlock("2026-10-27T11:00"), lock("2026-10-27T12:00"))

        assertEquals(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `eventos desordenados se ordenan por hora`() {
        val events = listOf(unlock("2026-10-27T11:00"), lock("2026-10-27T10:00"), unlock("2026-10-27T13:00"), lock("2026-10-27T12:00"))

        assertEquals(
            listOf(pause("2026-10-27T10:00", "2026-10-27T11:00"), pause("2026-10-27T12:00", "2026-10-27T13:00")),
            PauseAssembler.closedPauses(events, zone),
        )
    }

    @Test
    fun `sin eventos no hay pausas`() {
        assertEquals(emptyList<Pause>(), PauseAssembler.closedPauses(emptyList(), zone))
    }

    private fun serviceStart(at: String, key: Int = 1) = ScreenEvent(LocalDateTime.parse(at).toInstant(zone), ScreenEvent.Kind.SERVICE_START, key)
    private fun serviceStop(at: String, key: Int = 1) = ScreenEvent(LocalDateTime.parse(at).toInstant(zone), ScreenEvent.Kind.SERVICE_STOP, key)

    @Test
    fun `un servicio que arranco poco antes de bloquear cuenta sus minutos dentro de la pausa`() {
        // Música puesta a las 9:50, bloqueo a las 10:00, la música para a las 10:40
        val events = listOf(serviceStart("2026-10-27T09:50"), lock("2026-10-27T10:00"), serviceStop("2026-10-27T10:40"), unlock("2026-10-27T11:00"))

        assertEquals(40, PauseAssembler.closedPauses(events, zone).single().serviceMinutes)
    }

    @Test
    fun `un servicio que corre desde hace horas no cuenta`() {
        // Por ejemplo una VPN o el contador de pasos
        val events = listOf(serviceStart("2026-10-27T06:00"), lock("2026-10-27T10:00"), unlock("2026-10-27T11:00"))

        assertEquals(0, PauseAssembler.closedPauses(events, zone).single().serviceMinutes)
    }

    @Test
    fun `un servicio que arranca durante la pausa cuenta hasta el desbloqueo`() {
        val events = listOf(lock("2026-10-27T10:00"), serviceStart("2026-10-27T10:20"), unlock("2026-10-27T11:00"))

        assertEquals(40, PauseAssembler.closedPauses(events, zone).single().serviceMinutes)
    }

    @Test
    fun `dos servicios superpuestos no cuentan dos veces`() {
        val events = listOf(
            lock("2026-10-27T10:00"),
            serviceStart("2026-10-27T10:00", key = 1), serviceStart("2026-10-27T10:10", key = 2),
            serviceStop("2026-10-27T10:30", key = 1), serviceStop("2026-10-27T10:40", key = 2),
            unlock("2026-10-27T11:00"),
        )

        assertEquals(40, PauseAssembler.closedPauses(events, zone).single().serviceMinutes)
    }

    @Test
    fun `un servicio que termino antes de la pausa no cuenta`() {
        val events = listOf(serviceStart("2026-10-27T09:40"), serviceStop("2026-10-27T09:55"), lock("2026-10-27T10:00"), unlock("2026-10-27T11:00"))

        assertEquals(0, PauseAssembler.closedPauses(events, zone).single().serviceMinutes)
    }

    private fun screenOff(at: String) = event(at, ScreenEvent.Kind.SCREEN_OFF)
    private fun screenOn(at: String) = event(at, ScreenEvent.Kind.SCREEN_ON)

    @Test
    fun `sin bloqueo de pantalla la pausa va de apagar a prender la pantalla`() {
        // Con el bloqueo en "Ninguno" Android no emite KEYGUARD_SHOWN ni KEYGUARD_HIDDEN
        val events = listOf(screenOff("2026-10-27T10:00"), screenOn("2026-10-27T11:00"))

        assertEquals(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `con bloqueo prender la pantalla sin desbloquear no corta la pausa`() {
        val events = listOf(
            screenOff("2026-10-27T10:00"), lock("2026-10-27T10:00"),
            screenOn("2026-10-27T10:20"), screenOff("2026-10-27T10:21"),
            screenOn("2026-10-27T10:59"), unlock("2026-10-27T11:00"),
        )

        assertEquals(listOf(pause("2026-10-27T10:00", "2026-10-27T11:00")), PauseAssembler.closedPauses(events, zone))
    }

    @Test
    fun `si se prende la pantalla antes de que aparezca el bloqueo la pausa termina ahi`() {
        // "Bloquear 5 s después de apagar": se apagó y se volvió a prender en 3 s
        val events = listOf(screenOff("2026-10-27T10:00:00"), screenOn("2026-10-27T10:00:03"))

        assertEquals(
            listOf(Pause(LocalDateTime.parse("2026-10-27T10:00:00"), LocalDateTime.parse("2026-10-27T10:00:03"))),
            PauseAssembler.closedPauses(events, zone),
        )
    }
}
