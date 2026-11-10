package com.takeabreak.testing

import com.takeabreak.data.local.BusinessEntity
import com.takeabreak.data.local.CatalogDao
import com.takeabreak.data.local.RedemptionDao
import com.takeabreak.data.local.RedemptionEntity
import com.takeabreak.data.local.RewardEntity
import com.takeabreak.data.local.RewardWithBusiness
import com.takeabreak.domain.model.RedemptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

// DAOs en memoria para probar los repositorios en la JVM

class FakeRedemptionDao : RedemptionDao {
    val rows = MutableStateFlow<Map<String, RedemptionEntity>>(emptyMap())

    override fun observeAll(): Flow<List<RedemptionEntity>> = rows.map { it.values.toList() }
    override fun observeSpentPoints(): Flow<Int> = rows.map { it.values.sumOf { row -> row.points } }
    override suspend fun active() = rows.value.values.filter { it.status == RedemptionStatus.ACTIVE }
    override suspend fun updateStatus(id: String, status: RedemptionStatus) {
        rows.value += id to rows.value.getValue(id).copy(status = status)
    }
    override suspend fun upsert(redemption: RedemptionEntity) {
        rows.value += redemption.id to redemption
    }
}

class FakeCatalogDao : CatalogDao() {
    val rewards = mutableMapOf<String, RewardEntity>()
    val businesses = mutableMapOf<String, BusinessEntity>()
    var lastUpdateValue: Long? = null

    override fun observeRewards(): Flow<List<RewardWithBusiness>> = emptyFlow()
    override suspend fun lastUpdate(): Long? = lastUpdateValue
    override suspend fun upsertBusinesses(businesses: List<BusinessEntity>) {
        businesses.forEach { this.businesses[it.id] = it }
    }
    override suspend fun upsertRewards(rewards: List<RewardEntity>) {
        rewards.forEach { this.rewards[it.id] = it }
    }
    override suspend fun deleteRewards(ids: List<String>) {
        ids.forEach { rewards.remove(it) }
    }
}
