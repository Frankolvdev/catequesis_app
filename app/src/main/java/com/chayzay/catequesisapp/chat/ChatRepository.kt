package com.chayzay.catequesisapp.chat

import com.chayzay.catequesisapp.auth.UserSession
import com.chayzay.catequesisapp.profile.ProfileSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Compatible con person_cateq/get_users_cateq y message/register_message del cliente original. */
data class ChatContact(val key: String, val name: String, val picture: String)

class ChatRepository(private val baseUrl: String) {
    suspend fun catechists(user: UserSession, profile: ProfileSettings): List<ChatContact> = withContext(Dispatchers.IO) {
        val response = post("person_cateq/get_users_cateq", user.apiKey,
            JSONObject().put("gender", profile.gender).put("language", Locale.getDefault().language), allowEmpty = true)
        val rows = response.optJSONArray("data") ?: return@withContext emptyList()
        (0 until rows.length()).mapNotNull { index ->
            val item = rows.optJSONObject(index) ?: return@mapNotNull null
            val key = item.optString("api_key")
            if (key.isBlank() || key == user.apiKey) return@mapNotNull null
            ChatContact(key, listOf(item.optString("first_name"), item.optString("last_name"))
                .filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Catequista" }, item.optString("picture"))
        }
    }

    /** La API asigna la clave datetime; usarla conserva los mensajes en el backoffice. */
    suspend fun send(user: UserSession, receiver: String, message: String): String = withContext(Dispatchers.IO) {
        val response = post("message/register_message", user.apiKey,
            JSONObject().put("api_key_send", user.apiKey)
                .put("api_key_receiver", receiver).put("message_text", message))
        response.optString("datetime").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("El servidor no devolvió la fecha del mensaje")
    }

    private fun post(path: String, key: String, body: JSONObject, allowEmpty: Boolean = false): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + "/" + path)
        require(url.protocol == "https") { "El chat necesita HTTPS" }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", key)
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val response = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IllegalStateException("El servidor respondió ${connection.responseCode}")
            val result = try { JSONObject(response) } catch (_: Exception) {
                throw IllegalStateException("Respuesta inválida del servidor")
            }
            if (result.optString("status") != "1" && !(allowEmpty && result.optString("status") == "11"))
                throw IllegalStateException(result.optString("message").ifBlank { "No se completó la operación" })
            result
        } finally { connection.disconnect() }
    }
}
