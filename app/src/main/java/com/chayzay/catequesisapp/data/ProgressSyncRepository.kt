package com.chayzay.catequesisapp.data

import com.chayzay.catequesisapp.auth.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Une las aprobaciones en ambos sentidos sin borrar avances de una instalación sin conexión. */
class ProgressSyncRepository(private val baseUrl: String) {
    suspend fun sync(user: UserSession, store: ClassProgressStore): SyncResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("id_user", user.id)
        // Leer las dos listas antes de enviar una sola modificación.
        val remoteTests = readApproved("test_class/all", user, body, "id_class_course", true)
        val remoteCourses = readApproved("course_approved/all", user, body, "id_course", false)
        store.mergeApprovals(remoteTests, remoteCourses)
        val pendingTests = store.approvedTests() - remoteTests
        val pendingCourses = store.approvedCourses() - remoteCourses
        if (pendingTests.isNotEmpty()) {
            val data = JSONArray()
            pendingTests.sorted().forEach { data.put(JSONObject().put("id_class_course", it)
                .put("approved", 1).put("score", 10)) }
            post("test_class/manipulate", user, JSONObject().put("id_user", user.id).put("data", data))
        }
        if (pendingCourses.isNotEmpty()) {
            val data = JSONArray()
            pendingCourses.sorted().forEach { data.put(JSONObject().put("id_course", it).put("approved", 1)) }
            post("course_approved/manipulate", user, JSONObject().put("id_user", user.id).put("data", data))
        }
        SyncResult(remoteTests.size + remoteCourses.size, pendingTests.size + pendingCourses.size)
    }

    private fun readApproved(path: String, user: UserSession, body: JSONObject,
                             idField: String, scoreRequired: Boolean): Set<Int> {
        val response = post(path, user, body, emptyStatus = true)
        val data = response.optJSONArray("data") ?: return emptySet()
        return (0 until data.length()).mapNotNull { index ->
            val row = data.optJSONObject(index) ?: return@mapNotNull null
            row.optInt(idField).takeIf { it > 0 && row.optInt("approved") == 1 &&
                (!scoreRequired || row.optInt("score") == 10) }
        }.toSet()
    }

    private fun post(path: String, user: UserSession, body: JSONObject, emptyStatus: Boolean = false): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + "/" + path)
        require(url.protocol == "https") { "La sincronización requiere HTTPS" }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", user.apiKey)
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val text = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IllegalStateException("El servidor respondió ${connection.responseCode}")
            val result = try { JSONObject(text) } catch (_: Exception) {
                throw IllegalStateException("El servidor devolvió datos inválidos")
            }
            val status = result.optString("status")
            if (status != "1" && !(emptyStatus && status == "2" &&
                result.optString("message").equals("No hay datos", ignoreCase = true)))
                throw IllegalStateException(ApiMessages.fromServer(result.optString("message"),
                    "Error al sincronizar el progreso"))
            result
        } finally { connection.disconnect() }
    }
}

data class SyncResult(val recovered: Int, val uploaded: Int)
