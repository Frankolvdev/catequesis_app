package com.chayzay.catequesisapp.game

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.data.ApiMessages
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
    var imageAttempt by remember(classId, type, index) { mutableIntStateOf(0) }
    var seconds by remember(classId, type, index) { mutableIntStateOf(20) }

    LaunchedEffect(classId, type) {
        try { images = withContext(Dispatchers.IO) { repository.getImageActivities(classId, type) } }
        catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudieron cargar las imágenes") }
    }
    val current = images?.getOrNull(index)
    LaunchedEffect(current?.id, imageAttempt) {
        if (current != null) {
            imageLoaded = false; bitmap = null
            bitmap = try { withContext(Dispatchers.IO) { repository.loadImageActivity(current) } } catch (_: Exception) { null }
            imageLoaded = true
        }
    }
    LaunchedEffect(images) {
        if (images?.isEmpty() == true) {
            android.widget.Toast.makeText(context, "No hay información", android.widget.Toast.LENGTH_SHORT).show(); onExit()
        }
    }
    DisposableEffect(classId, type, index, imageLoaded, bitmap) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var acceleration = 0f; var now = SensorManager.GRAVITY_EARTH; var last = SensorManager.GRAVITY_EARTH
        val listener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            override fun onSensorChanged(event: SensorEvent) {
                if (type != "GAME_ADIVINA" || !imageLoaded || bitmap == null || current == null) return
                val (x,y,z) = event.values; last = now; now = kotlin.math.sqrt(x*x+y*y+z*z)
                acceleration = acceleration * .9f + (now-last)
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
            GameFeedback.timeout(context); index++
        }
    }

    when {
        error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error!!) }
        images == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        current == null -> AlertDialog(onDismissRequest = {}, title = { Text("¿Quieres volver a jugar?") },
            confirmButton = { TextButton(onClick = { index = 0 }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = onExit) { Text("Cancelar") } })
        else -> when (type) {
            "IMAGES" -> LegacySoloImage(current, bitmap, imageLoaded, index, images!!.lastIndex,
                onPrevious = { index-- }, onNext = { index++ }, onRetry = { imageAttempt++ })
            "IMAGES_TEXT" -> LegacyImageWithText(current, bitmap, imageLoaded, index, images!!.lastIndex,
                onPrevious = { index-- }, onNext = { index++ }, onRetry = { imageAttempt++ })
            else -> LegacyAdivina(current, bitmap, imageLoaded, seconds, onNext = { index++ }, onRetry = { imageAttempt++ })
        }
    }
}

@Composable private fun LegacySoloImage(item: ImageActivity, bitmap: Bitmap?, loaded: Boolean, index: Int, last: Int,
    onPrevious: () -> Unit, onNext: () -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LegacyImage(bitmap, loaded, item.text, ContentScale.Fit, Modifier.fillMaxSize(), onRetry)
        if (index > 0) Image(painterResource(R.drawable.left_arrow), null,
            Modifier.align(Alignment.CenterStart).padding(start = 5.dp).width(36.dp).height(48.dp).clickable(onClick = onPrevious))
        if (index < last) Image(painterResource(R.drawable.right_arrow), null,
            Modifier.align(Alignment.CenterEnd).padding(end = 5.dp).width(36.dp).height(48.dp).clickable(onClick = onNext))
    }
}

@Composable private fun LegacyImageWithText(item: ImageActivity, bitmap: Bitmap?, loaded: Boolean, index: Int, last: Int,
    onPrevious: () -> Unit, onNext: () -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            LegacyImage(bitmap, loaded, item.text, ContentScale.Crop, Modifier.fillMaxWidth().height(200.dp), onRetry)
            Text(item.text, fontSize = 13.sp, color = Color.Black, modifier = Modifier.padding(10.dp))
        }
        if (index > 0) Image(painterResource(R.drawable.left_arrow), null,
            Modifier.align(Alignment.TopStart).padding(start = 5.dp, top = 10.dp).width(36.dp).height(48.dp).clickable(onClick = onPrevious))
        if (index < last) Image(painterResource(R.drawable.right_arrow), null,
            Modifier.align(Alignment.TopEnd).padding(end = 5.dp, top = 10.dp).width(36.dp).height(48.dp).clickable(onClick = onNext))
    }
}

@Composable private fun LegacyAdivina(item: ImageActivity, bitmap: Bitmap?, loaded: Boolean, seconds: Int,
    onNext: () -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        LegacyImage(bitmap, loaded, item.text, ContentScale.Fit, Modifier.fillMaxSize(), onRetry)
        Text("0:${seconds.toString().padStart(2,'0')}", color = if (seconds in 1..10) Color(0xFFD23131) else Color.White,
            fontSize = 32.sp, modifier = Modifier.align(Alignment.TopStart).padding(start = 20.dp, top = 10.dp)
                .background(Color(0x88000000)).padding(5.dp).legacyCountdownWarning(seconds))
        if (item.text.isNotBlank()) Text(item.text, color = Color.White, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Center).background(Color(0x88000000)).padding(5.dp))
        Text("Siguiente imagen", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0x88000000))
                .clickable(onClick = onNext).padding(vertical = 14.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable private fun LegacyImage(bitmap: Bitmap?, loaded: Boolean, description: String, scale: ContentScale,
    modifier: Modifier, onRetry: () -> Unit) {
    when {
        !loaded -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        bitmap != null -> Image(bitmap.asImageBitmap(), description, contentScale = scale, modifier = modifier)
        else -> Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("No se pudo cargar esta imagen del servidor."); Button(onClick = onRetry) { Text("Reintentar imagen") }
        }
    }
}
