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
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Enigma: reordena todas las letras de una palabra de la clase en un minuto. */
@Composable
fun EnigmaScreen(classId: Int, repository: CourseRepository, accent: Color) {
    var words by remember(classId) { mutableStateOf<List<HangmanWord>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var round by remember(classId) { mutableIntStateOf(0) }
    var scrambled by remember(classId, round) { mutableStateOf<List<Char>>(emptyList()) }
    var chosen by remember(classId, round) { mutableStateOf<List<Int>>(emptyList()) }
    var seconds by remember(classId, round) { mutableIntStateOf(60) }
    var showHint by remember(classId, round) { mutableStateOf(false) }
    LaunchedEffect(classId) {
        try { words = withContext(Dispatchers.IO) { repository.getEnigmaWords(classId) } }
        catch (cause: Exception) { error = cause.localizedMessage ?: "No se pudieron cargar las palabras" }
    }
    val word = words?.takeIf { it.isNotEmpty() }?.let { it[round % it.size] }
    LaunchedEffect(classId, round, word?.id) {
        if (word != null) {
            val letters = word.word.toList()
            scrambled = letters.shuffled().let { if (it == letters && letters.size > 1) it.reversed() else it }
            chosen = emptyList()
            seconds = 60
            repeat(60) {
                delay(1000)
                if (chosen.size == scrambled.size && chosen.map { scrambled[it] }.joinToString("") == word.word) return@LaunchedEffect
                seconds--
            }
        }
    }
    val attempt = chosen.mapNotNull { scrambled.getOrNull(it) }.joinToString("")
    val won = word != null && scrambled.isNotEmpty() && attempt == word.word
    val finished = won || seconds == 0
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Enigma", fontWeight = FontWeight.Bold, color = accent)
        when {
            error != null -> Text(error!!)
            words == null -> CircularProgressIndicator()
            word == null -> Text("Esta clase no tiene palabras para Enigma.")
            else -> {
                Text("Tiempo: ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}")
                if (showHint) Text("Pista: ${word.clue.ifBlank { "No hay pista disponible" }}")
                else Button(onClick = { showHint = true }) { Text("Ver pista") }
                Text("Forma la palabra con las letras:")
                chosen.chunked(7).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { index ->
                            Text(scrambled[index].toString(), Modifier.background(accent)
                                .clickable(enabled = !finished) { chosen = chosen - index }
                                .padding(12.dp), color = Color.White)
                        }
                    }
                }
                if (chosen.isEmpty()) Text("Selecciona una letra para empezar")
                scrambled.indices.filter { it !in chosen }.chunked(7).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { index ->
                            Text(scrambled[index].toString(), Modifier.background(Color.White)
                                .clickable(enabled = !finished) { chosen = chosen + index }
                                .padding(12.dp), color = Color.DarkGray)
                        }
                    }
                }
                if (finished) {
                    Text(if (won) "¡Correcto!" else "Tiempo agotado. La palabra era ${word.word}")
                    Button(onClick = { round++ }) { Text("Otra palabra") }
                } else if (chosen.isNotEmpty()) Button(onClick = { chosen = emptyList() }) { Text("Reiniciar letras") }
            }
        }
    }
}
