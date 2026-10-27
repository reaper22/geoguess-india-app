package com.example.geoguessindia

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.geoguessindia.db.DbHelper
import com.example.geoguessindia.firebase.AuthHelper
import com.example.geoguessindia.firebase.LeaderboardRepository
import com.example.geoguessindia.util.Extras
import com.example.geoguessindia.util.PrefsManager


class ResultActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var tvSaveStatus: TextView

    private var score = 0
    private var difficulty = "Easy"
    private var rounds = 0
    private var names: Array<String> = emptyArray()
    private var distances = DoubleArray(0)
    private var points = IntArray(0)
    private var lats = DoubleArray(0)
    private var lngs = DoubleArray(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        prefs = PrefsManager(this)


        score = intent.getIntExtra(Extras.SCORE, 0)
        difficulty = intent.getStringExtra(Extras.DIFFICULTY) ?: "Easy"
        rounds = intent.getIntExtra(Extras.ROUNDS, 0)
        names = intent.getStringArrayExtra(Extras.NAMES) ?: emptyArray()
        distances = intent.getDoubleArrayExtra(Extras.DISTANCES) ?: DoubleArray(0)
        points = intent.getIntArrayExtra(Extras.POINTS) ?: IntArray(0)
        lats = intent.getDoubleArrayExtra(Extras.LATS) ?: DoubleArray(0)
        lngs = intent.getDoubleArrayExtra(Extras.LNGS) ?: DoubleArray(0)

        findViewById<TextView>(R.id.tvTotal).text = "$score / ${rounds * 5000}"
        findViewById<TextView>(R.id.tvMeta).text = "Difficulty: $difficulty  |  $rounds rounds"
        tvSaveStatus = findViewById(R.id.tvSaveStatus)

        buildTable(findViewById(R.id.resultTable))

        findViewById<Button>(R.id.btnShare).setOnClickListener { shareScore() }
        findViewById<Button>(R.id.btnLeaderboard).setOnClickListener { openLeaderboard() }
        findViewById<Button>(R.id.btnHome).setOnClickListener { finish() }


        if (savedInstanceState == null) saveScore()
    }



    private fun saveScore() {

        DbHelper(this).insertScore(AuthHelper.currentName(), score, difficulty)


        val newBest = prefs.updateBestScore(score)
        findViewById<TextView>(R.id.tvBestNote).visibility = if (newBest) View.VISIBLE else View.GONE


        tvSaveStatus.text = "Saving to the online leaderboard (if you are offline, it syncs later)..."
        LeaderboardRepository.submitScore(score, difficulty, rounds, newBest) { ok, error ->
            if (isFinishing || isDestroyed) return@submitScore
            tvSaveStatus.text = if (ok) "Saved to the online leaderboard."
            else "Could not save online: $error"
        }
    }


    private fun buildTable(table: TableLayout) {
        table.setColumnShrinkable(1, true)
        table.setColumnStretchable(1, true)

        addRow(table, "Round", "Place", "Distance km", "Points", header = true)
        for (i in names.indices) {
            val distance = if (distances[i] < 0) "-" else "%.1f".format(distances[i])
            val row = addRow(table, "${i + 1}", names[i], distance, "${points[i]}", header = false)
            row.setOnClickListener { openInMaps(i) }
        }
        addRow(table, "", "Total", "", "$score", header = true)
    }

    private fun addRow(
        table: TableLayout,
        round: String, place: String, distance: String, pts: String,
        header: Boolean
    ): TableRow {
        val row = TableRow(this)
        if (header) row.setBackgroundResource(R.color.table_header)
        for ((index, text) in listOf(round, place, distance, pts).withIndex()) {
            val cell = TextView(this)
            cell.text = text
            cell.setPadding(dp(8), dp(10), dp(8), dp(10))
            cell.gravity = if (index == 1) Gravity.START else Gravity.CENTER
            if (header) cell.setTypeface(null, Typeface.BOLD)
            row.addView(cell)
        }
        table.addView(row)
        return row
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

      private fun shareScore() {
        val text = "I scored $score/${rounds * 5000} on GeoGuess India ($difficulty mode)! Can you beat me?"
        val send = Intent(Intent.ACTION_SEND)
        send.type = "text/plain"
        send.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, "Share your score"))
    }

    private fun openInMaps(index: Int) {
        val uri = Uri.parse("geo:0,0?q=${lats[index]},${lngs[index]}(${Uri.encode(names[index])})")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No maps app found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLeaderboard() {
        val intent = Intent(this, LeaderboardActivity::class.java)
        intent.putExtra(Extras.SCORE, score)
        intent.putExtra(Extras.DIFFICULTY, difficulty)
        intent.putExtra(Extras.ROUNDS, rounds)
        startActivity(intent)
    }
}