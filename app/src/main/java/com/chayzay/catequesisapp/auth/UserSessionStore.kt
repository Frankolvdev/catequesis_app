package com.chayzay.catequesisapp.auth

import android.content.Context
import org.json.JSONObject

data class UserSession(val id: Int, val email: String, val firstName: String, val lastName: String,
    val apiKey: String, val picture: String, val gender: String) {
    val displayName: String get() = "$firstName $lastName".trim().ifEmpty { email }
}

/** La clave de API devuelta por loginApp identifica al usuario en API y Chat. */
class UserSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "com.chayzay.catequesisapp_user_session_v2", Context.MODE_PRIVATE)

    fun load(): UserSession? {
        return try {
            val item = JSONObject(preferences.getString("session", null) ?: return null)
            val user = UserSession(item.getInt("id_user"), item.getString("email"),
                item.optString("first_name"), item.optString("last_name"),
                item.getString("api_key"), item.optString("picture"), item.optString("gender"))
            user.takeIf { it.id > 0 && it.apiKey.isNotBlank() }
        } catch (_: Exception) { null }
    }

    fun save(user: UserSession) {
        preferences.edit().putString("session", JSONObject()
            .put("id_user", user.id).put("email", user.email)
            .put("first_name", user.firstName).put("last_name", user.lastName)
            .put("api_key", user.apiKey).put("picture", user.picture)
            .put("gender", user.gender).toString()).apply()
    }

    fun clear() { preferences.edit().remove("session").apply() }
}
