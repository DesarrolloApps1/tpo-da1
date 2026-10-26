package com.takeabreak.domain.model

data class Coordinates(val lat: Double, val lng: Double)

data class Business(
    val id: String,
    val name: String,
    val address: String,
    val neighborhood: String,
    val location: Coordinates,
    val openingHours: String,
)
