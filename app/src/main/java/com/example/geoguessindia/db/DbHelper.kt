package com.example.geoguessindia.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.geoguessindia.model.Landmark
import com.example.geoguessindia.model.LocalScore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    // Called once, the first time the database file is created
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $T_SCORES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player TEXT NOT NULL,
                score INTEGER NOT NULL,
                difficulty TEXT NOT NULL,
                date TEXT NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE $T_PLACES (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                region TEXT,
                lat REAL NOT NULL,
                lng REAL NOT NULL,
                image_url TEXT,
                hint TEXT,
                difficulty TEXT,
                fact TEXT,
                learn_more_url TEXT)"""
        )
    }

    // Called when DB_VERSION goes up. Simple approach for a course project: rebuild.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_SCORES")
        db.execSQL("DROP TABLE IF EXISTS $T_PLACES")
        onCreate(db)
    }



    fun insertScore(player: String, score: Int, difficulty: String): Long {
        val values = ContentValues().apply {
            put("player", player)
            put("score", score)
            put("difficulty", difficulty)
            put("date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()))
        }
        return writableDatabase.insert(T_SCORES, null, values)
    }


    fun updatePlayerName(oldName: String, newName: String): Int {
        val values = ContentValues().apply { put("player", newName) }
        return writableDatabase.update(T_SCORES, values, "player = ?", arrayOf(oldName))
    }


    fun deleteScore(id: Long): Int =
        writableDatabase.delete(T_SCORES, "id = ?", arrayOf(id.toString()))


    fun clearScores(): Int = writableDatabase.delete(T_SCORES, null, null)


    fun getTop10(difficulty: String? = null): List<LocalScore> {
        val list = ArrayList<LocalScore>()
        val selection = if (difficulty == null) null else "difficulty = ?"
        val args = if (difficulty == null) null else arrayOf(difficulty)
        readableDatabase.query(
            T_SCORES, null, selection, args, null, null,
            "score DESC", "10"
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    LocalScore(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("player")),
                        c.getInt(c.getColumnIndexOrThrow("score")),
                        c.getString(c.getColumnIndexOrThrow("difficulty")),
                        c.getString(c.getColumnIndexOrThrow("date"))
                    )
                )
            }
        }
        return list
    }


    fun cachePlaces(places: List<Landmark>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(T_PLACES, null, null)
            for (p in places) {
                val values = ContentValues().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("region", p.region)
                    put("lat", p.lat)
                    put("lng", p.lng)
                    put("image_url", p.imageUrl)
                    put("hint", p.hint)
                    put("difficulty", p.difficulty)
                    put("fact", p.fact)
                    put("learn_more_url", p.learnMoreUrl)
                }
                db.insert(T_PLACES, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getCachedPlaces(): List<Landmark> {
        val list = ArrayList<Landmark>()
        readableDatabase.query(T_PLACES, null, null, null, null, null, "id ASC").use { c ->
            while (c.moveToNext()) {
                list.add(
                    Landmark(
                        c.getInt(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("region")) ?: "",
                        c.getDouble(c.getColumnIndexOrThrow("lat")),
                        c.getDouble(c.getColumnIndexOrThrow("lng")),
                        c.getString(c.getColumnIndexOrThrow("image_url")) ?: "",
                        c.getString(c.getColumnIndexOrThrow("hint")) ?: "",
                        c.getString(c.getColumnIndexOrThrow("difficulty")) ?: "",
                        c.getString(c.getColumnIndexOrThrow("fact")) ?: "",
                        c.getString(c.getColumnIndexOrThrow("learn_more_url")) ?: ""
                    )
                )
            }
        }
        return list
    }

    companion object {
        private const val DB_NAME = "geoguess.db"
        private const val DB_VERSION = 1
        private const val T_SCORES = "scores"
        private const val T_PLACES = "cached_places"
    }
}