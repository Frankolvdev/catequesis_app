package com.chayzay.catequesisapp.chat

import android.content.Context
import com.chayzay.catequesisapp.auth.UserSession
import com.google.firebase.database.DatabaseReference
import org.json.JSONArray
import org.json.JSONObject

/** Conserva en el dispositivo la copia Firebase de mensajes que PHP ya aceptó. */
class PendingRealtimeStore(context: Context) {
    private val prefs = context.getSharedPreferences("chat_realtime_outbox_v1", Context.MODE_PRIVATE)

    data class Pending(val id: String, val conversation: String, val recipient: ChatContact,
                       val message: String, val datetime: String)

    private fun all(): JSONArray = try { JSONArray(prefs.getString("items", "[]")) }
        catch (_: Exception) { JSONArray() }

    @Synchronized
    fun forUser(user: UserSession): List<Pending> {
        val rows = all()
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            if (row.optString("sender") != user.apiKey) return@mapNotNull null
            val key = row.optString("receiver")
            val conversation = row.optString("conversation")
            val datetime = row.optString("datetime")
            if (key.isBlank() || conversation.isBlank() || datetime.isBlank()) return@mapNotNull null
            Pending(row.optString("id"), conversation,
                ChatContact(key, row.optString("name"), row.optString("picture")),
                row.optString("message"), datetime)
        }
    }

    /** commit() completa la escritura local antes de intentar el envío a Firebase. */
    @Synchronized
    fun enqueue(user: UserSession, recipient: ChatContact, conversation: String,
                datetime: String, message: String): Pending {
        val id = "${user.apiKey}:${recipient.key}:$datetime"
        val pending = Pending(id, conversation, recipient, message, datetime)
        val rows = all()
        for (i in 0 until rows.length()) {
            if (rows.optJSONObject(i)?.optString("id") == id) return pending
        }
        rows.put(JSONObject().put("id", id).put("sender", user.apiKey)
            .put("receiver", recipient.key).put("name", recipient.name)
            .put("picture", recipient.picture).put("conversation", conversation)
            .put("datetime", datetime).put("message", message))
        check(prefs.edit().putString("items", rows.toString()).commit()) {
            "El servidor guardó el mensaje pero no se pudo guardar su publicación pendiente en el dispositivo"
        }
        return pending
    }

    @Synchronized
    private fun remove(id: String) {
        val input = all()
        val output = JSONArray()
        for (i in 0 until input.length()) {
            val row = input.optJSONObject(i) ?: continue
            if (row.optString("id") != id) output.put(row)
        }
        prefs.edit().putString("items", output.toString()).commit()
    }

    /** Firebase confirma la escritura; PHP no vuelve a recibir el mensaje al reintentar. */
    fun flush(root: DatabaseReference, user: UserSession, onResult: (Throwable?) -> Unit) {
        forUser(user).forEach { item ->
            val updates: Map<String, Any> = mapOf(
                "${item.conversation}/Users/${user.apiKey}" to mapOf(
                    "name_user" to user.displayName, "picture" to user.picture),
                "${item.conversation}/Users/${item.recipient.key}" to mapOf(
                    "name_user" to item.recipient.name, "picture" to item.recipient.picture),
                "${item.conversation}/Messages/${item.datetime}" to mapOf(
                    "user_send" to user.apiKey, "message" to item.message, "check" to false)
            )
            try {
                root.updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        remove(item.id)
                        onResult(null)
                    } else onResult(task.exception ?: IllegalStateException("Firebase no confirmó el mensaje"))
                }
            } catch (error: Exception) { onResult(error) }
        }
    }
}
