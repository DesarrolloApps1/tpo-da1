package com.takeabreak.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinksTest {

    @Test
    fun `pausa validada sin meta alcanzada abre Inicio`() {
        assertEquals("takeabreak://home", DeepLinks.afterBreak(reachedGoalId = null))
    }

    @Test
    fun `pausa validada con la meta alcanzada abre el premio meta`() {
        assertEquals("takeabreak://reward/yoga-1", DeepLinks.afterBreak(reachedGoalId = "yoga-1"))
    }

    @Test
    fun `el link del premio coincide con el patron del grafo`() {
        // navDeepLink<Route.RewardDetail>(basePath = REWARD) arma REWARD/{id}
        assertEquals("${DeepLinks.REWARD}/abc", DeepLinks.reward("abc"))
    }
}
