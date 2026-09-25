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
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
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
fun GameHubScreen(course: Course, classId: Int, repository: CourseRepository, images: CourseImageRepository,
                  accent: Color, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { AppPreferences(context) }
    val scope = rememberCoroutineScope()
    var downloadGroup by remember(course.id) { mutableStateOf<Int?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf("") }
    var activeGames by remember(classId) { mutableStateOf<Map<Int, Boolean>?>(null) }
    var availableImageTypes by remember(classId) { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(classId) {
        activeGames = try { withContext(Dispatchers.IO) { repository.getActiveGames(classId) } }
        catch (_: Exception) { emptyMap() }
        availableImageTypes = try { withContext(Dispatchers.IO) { repository.getAvailableImageGameTypes(classId) } }
        catch (_: Exception) { null }
    }
    // Orden y agrupación originales de ActivitiesClassFragment: imágenes, palabras, preguntas, pizarra y selfie.
    val groups = listOf(
        Triple(Triple(R.drawable.image_adivina, "Imágenes", "Juegos con imágenes"), listOf(1, 2, 3),
            listOf("Imágenes" to "IMAGES", "Imagen con texto" to "IMAGES_TEXT", "Juego \"adivina\"" to "GAME_ADIVINA")),
        Triple(Triple(R.drawable.crucigrama, "Juegos con palabras", "El ahorcado, crucigramas, enigma"), listOf(4, 5, 6),
            listOf("El ahorcado" to "hangman", "Crucigramas" to "crossword", "Enigma" to "enigma")),
        Triple(Triple(R.drawable.course_approved, "Preguntas", "Juegos de trivia"), listOf(7, 8, 9),
            listOf("Preguntados (Quiz)" to "quiz", "Hacer el match" to "match", "Verdadero o Falso" to "truefalse")),
        Triple(Triple(R.drawable.pizarra, "Pizarra", "Pizarra para tu clase"), listOf(10), listOf("Pizarra" to "board")),
        Triple(Triple(R.drawable.selfie, "Selfie", "Autoanalizarse"), listOf(11), listOf("Selfie" to "selfie"))
    )
    val visibleGroups = groups.filter { (_, ids, _) ->
        val flags = activeGames
        flags == null || flags.isEmpty() || ids.any { flags[it] == true }
    }.map { (card, ids, choices) ->
        val flags = activeGames
        val activeChoices = if (flags == null || flags.isEmpty()) choices
            else choices.filterIndexed { i, _ -> flags[ids[i]] == true }
        // Igual que onImageGameDialog() antiguo: un juego de imágenes activo no se ofrece si no tiene contenido.
        val filteredChoices = if (ids == listOf(1, 2, 3) && availableImageTypes != null)
            activeChoices.filter { it.second in availableImageTypes!! } else activeChoices
        card to filteredChoices
    }.filter { (_, choices) -> choices.isNotEmpty() }
    var openGroup by remember { mutableStateOf<Int?>(null) }
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        itemsIndexed(visibleGroups) { index, group ->
            val (card, choices) = group
            val (icon, title, summary) = card
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
                .background(Color(0x77FFFFFF)).padding(10.dp).clickable {
                    // La app antigua sólo exigía recursos descargados para los juegos con imágenes.
                    if (choices.any { it.second in listOf("IMAGES", "IMAGES_TEXT", "GAME_ADIVINA") }) {
                        if (prefs.downloaded(course.id)) openGroup = index else {
                            downloadGroup = index; downloadError = ""
                        }
                    } else if (choices.size > 1) openGroup = index
                    else choices.firstOrNull()?.second?.let(onSelect)
                }, verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(48.dp))
                Text(if (summary.isBlank()) title else "$title\n$summary", fontSize = 13.sp,
                    color = Color(0xFF505050), modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
    downloadGroup?.let { group ->
        if (!downloading) {
            // GameOfflineActivity legacy: AlertDialog.Builder sin título, mensaje exacto y ACEPTAR/CANCELAR.
            com.chayzay.catequesisapp.ui.LegacyConfirmDialog(
                message = if (downloadError.isBlank())
                    "Para poder jugar es necesario descargar el contenido de este curso"
                else downloadError,
                confirmText = "Aceptar",
                dismissText = "Cancelar",
                onDismiss = { downloadGroup = null },
                onConfirm = {
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
                }
            )
        } else {
            // Image_activity_offline legacy mostraba un ProgressDialog no cancelable con “Descargando contenido”.
            Dialog(onDismissRequest = {}) {
                androidx.compose.material3.Surface(color = Color.White, shape = androidx.compose.ui.graphics.RectangleShape) {
                    Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                        Text("Descargando contenido", color = Color(0xFF424242), fontSize = 16.sp)
                    }
                }
            }
        }
    }
    openGroup?.let { group ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { openGroup = null }) {
            // GameOfflineActivity/ActivitiesClassFragment inflaban dialog_settings_multiselected.xml.
            // Evitamos AlertDialog Material 3 para conservar la caja rectangular y sus márgenes legacy.
            androidx.compose.material3.Surface(color = Color.White, shape = androidx.compose.ui.graphics.RectangleShape) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Seleccione un juego", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = Color.Black, modifier = Modifier.padding(8.dp))
                    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        visibleGroups[group].second.forEach { (label, key) ->
                            Text(label, modifier = Modifier.fillMaxWidth().clickable {
                                openGroup = null
                                onSelect(key)
                            }.padding(vertical = 10.dp), color = Color.Black, fontSize = 13.sp)
                        }
                    }
                    Text("Cancelar", color = Color(0xFF2C7CB1), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().clickable { openGroup = null }.padding(10.dp))
                }
            }
        }
    }
}
