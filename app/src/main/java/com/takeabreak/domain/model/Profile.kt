package com.takeabreak.domain.model

// Lo que muestra Perfil (frame 25)
data class Profile(
    val account: Account?,
    val totalEarned: Int,
    val redemptionCount: Int,
    val activeRedemptions: Int,
    val goal: Reward?,
    val dailyGoal: Int,
    val notificationsEnabled: Boolean,
    val catalog: List<Reward>,
)
