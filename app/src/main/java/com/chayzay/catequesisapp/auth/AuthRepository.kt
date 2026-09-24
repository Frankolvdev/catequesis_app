package com.chayzay.catequesisapp.auth

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.chayzay.catequesisapp.data.ApiMessages

/** Contrato PHP original: user/loginApp y user/register_user, ambos POST JSON. */
class AuthRepository(private val baseUrl: String) {
    fun login(email: String, password: String): UserSession {
        val response = post("user/loginApp", JSONObject().put("email", email).put("password", password))
        val user = response.optJSONObject("usuario")
            ?: throw IllegalStateException("El servidor no devolvió los datos del usuario")
        val session = UserSession(user.optInt("id_user", -1), user.optString("email"),
            user.optString("first_name"), user.optString("last_name"),
            user.optString("api_key"), user.optString("picture"), user.optString("gender"))
        if (session.id <= 0 || session.apiKey.isBlank()) throw IllegalStateException("La respuesta de acceso está incompleta")
        return session
    }

    fun register(email: String, password: String, firstName: String, lastName: String, gender: String) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val base = baseUrl.substringBefore("/API/").trimEnd('/')
        post("user/register_user", JSONObject().put("email", email).put("password", password)
            .put("first_name", firstName).put("last_name", lastName)
            .put("profile", "STUDENT").put("gender", gender).put("active", "1")
            .put("picture", "$base/img/${if (gender == "MALE") "user.png" else "female.png"}")
            .put("date_created", date).put("date_modified", date).put("locale", "es"))
    }

    /** Contratos usados por User.updateUserVolley y User.deleteUserVolley en la app original. */
    fun update(user: UserSession, email: String, firstName: String, lastName: String, gender: String): UserSession {
        post("user/updateApp", JSONObject().put("api_key", user.apiKey).put("email", email)
            .put("first_name", firstName).put("last_name", lastName).put("gender", gender), user.apiKey)
        return user.copy(email = email, firstName = firstName, lastName = lastName, gender = gender)
    }

    fun delete(user: UserSession) {
        post("user/deleteFromApp", JSONObject().put("id_user", user.id.toString()), user.apiKey)
    }

    private fun post(path: String, body: JSONObject, authorization: String? = null): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + "/" + path)
        if (url.protocol != "https") throw IllegalStateException("El acceso necesita HTTPS")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            if (authorization != null) setRequestProperty("Authorization", authorization)
        }
        return try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val text = (if (connection.responseCode in 200..299) connection.inputStream
                else connection.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IllegalStateException("El servidor respondió ${connection.responseCode}")
            val result = try { JSONObject(text) } catch (_: Exception) {
                throw IllegalStateException("El servidor devolvió una respuesta inválida")
            }
            if (result.optString("status") != "1") {
                throw IllegalStateException(ApiMessages.fromServer(result.optString("message"),
                    "No se pudo completar la solicitud. Inténtalo nuevamente.",
                    registering = path == "user/register_user"))
            }
            result
        } finally { connection.disconnect() }
    }
}
