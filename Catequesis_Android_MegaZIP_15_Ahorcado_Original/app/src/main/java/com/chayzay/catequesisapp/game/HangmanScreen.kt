package com.chayzay.catequesisapp.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.HangmanWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

/** Ahorcado de la app original: palabras marcadas in_ahor, pista y seis fallos. */
@Composable
fun HangmanScreen(classId: Int, repository: CourseRepository, accent: Color) {
    var words by remember(classId) { mutableStateOf<List<HangmanWord>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var round by remember(classId) { mutableStateOf(0) }
    var selected by remember(classId, round) { mutableStateOf<Set<Char>>(emptySet()) }
    LaunchedEffect(classId) {
        try { words = withContext(Dispatchers.IO) { repository.getHangmanWords(classId) } }
        catch (cause: Exception) { error = cause.localizedMessage ?: "No se pudieron cargar las palabras" }
    }
    val current = words?.takeIf { it.isNotEmpty() }?.let { it[round % it.size] }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Ahorcado", fontWeight = FontWeight.Bold, color = accent)
        when {
            error != null -> Text(error!!)
            words == null -> CircularProgressIndicator()
            current == null -> Text("Esta clase no tiene palabras para el ahorcado.")
            else -> {
                val answer = normalize(current.word)
                val mistakes = selected.count { it !in answer }
                val won = answer.filter { it in 'A'..'Z' }.all { it in selected }
                val finished = won || mistakes >= 6
                Text("Fallos: $mistakes / 6")
                Text("Pista: ${current.clue.ifBlank { "Sin pista disponible" }}")
                Text(answer.map { letter ->
                    if (letter !in 'A'..'Z' || letter in selected || finished) letter.toString() else "_"
                }.joinToString(" "), fontWeight = FontWeight.Bold)
                if (finished) {
                    Text(if (won) "¡Ganaste!" else "Se agotaron los intentos. La palabra era $answer")
                    Button(onClick = { round++ }) { Text("Otra palabra") }
                } else {
                    "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ".chunked(7).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { letter ->
                                Text(letter.toString(), modifier = Modifier
                                    .background(if (letter in selected) Color.LightGray else accent)
                                    .clickable { if (letter !in selected) selected = selected + letter }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                    color = if (letter in selected) Color.DarkGray else Color.White)
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
