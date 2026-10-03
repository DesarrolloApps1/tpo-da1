package com.takeabreak.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.takeabreak.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit

private val Context.dataStore by preferencesDataStore(name = "settings")

// Lo que no es una tabla. TODO: flag de notificaciones y token de sesión cifrado con Keystore
class SettingsDataStore(context: Context) : SettingsRepository {

    private val store = context.dataStore

    override val goalRewardId: Flow<String?> = store.data.map { it[GOAL_REWARD_ID] }
    override val dailyGoal: Flow<Int> = store.data.map { it[DAILY_GOAL] ?: DEFAULT_DAILY_GOAL }
    override val lastCatalogSync: Flow<Instant?> = store.data.map { prefs -> prefs[LAST_CATALOG_SYNC]?.let(Instant::ofEpochMilli) }

    override suspend fun setGoal(rewardId: String) {
        store.edit { it[GOAL_REWARD_ID] = rewardId }
    }

    override suspend fun saveCatalogSync(at: Instant) {
        store.edit { it[LAST_CATALOG_SYNC] = at.toEpochMilli() }
    }

    // La primera vez arranca desde ayer
    override suspend fun checkpoint(): Instant =
        store.data.first()[CHECKPOINT]?.let(Instant::ofEpochMilli) ?: Instant.now().minus(1, ChronoUnit.DAYS)

    override suspend fun saveCheckpoint(at: Instant) {
        store.edit { it[CHECKPOINT] = at.toEpochMilli() }
    }

    private companion object {
        const val DEFAULT_DAILY_GOAL = 60
        val GOAL_REWARD_ID = stringPreferencesKey("goal_reward_id")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val LAST_CATALOG_SYNC = longPreferencesKey("last_catalog_sync")
        val CHECKPOINT = longPreferencesKey("events_checkpoint")
    }
}
