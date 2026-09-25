package com.chayzay.catequesisapp.game
import com.chayzay.catequesisapp.data.ApiMessages

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.HangmanWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Casillas y botones del layout original con interacción de arrastrar y soltar. */
@Composable
fun EnigmaScreen(classId: Int, repository: CourseRepository, accent: Color, onExit: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var words by remember(classId) { mutableStateOf<List<HangmanWord>?>(null) }
    var word by remember(classId) { mutableStateOf<HangmanWord?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var round by remember(classId) { mutableIntStateOf(0) }
    var refresh by remember(classId, round) { mutableIntStateOf(0) }
    var seconds by remember(classId, round, refresh) { mutableIntStateOf(60) }
    var finished by remember(classId, round, refresh) { mutableStateOf<String?>(null) }
    var resultDismissed by remember(classId, round, refresh) { mutableStateOf(false) }
    var showHint by remember(classId, round, refresh) { mutableStateOf(false) }
    LaunchedEffect(classId) {
        try {
            val loaded = withContext(Dispatchers.IO) { repository.getEnigmaWords(classId) }
            words = loaded
            word = loaded.takeIf { it.isNotEmpty() }?.random()
        }
        catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudieron cargar las palabras") }
    }
    LaunchedEffect(round) {
        if (round > 0) word = words?.takeIf { it.isNotEmpty() }?.random()
    }
    LaunchedEffect(round, refresh, finished) {
        if (finished != null) com.chayzay.catequesisapp.settings.GameFeedback.finish(
            context, finished == "¡Ganaste!")
    }
    LaunchedEffect(classId, round, refresh, word?.id) {
        if (word != null) repeat(60) {
            delay(1000)
            if (finished != null) return@LaunchedEffect
            seconds--
            if (seconds == 0) finished = "Perdiste"
        }
    }
    val activeWord = word
    LaunchedEffect(words) {
        if (words?.isEmpty() == true) {
            android.widget.Toast.makeText(context, "No hay información", android.widget.Toast.LENGTH_SHORT).show()
            onExit()
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 5.dp)) {
        when {
            error != null -> Text(error!!)
            words == null -> CircularProgressIndicator()
            activeWord == null -> Text("Esta clase no tiene palabras para Enigma.")
            else -> {
                key(activeWord.id, round, refresh) {
                    AndroidView(factory = { context -> EnigmaBoard(context, activeWord.word) { finished = "¡Ganaste!" } },
                        modifier = Modifier.fillMaxWidth().weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (resultDismissed) {
                        TextButton(onClick = { round++ }) { Text("Nuevo juego") }
                    } else {
                        listOf(
                            R.drawable.refresh_word to { refresh++ },
                            R.drawable.resolver_word to { round++ },
                            R.drawable.bombillo_word to { showHint = true }
                        ).forEach { (icon, action) ->
                            Image(painterResource(icon), contentDescription = when (icon) {
                                R.drawable.refresh_word -> "Reiniciar palabra"
                                R.drawable.resolver_word -> "Otra palabra"
                                else -> "Ver pista"
                            }, modifier = Modifier.size(32.dp).clickable { action() })
                        }
                    }
                    Text(if (seconds == 0) "Has perdido :(" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                        modifier = Modifier.weight(1f).legacyCountdownWarning(seconds), textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        fontSize = 28.sp, color = if (seconds <= 10) Color(0xFFB71C1C) else Color(0xFF505050))
                }
                if (showHint) AlertDialog(onDismissRequest = { showHint = false },
                    text = { Text(activeWord.clue.ifBlank { "No hay pista disponible" }) },
                    confirmButton = { TextButton(onClick = { showHint = false }) { Text("Aceptar") } })
                finished?.takeUnless { resultDismissed }?.let { result ->
                    AlertDialog(onDismissRequest = { }, title = { Text(result, color = accent) },
                        text = {
                            Column {
                                Text(if (result == "¡Ganaste!") "Has ganado felicidades tu respuesta es correcta."
                                    else "Has perdido la respuesta correcta era: ${activeWord.word}")
                                GameCharacterFeedback(result == "¡Ganaste!")
                            }
                        },
                        confirmButton = { TextButton(onClick = {
                            com.chayzay.catequesisapp.settings.GameFeedback.stop()
                            resultDismissed = true
                        }) { Text("Aceptar") } })
                }
            }
        }
    }
}
