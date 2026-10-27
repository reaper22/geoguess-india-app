package com.example.geoguessindia.util

import android.location.Location
import kotlin.math.max
import kotlin.math.roundToInt


object ScoreCalculator {


    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)  // result[0] is in metres
        return result[0] / 1000.0
    }


    fun score(distanceKm: Double): Int =
        max(0.0, 5000.0 - distanceKm * 2.5).roundToInt()
}