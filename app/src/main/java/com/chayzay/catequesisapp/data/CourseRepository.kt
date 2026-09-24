package com.chayzay.catequesisapp.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.File

/** Contratos originales: course/all, class_course/all y theme/all devuelven {status, data}. */
data class Course(val id: Int, val name: String, val imageUrl: String)
data class CourseClass(val id: Int, val courseId: Int, val number: Int, val name: String)
data class ClassTheme(val id: Int, val classId: Int, val number: Int, val name: String)
data class Lesson(val id: Int, val themeId: Int, val number: Int, val name: String, val html: String)

class CourseRepository(private val baseUrl: String, private val cacheDir: File) {
    fun getCourses(): List<Course> = request("course/all").mapNotNull { item ->
        val id = item.optInt("id_course", -1)
        val name = item.optString("name_course").trim()
        if (id < 0 || name.isEmpty()) null else Course(id, name,
            item.optString("path_image_solve").let { image ->
                if (baseUrl.startsWith("https://")) image.replaceFirst("http://", "https://") else image
            })
    }

    fun getClasses(courseId: Int): List<CourseClass> = request("class_course/all")
        .mapNotNull { item ->
            val id = item.optInt("id_class_course", -1)
            val parentId = item.optInt("id_course", -1)
            val name = item.optString("name_class_course").trim()
            if (parentId != courseId || id < 0 || name.isEmpty()) null
            else CourseClass(id, parentId, item.optInt("number_class", 0), name)
        }.sortedWith(compareBy<CourseClass> { it.number }.thenBy { it.id })

    fun getThemes(classId: Int): List<ClassTheme> = request("theme/all")
        .mapNotNull { item ->
            val id = item.optInt("id_theme", -1)
            val parentId = item.optInt("id_class_course", -1)
            val name = item.optString("name_theme").trim()
            if (parentId != classId || id < 0 || name.isEmpty()) null
            else ClassTheme(id, parentId, item.optInt("number_theme", 0), name)
        }.sortedWith(compareBy<ClassTheme> { it.number }.thenBy { it.id })

    fun getLessons(themeId: Int): List<Lesson> = request("lesson_class/all")
        .mapNotNull { item ->
            val id = item.optInt("id_lesson_class", -1)
            val parentId = item.optInt("id_theme", -1)
            val name = item.optString("name_lesson_class").trim()
            if (parentId != themeId || id < 0 || name.isEmpty()) null
            else Lesson(id, parentId, item.optInt("number_lesson", 0),
                name, item.optString("content_lesson_class"))
        }.sortedWith(compareBy<Lesson> { it.number }.thenBy { it.id })

    private fun request(path: String): List<JSONObject> {
        val cache = File(cacheDir, path.replace('/', '_') + ".json")
        val body = try {
            val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("El servidor respondió ${connection.responseCode}")
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        } catch (error: Exception) {
            // Al perder conexión, conserva la experiencia de lectura de contenido descargado.
            if (cache.exists()) cache.readText(Charsets.UTF_8) else throw error
        }
        val response = JSONObject(body)
        if (response.optString("status") != "1") {
            throw IllegalStateException(response.optString("message", "No se pudieron cargar los datos"))
        }
        val data = response.optJSONArray("data")
            ?: throw IllegalStateException("La respuesta no contiene la lista solicitada")
        // Solo se conservan respuestas completas y válidas. La cache es interna a la app.
        if (!cache.exists() || cache.readText(Charsets.UTF_8) != body) {
            try { cache.writeText(body, Charsets.UTF_8) } catch (_: Exception) { /* lectura en línea sigue disponible */ }
        }
        return (0 until data.length()).mapNotNull { data.optJSONObject(it) }
    }
}
