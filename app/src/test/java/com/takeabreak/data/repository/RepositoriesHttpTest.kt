package com.takeabreak.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.takeabreak.data.local.SessionStore
import com.takeabreak.data.local.TokenCipher
import com.takeabreak.data.remote.TakeABreakApi
import com.takeabreak.domain.model.DeliveryResult
import com.takeabreak.domain.model.RedeemError
import com.takeabreak.domain.model.RedeemResult
import com.takeabreak.domain.model.RedemptionStatus
import com.takeabreak.domain.model.ServerException
import com.takeabreak.testing.FakeCatalogDao
import com.takeabreak.testing.FakeRedemptionDao
import com.takeabreak.testing.FakeSettingsRepository
import com.takeabreak.testing.reward
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.io.IOException
import java.time.LocalDate

// Prueba los repositorios con Retrofit de verdad: rutas, cuerpos JSON y códigos HTTP
class RepositoriesHttpTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val redemptionDao = FakeRedemptionDao()
    private val catalogDao = FakeCatalogDao()
    private val settings = FakeSettingsRepository()
    private var pointsSyncs = 0

    private val api: TakeABreakApi by lazy {
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }

    private val session by lazy {
        val store = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("session.preferences_pb").also { it.delete() } }
        SessionStore(store, object : TokenCipher {
            override fun encrypt(plain: String) = plain
            override fun decrypt(encoded: String) = encoded
        })
    }

    private val redemptions by lazy { RedemptionRepositoryImpl(api, redemptionDao) { pointsSyncs++ } }
    private val rewards by lazy { RewardRepositoryImpl(api, catalogDao, settings) }
    private val points by lazy { PointsRepositoryImpl(api, session) }

    private val day = LocalDate.of(2026, 11, 9)

    private fun redemptionJson(id: String = "c-1", status: String = "ACTIVE") =
        """{"id":"$id","rewardId":"cafe","points":60,"code":"ABC234","status":"$status","createdAt":1,"expiresAt":2}"""

    private fun respond(code: Int, body: String = "{}") {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() {
        scope.cancel()
        runCatching { server.shutdown() }
    }

    @Test
    fun `el catalogo pide todo la primera vez y despues solo lo que cambio`() = runBlocking {
        respond(200, """{"businesses":[],"rewards":[]}""")
        rewards.refresh()
        assertEquals("/rewards", server.takeRequest().path)

        catalogDao.lastUpdateValue = 123L
        respond(200, """{"businesses":[],"rewards":[]}""")
        rewards.refresh()

        assertEquals("/rewards?since=123", server.takeRequest().path)
    }

    @Test
    fun `el catalogo aplica altas y bajas que llegan por HTTP`() = runBlocking {
        catalogDao.rewards["viejo"] = com.takeabreak.data.local.RewardEntity("viejo", "b1", "Viejo", 10, 1, 1L)
        respond(
            200,
            """{"businesses":[{"id":"b1","name":"Cafe","address":"Gorriti","neighborhood":"Palermo","lat":0.0,"lng":0.0,"openingHours":"9 a 20"}],
               "rewards":[{"id":"nuevo","businessId":"b1","title":"Nuevo","points":60,"dailyStock":10,"updatedAt":5,"deleted":false},
                          {"id":"viejo","businessId":"b1","title":"Viejo","points":10,"dailyStock":1,"updatedAt":5,"deleted":true}]}""",
        )

        rewards.refresh()

        assertEquals(setOf("nuevo"), catalogDao.rewards.keys)
    }

    @Test
    fun `un error 5xx del catalogo sale como error de dominio`() = runBlocking {
        respond(503)

        val error = runCatching { rewards.refresh() }.exceptionOrNull()

        assertTrue(error is ServerException)
        assertTrue((error as ServerException).isRetryable)
    }

    @Test
    fun `canje 201 manda el id y guarda el canje activo`() = runBlocking {
        respond(201, redemptionJson())

        val result = redemptions.redeem("c-1", reward("cafe", points = 60))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/redemptions", request.path)
        assertTrue(request.body.readUtf8().contains(""""id":"c-1""""))
        assertTrue(result is RedeemResult.Success)
        assertEquals(RedemptionStatus.ACTIVE, redemptionDao.rows.value.getValue("c-1").status)
    }

    @Test
    fun `reintentar un canje manda el mismo id y no duplica nada`() = runBlocking {
        respond(201, redemptionJson())
        respond(200, redemptionJson())

        redemptions.redeem("c-1", reward("cafe", points = 60))
        val retry = redemptions.redeem("c-1", reward("cafe", points = 60))

        assertTrue(retry is RedeemResult.Success)
        assertEquals(server.takeRequest().body.readUtf8(), server.takeRequest().body.readUtf8())
        assertEquals(1, redemptionDao.rows.value.size)
    }

    @Test
    fun `canje sin stock o sin saldo devuelve el error de dominio`() = runBlocking {
        respond(409, """{"error":"out_of_stock"}""")
        assertEquals(RedeemResult.Failure(RedeemError.OUT_OF_STOCK), redemptions.redeem("c-1", reward("cafe")))

        respond(422, """{"error":"insufficient_balance"}""")
        assertEquals(RedeemResult.Failure(RedeemError.INSUFFICIENT_BALANCE), redemptions.redeem("c-2", reward("cafe")))

        assertTrue(redemptionDao.rows.value.isEmpty())
    }

    @Test
    fun `sin red el canje devuelve el error de dominio`() = runBlocking {
        server.shutdown()

        assertEquals(RedeemResult.Failure(RedeemError.NO_CONNECTION), redemptions.redeem("c-1", reward("cafe")))
    }

    @Test
    fun `el PIN correcto llega al servidor y deja el canje usado`() = runBlocking {
        respond(201, redemptionJson())
        redemptions.redeem("c-1", reward("cafe", points = 60))
        server.takeRequest()
        respond(200, redemptionJson(status = "USED"))

        val result = redemptions.confirmDelivery("c-1", "4821")

        val request = server.takeRequest()
        assertEquals("/redemptions/c-1/use", request.path)
        assertEquals("""{"pin":"4821"}""", request.body.readUtf8())
        assertTrue(result is DeliveryResult.Delivered)
        assertEquals(RedemptionStatus.USED, redemptionDao.rows.value.getValue("c-1").status)
    }

    @Test
    fun `el uso con PIN traduce incorrecto, rechazado, vencido y ya usado`() = runBlocking {
        respond(201, redemptionJson())
        redemptions.redeem("c-1", reward("cafe", points = 60))

        respond(403, """{"error":"wrong_pin","attemptsLeft":2}""")
        assertEquals(DeliveryResult.WrongPin(2), redemptions.confirmDelivery("c-1", "0000"))

        respond(409, """{"error":"rejected"}""")
        assertEquals(DeliveryResult.Rejected, redemptions.confirmDelivery("c-1", "0000"))
        assertEquals(RedemptionStatus.REFUNDED, redemptionDao.rows.value.getValue("c-1").status)

        respond(409, """{"error":"expired"}""")
        assertEquals(DeliveryResult.Expired, redemptions.confirmDelivery("c-1", "4821"))
        assertEquals(RedemptionStatus.EXPIRED, redemptionDao.rows.value.getValue("c-1").status)

        respond(409, """{"error":"already_used"}""")
        assertEquals(DeliveryResult.AlreadyUsed, redemptions.confirmDelivery("c-1", "4821"))
        assertEquals(RedemptionStatus.USED, redemptionDao.rows.value.getValue("c-1").status)
    }

    @Test
    fun `los puntos diarios mandan solo dia y total y guardan el saldo`() = runBlocking {
        respond(200, """{"balance":120}""")

        points.report(mapOf(day to 120))

        val request = server.takeRequest()
        assertEquals("/points/daily", request.path)
        assertEquals("""[{"day":"2026-11-09","points":120}]""", request.body.readUtf8())
        assertEquals(120, points.lastKnownBalance.first())
        assertEquals(mapOf(day to 120), points.reportedDays.first())
    }

    @Test
    fun `reenviar los puntos de un dia no duplica nada`() = runBlocking {
        respond(200, """{"balance":120}""")
        respond(200, """{"balance":120}""")

        points.report(mapOf(day to 120))
        points.report(mapOf(day to 120))

        assertEquals(mapOf(day to 120), points.reportedDays.first())
        assertEquals(120, points.lastKnownBalance.first())
    }

    @Test
    fun `si el envio de puntos falla no se marca el dia como informado`() = runBlocking {
        respond(500)
        assertTrue(runCatching { points.report(mapOf(day to 120)) }.exceptionOrNull() is ServerException)

        server.shutdown()
        assertTrue(runCatching { points.report(mapOf(day to 120)) }.exceptionOrNull() is IOException)

        assertEquals(emptyMap<LocalDate, Int>(), points.reportedDays.first())
    }
}
