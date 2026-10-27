package com.example.geoguessindia.model

/** One landmark from places.json (also what we cache in SQLite). */
data class Landmark(
    val id: Int,
    val name: String,
    val region: String,
    val lat: Double,
    val lng: Double,
    val imageUrl: String,
    val hint: String,
    val difficulty: String,
    val fact: String,
    val learnMoreUrl: String
)