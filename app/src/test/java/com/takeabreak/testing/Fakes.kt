package com.takeabreak.testing

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.Business
import com.takeabreak.domain.model.Coordinates
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.repository.RedemptionRepository
import com.takeabreak.domain.repository.RewardRepository
import com.takeabreak.domain.repository.SettingsRepository
import com.takeabreak.platform.notifications.Notifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

// Repositorios en memoria para los tests de Perfil, canje y notificaciones

class FakeSettingsRepository : SettingsRepository {
    override val goalRewardId = MutableStateFlow<String?>(null)
    override val dailyGoal = MutableStateFlow(60)
    override val lastCatalogSync = MutableStateFlow<Instant?>(null)
    override val notificationsEnabled = MutableStateFlow(true)

    override suspend fun setGoal(rewardId: String) {
        goalRewardId.value = rewardId
    }

    override suspend fun setDailyGoal(minutes: Int) {
        dailyGoal.value = minutes
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled.value = enabled
    }

    override suspend fun saveCatalogSync(at: Instant) {
        lastCatalogSync.value = at
    }

    override suspend fun checkpoint(): Instant = Instant.EPOCH

    override suspend fun saveCheckpoint(at: Instant) = Unit
}

class FakeBreakRepository : BreakRepository {
    val sessions = MutableStateFlow<List<BreakSession>>(emptyList())

    override fun observeSince(day: LocalDate): Flow<List<BreakSession>> =
        sessions.map { list -> list.filter { !it.start.toLocalDate().isBefore(day) } }

    override fun observeTotalPoints(): Flow<Int> = sessions.map { list -> list.sumOf { it.points } }

    override suspend fun pointsOn(day: LocalDate): Int = sessions.value.filter { it.start.toLocalDate() == day }.sumOf { it.points }

    override suspend fun save(sessions: List<BreakSession>) {
        this.sessions.value += sessions
    }
}

class FakeRedemptionRepository : RedemptionRepository {
    val redemptions = MutableStateFlow<List<Redemption>>(emptyList())

    // El canje que devuelve redeem; null hace que falle
    var nextResult: Redemption? = null

    override fun observeRedemptions(): Flow<List<Redemption>> = redemptions

    override fun observeSpentPoints(): Flow<Int> =
        redemptions.map { list -> list.filter { it.status != RedemptionStatus.REFUNDED }.sumOf { it.points } }

    override suspend fun redeem(reward: Reward): Redemption {
        val result = checkNotNull(nextResult) { "sin respuesta" }
        redemptions.value += result
        return result
    }

    override suspend fun syncPending() = Unit
}

class FakeRewardRepository : RewardRepository {
    val rewards = MutableStateFlow<List<Reward>>(emptyList())

    override fun observeRewards(): Flow<List<Reward>> = rewards

    override suspend fun refresh() = Unit
}

class FakeNetworkMonitor : NetworkMonitor {
    override val isOnline = MutableStateFlow(true)
}

class FakeNotifier : Notifier {
    val breaks = mutableListOf<Pair<BreakSession, Reward?>>()
    val redemptions = mutableListOf<Redemption>()

    override suspend fun breakValidated(session: BreakSession, reachedGoal: Reward?) {
        breaks += session to reachedGoal
    }

    override suspend fun redemptionConfirmed(redemption: Redemption) {
        redemptions += redemption
    }
}

fun reward(id: String, points: Int = 100) = Reward(
    id = id,
    business = Business("b-$id", "Comercio $id", "Calle 123", "Palermo", Coordinates(0.0, 0.0), "9 a 20"),
    title = "Premio $id",
    points = points,
    dailyStock = 5,
)

fun redemption(id: String, points: Int = 100, status: RedemptionStatus = RedemptionStatus.ACTIVE) = Redemption(
    id = id,
    rewardId = "r-$id",
    points = points,
    code = "TAB-$id",
    status = status,
    createdAt = Instant.EPOCH,
    expiresAt = null,
)
