package com.chayzay.catequesisapp.game

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ImageActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ImageActivitiesScreen(classId: Int, type: String, repository: CourseRepository, accent: Color) {
    var images by remember(classId, type) { mutableStateOf<List<ImageActivity>?>(null) }
    var error by remember(classId, type) { mutableStateOf<String?>(null) }
    var index by remember(classId, type) { mutableIntStateOf(0) }
    var bitmap by remember(classId, type, index) { mutableStateOf<Bitmap?>(null) }
    var imageLoaded by remember(classId, type, index) { mutableStateOf(false) }
    var enlarged by remember(classId, type, index) { mutableStateOf(false) }
    LaunchedEffect(classId, type) {
        try { images = withContext(Dispatchers.IO) { repository.getImageActivities(classId, type) } }
        catch (cause: Exception) { error = cause.localizedMessage ?: "No se pudieron cargar las imágenes" }
    }
    val current = images?.getOrNull(index)
    LaunchedEffect(current?.id) {
        if (current != null) {
            bitmap = try { withContext(Dispatchers.IO) { repository.loadImageActivity(current) } }
                catch (_: Exception) { null }
            imageLoaded = true
        }
    }
    var seconds by remember(classId, type, index) { mutableIntStateOf(20) }
    LaunchedEffect(classId, type, index, images) {
        if (type == "GAME_ADIVINA" && current != null) {
            repeat(20) { delay(1000); seconds-- }
            index++
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val title = when (type) {
            "IMAGES" -> "Imágenes"
            "IMAGES_TEXT" -> "Imagen y texto"
            else -> "Adivina la imagen"
        }
        Text(title, fontWeight = FontWeight.Bold, color = accent)
        when {
            error != null -> Text(error!!)
            images == null -> CircularProgressIndicator()
            images!!.isEmpty() -> Text("Esta clase no tiene contenido de $title.")
            current == null -> {
                Text("Terminaste de ver las imágenes de la clase")
                Button(onClick = { index = 0 }) { Text("Volver a empezar") }
            }
            else -> {
                Text("${index + 1} / ${images!!.size}" + if (type == "GAME_ADIVINA") "  ·  00:${seconds.toString().padStart(2, '0')}" else "")
                if (!imageLoaded) CircularProgressIndicator()
                else if (bitmap == null) Text("No se pudo cargar esta imagen del servidor.")
                bitmap?.let { loaded ->
                    Image(loaded.asImageBitmap(), contentDescription = current.text,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                            .clickable { enlarged = true })
                    if (enlarged) Dialog(onDismissRequest = { enlarged = false }) {
                        Image(loaded.asImageBitmap(), contentDescription = current.text,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().clickable { enlarged = false })
                    }
                }
                if (type != "IMAGES" && current.text.isNotBlank()) Text(current.text, color = Color.DarkGray)
                Button(onClick = { index++ }) { Text(if (type == "GAME_ADIVINA") "Siguiente" else "Siguiente imagen") }
            }
        }
    }
}
