package com.example.geoguessindia

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.geoguessindia.db.DbHelper
import com.example.geoguessindia.map.MapConfig
import com.example.geoguessindia.map.StadiaTileSource
import com.example.geoguessindia.model.Landmark
import com.example.geoguessindia.model.RoundResult
import com.example.geoguessindia.net.ImageLoaderTask
import com.example.geoguessindia.net.PlacesLoaderTask
import com.example.geoguessindia.util.AppConfig
import com.example.geoguessindia.util.Extras
import com.example.geoguessindia.util.GamePicker
import com.example.geoguessindia.util.PrefsManager
import com.example.geoguessindia.util.ScoreCalculator
import com.example.geoguessindia.util.SoundPlayer
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline



@Suppress("DEPRECATION")
class GameActivity : AppCompatActivity() {

    private lateinit var db: DbHelper
    private lateinit var prefs: PrefsManager
    private lateinit var sound: SoundPlayer

    private lateinit var mapView: MapView
    private lateinit var imgLandmark: ImageView
    private lateinit var imgProgress: ProgressBar
    private lateinit var tvRound: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvHint: TextView
    private lateinit var btnSubmit: Button
    private var timerDefaultColor = 0

    private lateinit var difficulty: String
    private lateinit var region: String

    private var rounds: List<Landmark> = emptyList()
    private var roundIndex = 0
    private var totalScore = 0
    private val results = ArrayList<RoundResult>()

    private var guess: GeoPoint? = null
    private var guessMarker: Marker? = null
    private val roundOverlays = ArrayList<Overlay>()
    private var roundActive = false

    private var timer: CountDownTimer? = null
    private var imageTask: ImageLoaderTask? = null
    private var placesTask: PlacesLoaderTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapConfig.init(this)
        setContentView(R.layout.activity_game)

        db = DbHelper(this)
        prefs = PrefsManager(this)
        sound = SoundPlayer(this, prefs.soundOn)

        // SYLLABUS: Intents. Read the extras sent by the Home screen
        difficulty = intent.getStringExtra(Extras.DIFFICULTY) ?: "Easy"
        region = intent.getStringExtra(Extras.REGION) ?: "All"

        mapView = findViewById(R.id.mapView)
        imgLandmark = findViewById(R.id.imgLandmark)
        imgProgress = findViewById(R.id.imgProgress)
        tvRound = findViewById(R.id.tvRound)
        tvScore = findViewById(R.id.tvScore)
        tvTimer = findViewById(R.id.tvTimer)
        tvHint = findViewById(R.id.tvHint)
        btnSubmit = findViewById(R.id.btnSubmit)
        timerDefaultColor = tvTimer.currentTextColor

        setupMap()


