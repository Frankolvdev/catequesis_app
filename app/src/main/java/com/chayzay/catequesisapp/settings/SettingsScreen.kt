package com.chayzay.catequesisapp.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.data.Course
import com.chayzay.catequesisapp.data.CourseImageRepository
import com.chayzay.catequesisapp.data.CourseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(repository: CourseRepository, images: CourseImageRepository,
                   onFontChanged: (Int) -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    var news by remember { mutableStateOf(prefs.news) }
    var sound by remember { mutableStateOf(prefs.sound) }
    var font by remember { mutableIntStateOf(prefs.font) }
    var extras by remember { mutableStateOf(listOf("Ampliación", "Anécdotas", "Catecismo")
        .associateWith { prefs.extra(it) }) }
    var showRealActivities by remember { mutableStateOf(prefs.showRealActivities) }
    var showOnlineActivities by remember { mutableStateOf(prefs.showOnlineActivities) }
    var showOfflineActivities by remember { mutableStateOf(prefs.showOfflineActivities) }
    var courses by remember { mutableStateOf<List<Course>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ajustes")
        Text("Recibir noticias de actualidad sobre la Iglesia")
        Switch(checked = news, onCheckedChange = { news = it; prefs.news = it })
        Button(onClick = {
            try { context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))) }
            catch (_: Exception) { message = "No se pudo abrir Google Play" }
        }) { Text("Buscar actualizaciones de la app") }
        Text("Tamaño de la letra")
        listOf("Pequeño", "Mediano", "Grande", "Extra grande").forEachIndexed { index, name ->
            TextButton(onClick = { font = index + 1; prefs.font = font; onFontChanged(font) }) {
                Text("${if (font == index + 1) "●" else "○"} $name")
            }
        }
        Text("Opciones extra de lectura: mostrar abierto al entrar")
        extras.forEach { (name, enabled) ->
            Text(name)
            Switch(enabled, onCheckedChange = { checked ->
                prefs.setExtra(name, checked)
                extras = extras + (name to checked)
            })
        }
        Text("Actividades reales")
        Switch(showRealActivities, onCheckedChange = {
            showRealActivities = it; prefs.showRealActivities = it
        })
        Text("Actividades virtuales off-line")
        Switch(showOfflineActivities, onCheckedChange = {
            showOfflineActivities = it; prefs.showOfflineActivities = it
        })
        Text("Actividades virtuales on-line")
        Switch(showOnlineActivities, onCheckedChange = {
            showOnlineActivities = it; prefs.showOnlineActivities = it
        })
        Text("Sonido en los juegos")
        Switch(sound, onCheckedChange = { sound = it; prefs.sound = it })
        Button(enabled = !loading, onClick = {
            if (courses == null) scope.launch {
                loading = true
                message = ""
                try { courses = withContext(Dispatchers.IO) { repository.getCourses() } }
                catch (cause: Exception) { message = ApiMessages.fromException(cause,
                    "No se pudieron cargar los cursos") }
                finally { loading = false }
            }
        }) { Text("Descargar recursos para usarlos sin internet") }
        courses?.forEach { course ->
            Button(enabled = !loading, modifier = Modifier.fillMaxWidth(), onClick = {
                scope.launch {
                    loading = true
                    message = "Descargando ${course.name}…"
                    try {
                        val count = withContext(Dispatchers.IO) { repository.downloadCourseOffline(course, images) }
                        prefs.setDownloaded(course.id)
                        message = "${course.name}: contenido e imágenes guardados ($count imágenes de actividades)."
                    } catch (cause: Exception) { message = ApiMessages.fromException(cause,
                        "No se pudo completar la descarga. Inténtalo de nuevo.") }
                    finally { loading = false }
                }
            }) { Text("${course.name}${if (prefs.downloaded(course.id)) " ✓" else ""}") }
        }
        if (loading) CircularProgressIndicator()
        if (message.isNotBlank()) Text(message)
    }
}
