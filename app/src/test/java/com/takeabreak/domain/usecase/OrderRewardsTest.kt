package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.Coordinates
import com.takeabreak.testing.reward
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRewardsTest {

    // Obelisco y dos puntos conocidos de CABA
    private val obelisco = Coordinates(-34.6037, -58.3816)
    private val plazaDeMayo = Coordinates(-34.6083, -58.3712)
    private val palermo = Coordinates(-34.5875, -58.4250)

    private fun at(id: String, where: Coordinates, points: Int = 100, title: String = id) =
        reward(id, points).let { it.copy(title = title, business = it.business.copy(location = where)) }

    @Test
    fun `la distancia entre el Obelisco y Plaza de Mayo es de alrededor de 1 km`() {
        val meters = OrderRewards.distanceMeters(obelisco, plazaDeMayo)

        assertTrue("fueron $meters m", meters in 1000..1200)
    }

    @Test
    fun `con ubicacion ordena por distancia`() {
        val rewards = listOf(at("palermo", palermo), at("plaza", plazaDeMayo))

        val items = OrderRewards(rewards, obelisco, spendable = 500, onlyAffordable = false)

        assertEquals(listOf("plaza", "palermo"), items.map { it.reward.id })
        assertTrue(items.all { it.distanceMeters != null })
    }

    @Test
    fun `sin ubicacion ordena de la A a la Z y no muestra distancias`() {
        val rewards = listOf(at("2", plazaDeMayo, title = "Yoga"), at("1", palermo, title = "café"))

        val items = OrderRewards(rewards, from = null, spendable = 500, onlyAffordable = false)

        assertEquals(listOf("café", "Yoga"), items.map { it.reward.title })
        assertTrue(items.all { it.distanceMeters == null })
    }

    @Test
    fun `me alcanza deja solo lo que se puede pagar con el saldo`() {
        val rewards = listOf(at("barato", palermo, points = 100), at("caro", plazaDeMayo, points = 300))

        val all = OrderRewards(rewards, from = null, spendable = 150, onlyAffordable = false)
        val affordable = OrderRewards(rewards, from = null, spendable = 150, onlyAffordable = true)

        assertEquals(listOf(true, false), all.map { it.affordable })
        assertEquals(listOf("barato"), affordable.map { it.reward.id })
    }
}
