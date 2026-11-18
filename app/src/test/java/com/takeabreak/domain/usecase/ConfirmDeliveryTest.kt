package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.DeliveryResult
import com.takeabreak.testing.FakeRedemptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfirmDeliveryTest {

    private val redemptions = FakeRedemptionRepository()
    private val confirmDelivery = ConfirmDelivery(redemptions)

    @Test
    fun `manda el canje y el PIN que escribio el cajero`() = runTest {
        confirmDelivery("canje-1", "4821")

        assertEquals(listOf("canje-1" to "4821"), redemptions.deliveries)
    }

    @Test
    fun `devuelve los intentos que quedan si el PIN es incorrecto`() = runTest {
        redemptions.nextDelivery = DeliveryResult.WrongPin(attemptsLeft = 3)

        assertEquals(DeliveryResult.WrongPin(3), confirmDelivery("canje-1", "0000"))
    }

    @Test
    fun `un canje ya usado no se puede volver a entregar`() = runTest {
        redemptions.nextDelivery = DeliveryResult.AlreadyUsed

        assertEquals(DeliveryResult.AlreadyUsed, confirmDelivery("canje-1", "4821"))
    }
}
