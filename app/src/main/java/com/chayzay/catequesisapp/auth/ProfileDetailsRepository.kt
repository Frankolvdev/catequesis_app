package com.chayzay.catequesisapp.auth

import com.chayzay.catequesisapp.data.ApiMessages
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ProfileOption(val id: Int, val label: String)
data class PersonalData(val countryId: Int = 0, val city: String = "", val address: String = "",
    val civilStatusId: Int = 0, val phone: String = "", val birthDate: String = "")

/** Mismos endpoints y campos de Person_data / Person_social_contact / User anteriores. */
class ProfileDetailsRepository(private val baseUrl: String) {
    fun countries(): List<ProfileOption> = options("country/all", "id_country", "name_country")
    fun civilStatuses(): List<ProfileOption> = options("civil_status/all", "id_civil_status", "name_civil_status")

    private fun options(path: String, id: String, name: String): List<ProfileOption> {
        val response = request(path)
        val rows = response.optJSONArray("data") ?: return emptyList()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val code = row.optInt(id)
            val label = row.optString(name)
            if (code <= 0 || label.isBlank()) null else ProfileOption(code, label)
        }
    }

    fun personal(user: UserSession): PersonalData? {
        val response = request("person_data/person_dataApp", JSONObject().put("api_key", user.apiKey), user,
            emptyStatus = true)
        val data = response.optJSONObject("data") ?: return null
        return PersonalData(data.optInt("id_country"), data.optString("city"),
            data.optString("address"), data.optInt("id_civil_status"), data.optString("phone"),
            data.optString("birthdate"))
    }

    fun savePersonal(user: UserSession, data: PersonalData) {
        request("person_data/allconfirm", JSONObject().put("id_user", user.id.toString())
            .put("id_country", data.countryId.toString()).put("city", data.city)
            .put("address", data.address).put("id_civil_status", data.civilStatusId.toString())
            .put("phone", data.phone).put("birthdate", data.birthDate), user)
    }

    fun social(user: UserSession): Map<String, String> {
        val response = request("person_social_contact/get_data", JSONObject().put("id_user", user.id), user,
            emptyStatus = true)
        val rows = response.optJSONArray("data") ?: return emptyMap()
        return (0 until rows.length()).mapNotNull { index ->
            val item = rows.optJSONObject(index) ?: return@mapNotNull null
            item.optString("provider").takeIf { it.isNotBlank() }?.let { it to item.optString("contact_name") }
        }.toMap()
    }

    fun saveSocial(user: UserSession, contacts: Map<String, String>) {
        // La app vieja hacía cuatro POST consecutivos al endpoint allConfirm.
        listOf("FACEBOOK", "GOOGLE+", "TWITTER", "INSTAGRAM").forEach { provider ->
            request("person_social_contact/allConfirm", JSONObject().put("id_user", user.id.toString())
                .put("provider", provider).put("contact_name", contacts[provider].orEmpty()), user)
        }
    }

    fun savePicture(user: UserSession, imageBase64: String): UserSession {
        val response = request("user/update_picture_api", JSONObject().put("api_key", user.apiKey)
            .put("imageBase64", imageBase64).put("extensionFile", "jpg"), user,
            messageIsStatus = true)
        val image = response.optString("picture")
        if (image.isBlank()) throw IllegalStateException("El servidor no devolvió la nueva foto")
        return user.copy(picture = image.replaceFirst("http://", "https://"))
    }

    private fun request(path: String, body: JSONObject? = null, user: UserSession? = null,
                        emptyStatus: Boolean = false, messageIsStatus: Boolean = false): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + "/" + path)
        require(url.protocol == "https") { "El perfil necesita HTTPS" }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = if (body == null) "GET" else "POST"
            doOutput = body != null
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            if (body != null) setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (user != null) setRequestProperty("Authorization", user.apiKey)
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val text = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IllegalStateException("El servidor respondió ${connection.responseCode}")
            val result = try { JSONObject(text) } catch (_: Exception) {
                throw IllegalStateException("El servidor devolvió datos inválidos")
            }
            val status = if (messageIsStatus) result.optString("message") else result.optString("status")
            if (status != "1" && !(emptyStatus && status == "11")) {
                val raw = if (messageIsStatus && status.toIntOrNull() != null) result.optString("error")
                    else result.optString("message")
                throw IllegalStateException(ApiMessages.fromServer(raw,
                    "No se pudo completar la operación"))
            }
            return result
        } finally { connection.disconnect() }
    }
}
