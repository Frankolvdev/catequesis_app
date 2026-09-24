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
data class ClassGoal(val id: Int, val number: Int, val content: String)
data class ClassActivity(val id: Int, val content: String)
data class OnlineActivity(val id: Int, val title: String, val link: String, val type: String)
data class ExamAnswer(val id: Int, val text: String, val correct: Boolean)
data class ExamQuestion(val id: Int, val text: String, val type: String, val answers: List<ExamAnswer>)

class CourseRepository(private val baseUrl: String, private val cacheDir: File) {
    fun getExam(classId: Int): List<ExamQuestion> {
        val themeIds = getThemes(classId).map { it.id }.toSet()
        val questions = request("question/all").filter {
            it.optInt("id_theme", -1) in themeIds &&
                it.optString("type_question") in setOf("SIMPLE", "CLOSED")
        }
        val answersByQuestion = request("response/all").mapNotNull { item ->
            val id = item.optInt("id_response", -1)
            val questionId = item.optInt("id_question", -1)
            val text = item.optString("content_response").trim()
            if (id < 0 || questionId < 0 || text.isEmpty()) null
            else questionId to ExamAnswer(id, text, item.optString("correct_response") == "YES")
        }.groupBy({ it.first }, { it.second })
        return questions.mapNotNull { item ->
            val id = item.optInt("id_question", -1)
            val content = item.optString("content_question").trim()
            val answers = answersByQuestion[id].orEmpty()
            val correct = answers.filter { it.correct }
            val incorrect = answers.filterNot { it.correct }
            // La app original mostraba primero las correctas y rellenaba hasta cuatro.
            val selected = (correct + incorrect.take((4 - correct.size).coerceAtLeast(0))).shuffled()
            if (id < 0 || content.isEmpty() || correct.isEmpty() || correct.size > 4 || selected.size < 2) null
            else ExamQuestion(id, content, item.optString("type_question"), selected)
        }.shuffled().take(10)
    }

    fun getGoals(classId: Int): List<ClassGoal> = request("meta_class/all")
        .filter { it.optInt("id_class_course", -1) == classId }
        .mapNotNull { item ->
            val id = item.optInt("id_meta_class", -1)
            val content = item.optString("content_meta").trim()
            if (id < 0 || content.isEmpty()) null else ClassGoal(id, item.optInt("number_meta", 0), content)
        }.sortedWith(compareBy<ClassGoal> { it.number }.thenBy { it.id })

    fun getRealActivities(classId: Int): List<ClassActivity> = request("real_activities/all")
        .filter { it.optInt("id_class_course", -1) == classId }
        .mapNotNull { item ->
            val id = item.optInt("id_real_activities", -1)
            val content = item.optString("content_activity").trim()
            if (id < 0 || content.isEmpty()) null else ClassActivity(id, content)
        }

    fun getOnlineActivities(classId: Int): List<OnlineActivity> = request("activity_online/all")
        .filter { it.optInt("id_class_course", -1) == classId && it.optString("language") == "es" }
        .mapNotNull { item ->
            val id = item.optInt("id_activity_online", -1)
            val title = item.optString("title").trim()
            val link = item.optString("link").trim()
            if (id < 0 || title.isEmpty() || !(link.startsWith("https://") || link.startsWith("http://"))) null
            else OnlineActivity(id, title, link, item.optString("type_online"))
        }

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
