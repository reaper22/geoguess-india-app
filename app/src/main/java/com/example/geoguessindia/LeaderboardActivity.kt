package com.example.geoguessindia

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.geoguessindia.db.DbHelper
import com.example.geoguessindia.firebase.LeaderboardRepository
import com.example.geoguessindia.model.ScoreEntry
import com.example.geoguessindia.util.Extras
import com.google.android.material.tabs.TabLayout


class LeaderboardActivity : AppCompatActivity() {

    private lateinit var db: DbHelper
    private lateinit var listView: ListView
    private lateinit var tvStatus: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnClear: Button
    private lateinit var spFilter: Spinner

    private var showOnline = true
    private var requestId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)
        db = DbHelper(this)

        listView = findViewById(R.id.listView)
        tvStatus = findViewById(R.id.tvStatus)
        progress = findViewById(R.id.progress)
        btnClear = findViewById(R.id.btnClear)
        spFilter = findViewById(R.id.spFilter)


        val lastScore = intent.getIntExtra(Extras.SCORE, -1)
        val lastDifficulty = intent.getStringExtra(Extras.DIFFICULTY)
        val lastRounds = intent.getIntExtra(Extras.ROUNDS, 0)
        if (lastScore >= 0) {
            findViewById<TextView>(R.id.tvLast).apply {
                text = "Your last game: $lastScore / ${lastRounds * 5000} ($lastDifficulty)"
                visibility = View.VISIBLE
            }
        }

        spFilter.adapter = ArrayAdapter.createFromResource(
            this, R.array.leaderboard_filters, android.R.layout.simple_spinner_item
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val filters = resources.getStringArray(R.array.leaderboard_filters)
        spFilter.setSelection(filters.indexOf(lastDifficulty).coerceAtLeast(0))
        spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) = refresh()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val tabs = findViewById<TabLayout>(R.id.tabs)
        tabs.addTab(tabs.newTab().setText("Online"))
        tabs.addTab(tabs.newTab().setText("This device"))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showOnline = tab.position == 0
                refresh()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        btnClear.setOnClickListener { confirmClear() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun currentFilter(): String? =
        spFilter.selectedItem.toString().let { if (it == "All") null else it }

    private fun refresh() {
        val filter = currentFilter()
        val thisRequest = ++requestId
        btnClear.visibility = if (showOnline) View.GONE else View.VISIBLE
        tvStatus.visibility = View.GONE

        if (showOnline) {
            progress.visibility = View.VISIBLE
            listView.adapter = ScoreAdapter(this, emptyList())
            LeaderboardRepository.getTop10(filter) { entries, error ->
                if (thisRequest != requestId || isFinishing || isDestroyed) return@getTop10
                progress.visibility = View.GONE
                if (entries == null) {
                    showMessage("Could not load the online leaderboard.\n$error")
                } else {
                    show(entries)
                }
            }
        } else {
            progress.visibility = View.GONE
            // SYLLABUS: SQLite top-10 query
            show(db.getTop10(filter).map { ScoreEntry(it.player, it.score, it.difficulty, it.date) })
        }
    }

    private fun show(entries: List<ScoreEntry>) {
        listView.adapter = ScoreAdapter(this, entries)
        if (entries.isEmpty()) showMessage("No scores yet. Be the first!")
    }

    private fun showMessage(text: String) {
        tvStatus.text = text
        tvStatus.visibility = View.VISIBLE
    }


    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle("Clear scores on this device?")
            .setMessage("This removes your saved scores from this phone. The online leaderboard is not affected.")
            .setPositiveButton("Clear") { _, _ ->
                val removed = db.clearScores()
                Toast.makeText(this, "$removed scores removed", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private class ScoreAdapter(context: Context, private val items: List<ScoreEntry>) :
        ArrayAdapter<ScoreEntry>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.item_score, parent, false)
            val entry = items[position]
            view.findViewById<TextView>(R.id.tvRank).text = "#${position + 1}"
            view.findViewById<TextView>(R.id.tvName).text = entry.player
            view.findViewById<TextView>(R.id.tvMeta).text = "${entry.difficulty}  |  ${entry.date}"
            view.findViewById<TextView>(R.id.tvScore).text = entry.score.toString()
            return view
        }
    }
}