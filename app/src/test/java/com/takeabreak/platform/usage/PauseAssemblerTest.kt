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
}
