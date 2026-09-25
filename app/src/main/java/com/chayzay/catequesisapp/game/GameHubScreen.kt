package com.chayzay.catequesisapp.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.data.Course
import com.chayzay.catequesisapp.data.CourseImageRepository
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.settings.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Reproduce las cinco tarjetas y el selector de juegos de GameOfflineActivity. */
@Composable
fun GameHubScreen(course: Course, repository: CourseRepository, images: CourseImageRepository,
                  accent: Color, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    var downloadGroup by remember(course.id) { mutableStateOf<Int?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf("") }
    val cards = listOf(
        Triple(R.drawable.crucigrama, "Juegos con palabras", "El ahorcado, crucigramas, enigma"),
        Triple(R.drawable.image_adivina, "Imágenes", "Juegos con imágenes"),
        Triple(R.drawable.course_approved, "Preguntas", "Juegos de trivia"),
        Triple(R.drawable.pizarra, "Pizarra", "Pizarra para tu clase"),
        Triple(R.drawable.selfie, "Selfie", "Autoanalizarse")
    )
    val selections = listOf(
        listOf("El ahorcado" to "hangman", "Crucigramas" to "crossword", "Enigma" to "enigma"),
        listOf("Imágenes" to "IMAGES", "Imagen con texto" to "IMAGES_TEXT", "Juego \"adivina\"" to "GAME_ADIVINA"),
        listOf("Preguntados (Quiz)" to "quiz", "Hacer el match" to "match", "Verdadero o Falso" to "truefalse")
    )
    var openGroup by remember { mutableStateOf<Int?>(null) }
    var selected by remember(openGroup) { mutableIntStateOf(0) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        itemsIndexed(cards) { index, (icon, title, summary) ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp)
                .clickable {
                    if (index == 1 || index == 2) {
                        if (prefs.downloaded(course.id)) openGroup = index else {
                            downloadGroup = index; downloadError = ""
                        }
                    } else if (index == 0) openGroup = index
                    else onSelect(if (index == 3) "board" else "selfie")
                }) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(48.dp))
                    Column(Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A9989))
                        Text(summary, fontSize = 12.sp, color = Color(0xFF505050))
                    }
                }
            }
        }
    }
    downloadGroup?.let { group ->
        AlertDialog(onDismissRequest = { if (!downloading) downloadGroup = null },
            title = { Text("Recursos sin conexión") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Descarga los recursos de ${course.name} para usar estos juegos sin internet.")
                if (downloading) CircularProgressIndicator()
                if (downloadError.isNotBlank()) Text(downloadError, color = Color(0xFF8B2626))
            } },
            confirmButton = { Button(enabled = !downloading, onClick = {
                downloading = true
                downloadError = ""
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { repository.downloadCourseOffline(course, images) }
                        prefs.setDownloaded(course.id)
                        downloadGroup = null
                        openGroup = group
                    } catch (error: Exception) {
                        downloadError = ApiMessages.fromException(error, "No se pudo completar la descarga")
                    } finally { downloading = false }
                }
            }) { Text(if (downloading) "Descargando…" else "Descargar") } },
            dismissButton = { TextButton(enabled = !downloading, onClick = { downloadGroup = null }) {
                Text("Cancelar")
            } })
    }
    openGroup?.let { group ->
        AlertDialog(onDismissRequest = { openGroup = null },
            title = { Text("Seleccione un juego") },
            text = {
                Column {
                    selections[group].forEachIndexed { index, (label, _) ->
                        Text(label, modifier = Modifier.fillMaxWidth().clickable { selected = index }
                            .padding(12.dp),
                            fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected == index) accent else Color(0xFF505050))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { val key = selections[group][selected].second; openGroup = null; onSelect(key) }) {
                    Text("Comenzar")
                }
            },
            dismissButton = { TextButton(onClick = { openGroup = null }) { Text("Cancelar") } })
    }
}
