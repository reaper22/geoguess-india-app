package com.example.geoguessindia.net

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


@Suppress("DEPRECATION")
class ImageLoaderTask(
    context: Context,
    private val imageUrl: String,
    private val listener: Listener
) : AsyncTask<Void, Void, Bitmap?>() {

    fun interface Listener {
        fun onImageLoaded(bitmap: Bitmap?)
    }


    private val appContext = context.applicationContext

    override fun doInBackground(vararg params: Void?): Bitmap? {
        val dir = appContext.getExternalFilesDir("images") ?: File(appContext.filesDir, "images")
        dir.mkdirs()
        val file = File(dir, fileNameFor(imageUrl))


        if (file.exists() && file.length() > 0) {
            decode(file)?.let { return it }
            file.delete()
        }


        var conn: HttpURLConnection? = null
        return try {
            conn = URL(imageUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 10000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            conn.inputStream.use { input ->
                file.outputStream().use { out -> input.copyTo(out) }
            }
            decode(file)
        } catch (e: Exception) {
            file.delete()
            null
        } finally {
            conn?.disconnect()
        }
    }


    private fun decode(file: File, maxWidth: Int = 1080): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun fileNameFor(url: String): String {
        val name = url.substringAfterLast('/').substringBefore('?')
        return if (name.isBlank()) "img_${url.hashCode()}.jpg" else name
    }

    override fun onPostExecute(result: Bitmap?) {
        listener.onImageLoaded(result)
    }
}