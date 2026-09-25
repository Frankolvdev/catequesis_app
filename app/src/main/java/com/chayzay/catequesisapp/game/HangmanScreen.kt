package com.chayzay.catequesisapp.game
import com.chayzay.catequesisapp.data.ApiMessages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.HangmanWord
import com.chayzay.catequesisapp.settings.GameFeedback
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

private val gallows = intArrayOf(R.drawable.ahorcado0, R.drawable.ahorcado1,
    R.drawable.ahorcado2, R.drawable.ahorcado3, R.drawable.ahorcado4,
    R.drawable.ahorcado5, R.drawable.ahorcado6, R.drawable.ahorcado7, R.drawable.ahorcadowin)

/** Aspecto del antiguo Ahorcado: horca original y teclado QWERTY de tres filas. */
@Composable
fun HangmanScreen(classId: Int, repository: CourseRepository, accent: Color, onExit: () -> Unit) {
    val context = LocalContext.current
    StopGameAudioOnDispose()
    var words by remember(classId) { mutableStateOf<List<HangmanWord>?>(null) }
    var current by remember(classId) { mutableStateOf<HangmanWord?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var round by remember(classId) { mutableIntStateOf(0) }
    var selected by remember(classId, round) { mutableStateOf<Set<Char>>(emptySet()) }
    var showHint by remember(classId, round) { mutableStateOf(false) }
    var showResult by remember(classId, round) { mutableStateOf(false) }
    var resultAcknowledged by remember(classId, round) { mutableStateOf(false) }
    LaunchedEffect(classId) {
        try {
            val loaded = withContext(Dispatchers.IO) { repository.getHangmanWords(classId) }
            words = loaded
            current = loaded.takeIf { it.isNotEmpty() }?.random()
        }
        catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudieron cargar las palabras") }
    }
    LaunchedEffect(round) {
        if (round > 0) current = words?.takeIf { it.isNotEmpty() }?.random()
    }
    val active = current
    LaunchedEffect(words) {
        if (words?.isEmpty() == true) {
            android.widget.Toast.makeText(context, "No hay información", android.widget.Toast.LENGTH_SHORT).show()
            onExit()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            error != null -> Text(error!!)
            words == null -> CircularProgressIndicator()
            active == null -> Text("Esta clase no tiene palabras para el ahorcado.")
            else -> {
                val answer = normalize(active.word)
                val mistakes = selected.count { it !in answer }
                val won = answer.filter { it in 'A'..'Z' }.all { it in selected }
                val finished = won || mistakes >= 6
                LaunchedEffect(round, finished) {
                    if (finished) { GameFeedback.finish(context, won); showResult = true }
                }
                Text(answer.map { letter ->
                    if (letter !in 'A'..'Z' || letter in selected || (finished && resultAcknowledged)) letter.toString() else "_"
                }.joinToString(" "), fontSize = 19.sp, color = Color(0xFF505050))
                Image(painterResource(gallows[if (won) 8 else (mistakes + 1).coerceIn(1, 7)]),
                    contentDescription = "Ahorcado: $mistakes fallos", modifier = Modifier.size(130.dp))
                if (showHint) Text(active.clue.ifBlank { "Sin pista disponible" })
                else Text("¿Pista?", modifier = Modifier.clickable { showHint = true }.padding(8.dp), color = accent)
                if (finished) {
                    Text(if (won) "¡Ganaste!" else if (resultAcknowledged) "La palabra era $answer"
                        else "Perdiste", color = Color(0xFF653E26))
                    if (resultAcknowledged) Button(onClick = { round++ }) { Text("Jugar otra vez") }
                    if (showResult) GameResultDialog(won,
                        if (won) "Has ganado felicidades tu respuesta es correcta."
                        else "Has perdido, toca aceptar para ver la respuesta correcta y comenzar una nueva partida.") {
                        showResult = false
                        resultAcknowledged = true
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val keyWidth = (maxWidth / 10).coerceAtMost(32.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("QWERTYUIOP", "ASDFGHJKLÑ", "ZXCVBNM").forEach { line ->
                            Row(horizontalArrangement = Arrangement.Center) {
                                line.forEach { letter ->
                                    val guessed = letter in selected
                                    Text(letter.toString(), modifier = Modifier.padding(1.dp).size(keyWidth)
                                        .background(when {
                                            !guessed -> Color(0xFF653E26)
                                            letter in answer -> Color(0xFFD48656)
                                            else -> Color.Transparent
                                        }).clickable(enabled = !guessed && !finished) {
                                            selected = selected + letter
                                        },
                                        color = if (guessed && letter !in answer) Color.Transparent else Color.White,
                                        fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun normalize(value: String): String = Normalizer.normalize(value.uppercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
