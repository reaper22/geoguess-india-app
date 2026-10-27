package com.example.geoguessindia.util

import com.example.geoguessindia.model.Landmark

object GamePicker {

    const val ROUNDS = 5


    fun pick(all: List<Landmark>, difficulty: String, region: String): List<Landmark> {
        val pool = when (difficulty) {
            "Easy" -> all.filter { it.difficulty == "Easy" }
            "Medium" -> all.filter { it.difficulty == "Easy" || it.difficulty == "Medium" }
            else -> all
        }
        val regional = if (region == "All") pool else pool.filter { it.region == region }

        val chosen = regional.shuffled().take(ROUNDS).toMutableList()
        if (chosen.size < ROUNDS) {
            chosen += pool.filter { it !in chosen }.shuffled().take(ROUNDS - chosen.size)
        }
        if (chosen.size < ROUNDS) {
            chosen += all.filter { it !in chosen }.shuffled().take(ROUNDS - chosen.size)
        }
        return chosen.shuffled()
    }
}