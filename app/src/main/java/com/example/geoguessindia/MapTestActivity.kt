package com.example.geoguessindia

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.geoguessindia.map.MapConfig
import com.example.geoguessindia.map.StadiaTileSource
import com.example.geoguessindia.util.ScoreCalculator
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/** Temporary screen to prove the Stadia map, tap handling and scoring work. */
class MapTestActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var tvInfo: TextView
    private var guessMarker: Marker? = null

    // Pretend the "real" answer is the Taj Mahal
    private val targetLat = 27.1751
    private val targetLng = 78.0421

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapConfig.init(this)                       // must come before the MapView is created
        setContentView(R.layout.activity_map_test)

        mapView = findViewById(R.id.mapView)
        tvInfo = findViewById(R.id.tvInfo)

        if (BuildConfig.STADIA_API_KEY.isBlank()) {
            Toast.makeText(this, "STADIA_API_KEY is missing in local.properties", Toast.LENGTH_LONG).show()
        }

        mapView.setTileSource(StadiaTileSource(MapConfig.TILE_STYLE, BuildConfig.STADIA_API_KEY))
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 3.0
        mapView.maxZoomLevel = 16.0
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        // Start centred on India
        mapView.controller.setZoom(4.5)
        mapView.controller.setCenter(GeoPoint(22.5, 79.0))

        // SYLLABUS: Event handling. This is the osmdroid version of OnMapClickListener.
        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                placeOrMoveGuess(p)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        // Index 0 keeps the tap-catching layer underneath any markers
        mapView.overlays.add(0, MapEventsOverlay(receiver))
    }

    private fun placeOrMoveGuess(p: GeoPoint) {
        // Place the marker the first time, then just move it
        val marker = guessMarker ?: Marker(mapView).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            it.title = "Your guess"
            mapView.overlays.add(it)
            guessMarker = it
        }
        marker.position = p
        mapView.invalidate()

        val km = ScoreCalculator.distanceKm(p.latitude, p.longitude, targetLat, targetLng)
        val points = ScoreCalculator.score(km)
        tvInfo.text = String.format(
            "Pin: %.4f, %.4f\nDistance to Taj Mahal: %.1f km\nScore: %d / 5000",
            p.latitude, p.longitude, km, points
        )
    }

    // osmdroid needs these lifecycle calls so tile downloading starts and stops properly
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}