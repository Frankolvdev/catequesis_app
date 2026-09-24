package com.chayzay.catequesisapp.chat

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GuestChatMessage(val recipient: String, val name: String, val picture: String,
                            val text: String, val time: String)

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
            .mapNotNull { row ->
                val columns = row.split('|', limit = 5)
                if (columns.size < 5 || columns[0].isBlank()) null else
                    GuestChatMessage(columns[0], columns[1], columns[4], columns[2], columns[3])
            }
        prefs.edit().putString("items", encode(recovered)).commit()
        return recovered
    }

    @Synchronized
    fun add(contact: ChatContact, text: String): GuestChatMessage {
        val content = text.trim()
        require(content.isNotEmpty() && content.length <= 500) { "Escribe un mensaje de hasta 500 caracteres" }
        val item = GuestChatMessage(contact.key, contact.name, contact.picture, content,
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        check(prefs.edit().putString("items", encode(all() + item)).commit()) {
            "No se pudo guardar el mensaje en este teléfono"
        }
        return item
    }

    private fun parse(text: String): List<GuestChatMessage> = try {
        val array = JSONArray(text)
        (0 until array.length()).mapNotNull { index ->
            val row = array.optJSONObject(index) ?: return@mapNotNull null
            val recipient = row.optString("recipient")
            if (recipient.isBlank()) null else GuestChatMessage(recipient, row.optString("name"),
                row.optString("picture"), row.optString("text"), row.optString("time"))
        }
    } catch (_: Exception) { emptyList() }

    private fun encode(items: List<GuestChatMessage>): String = JSONArray().apply {
        items.forEach { put(JSONObject().put("recipient", it.recipient).put("name", it.name)
            .put("picture", it.picture).put("text", it.text).put("time", it.time)) }
    }.toString()
}
