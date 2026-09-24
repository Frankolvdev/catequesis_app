package com.chayzay.catequesisapp.profile

import android.content.Context
import androidx.compose.ui.graphics.Color
import java.util.Calendar

/** Reutiliza el archivo y las claves SharedPreferences de la aplicación publicada. */
data class ProfileSettings(val birthYear: Int, val gender: String) {
    val age: Int get() = Calendar.getInstance().get(Calendar.YEAR) - birthYear
    val accent: Color get() {
        val index = when (age) {
            in 0..11 -> 0
            in 12..15 -> 1
            in 16..20 -> 2
            in 21..30 -> 3
            in 31..45 -> 4
            else -> 5
        }
        val male = longArrayOf(0xFF037AD8, 0xFF036EC3, 0xFF0266B4,
            0xFF025BA2, 0xFF02579A, 0xFF02528C)
        val female = longArrayOf(0xFF916BF0, 0xFF8A65E4, 0xFF835FD7,
            0xFF694CAE, 0xFF512DA7, 0xFF523696)
        return Color((if (gender == "MALE") male else female)[index].toInt())
    }
    val baseColor: Color get() = Color(if (gender == "MALE") 0xFF90CAF8.toInt() else 0xFFD2C3EA.toInt())

    fun save(context: Context) {
        preferences(context).edit()
            .putString(GENDER_KEY, gender)
            .putString(BIRTHDATE_KEY, "01/01/$birthYear")
            .putBoolean("pref_key_init_config", true)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "com.chayzay.catequesisapp_preferences"
        private const val GENDER_KEY = "pref_key_gender"
        private const val BIRTHDATE_KEY = "pref_key_birthdate"
        private fun preferences(context: Context) =
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

        fun load(context: Context): ProfileSettings? {
            val prefs = preferences(context)
            val gender = prefs.getString(GENDER_KEY, null) ?: return null
            val year = prefs.getString(BIRTHDATE_KEY, null)?.substringAfterLast('/')?.toIntOrNull()
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            if (gender !in listOf("MALE", "FEMALE") || year == null ||
                year !in 1940..currentYear || currentYear - year < 8) return null
            return ProfileSettings(year, gender)
        }
    }
}
