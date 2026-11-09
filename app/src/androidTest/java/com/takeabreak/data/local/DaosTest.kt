package com.takeabreak.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.takeabreak.domain.model.BreakStatus
import com.takeabreak.domain.model.RedemptionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaosTest {

    private lateinit var db: AppDatabase

    private val business = BusinessEntity("b1", "Café", "Gorriti", "Palermo", 0.0, 0.0, "9 a 20")
    private fun reward(id: String, updatedAt: Long = 1L) = RewardEntity(id, "b1", "Premio $id", 60, 10, updatedAt)
    private fun session(startedAt: Long, points: Int) = BreakSessionEntity(startedAt, startedAt + 60_000, 1, points, BreakStatus.VALID, 0)
    private fun redemption(id: String, status: RedemptionStatus, rewardId: String = "r1") =
        RedemptionEntity(id, rewardId, 60, "ABC234", status, 1L, 2L)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun guardarDosVecesLaMismaPausaNoLaDuplica() = runBlocking {
        db.breakDao().upsert(listOf(session(1_000, 60)))
        db.breakDao().upsert(listOf(session(1_000, 60)))

        assertEquals(1, db.breakDao().observeFrom(0).first().size)
        assertEquals(60, db.breakDao().observeTotalPoints().first())
    }

    @Test
    fun losPuntosDeUnRangoIncluyenElInicioYNoElFin() = runBlocking {
        db.breakDao().upsert(listOf(session(1_000, 10), session(2_000, 20), session(3_000, 40)))

        assertEquals(30, db.breakDao().pointsBetween(1_000, 3_000))
    }

    @Test
    fun elCatalogoAplicaAltasCambiosYBajasJuntos() = runBlocking {
        val dao = db.catalogDao()
        dao.applyCatalog(listOf(business), listOf(reward("r1"), reward("r2")), emptyList())

        dao.applyCatalog(emptyList(), listOf(reward("r1", updatedAt = 9L)), listOf("r2"))

        val rewards = dao.observeRewards().first()
        assertEquals(listOf("r1"), rewards.map { it.reward.id })
        assertEquals("b1", rewards.single().business.id)
        assertEquals(9L, dao.lastUpdate())
    }

    @Test
    fun unPremioNoPuedeExistirSinSuComercio() {
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { db.catalogDao().applyCatalog(emptyList(), listOf(reward("r1")), emptyList()) }
        }
    }

    @Test
    fun siFallaElCatalogoNoQuedaNadaAMedias() = runBlocking {
        val dao = db.catalogDao()

        runCatching { dao.applyCatalog(listOf(business), listOf(reward("r1").copy(businessId = "no-existe")), emptyList()) }

        assertTrue(dao.observeRewards().first().isEmpty())
        assertEquals(null, dao.lastUpdate())
    }

    @Test
    fun noSePuedeBorrarUnComercioQueTienePremios() = runBlocking<Unit> {
        db.catalogDao().applyCatalog(listOf(business), listOf(reward("r1")), emptyList())

        assertThrows(SQLiteConstraintException::class.java) {
            db.openHelper.writableDatabase.execSQL("DELETE FROM businesses WHERE id = 'b1'")
        }
    }

    @Test
    fun elCanjeSobreviveAunqueElPremioSalgaDelCatalogo() = runBlocking {
        db.catalogDao().applyCatalog(listOf(business), listOf(reward("r1")), emptyList())
        db.redemptionDao().upsert(redemption("c1", RedemptionStatus.ACTIVE))

        db.catalogDao().applyCatalog(emptyList(), emptyList(), listOf("r1"))

        assertEquals(listOf("c1"), db.redemptionDao().observeAll().first().map { it.id })
    }

    @Test
    fun losCanjesDevueltosYVencidosNoCuentanComoGastados() = runBlocking {
        val dao = db.redemptionDao()
        dao.upsert(redemption("activo", RedemptionStatus.ACTIVE))
        dao.upsert(redemption("usado", RedemptionStatus.USED))
        dao.upsert(redemption("vencido", RedemptionStatus.EXPIRED))
        dao.upsert(redemption("devuelto", RedemptionStatus.REFUNDED))

        assertEquals(120, dao.observeSpentPoints().first())
        assertEquals(listOf("activo"), dao.active().map { it.id })
    }

    @Test
    fun actualizarElEstadoDeUnCanje() = runBlocking {
        val dao = db.redemptionDao()
        dao.upsert(redemption("c1", RedemptionStatus.ACTIVE))

        dao.updateStatus("c1", RedemptionStatus.USED)

        assertEquals(RedemptionStatus.USED, dao.observeAll().first().single().status)
    }
}
