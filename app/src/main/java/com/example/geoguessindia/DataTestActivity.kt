package com.example.geoguessindia

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.geoguessindia.db.DbHelper
import com.example.geoguessindia.firebase.AuthHelper
import com.example.geoguessindia.model.Landmark
import com.example.geoguessindia.net.ImageLoaderTask
import com.example.geoguessindia.net.PlacesLoaderTask
import com.example.geoguessindia.util.AppConfig
import com.example.geoguessindia.util.PrefsManager
import kotlin.random.Random

/** Temporary screen to prove network loading, SQLite caching and offline fallback. */
@Suppress("DEPRECATION")
class DataTestActivity : AppCompatActivity() {

    private lateinit var db: DbHelper
    private lateinit var prefs: PrefsManager
    private lateinit var progress: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvScores: TextView
    private lateinit var imageView: ImageView

    private var placesTask: PlacesLoaderTask? = null
    private var imageTask: ImageLoaderTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_test)

        db = DbHelper(this)
        prefs = PrefsManager(this)
        progress = findViewById(R.id.progress)
        tvStatus = findViewById(R.id.tvStatus)
        tvScores = findViewById(R.id.tvScores)
        imageView = findViewById(R.id.imageView)

        findViewById<Button>(R.id.btnLoad).setOnClickListener { loadPlaces() }

        findViewById<Button>(R.id.btnAddScore).setOnClickListener {
            val score = Random.nextInt(0, 5001)
            db.insertScore(AuthHelper.currentName(), score, "Easy")
            val newBest = prefs.updateBestScore(score)
            if (newBest) Toast.makeText(this, "New best score: $score", Toast.LENGTH_SHORT).show()
            showScores()
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            db.clearScores()
            showScores()
        }

        showScores()
    }

    private fun loadPlaces() {
        placesTask?.cancel(true)
        placesTask = PlacesLoaderTask(AppConfig.PLACES_URL, object : PlacesLoaderTask.Listener {

            override fun onStart() {
                progress.visibility = View.VISIBLE
                tvStatus.text = "Loading landmarks..."
            }

            override fun onSuccess(places: List<Landmark>) {
                progress.visibility = View.GONE
                db.cachePlaces(places)                         // save for offline use
                tvStatus.text = "Loaded ${places.size} places from the server and cached them."
                loadImage(places.first())
            }

            override fun onError(message: String) {
                progress.visibility = View.GONE
                Toast.makeText(this@DataTestActivity, message, Toast.LENGTH_LONG).show()
                // Offline fallback: use the SQLite cache
                val cached = db.getCachedPlaces()
                if (cached.isNotEmpty()) {
                    tvStatus.text = "$message.\nOffline mode: using ${cached.size} cached places."
                    loadImage(cached.first())
                } else {
                    tvStatus.text = "$message.\nNothing cached yet, so connect once first."
                }
            }
        }).also { it.execute() }
    }

    private fun loadImage(place: Landmark) {
        imageTask?.cancel(true)
        imageTask = ImageLoaderTask(this, place.imageUrl) { bitmap: Bitmap? ->
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                tvStatus.append("\nFirst place: ${place.name} (photo loaded)")
            } else {
                tvStatus.append("\nFirst place: ${place.name} (photo failed to load)")
            }
        }.also { it.execute() }
    }

    private fun showScores() {
        val top = db.getTop10()
        tvScores.text = if (top.isEmpty()) {
            "No local scores yet.   Best (prefs): ${prefs.bestScore}"
        } else {
            "Best (prefs): ${prefs.bestScore}\n" +
                    top.mapIndexed { i, s -> "${i + 1}. ${s.player}  ${s.score}  ${s.difficulty}  ${s.date}" }
                        .joinToString("\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        placesTask?.cancel(true)
        imageTask?.cancel(true)
    }
}