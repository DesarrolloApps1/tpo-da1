package com.takeabreak.domain.model

import com.takeabreak.testing.redemption
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RedemptionTest {

    private val expiresAt = Instant.parse("2026-11-09T12:10:00Z")

    @Test
    fun `un canje activo figura vencido desde que pasa la hora`() {
        val active = redemption("a").copy(expiresAt = expiresAt)

        assertEquals(RedemptionStatus.ACTIVE, active.statusAt(expiresAt.minusSeconds(1)))
        assertEquals(RedemptionStatus.EXPIRED, active.statusAt(expiresAt))
    }

    @Test
    fun `un canje usado sigue usado aunque pase la hora`() {
        val used = redemption("a", status = RedemptionStatus.USED).copy(expiresAt = expiresAt)

        assertEquals(RedemptionStatus.USED, used.statusAt(expiresAt.plusSeconds(60)))
    }
}
