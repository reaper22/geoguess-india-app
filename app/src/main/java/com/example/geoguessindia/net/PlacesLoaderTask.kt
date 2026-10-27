package com.example.geoguessindia.net

import android.os.AsyncTask
import com.example.geoguessindia.model.Landmark
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException


@Suppress("DEPRECATION")
class PlacesLoaderTask(
    private val url: String,
    private val listener: Listener
) : AsyncTask<Void, Void, PlacesLoaderTask.Result>() {

    interface Listener {
        fun onStart()
        fun onSuccess(places: List<Landmark>)
        fun onError(message: String)
    }

    class Result(val places: List<Landmark>? = null, val error: String? = null)

    override fun onPreExecute() {
        listener.onStart()
    }

    override fun doInBackground(vararg params: Void?): Result {
        var conn: HttpURLConnection? = null
        try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 8000     // ms to establish the connection
            conn.readTimeout = 8000        // ms to wait for data

            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                return Result(error = "Server error (HTTP $code)")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            return Result(places = parse(text))

        } catch (e: SocketTimeoutException) {
            return Result(error = "Connection timed out")
        } catch (e: UnknownHostException) {
            return Result(error = "No internet connection")
        } catch (e: ConnectException) {
            return Result(error = "No internet connection")
        } catch (e: JSONException) {
            return Result(error = "Landmark data is malformed")
        } catch (e: IOException) {
            return Result(error = "Network error")
        } finally {
            conn?.disconnect()
        }
    }


    private fun parse(text: String): List<Landmark> {
        val array = JSONArray(text)
        if (array.length() == 0) throw JSONException("empty list")
        val list = ArrayList<Landmark>()
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val lat = o.getDouble("lat")
            val lng = o.getDouble("lng")
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                throw JSONException("bad coordinates")
            }
            list.add(
                Landmark(
                    id = o.getInt("id"),
                    name = o.getString("name"),
                    region = o.optString("region", ""),
                    lat = lat,
                    lng = lng,
                    imageUrl = o.getString("imageUrl"),
                    hint = o.optString("hint", ""),
                    difficulty = o.optString("difficulty", "Easy"),
                    fact = o.optString("fact", ""),
                    learnMoreUrl = o.optString("learnMoreUrl", "")
                )
            )
        }
        return list
    }

    override fun onPostExecute(result: Result) {
        val places = result.places
        if (places != null) listener.onSuccess(places)
        else listener.onError(result.error ?: "Unknown error")
    }
}