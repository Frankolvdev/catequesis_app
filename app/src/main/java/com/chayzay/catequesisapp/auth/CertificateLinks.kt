package com.chayzay.catequesisapp.auth

import android.net.Uri
import android.util.Base64

/** Mismo enlace de certificado desde el perfil y desde el mapa del curso. */
fun certificateUri(apiBaseUrl: String, userId: Int, courseId: Int): Uri {
    val host = Uri.parse(apiBaseUrl).host ?: "www.catequesis.org"
    return Uri.Builder().scheme("https").authority(host)
        .appendPath("certificate").appendPath("certificate_course.php")
        .appendQueryParameter("user", Base64.encodeToString(
            userId.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
        .appendQueryParameter("course", Base64.encodeToString(
            courseId.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
        .build()
}
