package com.chayzay.catequesisapp.settings

import android.content.Context

/** Conserva las claves SharedPreferences de la app antigua. */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("com.chayzay.catequesisapp_preferences", Context.MODE_PRIVATE)
    var news: Boolean
        get() = prefs.getBoolean("pref_key_news", true)
        set(value) { prefs.edit().putBoolean("pref_key_news", value).apply() }
    var sound: Boolean
        get() = prefs.getBoolean("pref_key_sound_games", false)
        set(value) { prefs.edit().putBoolean("pref_key_sound_games", value).apply() }
    var font: Int
        get() = prefs.getInt("pref_key_size_font_app", 2).coerceIn(1, 4)
        set(value) { prefs.edit().putInt("pref_key_size_font_app", value.coerceIn(1, 4)).apply() }
    fun extra(title: String): Boolean = prefs.getBoolean(key(title), false)
    fun setExtra(title: String, enabled: Boolean) { prefs.edit().putBoolean(key(title), enabled).apply() }
    private fun key(title: String): String = when (title) {
        "Ampliación" -> "pref_key_extension_theme"
        "Anécdotas" -> "pref_key_anecdote_theme"
        "Catecismo" -> "pref_key_catechism_theme"
        else -> throw IllegalArgumentException("Anexo desconocido")
    }
    fun downloaded(courseId: Int): Boolean = prefs.getBoolean("pref_key_dw_course_$courseId", false)
    fun setDownloaded(courseId: Int) { prefs.edit().putBoolean("pref_key_dw_course_$courseId", true).apply() }
}