        btnSubmit.setOnClickListener { submitGuess(timedOut = false) }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmQuit()
        })

        loadPlacesAndStart()
    }



    private fun setupMap() {
        mapView.setTileSource(StadiaTileSource(MapConfig.TILE_STYLE, BuildConfig.STADIA_API_KEY))
        mapView.setMultiTouchControls(true)
        mapView.minZoomLevel = 3.0
        mapView.maxZoomLevel = 16.0
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        resetMapView()


        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onMapTap(p)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        mapView.overlays.add(0, MapEventsOverlay(receiver))
    }

    private fun resetMapView() {
        mapView.controller.setZoom(4.5)
        mapView.controller.setCenter(GeoPoint(22.5, 79.0))     // centre of India
    }


    private fun loadPlacesAndStart() {
        val cached = db.getCachedPlaces()
        if (cached.isNotEmpty()) {
            startGame(cached)
            return
        }
        placesTask = PlacesLoaderTask(AppConfig.PLACES_URL, object : PlacesLoaderTask.Listener {
            override fun onStart() {
                imgProgress.visibility = View.VISIBLE
            }
            override fun onSuccess(places: List<Landmark>) {
                db.cachePlaces(places)
                startGame(places)
            }
            override fun onError(message: String) {
                imgProgress.visibility = View.GONE
                Toast.makeText(
                    this@GameActivity,
                    "$message. Connect to the internet once to download the landmarks.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }).also { it.execute() }
    }

    private fun startGame(all: List<Landmark>) {
        rounds = GamePicker.pick(all, difficulty, region)
        if (rounds.isEmpty()) {
            Toast.makeText(this, "No landmarks available", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        roundIndex = 0
        totalScore = 0
        results.clear()
        startRound()
    }



    private fun startRound() {
        val place = rounds[roundIndex]


        mapView.overlays.removeAll(roundOverlays.toSet())
        roundOverlays.clear()
        guessMarker = null
        guess = null
        resetMapView()
        mapView.invalidate()

        roundActive = false
        btnSubmit.isEnabled = false
        tvRound.text = "Round ${roundIndex + 1}/${rounds.size}"
        tvScore.text = "Score: $totalScore"
        tvTimer.text = "Time: 30s"
        tvTimer.setTextColor(timerDefaultColor)


        if (difficulty == "Easy") {
            tvHint.text = "Hint: ${place.hint}"
            tvHint.visibility = View.VISIBLE
        } else {
            tvHint.visibility = View.GONE
        }


        imgLandmark.setImageDrawable(null)
        imgProgress.visibility = View.VISIBLE
        imageTask?.cancel(true)
        imageTask = ImageLoaderTask(this, place.imageUrl) { bitmap ->
            imgProgress.visibility = View.GONE
            if (bitmap != null) {
                imgLandmark.setImageBitmap(bitmap)
            } else {
                Toast.makeText(this, "Photo could not load. Use the hint!", Toast.LENGTH_LONG).show()
                tvHint.text = "Hint: ${place.hint}"
                tvHint.visibility = View.VISIBLE
            }
            beginTimer()
        }.also { it.execute() }
    }


    private fun beginTimer() {
        roundActive = true
        timer?.cancel()
        timer = object : CountDownTimer(30_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = (millisUntilFinished + 999) / 1000
                tvTimer.text = "Time: ${secs}s"
                if (secs <= 10) tvTimer.setTextColor(Color.RED)
            }

            override fun onFinish() {
                tvTimer.text = "Time: 0s"
                submitGuess(timedOut = true)
            }
        }.start()
    }

    private fun onMapTap(p: GeoPoint) {
        if (!roundActive) return
        val existing = guessMarker
        if (existing == null) {
            val marker = makeMarker(p, "Your guess", Color.BLUE)
            marker.setInfoWindow(null)
            mapView.overlays.add(marker)
            roundOverlays.add(marker)
            guessMarker = marker
        } else {
            existing.position = p
        }
        guess = p
        mapView.invalidate()
        btnSubmit.isEnabled = true
    }

    private fun submitGuess(timedOut: Boolean) {
        if (!roundActive) return
        roundActive = false
        timer?.cancel()
        btnSubmit.isEnabled = false

        val place = rounds[roundIndex]
        val g = guess
        val distanceKm: Double
        val points: Int
        if (g == null) {
            distanceKm = -1.0
            points = 0
        } else {

            distanceKm = ScoreCalculator.distanceKm(g.latitude, g.longitude, place.lat, place.lng)
            points = ScoreCalculator.score(distanceKm)
        }
        totalScore += points
        tvScore.text = "Score: $totalScore"
        results.add(RoundResult(place.name, distanceKm, points, place.lat, place.lng, place.learnMoreUrl))

        if (timedOut) Toast.makeText(this, "Time's up!", Toast.LENGTH_SHORT).show()


        if (points >= 3000) sound.playCorrect() else sound.playWrong()

        revealOnMap(place, g, distanceKm)
        showRoundDialog(place, distanceKm, points)
    }


    private fun revealOnMap(place: Landmark, g: GeoPoint?, distanceKm: Double) {
        val target = GeoPoint(place.lat, place.lng)

        val targetMarker = makeMarker(target, place.name, Color.RED)
        mapView.overlays.add(targetMarker)
        roundOverlays.add(targetMarker)

        if (g != null) {
            val line = Polyline(mapView)
            line.setPoints(listOf(g, target))
            line.outlinePaint.color = Color.RED
            line.outlinePaint.strokeWidth = 6f
            mapView.overlays.add(line)
            roundOverlays.add(line)
        }
        mapView.invalidate()

        if (g == null || distanceKm < 5) {
            mapView.controller.setZoom(10.0)
            mapView.controller.animateTo(target)
        } else {
            val box = BoundingBox.fromGeoPointsSafe(listOf(g, target))
            mapView.post { mapView.zoomToBoundingBox(box, true, 150) }
        }
    }

    private fun makeMarker(p: GeoPoint, title: String, color: Int): Marker {
        val marker = Marker(mapView)
        marker.position = p
        marker.title = title
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon?.let { marker.icon = it.mutate().also { d -> d.setTint(color) } }
        return marker
    }




    private fun showRoundDialog(place: Landmark, distanceKm: Double, points: Int) {
        val message = StringBuilder()
        if (distanceKm < 0) {
            message.append("You did not place a guess.\n")
        } else {
            message.append("Your guess was %.1f km away.\n".format(distanceKm))
        }
        message.append("Points: $points / 5000\n\n")
        message.append("Did you know?\n").append(place.fact)

        val isLast = roundIndex == rounds.lastIndex
        val dialog = AlertDialog.Builder(this)
            .setTitle(place.name)
            .setMessage(message.toString())
            .setCancelable(false)
            .setPositiveButton(if (isLast) "See results" else "Next round", null)
            .setNegativeButton("Learn more", null)
            .setNeutralButton("Open in Maps", null)
            .create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dialog.dismiss()
            goNext()
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { openLearnMore(place) }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { openInMaps(place) }


        dialog.window?.setGravity(Gravity.TOP)
        dialog.window?.setDimAmount(0.2f)
    }

    private fun goNext() {
        roundIndex++
        if (roundIndex >= rounds.size) finishGame() else startRound()
    }

    private fun finishGame() {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(Extras.SCORE, totalScore)
        intent.putExtra(Extras.DIFFICULTY, difficulty)
        intent.putExtra(Extras.ROUNDS, results.size)
        intent.putExtra(Extras.NAMES, results.map { it.placeName }.toTypedArray())
        intent.putExtra(Extras.DISTANCES, results.map { it.distanceKm }.toDoubleArray())
        intent.putExtra(Extras.POINTS, results.map { it.points }.toIntArray())
        intent.putExtra(Extras.LATS, results.map { it.lat }.toDoubleArray())
        intent.putExtra(Extras.LNGS, results.map { it.lng }.toDoubleArray())
        startActivity(intent)
        finish()
    }

    private fun confirmQuit() {
        AlertDialog.Builder(this)
            .setTitle("Quit game?")
            .setMessage("Your progress in this game will be lost.")
            .setPositiveButton("Quit") { _, _ -> finish() }
            .setNegativeButton("Keep playing", null)
            .show()
    }




    private fun openLearnMore(place: Landmark) {
        if (place.learnMoreUrl.isBlank()) {
            Toast.makeText(this, "No web page for this place", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra(Extras.URL, place.learnMoreUrl)
        intent.putExtra(Extras.TITLE, place.name)
        startActivity(intent)
    }


    private fun openInMaps(place: Landmark) {
        val uri = Uri.parse("geo:0,0?q=${place.lat},${place.lng}(${Uri.encode(place.name)})")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No maps app found on this device", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        imageTask?.cancel(true)
        placesTask?.cancel(true)
        sound.release()
    }
}