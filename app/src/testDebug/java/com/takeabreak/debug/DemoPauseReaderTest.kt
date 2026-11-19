package com.takeabreak.debug

import com.takeabreak.domain.model.Pause
import com.takeabreak.platform.usage.UsageEventsReader
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoPauseReaderTest {

    private class MemoryStore : DemoPauseStore {
        var pending: Pause? = null
        override fun put(pause: Pause) {
            pending = pause
        }
        override fun take(): Pause? = pending.also { pending = null }
    }

    private val real = Pause(LocalDateTime.parse("2026-10-27T10:00"), LocalDateTime.parse("2026-10-27T11:00"))
    private val demo = Pause(LocalDateTime.parse("2026-10-27T14:00"), LocalDateTime.parse("2026-10-27T15:00"))
    private val store = MemoryStore()
    private val reader = DemoPauseReader(
        object : UsageEventsReader {
            override suspend fun closedPausesSince(checkpoint: Instant) = listOf(real)
        },
        store,
    )

    @Test
    fun `sin pausa simulada devuelve solo las reales`() = runTest {
        assertEquals(listOf(real), reader.closedPausesSince(Instant.EPOCH))
    }

    @Test
    fun `la pausa simulada se suma a las reales una sola vez`() = runTest {
        store.put(demo)

        assertEquals(listOf(real, demo), reader.closedPausesSince(Instant.EPOCH))
        assertEquals(listOf(real), reader.closedPausesSince(Instant.EPOCH))
    }
}
