package com.takeabreak.ui.navigation

import kotlinx.serialization.Serializable

// Destinos de la app: tres pestañas, detalle, canje, perfil y primer uso
sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Home : Route
    @Serializable data object Explore : Route
    @Serializable data object Activity : Route
    @Serializable data object Profile : Route
    @Serializable data object Redemptions : Route
    @Serializable data class RewardDetail(val id: String) : Route
    @Serializable data class Redeem(val id: String) : Route
}
