package com.chayzay.catequesisapp.chat

import android.content.Context
import com.chayzay.catequesisapp.auth.UserSession
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class GuestChatMessage(val recipient: String, val name: String, val picture: String,
                            val text: String, val time: String, val id: String,
                            val serverDatetime: String = "", val conversation: String = "",
                            val acceptedUser: String = "")

/** Los mensajes del chat invitado anterior se almacenaban solo en SharedPreferences. */
class GuestChatStore(context: Context) {
    private val prefs = context.getSharedPreferences("guest_chat_local_v1", Context.MODE_PRIVATE)
    private val legacy = context.getSharedPreferences(
        "com.chayzay.catequesisapp_preferences_messages", Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<GuestChatMessage> {
        val source = prefs.getString("items", null)
        if (source != null) return parse(source)
        val recovered = legacy.all.entries.sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
            .flatMap { entry -> (entry.value as? Set<*>)?.filterIsInstance<String>().orEmpty() }
            .mapIndexedNotNull { index, row ->
                val columns = row.split('|', limit = 5)
                if (columns.size < 5 || columns[0].isBlank()) null else
                    GuestChatMessage(columns[0], columns[1], columns[4], columns[2], columns[3],
                        legacyId(index, row))
            }
        prefs.edit().putString("items", encode(recovered)).commit()
        return recovered
    }

    fun pendingFor(user: UserSession): List<GuestChatMessage> = all().filter {
        it.acceptedUser.isBlank() || it.acceptedUser == user.apiKey
    }

    @Synchronized
    fun add(contact: ChatContact, text: String): GuestChatMessage {
        val content = text.trim()
        require(content.isNotEmpty() && content.length <= 500) { "Escribe un mensaje de hasta 500 caracteres" }
        val item = GuestChatMessage(contact.key, contact.name, contact.picture, content,
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()), UUID.randomUUID().toString())
        check(prefs.edit().putString("items", encode(all() + item)).commit()) {
            "No se pudo guardar el mensaje en este teléfono"
        }
        return item
    }

    /** La confirmación PHP se persiste antes de publicar el mensaje en Firebase. */
    @Synchronized
    fun markAccepted(id: String, datetime: String, conversation: String, user: UserSession) {
        val updated = all().map { if (it.id == id) it.copy(serverDatetime = datetime,
            conversation = conversation, acceptedUser = user.apiKey) else it }
        check(prefs.edit().putString("items", encode(updated)).commit()) {
            "El servidor guardó el mensaje; no se pudo registrar la confirmación en este teléfono"
        }
    }

    @Synchronized
    fun removeSent(id: String) {
        check(prefs.edit().putString("items", encode(all().filterNot { it.id == id })).commit()) {
            "El mensaje se publicó pero no pudo retirarse de los pendientes locales"
        }
    }

    private fun legacyId(index: Int, row: String): String = "legacy_${index}_" +
        UUID.nameUUIDFromBytes(row.toByteArray(Charsets.UTF_8)).toString()

    private fun parse(text: String): List<GuestChatMessage> = try {
        val array = JSONArray(text)
        (0 until array.length()).mapNotNull { index ->
            val row = array.optJSONObject(index) ?: return@mapNotNull null
            val recipient = row.optString("recipient")
            if (recipient.isBlank()) null else GuestChatMessage(recipient, row.optString("name"),
                row.optString("picture"), row.optString("text"), row.optString("time"),
                row.optString("id").ifBlank { legacyId(index, row.toString()) },
                row.optString("serverDatetime"), row.optString("conversation"),
                row.optString("acceptedUser"))
        }
    } catch (_: Exception) { emptyList() }

    private fun encode(items: List<GuestChatMessage>): String = JSONArray().apply {
        items.forEach { put(JSONObject().put("recipient", it.recipient).put("name", it.name)
            .put("picture", it.picture).put("text", it.text).put("time", it.time)
            .put("id", it.id).put("serverDatetime", it.serverDatetime)
            .put("conversation", it.conversation).put("acceptedUser", it.acceptedUser)) }
    }.toString()
}
