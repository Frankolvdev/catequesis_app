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
    var showRealActivities: Boolean
        get() = prefs.getBoolean("pref_key_real_activity_theme", false)
        set(value) { prefs.edit().putBoolean("pref_key_real_activity_theme", value).apply() }
    var showOnlineActivities: Boolean
        get() = prefs.getBoolean("pref_key_virtual_online_theme", false)
        set(value) { prefs.edit().putBoolean("pref_key_virtual_online_theme", value).apply() }
    var showOfflineActivities: Boolean
        get() = prefs.getBoolean("pref_key_virtual_offline_theme", false)
        set(value) { prefs.edit().putBoolean("pref_key_virtual_offline_theme", value).apply() }
    private fun key(title: String): String = when (title) {
        "Ampliación" -> "pref_key_extension_theme"
        "Anécdotas" -> "pref_key_anecdote_theme"
        "Catecismo" -> "pref_key_catechism_theme"
        else -> throw IllegalArgumentException("Anexo desconocido")
    }
    // Conserva exactamente las claves usadas por PreferencesStorage.java en la app publicada.
    // Esto permite reconocer recursos que el usuario ya había descargado antes de actualizar.
    private fun downloadKey(courseId: Int): String? = when (courseId) {
        1 -> "pref_key_dw_primera_comunion"
        2 -> "pref_key_dw_confirmacion"
        3 -> "pref_key_dw_prematrimonial"
        4 -> "pref_key_dw_matrimonio"
        5 -> "pref_key_dw_formacion_profesional"
        else -> null
    }
    fun downloaded(courseId: Int): Boolean = downloadKey(courseId)?.let { prefs.getBoolean(it, false) } ?: false
    fun setDownloaded(courseId: Int) {
        downloadKey(courseId)?.let { prefs.edit().putBoolean(it, true).apply() }
    }
}
