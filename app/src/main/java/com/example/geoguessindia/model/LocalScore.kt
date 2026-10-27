package com.example.geoguessindia.model


data class LocalScore(
    val id: Long,
    val player: String,
    val score: Int,
    val difficulty: String,
    val date: String
)