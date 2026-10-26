package com.takeabreak.ui.navigation

// URIs que abren la app en una pantalla: el grafo las declara y las notificaciones las usan
object DeepLinks {
    const val HOME = "takeabreak://home"
    const val REWARD = "takeabreak://reward"
    const val REDEMPTIONS = "takeabreak://redemptions"

    fun reward(id: String) = "$REWARD/$id"

    // Pausa validada: abre Inicio o, si ya alcanza para la meta, el premio meta
    fun afterBreak(reachedGoalId: String?): String = reachedGoalId?.let(::reward) ?: HOME
}
