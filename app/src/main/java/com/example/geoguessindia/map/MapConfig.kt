package com.example.geoguessindia.map

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File


object MapConfig {


    const val TILE_STYLE = "alidade_smooth"

    fun init(context: Context) {
        val cfg = Configuration.getInstance()

        cfg.userAgentValue = context.packageName
        val base = File(context.cacheDir, "osmdroid")
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = File(base, "tiles")
    }
}