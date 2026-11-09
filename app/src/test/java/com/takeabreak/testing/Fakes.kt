package com.takeabreak.testing

import com.takeabreak.domain.model.BreakSession
import com.takeabreak.domain.model.Business
import com.takeabreak.domain.model.Coordinates
import com.takeabreak.domain.model.DeliveryResult
import com.takeabreak.domain.model.RedeemError
import com.takeabreak.domain.model.RedeemResult
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.BreakRepository
import com.takeabreak.domain.repository.NetworkMonitor
import com.takeabreak.domain.repository.PointsRepository
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
    override val declaredBreakAt = MutableStateFlow<Instant?>(null)

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

    override suspend fun declareBreak(at: Instant) {
        declaredBreakAt.value = at
    }

    override suspend fun clearDeclaredBreak() {
        declaredBreakAt.value = null
    }

    val savedCheckpoints = mutableListOf<Instant>()

    override suspend fun checkpoint(): Instant = savedCheckpoints.lastOrNull() ?: Instant.EPOCH

    override suspend fun saveCheckpoint(at: Instant) {
        savedCheckpoints += at
    }
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

    // El canje que devuelve redeem; null hace que falle con nextError
    var nextResult: Redemption? = null
    var nextError = RedeemError.UNKNOWN
    val requestedIds = mutableListOf<String>()
    var refreshCalls = 0

    override fun observeRedemptions(): Flow<List<Redemption>> = redemptions

    override fun observeSpentPoints(): Flow<Int> =
        redemptions.map { list -> list.filter { it.status != RedemptionStatus.REFUNDED }.sumOf { it.points } }

    override suspend fun redeem(id: String, reward: Reward): RedeemResult {
        requestedIds += id
        val result = nextResult ?: return RedeemResult.Failure(nextError)
        redemptions.value += result
        return RedeemResult.Success(result)
    }

    override suspend fun confirmDelivery(id: String, pin: String): DeliveryResult = DeliveryResult.Unknown

    override suspend fun refreshActive() {
        refreshCalls++
    }
}

class FakeRewardRepository : RewardRepository {
    val rewards = MutableStateFlow<List<Reward>>(emptyList())

    override fun observeRewards(): Flow<List<Reward>> = rewards

    override val lastSync = MutableStateFlow<Instant?>(null)

    // Si no es null, actualizar falla con esta excepción
    var refreshError: Exception? = null
    var refreshCalls = 0

    override suspend fun refresh() {
        refreshCalls++
        refreshError?.let { throw it }
    }

    override suspend fun refreshIfStale() = refresh()
}

class FakePointsRepository : PointsRepository {
    override val lastKnownBalance = MutableStateFlow<Int?>(null)
    override val reportedDays = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val reports = mutableListOf<Map<LocalDate, Int>>()

    override suspend fun report(days: Map<LocalDate, Int>) {
        reports += days
        reportedDays.value += days
    }
}

class FakeNetworkMonitor : NetworkMonitor {
    override val isOnline = MutableStateFlow(true)
}

class FakeNotifier : Notifier {
    // Un elemento por aviso, con las pausas que incluyó
    val breaks = mutableListOf<Pair<List<BreakSession>, Reward?>>()

    override suspend fun breaksValidated(sessions: List<BreakSession>, reachedGoal: Reward?) {
        breaks += sessions to reachedGoal
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
