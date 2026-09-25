package com.chayzay.catequesisapp.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Descarga la misma path_image_solve que usaba ClassCourseActivity en la app anterior. */
class CourseImageRepository(private val cacheDir: File, private val previousCacheDir: File? = null) {
    fun load(course: Course): Bitmap? {
        if (!course.imageUrl.startsWith("https://", ignoreCase = true)) return null
        val cache = File(cacheDir, "course_image_${course.id}.img")
        if (cache.exists()) BitmapFactory.decodeFile(cache.absolutePath)?.let { return it }
        previousCacheDir?.let { previous ->
            File(previous, cache.name).takeIf { it.isFile }?.let { old ->
                BitmapFactory.decodeFile(old.absolutePath)?.let { image ->
                    try { old.copyTo(cache, overwrite = false) } catch (_: Exception) { }
                    return image
                }
            }
        }
        val connection = try {
            (URL(course.imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
            }
        } catch (_: Exception) { return null }
        return try {
            if (connection.responseCode !in 200..299) return null
            val buffer = ByteArray(8192)
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 6_000_000) return null
                    output.write(buffer, 0, count)
                }
            }
            val bytes = output.toByteArray()
            val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            try { cache.writeBytes(bytes) } catch (_: Exception) { /* imagen visible sin cache */ }
            image
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
