package com.chayzay.catequesisapp.help

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.auth.UserSession
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.data.ClassProgressStore
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ProgressSyncRepository
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val PRAYER_TEXT = "Ninguna catequesis se saca adelante sin oración y mortificación. Te agradeceremos que cuando leas esta nota, una vez al día, a la semana, al año o cuando quieras, reces algo, un Padrenuestro, una Avemaría o una simple jaculatoria, pidiendo que esta App haga mucho bien a la gente."

@Composable
fun HelpUsScreen(user: UserSession?, store: ClassProgressStore, courses: CourseRepository,
                 sync: ProgressSyncRepository, apiBaseUrl: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var askToSubmit by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(Color.White)) {
        TextButton(onClick = onBack) { Text("‹ Volver") }
        val entries = listOf(
            Triple(R.drawable.helpme_prayer, "Con oraciones", "Ayúdanos rezando por los frutos de Catequesis App."),
            Triple(R.drawable.helpme_teacher, "Catequista on-line", "Colabora como catequista cuando hayas completado los cursos."),
            Triple(R.drawable.helpme_paypal, "Con donaciones", "Ayuda al mantenimiento y crecimiento del proyecto.")
        )
        entries.forEachIndexed { index, item ->
            Card(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 2.dp)) {
                Row(Modifier.fillMaxWidth().background(Color.White).clickable(enabled = !loading) {
                    when (index) {
                        0 -> message = PRAYER_TEXT
                        1 -> {
                            if (user == null) message = "Solo es posible acceder si se registra :)"
                            else {
                                loading = true
                                scope.launch {
                                    try {
                                        sync.sync(user, store)
                                        val all = withContext(Dispatchers.IO) { courses.getCourses().map { it.id } }
                                        if (all.isEmpty() || all.any { !store.isCourseApproved(it) })
                                            message = "No tienes aún aprobado ningún curso. ¡Inténtalo de nuevo cuando lo apruebes!"
                                        else askToSubmit = true
                                    } catch (error: Exception) {
                                        message = ApiMessages.fromException(error, "No se pudieron comprobar los cursos")
                                    } finally { loading = false }
                                }
                            }
                        }
                        2 -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/donate?hosted_button_id=EV6956N6W2K4E"))
                            try { context.startActivity(intent) } catch (_: Exception) { message = "No se pudo abrir el enlace de donaciones" }
                        }
                    }
                }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(item.first), null, Modifier.size(48.dp))
                    Column(Modifier.padding(start = 5.dp)) {
                        Text(item.second, color = Color(0xFF7A9989), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(item.third, color = Color(0xFF424242), fontSize = 12.sp, modifier = Modifier.padding(start = 5.dp, top = 5.dp))
                    }
                }
            }
        }
        if (loading) CircularProgressIndicator(Modifier.padding(16.dp))
        message?.let { Text(it, Modifier.padding(16.dp)) }
    }
    if (askToSubmit && user != null) AlertDialog(onDismissRequest = { askToSubmit = false },
        title = { Text("Solicitud de catequista") },
        text = { Text("Felicidades cumples con los requisitos para ser catequista. ¿Deseas enviar una solicitud?") },
        confirmButton = { TextButton(onClick = {
            askToSubmit = false
            loading = true
            scope.launch {
                try {
                    message = withContext(Dispatchers.IO) { sendRequest(apiBaseUrl, user) }
                } catch (error: Exception) {
                    message = ApiMessages.fromException(error, "No se pudo enviar la solicitud")
                } finally { loading = false }
            }
        }) { Text("Enviar") } },
        dismissButton = { TextButton(onClick = { askToSubmit = false }) { Text("Cancelar") } })
}

private fun sendRequest(base: String, user: UserSession): String {
    val url = URL(base.trimEnd('/') + "/request_cateq/register")
    require(url.protocol == "https") { "La solicitud necesita HTTPS" }
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 12000
        readTimeout = 12000
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", user.apiKey)
    }
    try {
        connection.outputStream.use {
            it.write(JSONObject().put("id_user", user.id.toString()).toString().toByteArray(Charsets.UTF_8))
        }
        val body = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IllegalStateException("El servidor no respondió")
        val result = try { JSONObject(body) } catch (_: Exception) {
            throw IllegalStateException("Respuesta inválida del servidor")
        }
        return when (result.optString("status")) {
            "1" -> "Mensaje enviado."
            "2" -> {
                val status = result.optJSONObject("data")?.optString("status").orEmpty()
                if (status == "Solicitud") "Ya envió una solicitud, la misma no ha sido revisada."
                else "Ya envió una solicitud, el estado de la misma es: " +
                    ApiMessages.fromServer(status, "en revisión")
            }
            else -> throw IllegalStateException(ApiMessages.fromServer(result.optString("message"),
                "No se pudo enviar la solicitud"))
        }
    } finally { connection.disconnect() }
}
