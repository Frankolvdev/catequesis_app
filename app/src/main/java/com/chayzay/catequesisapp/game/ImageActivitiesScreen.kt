package com.chayzay.catequesisapp.game
import com.chayzay.catequesisapp.data.ApiMessages

import android.graphics.Bitmap
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ImageActivity
import com.chayzay.catequesisapp.settings.GameFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun ImageActivitiesScreen(classId: Int, type: String, repository: CourseRepository, accent: Color, onExit: () -> Unit) {
    val context = LocalContext.current
    StopGameAudioOnDispose()
    var images by remember(classId, type) { mutableStateOf<List<ImageActivity>?>(null) }
    var error by remember(classId, type) { mutableStateOf<String?>(null) }
    var index by remember(classId, type) { mutableIntStateOf(0) }
    var bitmap by remember(classId, type, index) { mutableStateOf<Bitmap?>(null) }
    var imageLoaded by remember(classId, type, index) { mutableStateOf(false) }
    var enlarged by remember(classId, type, index) { mutableStateOf(false) }
    var imageAttempt by remember(classId, type, index) { mutableIntStateOf(0) }
    var showEndDialog by remember(classId, type) { mutableStateOf(true) }
    LaunchedEffect(classId, type) {
        try { images = withContext(Dispatchers.IO) { repository.getImageActivities(classId, type) } }
        catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudieron cargar las imágenes") }
    }
    val current = images?.getOrNull(index)
    LaunchedEffect(current?.id, imageAttempt) {
        if (current != null) {
            imageLoaded = false
            bitmap = null
            bitmap = try { withContext(Dispatchers.IO) { repository.loadImageActivity(current) } }
                catch (_: Exception) { null }
            imageLoaded = true
        }
    }
    var seconds by remember(classId, type, index) { mutableIntStateOf(20) }
    DisposableEffect(classId, type, index, imageLoaded, bitmap) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var acceleration = 0f
        var current = SensorManager.GRAVITY_EARTH
        var last = SensorManager.GRAVITY_EARTH
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            override fun onSensorChanged(event: SensorEvent) {
                if (type != "GAME_ADIVINA" || !imageLoaded || bitmap == null ||
                    images.isNullOrEmpty() || index >= images!!.size) return
                val (x, y, z) = event.values
                last = current
                current = kotlin.math.sqrt(x * x + y * y + z * z)
                acceleration = acceleration * 0.9f + (current - last)
                // La actividad antigua cambia de imagen en cuanto supera el mismo umbral (> 7).
                if (acceleration > 7f) index++
            }
        }
        if (sensor != null) manager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { if (sensor != null) manager?.unregisterListener(listener) }
    }
    LaunchedEffect(classId, type, index, current?.id, imageLoaded, bitmap) {
        if (type == "GAME_ADIVINA" && current != null && imageLoaded && bitmap != null) {
            seconds = 20
            repeat(20) { delay(1000); seconds-- }
            GameFeedback.timeout(context)
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
                if (showEndDialog) androidx.compose.material3.AlertDialog(onDismissRequest = { },
                    title = { Text("Terminaste las imágenes") },
                    text = { Text("¿Quieres empezar de nuevo?") },
                    confirmButton = { androidx.compose.material3.TextButton(onClick = {
                        index = 0; showEndDialog = true
                    }) { Text("Volver a empezar") } },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = onExit) { Text("No") } })
                Text("Terminaste de ver las imágenes de la clase")
            }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1} / ${images!!.size}")
                    if (type == "GAME_ADIVINA") Text("·  00:${seconds.toString().padStart(2, '0')}",
                        modifier = Modifier.legacyCountdownWarning(seconds),
                        color = if (seconds in 1..10) Color(0xFFB71C1C) else Color.DarkGray)
                }
                if (!imageLoaded) CircularProgressIndicator()
                else if (bitmap == null) {
                    Text("No se pudo cargar esta imagen del servidor.")
                    Button(onClick = { imageAttempt++ }) { Text("Reintentar imagen") }
                }
                bitmap?.let { loaded ->
                    Image(loaded.asImageBitmap(), contentDescription = current.text,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                            .clickable { enlarged = true })
                    if (enlarged) Dialog(onDismissRequest = { enlarged = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)) {
                        var zoom by remember(current.id) { mutableStateOf(1f) }
                        var pan by remember(current.id) { mutableStateOf(Offset.Zero) }
                        Column(Modifier.fillMaxSize().background(Color.Black)) {
                            Button(onClick = { enlarged = false }, modifier = Modifier.padding(12.dp)) {
                                Text("Cerrar imagen")
                            }
                            Box(Modifier.fillMaxWidth().weight(1f)) {
                                Image(loaded.asImageBitmap(), contentDescription = current.text,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                        .pointerInput(current.id) {
                                            detectTransformGestures { _, change, translation, _ ->
                                                zoom = (zoom * change).coerceIn(1f, 5f)
                                                pan = if (zoom == 1f) Offset.Zero else pan + translation
                                            }
                                        }
                                        .graphicsLayer(scaleX = zoom, scaleY = zoom,
                                            translationX = pan.x, translationY = pan.y))
                            }
                        }
                    }
                }
                if (type != "IMAGES" && current.text.isNotBlank()) Text(current.text, color = Color.DarkGray)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (type != "GAME_ADIVINA") Button(enabled = index > 0,
                        onClick = { index-- }) { Text("‹ Anterior") }
                    Button(enabled = type == "GAME_ADIVINA" || index < images!!.lastIndex,
                        onClick = { index++ }) {
                        Text(if (type == "GAME_ADIVINA") "Siguiente" else "Siguiente imagen ›")
                    }
                }
            }
        }
    }
}
