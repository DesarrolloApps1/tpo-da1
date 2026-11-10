package com.takeabreak.data.repository

import com.takeabreak.data.local.CatalogDao
import com.takeabreak.data.mapper.toDomain
import com.takeabreak.data.mapper.toEntity
import com.takeabreak.data.remote.TakeABreakApi
import com.takeabreak.data.remote.withServerErrors
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.RewardRepository
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant

// Room es la única fuente de verdad: la red solo actualiza Room
class RewardRepositoryImpl(
    private val api: TakeABreakApi,
    private val dao: CatalogDao,
    private val settings: SettingsRepository,
    private val now: () -> Instant = Instant::now,
) : RewardRepository {

    override fun observeRewards(): Flow<List<Reward>> =
        dao.observeRewards().map { list -> list.map { it.toDomain() } }

    override val lastSync: Flow<Instant?> = settings.lastCatalogSync

    override suspend fun refresh() {
        val catalog = withServerErrors { api.rewards(since = dao.lastUpdate()) }
        val (deleted, current) = catalog.rewards.partition { it.deleted }
        dao.applyCatalog(catalog.businesses.map { it.toEntity() }, current.map { it.toEntity() }, deleted.map { it.id })
        settings.saveCatalogSync(now())
    }

    override suspend fun refreshIfStale() {
        val last = settings.lastCatalogSync.first()
        if (last == null || Duration.between(last, now()) > STALE_AFTER) refresh()
    }

    private companion object {
        val STALE_AFTER: Duration = Duration.ofHours(1)
    }
}
