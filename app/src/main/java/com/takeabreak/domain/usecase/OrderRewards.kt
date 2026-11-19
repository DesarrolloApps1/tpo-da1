package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.Coordinates
import com.takeabreak.domain.model.Reward
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Un premio de la lista de Explorar: a qué distancia está el comercio y si ya alcanza el saldo
data class RewardListItem(val reward: Reward, val distanceMeters: Int?, val affordable: Boolean)

// Orden de Explorar (RF03): por distancia con la ubicación del usuario, o de la A a la Z sin ella.
// onlyAffordable es el filtro "Me alcanza", con el saldo que se puede gastar
object OrderRewards {

    operator fun invoke(
        rewards: List<Reward>,
        from: Coordinates?,
        spendable: Int,
        onlyAffordable: Boolean,
    ): List<RewardListItem> {
        val items = rewards
            .map { RewardListItem(it, from?.let { here -> distanceMeters(here, it.business.location) }, it.points <= spendable) }
            .filter { !onlyAffordable || it.affordable }
        return if (from == null) {
            items.sortedBy { it.reward.title.lowercase() }
        } else {
            items.sortedWith(compareBy<RewardListItem> { it.distanceMeters }.thenBy { it.reward.title.lowercase() })
        }
    }

    // Distancia en línea recta sobre la Tierra (fórmula del haversine)
    fun distanceMeters(a: Coordinates, b: Coordinates): Int {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return (2 * EARTH_RADIUS_METERS * asin(sqrt(h))).roundToInt()
    }

    private const val EARTH_RADIUS_METERS = 6_371_000.0
}
