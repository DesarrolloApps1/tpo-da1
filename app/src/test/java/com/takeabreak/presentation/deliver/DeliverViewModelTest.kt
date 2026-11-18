package com.takeabreak.presentation.deliver

import com.takeabreak.domain.model.DeliveryResult
import com.takeabreak.domain.usecase.ConfirmDelivery
import com.takeabreak.presentation.deliver.DeliverViewModel.Phase
import com.takeabreak.testing.FakeRedemptionRepository
import com.takeabreak.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeliverViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val redemptions = FakeRedemptionRepository()
    private val vm = DeliverViewModel("canje-1", ConfirmDelivery(redemptions))

    private fun type(pin: String) = pin.forEach(vm::digit)

    @Test
    fun `con el cuarto digito manda el PIN una sola vez`() {
        redemptions.nextDelivery = DeliveryResult.Unknown
        type("1234")

        assertEquals(listOf("canje-1" to "1234"), redemptions.deliveries)
    }

    @Test
    fun `con el PIN correcto queda entregado`() {
        redemptions.nextDelivery = DeliveryResult.Delivered(redemption())
        type("1234")

        assertEquals(Phase.Delivered, vm.state.value.phase)
    }

    @Test
    fun `un PIN incorrecto borra lo escrito y muestra los intentos que quedan`() {
        redemptions.nextDelivery = DeliveryResult.WrongPin(attemptsLeft = 3)
        type("1111")

        val state = vm.state.value
        assertEquals(Phase.Entering, state.phase)
        assertEquals("", state.pin)
        assertEquals(3, state.attemptsLeft)
    }

    @Test
    fun `con demasiados intentos queda rechazado y no acepta mas digitos`() {
        redemptions.nextDelivery = DeliveryResult.Rejected
        type("1111")
        type("2222")

        assertEquals(Phase.Rejected, vm.state.value.phase)
        assertEquals(1, redemptions.deliveries.size)
    }

    @Test
    fun `borrar saca el ultimo digito`() {
        type("12")
        vm.delete()

        assertEquals("1", vm.state.value.pin)
        assertTrue(redemptions.deliveries.isEmpty())
    }

    @Test
    fun `sin conexion se puede reintentar con el canje todavia activo`() {
        redemptions.nextDelivery = DeliveryResult.NoConnection
        type("1234")
        assertEquals(Phase.NoConnection, vm.state.value.phase)

        vm.retry()

        assertEquals(Phase.Entering, vm.state.value.phase)
        assertEquals("", vm.state.value.pin)
    }

    @Test
    fun `un canje que no existe no se informa como vencido`() {
        redemptions.nextDelivery = DeliveryResult.NotFound
        type("1234")

        assertEquals(Phase.Failed, vm.state.value.phase)
    }

    private fun redemption() = com.takeabreak.domain.model.Redemption(
        id = "canje-1",
        rewardId = "cafe",
        points = 60,
        code = "ABC234",
        status = com.takeabreak.domain.model.RedemptionStatus.USED,
        createdAt = java.time.Instant.parse("2026-11-18T12:00:00Z"),
        expiresAt = java.time.Instant.parse("2026-11-18T12:10:00Z"),
    )
}
