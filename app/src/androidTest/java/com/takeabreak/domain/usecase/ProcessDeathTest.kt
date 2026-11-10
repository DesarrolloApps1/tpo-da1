package com.takeabreak.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.takeabreak.data.local.AppDatabase
import com.takeabreak.data.local.SettingsDataStore
import com.takeabreak.data.repository.BreakRepositoryImpl
import com.takeabreak.domain.model.Pause
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// RNF04: el proceso muere entre guardar la pausa y mover el checkpoint. La muerte se simula cortando la
// ejecución en ese punto y volviendo a abrir la base en disco, como haría el proceso nuevo
@RunWith(AndroidJUnit4::class)
class ProcessDeathTest {

    private class ProcessDeath : RuntimeException()

    private class DiesBeforeCheckpoint(private val delegate: SettingsRepository) : SettingsRepository by delegate {
        override suspend fun saveCheckpoint(at: Instant) = throw ProcessDeath()
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "rnf04-test.db"

    // 27/10/2026 es martes
    private val start = LocalDateTime.of(2026, 10, 27, 10, 0)
    private val pause = Pause(start, start.plusHours(1))

    private fun openDb() = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun siElProcesoMuereAntesDelCheckpointLaPausaNoSePierdeNiSeDuplica() = runBlocking {
        val settings = SettingsDataStore(context)
        val before = Instant.parse("2026-10-20T00:00:00Z")
        settings.saveCheckpoint(before)

        val firstProcess = openDb()
        val died = runCatching {
            SyncBreaks(BreakRepositoryImpl(firstProcess.breakDao()), DiesBeforeCheckpoint(settings))(listOf(pause))
        }
        firstProcess.close()
        assertTrue(died.exceptionOrNull() is ProcessDeath)

        val secondProcess = openDb()
        val breaks = BreakRepositoryImpl(secondProcess.breakDao())
        assertEquals(60, breaks.pointsOn(start.toLocalDate()))
        assertEquals(before, settings.checkpoint())

        // Como el checkpoint no avanzó, el proceso nuevo vuelve a leer la misma pausa
        SyncBreaks(breaks, settings)(listOf(pause))

        assertEquals(60, breaks.pointsOn(start.toLocalDate()))
        assertNotEquals(before, settings.checkpoint())
        assertEquals(pause.end.atZone(ZoneId.systemDefault()).toInstant(), settings.checkpoint())
        secondProcess.close()
    }
}
