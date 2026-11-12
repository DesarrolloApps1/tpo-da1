package com.takeabreak.domain.model

// Lo que muestra Inicio: saldo, avance del día, meta y lo que ya alcanza
data class HomeSummary(
    // Lo que se muestra como saldo: lo confirmado por el servidor más lo que todavía no se informó
    val balance: Int,
    val pointsToday: Int,
    val dailyGoal: Int,
    val goal: Reward?,
    val streakDays: Int,
    // Premios que se pueden canjear con el saldo confirmado
    val affordable: List<Reward>,
    // Parte de balance ganada en el teléfono que el servidor todavía no confirmó ("por confirmar")
    val pendingPoints: Int = 0,
    // Lo que se puede gastar: lo define el servidor. Sin ninguna sincronización todavía, el cálculo local
    val spendable: Int = balance,
)
