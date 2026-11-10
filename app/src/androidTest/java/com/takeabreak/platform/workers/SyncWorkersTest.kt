package com.takeabreak.platform.workers

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import com.takeabreak.TakeABreakApp
import com.takeabreak.data.local.BreakSessionEntity
import com.takeabreak.data.local.KeystoreTokenCipher
import com.takeabreak.data.local.SessionStore
import com.takeabreak.data.local.dataStore
import com.takeabreak.di.AppContainer
import com.takeabreak.domain.model.BreakStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class SyncWorkersTest {

    private val app: TakeABreakApp = ApplicationProvider.getApplicationContext()
    private val server = MockWebServer()
    private val bodies = CopyOnWriteArrayList<String>()

    // Por ruta y no por cola: una tarea periódica de la app no le puede robar la respuesta al test
    private var pointsResponse = MockResponse().setResponseCode(200).setBody("""{"balance":75}""")
    private var catalogResponse = MockResponse().setResponseCode(200).setBody(CATALOG)

    private val session by lazy { SessionStore(app.dataStore, KeystoreTokenCipher()) }

    @Before
    fun setUp() = runBlocking<Unit> {
        WorkManager.getInstance(app).cancelAllWork().result.get()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path.orEmpty().startsWith("/points/daily") -> {
                    bodies += request.body.readUtf8()
                    pointsResponse
                }
                request.path.orEmpty().startsWith("/rewards") -> catalogResponse
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
        app.container = AppContainer(app, server.url("/").toString())
        app.container.database.clearAllTables()
        app.dataStore.edit { it.clear() }
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        app.container = AppContainer(app)
    }

    private suspend fun earnToday(points: Int) {
        val start = LocalDate.now().atTime(0, 1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        app.container.database.breakDao().upsert(listOf(BreakSessionEntity(start, start + 60_000, 1, points, BreakStatus.VALID, 0)))
    }

    private suspend fun runPointsWorker() = TestListenableWorkerBuilder<PointsSyncWorker>(app).build().doWork()

    private suspend fun runCatalogWorker() = TestListenableWorkerBuilder<CatalogSyncWorker>(app).build().doWork()

    @Test
    fun pointsSyncWorkerMarcaLosDiasInformadosYGuardaElSaldo() = runBlocking {
        earnToday(75)

        assertEquals(Result.success(), runPointsWorker())

        assertEquals("""[{"day":"${LocalDate.now()}","points":75}]""", bodies.single())
        assertEquals(mapOf(LocalDate.now().toString() to 75), session.reportedDays.first())
        assertEquals(75, session.lastKnownBalance.first())
    }

    @Test
    fun pointsSyncWorkerNoVuelveAMandarUnDiaYaInformado() = runBlocking {
        earnToday(75)
        runPointsWorker()

        runPointsWorker()

        assertEquals("[]", bodies.last())
        assertEquals(75, session.reportedDays.first().values.single())
    }

    @Test
    fun pointsSyncWorkerReintentaSiElServidorEstaCaido() = runBlocking {
        earnToday(75)
        pointsResponse = MockResponse().setResponseCode(503)

        assertEquals(Result.retry(), runPointsWorker())
        assertTrue(session.reportedDays.first().isEmpty())
    }

    @Test
    fun pointsSyncWorkerNoReintentaUnRechazoDelServidor() = runBlocking {
        earnToday(75)
        pointsResponse = MockResponse().setResponseCode(400)

        assertEquals(Result.failure(), runPointsWorker())
    }

    @Test
    fun pointsSyncWorkerReintentaSinRed() = runBlocking {
        earnToday(75)
        server.shutdown()

        assertEquals(Result.retry(), runPointsWorker())
    }

    @Test
    fun catalogSyncWorkerActualizaLaUltimaSincronizacion() = runBlocking {
        assertEquals(Result.success(), runCatalogWorker())

        assertNotNull(app.container.settings.lastCatalogSync.first())
        assertEquals(listOf("r1"), app.container.database.catalogDao().observeRewards().first().map { it.reward.id })
    }

    @Test
    fun catalogSyncWorkerReintentaSiElServidorEstaCaido() = runBlocking {
        catalogResponse = MockResponse().setResponseCode(500)

        assertEquals(Result.retry(), runCatalogWorker())
        assertEquals(null, app.container.settings.lastCatalogSync.first())
    }

    private companion object {
        const val CATALOG = """{"businesses":[{"id":"b1","name":"Cafe","address":"Gorriti","neighborhood":"Palermo","lat":0.0,"lng":0.0,"openingHours":"9 a 20"}],
            "rewards":[{"id":"r1","businessId":"b1","title":"Cafe","points":60,"dailyStock":10,"updatedAt":5,"deleted":false}]}"""
    }
}
