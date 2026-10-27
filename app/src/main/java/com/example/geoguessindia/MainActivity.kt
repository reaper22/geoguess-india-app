package com.example.geoguessindia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.geoguessindia.db.DbHelper
import com.example.geoguessindia.firebase.AuthHelper
import com.example.geoguessindia.model.Landmark
import com.example.geoguessindia.net.PlacesLoaderTask
import com.example.geoguessindia.util.AppConfig
import com.example.geoguessindia.util.Extras
import com.example.geoguessindia.util.PrefsManager
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.tabs.TabLayout


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var panels: List<View>
    private lateinit var tvBest: TextView
    private var refreshTask: PlacesLoaderTask? = null


    private fun setupLeaderboardTab() {
        findViewById<Button>(R.id.btnOpenLeaderboard).setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (!AuthHelper.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        prefs = PrefsManager(this)
        prefs.playerName = AuthHelper.currentName()

        setupTabs()
        setupPlayTab()
        setupProfileTab()
        setupLeaderboardTab()
        refreshPlacesQuietly()
    }


    private fun setupTabs() {
        panels = listOf(
            findViewById(R.id.panelPlay),
            findViewById(R.id.panelLeaderboard),
            findViewById(R.id.panelProfile)
        )
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_play))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_leaderboard))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_profile))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                panels.forEachIndexed { i, panel ->
                    panel.visibility = if (i == tab.position) View.VISIBLE else View.GONE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }


    private fun setupPlayTab() {
        val spDifficulty = findViewById<Spinner>(R.id.spDifficulty)
        val spRegion = findViewById<Spinner>(R.id.spRegion)

        spDifficulty.adapter = ArrayAdapter.createFromResource(
            this, R.array.difficulties, android.R.layout.simple_spinner_item
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spRegion.adapter = ArrayAdapter.createFromResource(
            this, R.array.regions, android.R.layout.simple_spinner_item
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }


        val difficulties = resources.getStringArray(R.array.difficulties)
        spDifficulty.setSelection(difficulties.indexOf(prefs.lastDifficulty).coerceAtLeast(0))

        spDifficulty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.lastDifficulty = difficulties[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra(Extras.DIFFICULTY, spDifficulty.selectedItem.toString())
            intent.putExtra(Extras.REGION, spRegion.selectedItem.toString())
            startActivity(intent)
        }
    }

    private fun setupProfileTab() {
        findViewById<TextView>(R.id.tvWelcome).text = "Player: ${AuthHelper.currentName()}"
        tvBest = findViewById(R.id.tvBest)

        val swSound = findViewById<MaterialSwitch>(R.id.swSound)
        swSound.isChecked = prefs.soundOn
        swSound.setOnCheckedChangeListener { _, checked -> prefs.soundOn = checked }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            AuthHelper.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Developer shortcuts to the two test screens
        findViewById<TextView>(R.id.tvMapTest).setOnClickListener {
            startActivity(Intent(this, MapTestActivity::class.java))
        }
        findViewById<TextView>(R.id.tvDataTest).setOnClickListener {
            startActivity(Intent(this, DataTestActivity::class.java))
        }
    }


    private fun refreshPlacesQuietly() {
        refreshTask = PlacesLoaderTask(AppConfig.PLACES_URL, object : PlacesLoaderTask.Listener {
            override fun onStart() {}
            override fun onSuccess(places: List<Landmark>) {
                DbHelper(this@MainActivity).cachePlaces(places)
            }
            override fun onError(message: String) {}
        }).also { it.execute() }
    }

    override fun onResume() {
        super.onResume()
        if (::tvBest.isInitialized) tvBest.text = "Best score: ${prefs.bestScore}"
    }

    override fun onDestroy() {
        super.onDestroy()
        refreshTask?.cancel(true)
    }
}