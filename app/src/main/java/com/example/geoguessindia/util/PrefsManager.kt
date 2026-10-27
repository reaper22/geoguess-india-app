package com.example.geoguessindia.util

import android.content.Context
import android.content.SharedPreferences


class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("geoguess_prefs", Context.MODE_PRIVATE)

    var playerName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var lastDifficulty: String
        get() = prefs.getString(KEY_DIFFICULTY, "Easy") ?: "Easy"
        set(value) = prefs.edit().putString(KEY_DIFFICULTY, value).apply()

    var soundOn: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var bestScore: Int
        get() = prefs.getInt(KEY_BEST, 0)
        private set(value) = prefs.edit().putInt(KEY_BEST, value).apply()


    fun updateBestScore(score: Int): Boolean {
        if (score > bestScore) {
            bestScore = score
            return true
        }
        return false
    }

    private companion object {
        const val KEY_NAME = "player_name"
        const val KEY_DIFFICULTY = "last_difficulty"
        const val KEY_SOUND = "sound_on"
        const val KEY_BEST = "best_score"
    }
}