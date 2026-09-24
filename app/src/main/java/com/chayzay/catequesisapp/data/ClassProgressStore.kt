package com.chayzay.catequesisapp.data

import android.content.Context
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

/** Respeta las claves class_<id>, bold_font_view y class_finished del proyecto antiguo. */
class ClassProgressStore(private val context: Context) {
    private val internal = File(context.filesDir, "class_course_user.json")

    private fun read(): JSONObject {
        if (!internal.exists()) {
            // Archivos.getExternalBase de la app vieja cambió su base en Android 10.
            val external = context.getExternalFilesDir(null)
            val legacy = listOfNotNull(
                external?.resolve("Android/data/${context.packageName}/db/json/class_course_user.json"),
                external?.parentFile?.resolve("db/json/class_course_user.json")
            ).firstOrNull { it.isFile }
            if (legacy?.isFile == true) {
                try { internal.writeText(legacy.readText(Charsets.UTF_8), Charsets.UTF_8) }
                catch (_: Exception) { }
            }
        }
        return try { if (internal.isFile) JSONObject(internal.readText(Charsets.UTF_8)) else JSONObject() }
        catch (_: Exception) { JSONObject() }
    }

    fun flags(id: Int): Flags {
        val entry = read().optJSONObject("class_$id")
        return Flags(entry?.optBoolean("bold_font_view", false) == true,
            entry?.optBoolean("class_finished", false) == true)
    }

    fun setVisited(id: Int, visited: Boolean) {
        val all = read()
        val key = "class_$id"
        val entry = all.optJSONObject(key) ?: JSONObject().put("class_finished", false)
        entry.put("bold_font_view", visited)
        all.put(key, entry)
        internal.writeText(all.toString(), Charsets.UTF_8)
    }

    fun markPassed(id: Int) {
        val all = read()
        val key = "class_$id"
        val entry = all.optJSONObject(key) ?: JSONObject()
        entry.put("bold_font_view", true)
        entry.put("class_finished", true)
        all.put(key, entry)
        internal.writeText(all.toString(), Charsets.UTF_8)

        val file = File(context.filesDir, "test_class.json")
        val tests = try { if (file.isFile) JSONArray(file.readText(Charsets.UTF_8)) else JSONArray() }
            catch (_: Exception) { JSONArray() }
        var found = false
        for (position in 0 until tests.length()) {
            val item = tests.optJSONObject(position) ?: continue
            if (item.optInt("id_class_course", -1) == id) {
                item.put("approved", 1).put("score", 10)
                found = true
                break
            }
        }
        if (!found) tests.put(JSONObject().put("approved", 1).put("score", 10).put("id_class_course", id))
        file.writeText(tests.toString(), Charsets.UTF_8)
    }

    data class Flags(val visited: Boolean, val completed: Boolean)
}
