package com.example.geoguessindia.util

import android.content.Context
import android.media.MediaPlayer


class SoundPlayer(private val context: Context, var enabled: Boolean) {

    private var player: MediaPlayer? = null

    fun playCorrect() = play("correct")
    fun playWrong() = play("wrong")

    private fun play(name: String) {
        if (!enabled) return
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        if (resId == 0) return
        release()
        player = MediaPlayer.create(context, resId)?.also { mp ->
            mp.setOnCompletionListener {
                it.release()
                if (player === it) player = null
            }
            mp.start()
        }
    }

    fun release() {
        player?.release()
        player = null
    }
}