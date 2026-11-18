package com.takeabreak.presentation.deliver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.takeabreak.di.appContainer
import com.takeabreak.domain.model.DeliveryResult
import com.takeabreak.domain.usecase.ConfirmDelivery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Pantalla que usa el cajero: escribe el PIN del local en el teléfono del cliente
class DeliverViewModel(
    private val redemptionId: String,
    private val confirmDelivery: ConfirmDelivery,
) : ViewModel() {

    private val _state = MutableStateFlow(DeliverState())
    val state: StateFlow<DeliverState> = _state.asStateFlow()

    fun digit(value: Char) {
        val current = _state.value
        if (current.phase != Phase.Entering || current.pin.length >= PIN_LENGTH) return
        _state.value = current.copy(pin = current.pin + value)
        if (current.pin.length + 1 == PIN_LENGTH) submit()
    }

    fun delete() {
        val current = _state.value
        if (current.phase == Phase.Entering) _state.value = current.copy(pin = current.pin.dropLast(1))
    }

    private fun submit() {
        _state.value = _state.value.copy(phase = Phase.Sending)
        viewModelScope.launch {
            val pin = _state.value.pin
            _state.value = when (val result = confirmDelivery(redemptionId, pin)) {
                is DeliveryResult.Delivered -> _state.value.copy(phase = Phase.Delivered)
                // Un PIN mal escrito no cierra nada: se borra y se cuentan los intentos que quedan
                is DeliveryResult.WrongPin -> _state.value.copy(pin = "", phase = Phase.Entering, attemptsLeft = result.attemptsLeft)
                DeliveryResult.Rejected -> _state.value.copy(pin = "", phase = Phase.Rejected)
                DeliveryResult.AlreadyUsed -> _state.value.copy(pin = "", phase = Phase.AlreadyUsed)
                DeliveryResult.Expired -> _state.value.copy(pin = "", phase = Phase.Expired)
                DeliveryResult.NotFound -> _state.value.copy(pin = "", phase = Phase.Expired)
                DeliveryResult.NoConnection -> _state.value.copy(pin = "", phase = Phase.NoConnection)
                DeliveryResult.Unknown -> _state.value.copy(pin = "", phase = Phase.Failed)
            }
        }
    }

    enum class Phase { Entering, Sending, Delivered, Rejected, AlreadyUsed, Expired, NoConnection, Failed }

    companion object {
        const val PIN_LENGTH = 4

        val Factory = viewModelFactory {
            initializer {
                val id = requireNotNull(createSavedStateHandle().get<String>("id"))
                appContainer.run { DeliverViewModel(id, confirmDelivery) }
            }
        }
    }
}

data class DeliverState(
    val pin: String = "",
    val attemptsLeft: Int? = null,
    val phase: DeliverViewModel.Phase = DeliverViewModel.Phase.Entering,
)
