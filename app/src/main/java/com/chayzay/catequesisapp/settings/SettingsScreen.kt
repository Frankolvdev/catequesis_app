package com.chayzay.catequesisapp.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.data.Course
import com.chayzay.catequesisapp.data.CourseImageRepository
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.profile.ProfileSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsPage { MAIN, DOWNLOADS, EXTRA }

@Composable
fun SettingsScreen(repository: CourseRepository, images: CourseImageRepository,
                   onFontChanged: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) { AppPreferences(context) }
    val accent = remember { ProfileSettings.load(context)?.accent ?: Color(0xFF037AD8) }
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(SettingsPage.MAIN) }
    var news by remember { mutableStateOf(prefs.news) }
    var sound by remember { mutableStateOf(prefs.sound) }
    var font by remember { mutableIntStateOf(prefs.font) }
    var fontDialog by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var courses by remember { mutableStateOf<List<Course>?>(null) }

    var extras by remember {
        mutableStateOf(listOf(
            "Mostrar ampliación de la clase" to prefs.extra("Ampliación"),
            "Mostrar anécdotas" to prefs.extra("Anécdotas"),
            "Mostrar catecismo" to prefs.extra("Catecismo"),
            "Mostrar actividades reales" to prefs.showRealActivities,
            "Mostrar actividades virtuales off-line" to prefs.showOfflineActivities,
            "Mostrar actividades virtuales on-line" to prefs.showOnlineActivities
        ))
    }
    BackHandler(enabled = page != SettingsPage.MAIN) { page = SettingsPage.MAIN }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        LegacySettingsToolbar(
            title = when (page) {
                SettingsPage.MAIN -> "Ajustes"
                SettingsPage.DOWNLOADS -> "Descargar recursos para usar la app sin internet"
                SettingsPage.EXTRA -> "Opciones extra de lectura"
            }, accent = accent, showBack = page != SettingsPage.MAIN,
            onBack = { page = SettingsPage.MAIN }
        )
        when (page) {
            SettingsPage.MAIN -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                LegacySwitchRow("¿Desea recibir noticias de actualidad sobre la Iglesia Católica?", news) {
                    news = it; prefs.news = it
                }
                LegacyPlainRow("Buscar actualizaciones de la app") {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))) }
                    catch (_: Exception) { message = "No se pudo abrir Google Play" }
                }
                LegacyPlainRow("Descargar recursos para usar la app sin internet") { page = SettingsPage.DOWNLOADS }
                LegacyTwoLineRow("Tamaño de la letra", "Selección: ${fontName(font)}") { fontDialog = true }
                LegacyPlainRow("Opciones extra de lectura") { page = SettingsPage.EXTRA }
                LegacySwitchRow("Sonido en los juegos", sound) { sound = it; prefs.sound = it }
                if (message.isNotBlank()) Text(message, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
            SettingsPage.EXTRA -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                extras.forEachIndexed { index, pair ->
                    LegacySwitchRow(pair.first, pair.second) { checked ->
                        when (index) {
                            0 -> prefs.setExtra("Ampliación", checked)
                            1 -> prefs.setExtra("Anécdotas", checked)
                            2 -> prefs.setExtra("Catecismo", checked)
                            3 -> prefs.showRealActivities = checked
                            4 -> prefs.showOfflineActivities = checked
                            5 -> prefs.showOnlineActivities = checked
                        }
                        extras = extras.toMutableList().also { it[index] = pair.first to checked }
                    }
                }
            }
            SettingsPage.DOWNLOADS -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                LaunchedEffect(Unit) {
                    if (courses == null) {
                        loading = true
                        try { courses = withContext(Dispatchers.IO) { repository.getCourses().filter { it.id == 1 || it.id == 5 } } }
                        catch (cause: Exception) { message = ApiMessages.fromException(cause, "No se pudieron cargar los cursos") }
                        finally { loading = false }
                    }
                }
                courses?.forEach { course ->
                    LegacyDownloadRow(course.name, prefs.downloaded(course.id), loading) {
                        scope.launch {
                            loading = true; message = "Descargando ${course.name}…"
                            try {
                                withContext(Dispatchers.IO) { repository.downloadCourseOffline(course, images) }
                                prefs.setDownloaded(course.id)
                                courses = courses?.toList()
                                message = "${course.name}: contenido descargado correctamente."
                            } catch (cause: Exception) {
                                message = ApiMessages.fromException(cause, "No se pudo completar la descarga. Inténtalo de nuevo.")
                            } finally { loading = false }
                        }
                    }
                }
                if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
                if (message.isNotBlank()) Text(message, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
            }
        }
    }

    if (fontDialog) androidx.compose.ui.window.Dialog(onDismissRequest = { fontDialog = false }) {
        // dialog_settings_multiselected.xml: contenido blanco, márgenes de 8dp, título 13sp
        // y Cancelar como una fila de ancho completo, sin la geometría de AlertDialog Material 3.
        Surface(color = Color.White, shape = androidx.compose.ui.graphics.RectangleShape) {
            Column(Modifier.fillMaxWidth()) {
                Text("Tamaño de la letra", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = Color.Black, modifier = Modifier.padding(8.dp))
                Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    listOf("Pequeño", "Mediano", "Grande", "Extra grande").forEachIndexed { i, name ->
                        Row(Modifier.fillMaxWidth().clickable {
                            font = i + 1; prefs.font = font; onFontChanged(font); fontDialog = false
                        }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = font == i + 1, onClick = null)
                            Text(name, color = Color(0xFF333333), fontSize = 13.sp)
                        }
                    }
                }
                Text("Cancelar", color = Color(0xFF2C7CB1), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().clickable { fontDialog = false }.padding(10.dp))
            }
        }
    }
}

private fun fontName(font: Int) = listOf("Pequeño", "Mediano", "Grande", "Extra grande").getOrElse(font - 1) { "Mediano" }

@Composable private fun LegacySettingsToolbar(title: String, accent: Color, showBack: Boolean, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).background(accent).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) Icon(painterResource(R.drawable.ic_baseline_arrow_back_ios_24), contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(44.dp).padding(10.dp).clickable { onBack() })
        Text(title, color = Color.White, fontSize = 20.sp, maxLines = 1, modifier = Modifier.weight(1f))
    }
}

@Composable private fun LegacyPlainRow(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, color = Color.Black, fontSize = 13.sp)
    }
    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
}

@Composable private fun LegacySwitchRow(text: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Color.Black, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked, modifier = Modifier.height(32.dp))
    }
    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
}

@Composable private fun LegacyTwoLineRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onClick() }.padding(8.dp)) {
        Text(title, color = Color.Black, fontSize = 13.sp, modifier = Modifier.padding(bottom = 5.dp))
        Text(subtitle, color = Color.Black, fontSize = 10.sp, modifier = Modifier.padding(start = 10.dp))
    }
    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
}

@Composable private fun LegacyDownloadRow(name: String, downloaded: Boolean, disabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = !disabled) { onClick() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, color = Color.Black, fontSize = 13.sp, modifier = Modifier.padding(bottom = 5.dp))
            Text(if (downloaded) "Contenido descargado correctamente" else "Instalar contenido", color = Color.Black, fontSize = 10.sp, modifier = Modifier.padding(start = 10.dp))
        }
        Image(painterResource(if (downloaded) R.drawable.success else R.drawable.download), null, Modifier.size(24.dp))
    }
    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
}
