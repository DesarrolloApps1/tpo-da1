package com.takeabreak.domain.model

data class Reward(
    val id: String,
    val business: Business,
    val title: String,
    val points: Int,
    val dailyStock: Int,
)
