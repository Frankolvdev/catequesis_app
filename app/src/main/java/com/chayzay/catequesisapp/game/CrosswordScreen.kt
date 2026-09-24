package com.chayzay.catequesisapp.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.HangmanWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

/** Crucigrama horizontal de 15 columnas y pistas por palabra. */
@Composable
fun CrosswordScreen(classId: Int, repository: CourseRepository, accent: Color) {
    var words by remember(classId) { mutableStateOf<List<HangmanWord>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var selectedId by remember(classId) { mutableStateOf<Int?>(null) }
    var entries by remember(classId) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var solved by remember(classId) { mutableStateOf<Set<Int>>(emptySet()) }
    var feedback by remember(classId) { mutableStateOf("") }
    LaunchedEffect(classId) {
        try { words = withContext(Dispatchers.IO) { repository.getCrosswordWords(classId) } }
        catch (cause: Exception) { error = cause.localizedMessage ?: "No se pudo cargar el crucigrama" }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Crucigrama", fontWeight = FontWeight.Bold, color = accent)
        when {
            error != null -> Text(error!!)
            words == null -> CircularProgressIndicator()
            words!!.isEmpty() -> Text("Esta clase no tiene palabras de hasta 15 letras para el crucigrama.")
            else -> {
                val list = words!!
                val selected = list.firstOrNull { it.id == selectedId } ?: list.first()
                Text("Resueltas ${solved.size} / ${list.size}")
                // Misma anchura de 15 casillas que el generador del proyecto antiguo.
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cell = maxWidth.coerceAtLeast(330.dp) / 15
                    Column(Modifier.horizontalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        list.forEachIndexed { index, word ->
                            val answer = if (word.id in solved) crosswordNormalize(word.word)
                                else entries[word.id].orEmpty()
                            Row(Modifier.clickable { selectedId = word.id }) {
                                repeat(15) { position ->
                                    Text(if (position < word.word.length) {
                                        answer.getOrNull(position)?.toString() ?: " "
                                    } else "", modifier = Modifier.width(cell)
                                        .background(when {
                                            position >= word.word.length -> Color.Transparent
                                            word.id in solved -> Color(0xFFBDE4BB)
                                            word.id == selected.id -> accent.copy(alpha = 0.35f)
                                            else -> Color.White
                                        }).padding(vertical = 4.dp),
                                        color = Color.DarkGray)
                                }
                            }
                            Text("${index + 1}. ${word.clue}",
                                modifier = Modifier.clickable { selectedId = word.id }.padding(bottom = 4.dp),
                                color = if (word.id == selected.id) accent else Color.DarkGray)
                        }
                    }
                }
                Text("${list.indexOf(selected) + 1}. ${selected.clue}", color = accent)
                OutlinedTextField(
                    value = entries[selected.id].orEmpty(),
                    onValueChange = { input ->
                        if (selected.id !in solved) {
                            entries = entries + (selected.id to crosswordNormalize(input).take(selected.word.length))
                            feedback = ""
                        }
                    },
                    label = { Text("Respuesta (${selected.word.length} letras)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (entries[selected.id].orEmpty() == crosswordNormalize(selected.word)) {
                        solved = solved + selected.id
                        feedback = "¡Correcto!"
                    } else feedback = "Revisa la respuesta e inténtalo otra vez"
                }, enabled = selected.id !in solved) { Text("Comprobar") }
                if (feedback.isNotBlank()) Text(feedback)
                if (solved.size == list.size) {
                    Text("¡Completaste el crucigrama!")
                    Button(onClick = { solved = emptySet(); entries = emptyMap(); feedback = "" }) {
                        Text("Jugar de nuevo")
                    }
                }
            }
        }
    }
}

private fun crosswordNormalize(value: String): String =
    Normalizer.normalize(value.uppercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").filter { it.isLetter() }
