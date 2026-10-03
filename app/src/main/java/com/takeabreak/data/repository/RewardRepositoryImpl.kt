package com.takeabreak.data.repository

import com.takeabreak.data.local.CatalogDao
import com.takeabreak.data.mapper.toDomain
import com.takeabreak.data.mapper.toEntity
import com.takeabreak.data.remote.TakeABreakApi
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.RewardRepository
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

// Room es la única fuente de verdad: la red solo actualiza Room
class RewardRepositoryImpl(
    private val api: TakeABreakApi,
    private val dao: CatalogDao,
    private val settings: SettingsRepository,
) : RewardRepository {

    override fun observeRewards(): Flow<List<Reward>> =
        dao.observeRewards().map { list -> list.map { it.toDomain() } }

    // TODO: borrar los premios dados de baja cuando el backend los informe
    override suspend fun refresh() {
        val catalog = api.rewards(since = dao.lastUpdate())
        dao.upsertCatalog(catalog.businesses.map { it.toEntity() }, catalog.rewards.map { it.toEntity() })
        settings.saveCatalogSync(Instant.now())
    }
}
