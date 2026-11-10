package com.takeabreak.data.repository

import com.takeabreak.data.remote.BusinessDto
import com.takeabreak.data.remote.CatalogDto
import com.takeabreak.data.remote.RewardDto
import com.takeabreak.domain.model.ServerException
import com.takeabreak.testing.FakeApi
import com.takeabreak.testing.FakeCatalogDao
import com.takeabreak.testing.FakeSettingsRepository
import com.takeabreak.testing.httpError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class RewardRepositoryImplTest {

    private val now = Instant.parse("2026-11-09T12:00:00Z")
    private val dao = FakeCatalogDao()
    private val settings = FakeSettingsRepository()
    private val sinceAsked = mutableListOf<Long?>()

    private val business = BusinessDto("b1", "Café", "Gorriti 4800", "Palermo", 0.0, 0.0, "9 a 20")
    private fun rewardDto(id: String, deleted: Boolean = false) = RewardDto(id, "b1", "Premio", 60, 10, 5L, deleted)

    private fun repository(vararg rewards: RewardDto) = RewardRepositoryImpl(object : FakeApi() {
        override suspend fun rewards(since: Long?): CatalogDto {
            sinceAsked += since
            return CatalogDto(listOf(business), rewards.toList())
        }
    }, dao, settings) { now }

    @Test
    fun `aplica altas y bajas y guarda la hora`() = runTest {
        repository(rewardDto("viejo")).refresh()

        repository(rewardDto("nuevo"), rewardDto("viejo", deleted = true)).refresh()

        assertEquals(setOf("nuevo"), dao.rewards.keys)
        assertEquals(now, settings.lastCatalogSync.value)
    }

    @Test
    fun `pide solo lo que cambio desde el ultimo premio guardado`() = runTest {
        dao.lastUpdateValue = 123L

        repository().refresh()

        assertEquals(listOf<Long?>(123L), sinceAsked)
    }

    @Test
    fun `sin sincronizacion previa actualiza`() = runTest {
        repository().refreshIfStale()

        assertEquals(1, sinceAsked.size)
    }

    @Test
    fun `con menos de una hora no actualiza y con mas si`() = runTest {
        settings.lastCatalogSync.value = now.minus(Duration.ofMinutes(59))
        repository().refreshIfStale()
        assertEquals(0, sinceAsked.size)

        settings.lastCatalogSync.value = now.minus(Duration.ofMinutes(61))
        repository().refreshIfStale()
        assertEquals(1, sinceAsked.size)
    }

    @Test
    fun `un error del servidor no sale como excepcion de Retrofit ni guarda la hora`() = runTest {
        val repo = RewardRepositoryImpl(object : FakeApi() {
            override suspend fun rewards(since: Long?): CatalogDto = throw httpError(503)
        }, dao, settings) { now }

        val error = runCatching { repo.refresh() }.exceptionOrNull() as ServerException

        assertEquals(503, error.code)
        assertNull(settings.lastCatalogSync.value)
    }
}
