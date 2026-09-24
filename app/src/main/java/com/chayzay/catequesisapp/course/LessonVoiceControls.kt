package com.chayzay.catequesisapp.course

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.chayzay.catequesisapp.data.Lesson
import java.util.Locale

/** Controles de voz originales: reproducir, detener, anterior y siguiente. */
@Composable
fun LessonVoiceControls(lessons: List<Lesson>) {
    val context = LocalContext.current
    var reader by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var position by remember(lessons.map { it.id }) { mutableIntStateOf(0) }
    var segment by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    fun speakAt(target: Int) {
        val lesson = lessons.getOrNull(target) ?: return
        val engine = reader ?: return
        val text = "${lesson.name}. " + HtmlCompat.fromHtml(lesson.html,
            HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
        if (text.isBlank()) { message = "Esta lección no contiene texto para leer"; return }
        val language = engine.setLanguage(Locale("es", "ES"))
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            message = "Instala una voz en español para escuchar la lección"
            return
        }
        segment = 0
        if (engine.speak(speechSegments(text).first(), TextToSpeech.QUEUE_FLUSH,
                null, "catequesis-lesson-$target-0") == TextToSpeech.ERROR) {
            message = "No se pudo reproducir el contenido"
            playing = false
        } else {
            position = target
            playing = true
            message = "Escuchando ${target + 1} de ${lessons.size}"
        }
    }
    // Al cambiar de tema se detiene la voz anterior y se enlazan las lecciones nuevas.
    DisposableEffect(context, lessons.map { it.id }) {
        val handler = Handler(Looper.getMainLooper())
        val engine = TextToSpeech(context.applicationContext) { status ->
            handler.post {
                ready = status == TextToSpeech.SUCCESS
                if (!ready) message = "La lectura en voz alta no está disponible en este dispositivo"
            }
        }
        reader = engine
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                handler.post {
                    if (!playing || utteranceId != "catequesis-lesson-$position-$segment") return@post
                    val lesson = lessons.getOrNull(position) ?: return@post
                    val text = "${lesson.name}. " + HtmlCompat.fromHtml(lesson.html,
                        HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
                    val chunks = speechSegments(text)
                    if (segment < chunks.lastIndex) {
                        segment++
                        reader?.speak(chunks[segment], TextToSpeech.QUEUE_FLUSH,
                            null, "catequesis-lesson-$position-$segment")
                    } else if (position < lessons.lastIndex) speakAt(position + 1)
                    else { playing = false; message = "Lectura terminada" }
                }
            }
            override fun onError(utteranceId: String?) {
                handler.post { playing = false; message = "No se pudo terminar la lectura" }
            }
        })
        onDispose {
            playing = false
            ready = false
            reader = null
            engine.stop()
            engine.shutdown()
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(enabled = ready && lessons.isNotEmpty(), onClick = {
            val engine = reader ?: return@Button
            if (playing) { playing = false; engine.stop(); message = "Lectura detenida" }
            else speakAt(position)
        }) { Text(if (playing) "Detener" else "Escuchar") }
        Button(enabled = ready && position > 0, onClick = { speakAt(position - 1) }) { Text("‹") }
        Button(enabled = ready && position < lessons.lastIndex, onClick = { speakAt(position + 1) }) { Text("›") }
    }
    if (message.isNotBlank()) Text(message)
}

private fun speechSegments(content: String): List<String> {
    val result = mutableListOf<String>()
    val chunk = StringBuilder()
    content.split(Regex("\\s+")).forEach { word ->
        if (word.isEmpty()) return@forEach
        if (chunk.length + word.length + 1 > 2500 && chunk.isNotEmpty()) {
            result.add(chunk.toString())
            chunk.clear()
        }
        if (chunk.isNotEmpty()) chunk.append(' ')
        chunk.append(word.take(2500))
    }
    if (chunk.isNotEmpty()) result.add(chunk.toString())
    return result.ifEmpty { listOf(" ") }
}
