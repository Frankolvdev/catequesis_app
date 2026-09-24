package com.chayzay.catequesisapp.links

import android.net.Uri

/** Conserva la ruta y parámetros de los enlaces antiguos, pero abre sólo HTTPS. */
object HttpsLinks {
    fun external(raw: String): Uri? {
        val parsed = try { Uri.parse(raw.trim()) } catch (_: Exception) { return null }
        if (parsed.host.isNullOrBlank() || parsed.scheme !in listOf("http", "https")) return null
        return parsed.buildUpon().scheme("https").build()
    }

    /** Los textos del servidor pueden incluir hipervínculos de la época HTTP. */
    fun html(source: String): String = source.replace(Regex("http://", RegexOption.IGNORE_CASE), "https://")
}
