package com.chayzay.catequesisapp.contact

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.auth.UserSession
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.profile.ProfileSettings
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Temas y contrato de email_contact/register_contact_email de la app original. */
private val subjects = listOf(
    "Deseo asistir a charlas o retiros",
    "Preguntas sobre la fe católica",
    "Preguntas sobre los movimientos cristianos",
    "Quiero que el Papa rece por esta intención",
    "Necesito oraciones por una intención mía",
    "Sugerencias para la app",
    "Otro"
)

@Composable
fun ContactScreen(
    profile: ProfileSettings,
    session: UserSession?,
    apiBaseUrl: String,
    initialSubject: Int,
    onBack: () -> Unit
) {
    var email by remember(session?.email) { mutableStateOf(session?.email.orEmpty()) }
    var name by remember(session?.id) { mutableStateOf(session?.displayName.orEmpty()) }
    var content by remember { mutableStateOf("") }
    var subject by remember(initialSubject) { mutableStateOf(initialSubject.coerceIn(subjects.indices)) }
    var menuOpen by remember { mutableStateOf(false) }
    // Los accesos desde Fe y Oraciones fijaban el asunto en ContactsActivity.
    val subjectLocked = initialSubject != 6
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Color.White)) {
        // activity_contacts.xml: toolbar azul y formulario con 10dp laterales.
        Row(Modifier.fillMaxWidth().background(Color(0xFF037AD8)).padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_baseline_arrow_back_ios_24), contentDescription = "Volver",
                modifier = Modifier.size(40.dp).padding(end = 16.dp).clickable { onBack() })
            Text("Contactar", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            OutlinedTextField(value = email, onValueChange = { email = it.trim() },
                placeholder = { Text("Correo electrónico", fontSize = 13.sp) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(5.dp),
                colors = legacyFieldColors())
            OutlinedTextField(value = name, onValueChange = { name = it },
                placeholder = { Text("Nombre", fontSize = 13.sp) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(5.dp),
                colors = legacyFieldColors())
            if (subjectLocked) {
                Text(subjects[subject], fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(10.dp))
            } else {
                Row(Modifier.fillMaxWidth().border(1.dp, Color(0xFFACACAC), RoundedCornerShape(5.dp))
                    .clickable { menuOpen = true }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(subjects[subject], fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Image(painterResource(R.drawable.arrow_down), contentDescription = "Abrir", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    subjects.forEachIndexed { index, value -> DropdownMenuItem(text = { Text(value) }, onClick = {
                        subject = index; menuOpen = false
                    }) }
                }
            }
            OutlinedTextField(value = content, onValueChange = { content = it },
                placeholder = { Text("Mensaje", fontSize = 13.sp) }, modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(5.dp), colors = legacyFieldColors())
            if (loading) CircularProgressIndicator()
            message?.let { Text(it, fontSize = 13.sp) }
            Button(enabled = !loading, modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF446353)),
                shape = androidx.compose.ui.graphics.RectangleShape, onClick = {
                    message = when {
                        email.isBlank() || name.isBlank() || content.isBlank() -> "Este campo es requerido"
                        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Correo electrónico no es válido"
                        else -> null
                    }
                    if (message == null) scope.launch {
                        loading = true
                        try {
                            withContext(Dispatchers.IO) { sendContact(apiBaseUrl, name.trim(), email, content.trim(), subjects[subject]) }
                            message = "Mensaje enviado."; onBack()
                        } catch (error: Exception) { message = ApiMessages.fromException(error, "No se pudo enviar el mensaje") }
                        finally { loading = false }
                    }
                }) { Text("Enviar", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun legacyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
    focusedBorderColor = Color(0xFFBBBBBB), unfocusedBorderColor = Color(0xFFBBBBBB),
    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
    focusedPlaceholderColor = Color(0xFFB7B7B7), unfocusedPlaceholderColor = Color(0xFFB7B7B7)
)

private fun sendContact(baseUrl: String, name: String, email: String, content: String, subject: String) {
    val url = URL(baseUrl.trimEnd('/') + "/email_contact/register_contact_email")
    require(url.protocol == "https") { "El formulario necesita HTTPS" }
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 12000
        readTimeout = 12000
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
    }
    try {
        val request = JSONObject().put("name_user", name).put("email", email)
            .put("content", content).put("subject", subject).put("read_check", "0")
            .put("locale", Locale.getDefault().language)
        connection.outputStream.use { it.write(request.toString().toByteArray(Charsets.UTF_8)) }
        val text = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: throw IllegalStateException("El servidor no respondió")
        val response = try { JSONObject(text) } catch (_: Exception) {
            throw IllegalStateException("Respuesta inválida del servidor")
        }
        if (response.optString("status") != "1") {
            throw IllegalStateException(ApiMessages.fromServer(response.optString("message"),
                "No se pudo enviar el mensaje"))
        }
    } finally {
        connection.disconnect()
    }
}
