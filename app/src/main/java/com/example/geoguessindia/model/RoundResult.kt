package com.example.geoguessindia.model


data class RoundResult(
    val placeName: String,
    val distanceKm: Double,
    val points: Int,
    val lat: Double,
    val lng: Double,
    val learnMoreUrl: String
)