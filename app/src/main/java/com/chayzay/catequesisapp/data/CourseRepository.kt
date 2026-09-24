package com.chayzay.catequesisapp.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Mismo contrato de la app antigua: GET /API/v1/course/all -> { data: [...] }. */
data class Course(val id: Int, val name: String)

class CourseRepository(private val baseUrl: String) {
    fun getCourses(): List<Course> {
        val connection = (URL(baseUrl + "course/all").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("El servidor respondió ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val response = JSONObject(body)
            if (response.optString("status") != "1") {
                throw IllegalStateException(response.optString("message", "No se pudieron cargar los cursos"))
            }
            val data = response.optJSONArray("data")
                ?: throw IllegalStateException("La respuesta no contiene cursos")
            return (0 until data.length()).mapNotNull { index ->
                val item = data.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optInt("id_course", -1)
                val name = item.optString("name_course").trim()
                if (id < 0 || name.isEmpty()) null else Course(id, name)
            }
        } finally {
            connection.disconnect()
        }
    }
}
