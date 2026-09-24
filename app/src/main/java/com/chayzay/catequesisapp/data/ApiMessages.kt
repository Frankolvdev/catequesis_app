package com.chayzay.catequesisapp.data

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Solo muestra al usuario mensajes del servidor que no contienen detalles internos. */
object ApiMessages {
    fun fromServer(raw: String?, fallback: String, registering: Boolean = false): String {
        val message = raw?.trim().orEmpty()
        if (registering && message.contains("email_unique", ignoreCase = true) &&
            (message.contains("duplicate entry", ignoreCase = true) ||
                message.contains("duplicate", ignoreCase = true))) {
            return "Este correo ya está registrado. Inicia sesión."
        }
        if (message.isBlank() || message.equals("null", ignoreCase = true)) return fallback
        if (listOf("SQLSTATE", "PDOException", "Fatal error", "Stack trace", "Duplicate entry",
                "Integrity constraint violation", "Non-static method", "Undefined variable",
                "Call to undefined", "/home/", "<html", "<br", "Notice:", "Warning:")
                .any { message.contains(it, ignoreCase = true) }) return fallback
        return message
    }

    fun fromException(error: Exception, fallback: String): String = when (error) {
        is SSLException -> "No se pudo establecer una conexión segura. Comprueba la fecha del dispositivo."
        is SocketTimeoutException -> "La conexión tardó demasiado. Inténtalo de nuevo."
        is UnknownHostException -> "No hay conexión con el servidor. Comprueba tu internet."
        else -> fromServer(error.localizedMessage, fallback)
    }
}
