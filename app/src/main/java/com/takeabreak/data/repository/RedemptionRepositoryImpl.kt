package com.takeabreak.data.repository

import com.takeabreak.data.local.RedemptionDao
import com.takeabreak.data.mapper.toDomain
import com.takeabreak.data.mapper.toEntity
import com.takeabreak.data.remote.ErrorDto
import com.takeabreak.data.remote.RedemptionRequest
import com.takeabreak.data.remote.TakeABreakApi
import com.takeabreak.data.remote.UseRedemptionRequest
import com.takeabreak.domain.model.DeliveryResult
import com.takeabreak.domain.model.RedeemError
import com.takeabreak.domain.model.RedeemResult
import com.takeabreak.domain.model.Redemption
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.domain.model.Reward
import com.takeabreak.domain.repository.RedemptionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

// Cada vez que el servidor mueve puntos se pide actualizar el saldo guardado
class RedemptionRepositoryImpl(
    private val api: TakeABreakApi,
    private val dao: RedemptionDao,
    private val syncPoints: () -> Unit,
) : RedemptionRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeRedemptions(): Flow<List<Redemption>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSpentPoints(): Flow<Int> = dao.observeSpentPoints()

    // Recién con la respuesta se guarda: sin red no queda nada y no se descuentan puntos
    override suspend fun redeem(id: String, reward: Reward): RedeemResult = try {
        val entity = api.createRedemption(RedemptionRequest(id, reward.id, reward.points)).toEntity()
        dao.upsert(entity)
        syncPoints()
        RedeemResult.Success(entity.toDomain())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        RedeemResult.Failure(
            when {
                e.code() == 409 && e.error()?.error == "out_of_stock" -> RedeemError.OUT_OF_STOCK
                e.code() == 422 -> RedeemError.INSUFFICIENT_BALANCE
                e.code() == 404 -> RedeemError.REWARD_UNAVAILABLE
                else -> RedeemError.UNKNOWN
            },
        )
    } catch (_: IOException) {
        RedeemResult.Failure(RedeemError.NO_CONNECTION)
    } catch (_: Exception) {
        RedeemResult.Failure(RedeemError.UNKNOWN)
    }

    override suspend fun confirmDelivery(id: String, pin: String): DeliveryResult = try {
        val entity = api.useRedemption(id, UseRedemptionRequest(pin)).toEntity()
        dao.upsert(entity)
        DeliveryResult.Delivered(entity.toDomain())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        deliveryFailure(id, e)
    } catch (_: IOException) {
        DeliveryResult.NoConnection
    } catch (_: Exception) {
        DeliveryResult.Unknown
    }

    override suspend fun refreshActive() {
        var changed = false
        dao.active().forEach { active ->
            // Sin red, con un canje que el servidor no conoce o con una respuesta que no es del backend
            // (por ejemplo, el portal de un Wi-Fi público) queda lo guardado
            val entity = runCatchingNetwork { api.redemption(active.id).toEntity() } ?: return@forEach
            dao.upsert(entity)
            changed = changed || entity.status != active.status
        }
        if (changed) syncPoints()
    }

    private suspend fun deliveryFailure(id: String, e: HttpException): DeliveryResult {
        val error = e.error()
        return when {
            e.code() == 403 && error?.attemptsLeft != null -> DeliveryResult.WrongPin(error.attemptsLeft)
            e.code() == 404 -> DeliveryResult.NotFound
            error?.error == "already_used" -> {
                dao.updateStatus(id, RedemptionStatus.USED)
                DeliveryResult.AlreadyUsed
            }
            error?.error == "rejected" -> {
                dao.updateStatus(id, RedemptionStatus.REFUNDED)
                syncPoints()
                DeliveryResult.Rejected
            }
            error?.error == "expired" -> {
                dao.updateStatus(id, RedemptionStatus.EXPIRED)
                syncPoints()
                DeliveryResult.Expired
            }
            else -> DeliveryResult.Unknown
        }
    }

    private fun HttpException.error(): ErrorDto? = runCatching {
        json.decodeFromString<ErrorDto>(response()?.errorBody()?.string().orEmpty())
    }.getOrNull()

    private suspend fun <T> runCatchingNetwork(call: suspend () -> T): T? = try {
        call()
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        null
    } catch (_: HttpException) {
        null
    } catch (_: SerializationException) {
        null
    }
}
