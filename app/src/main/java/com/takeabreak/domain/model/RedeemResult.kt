package com.takeabreak.domain.model

// Resultado de un canje; la UI elige el mensaje según el error
sealed interface RedeemResult {
    data class Success(val redemption: Redemption) : RedeemResult
    data class Failure(val error: RedeemError) : RedeemResult
}

enum class RedeemError {
    NO_CONNECTION,
    OUT_OF_STOCK,
    INSUFFICIENT_BALANCE,
    REWARD_UNAVAILABLE,
    UNKNOWN,
}

// Resultado de confirmar la entrega con el PIN del comercio
sealed interface DeliveryResult {
    data class Delivered(val redemption: Redemption) : DeliveryResult
    data class WrongPin(val attemptsLeft: Int) : DeliveryResult
    data object Rejected : DeliveryResult
    data object AlreadyUsed : DeliveryResult
    data object Expired : DeliveryResult
    data object NotFound : DeliveryResult
    data object NoConnection : DeliveryResult
    data object Unknown : DeliveryResult
}
