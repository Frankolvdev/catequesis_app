package com.chayzay.catequesisapp.course

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
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
import com.chayzay.catequesisapp.links.HttpsLinks
import java.util.Locale

/** Fragmentos leídos en el mismo orden en que se muestran; los anexos cerrados se omiten. */
data class ReadingFocus(val lessonId: Int, val section: String, val paragraph: Int)
data class VoicePassage(val focus: ReadingFocus, val text: String)

fun readingParagraphs(html: String): List<String> = HtmlCompat.fromHtml(
    HttpsLinks.html(html), HtmlCompat.FROM_HTML_MODE_LEGACY
).toString().split('\n').map { it.trim() }.filter { it.isNotBlank() }

fun lessonVoicePassages(lessons: List<Lesson>): List<VoicePassage> = lessons.flatMap { lesson ->
    readingParagraphs(lesson.html).mapIndexed { index, text ->
        VoicePassage(ReadingFocus(lesson.id, "main", index), text)
    }
}

/** Lectura por párrafos, con selección, resaltado y navegación por contenido visible. */
@Composable
fun LessonVoiceControls(
    passages: List<VoicePassage>,
    onFocus: (ReadingFocus?) -> Unit = {},
    requestedFocus: ReadingFocus? = null,
    requestNumber: Int = 0
) {
    val context = LocalContext.current
    var reader by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var position by remember(passages.map { it.focus }) { mutableIntStateOf(0) }
    var segment by remember { mutableIntStateOf(0) }
    var consumedRequest by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("") }
    fun speakAt(index: Int) {
        val item = passages.getOrNull(index) ?: return
        val engine = reader ?: return
        val language = engine.setLanguage(Locale("es", "ES"))
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            message = "Instala una voz en español para escuchar la lección"
            playing = false
            return
        }
        segment = 0
        if (engine.speak(speechSegments(item.text).first(), TextToSpeech.QUEUE_FLUSH,
                null, "catequesis-read-$index-0") == TextToSpeech.ERROR) {
            message = "No se pudo reproducir el contenido"
            playing = false
        } else {
            position = index
            playing = true
            onFocus(item.focus)
            message = "Escuchando ${index + 1} de ${passages.size}"
        }
    }
    DisposableEffect(context, passages.map { it.focus }) {
        val handler = Handler(Looper.getMainLooper())
        val engine = TextToSpeech(context.applicationContext) { status ->
            handler.post {
                ready = status == TextToSpeech.SUCCESS
                if (!ready) {
                    message = "La lectura en voz alta no está disponible en este dispositivo"
                    try {
                        context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                    } catch (_: Exception) { }
                }
            }
        }
        reader = engine
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) {
                handler.post {
                    if (!playing || utteranceId != "catequesis-read-$position-$segment") return@post
                    val chunks = passages.getOrNull(position)?.let { speechSegments(it.text) } ?: return@post
                    if (segment < chunks.lastIndex) {
                        segment++
                        reader?.speak(chunks[segment], TextToSpeech.QUEUE_FLUSH,
                            null, "catequesis-read-$position-$segment")
                    } else if (position < passages.lastIndex) speakAt(position + 1)
                    else { playing = false; onFocus(null); message = "Lectura terminada" }
                }
            }
            override fun onError(utteranceId: String?) {
                handler.post {
                    if (playing && utteranceId == "catequesis-read-$position-$segment") {
                        playing = false; onFocus(null); message = "No se pudo terminar la lectura"
                    }
                }
            }
        })
        onDispose {
            playing = false
            ready = false
            reader = null
            engine.stop()
            engine.shutdown()
            onFocus(null)
        }
    }
    LaunchedEffect(requestNumber, ready) {
        if (requestNumber > consumedRequest && ready && requestedFocus != null) {
            val index = passages.indexOfFirst { it.focus == requestedFocus }
            if (index >= 0) { consumedRequest = requestNumber; speakAt(index) }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(enabled = ready && passages.isNotEmpty(), onClick = {
            val engine = reader ?: return@Button
            if (playing) {
                playing = false; engine.stop(); onFocus(null); message = "Lectura detenida"
            } else speakAt(position.coerceIn(passages.indices))
        }) { Text(if (playing) "Detener" else "Escuchar") }
        Button(enabled = ready && passages.isNotEmpty(), onClick = {
            if (!playing) message = "Inicia la lectura para usar anterior/siguiente"
            else if (position > 0) speakAt(position - 1)
        }) { Text("‹") }
        Button(enabled = ready && passages.isNotEmpty(), onClick = {
            if (!playing) message = "Inicia la lectura para usar anterior/siguiente"
            else if (position < passages.lastIndex) speakAt(position + 1)
        }) { Text("›") }
    }
    if (message.isNotBlank()) Text(message)
}

private fun speechSegments(content: String): List<String> {
    val result = mutableListOf<String>()
    val chunk = StringBuilder()
    content.split(Regex("\\s+")).forEach { word ->
        if (word.isEmpty()) return@forEach
        if (chunk.length + word.length + 1 > 2500 && chunk.isNotEmpty()) {
            result.add(chunk.toString()); chunk.clear()
        }
        if (chunk.isNotEmpty()) chunk.append(' ')
        chunk.append(word.take(2500))
    }
    if (chunk.isNotEmpty()) result.add(chunk.toString())
    return result.ifEmpty { listOf(" ") }
}
