package com.takeabreak.platform.usage

import com.takeabreak.domain.model.Pause
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class PauseAssemblerTest {

    private val zone = ZoneOffset.UTC

    private fun lock(at: String) = ScreenEvent(LocalDateTime.parse(at).toInstant(zone), locked = true)
    private fun unlock(at: String) = ScreenEvent(LocalDateTime.parse(at).toInstant(zone), locked = false)
    private fun pause(start: String, end: String) = Pause(LocalDateTime.parse(start), LocalDateTime.parse(end))

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
