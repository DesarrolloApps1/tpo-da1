package com.takeabreak.data.repository

import com.takeabreak.data.local.RedemptionDao
import com.takeabreak.data.local.RedemptionEntity
import com.takeabreak.data.remote.RedemptionDto
import com.takeabreak.data.remote.RedemptionRequest
import com.takeabreak.data.remote.UseRedemptionRequest
import com.takeabreak.domain.model.DeliveryResult
import com.takeabreak.domain.model.RedeemError
import com.takeabreak.domain.model.RedeemResult
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.testing.FakeApi
import com.takeabreak.testing.httpError
import com.takeabreak.testing.reward
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RedemptionRepositoryImplTest {

    private class FakeDao : RedemptionDao {
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

    private val dao = FakeDao()
    private var pointsSyncs = 0

    private fun dto(id: String = "c-1", status: String = "ACTIVE") = RedemptionDto(id, "cafe", 100, "ABC234", status, 1L, 2L)

    private fun entity(id: String = "c-1", status: RedemptionStatus = RedemptionStatus.ACTIVE) =
        RedemptionEntity(id, "cafe", 100, "ABC234", status, 1L, 2L)

    private fun repository(api: FakeApi) = RedemptionRepositoryImpl(api, dao) { pointsSyncs++ }

    private fun failingRedeem(error: Exception) = repository(object : FakeApi() {
        override suspend fun createRedemption(body: RedemptionRequest): RedemptionDto = throw error
    })

    private fun failingUse(error: Exception) = repository(object : FakeApi() {
        override suspend fun useRedemption(id: String, body: UseRedemptionRequest): RedemptionDto = throw error
    })

    @Test
    fun `un canje exitoso se guarda activo con el id pedido y actualiza el saldo`() = runTest {
        var sent: RedemptionRequest? = null
        val repo = repository(object : FakeApi() {
            override suspend fun createRedemption(body: RedemptionRequest): RedemptionDto {
                sent = body
                return dto(body.id)
            }
        })

        val result = repo.redeem("c-1", reward("cafe"))

        assertTrue(result is RedeemResult.Success)
        assertEquals("c-1", sent?.id)
        assertEquals(RedemptionStatus.ACTIVE, dao.rows.value.getValue("c-1").status)
        assertEquals(1, pointsSyncs)
    }

    @Test
    fun `sin red no guarda nada y no pide actualizar el saldo`() = runTest {
        val result = failingRedeem(IOException()).redeem("c-1", reward("cafe"))

        assertEquals(RedeemResult.Failure(RedeemError.NO_CONNECTION), result)
        assertTrue(dao.rows.value.isEmpty())
        assertEquals(0, pointsSyncs)
    }

    @Test
    fun `los errores del servidor se traducen a errores de dominio`() = runTest {
        val cases = listOf(
            httpError(409, """{"error":"out_of_stock"}""") to RedeemError.OUT_OF_STOCK,
            httpError(409, """{"error":"id_taken"}""") to RedeemError.UNKNOWN,
            httpError(422, """{"error":"insufficient_balance"}""") to RedeemError.INSUFFICIENT_BALANCE,
            httpError(404, """{"error":"reward_not_found"}""") to RedeemError.REWARD_UNAVAILABLE,
            httpError(500) to RedeemError.UNKNOWN,
        )

        cases.forEach { (error, expected) ->
            assertEquals(RedeemResult.Failure(expected), failingRedeem(error).redeem("c-1", reward("cafe")))
        }
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `el PIN correcto deja el canje usado`() = runTest {
        dao.upsert(entity())
        val repo = repository(object : FakeApi() {
            override suspend fun useRedemption(id: String, body: UseRedemptionRequest) = dto(id, "USED")
        })

        assertTrue(repo.confirmDelivery("c-1", "4821") is DeliveryResult.Delivered)
        assertEquals(RedemptionStatus.USED, dao.rows.value.getValue("c-1").status)
    }

    @Test
    fun `el PIN incorrecto informa los intentos que quedan y no cambia nada`() = runTest {
        dao.upsert(entity())

        val result = failingUse(httpError(403, """{"error":"wrong_pin","attemptsLeft":3}""")).confirmDelivery("c-1", "0000")

        assertEquals(DeliveryResult.WrongPin(3), result)
        assertEquals(RedemptionStatus.ACTIVE, dao.rows.value.getValue("c-1").status)
        assertEquals(0, pointsSyncs)
    }

    @Test
    fun `un canje rechazado queda devuelto y actualiza el saldo`() = runTest {
        dao.upsert(entity())

        val result = failingUse(httpError(409, """{"error":"rejected"}""")).confirmDelivery("c-1", "0000")

        assertEquals(DeliveryResult.Rejected, result)
        assertEquals(RedemptionStatus.REFUNDED, dao.rows.value.getValue("c-1").status)
        assertEquals(1, pointsSyncs)
    }

    @Test
    fun `un canje vencido queda vencido y actualiza el saldo`() = runTest {
        dao.upsert(entity())

        val result = failingUse(httpError(409, """{"error":"expired"}""")).confirmDelivery("c-1", "4821")

        assertEquals(DeliveryResult.Expired, result)
        assertEquals(RedemptionStatus.EXPIRED, dao.rows.value.getValue("c-1").status)
        assertEquals(1, pointsSyncs)
    }

    @Test
    fun `sin red al confirmar no cambia nada`() = runTest {
        dao.upsert(entity())

        assertEquals(DeliveryResult.NoConnection, failingUse(IOException()).confirmDelivery("c-1", "4821"))
        assertEquals(RedemptionStatus.ACTIVE, dao.rows.value.getValue("c-1").status)
    }

    @Test
    fun `refrescar actualiza los activos y si cambio alguno pide actualizar el saldo`() = runTest {
        dao.upsert(entity("c-1"))
        dao.upsert(entity("c-2", RedemptionStatus.USED))
        val asked = mutableListOf<String>()
        val repo = repository(object : FakeApi() {
            override suspend fun redemption(id: String): RedemptionDto {
                asked += id
                return dto(id, "EXPIRED")
            }
        })

        repo.refreshActive()

        assertEquals(listOf("c-1"), asked)
        assertEquals(RedemptionStatus.EXPIRED, dao.rows.value.getValue("c-1").status)
        assertEquals(1, pointsSyncs)
    }

    @Test
    fun `refrescar sin red deja lo guardado`() = runTest {
        dao.upsert(entity())
        val repo = repository(object : FakeApi() {
            override suspend fun redemption(id: String): RedemptionDto = throw IOException()
        })

        repo.refreshActive()

        assertEquals(RedemptionStatus.ACTIVE, dao.rows.value.getValue("c-1").status)
        assertEquals(0, pointsSyncs)
    }
}
