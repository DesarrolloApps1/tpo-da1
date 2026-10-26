package com.takeabreak.domain.model

// Lo que muestra Inicio: saldo, avance del día, meta y lo que ya alcanza
data class HomeSummary(
    val balance: Int,
    val pointsToday: Int,
    val dailyGoal: Int,
    val goal: Reward?,
    val streakDays: Int,
    val affordable: List<Reward>,
)
