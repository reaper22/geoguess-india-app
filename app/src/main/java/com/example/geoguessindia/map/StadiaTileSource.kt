package com.example.geoguessindia.map

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex


class StadiaTileSource(style: String, private val apiKey: String) :
    OnlineTileSourceBase(
        "Stadia-$style",                                   // name (also the cache key)
        0, 18,                                             // min / max zoom
        256,                                               // tile size in pixels
        ".png",                                            // file ending
        arrayOf("https://tiles.stadiamaps.com/tiles/$style/")
    ) {

    override fun getTileURLString(pMapTileIndex: Long): String {
        return baseUrl +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) +
                mImageFilenameEnding +
                "?api_key=" + apiKey
    }
}