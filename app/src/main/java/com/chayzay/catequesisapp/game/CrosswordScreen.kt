package com.chayzay.catequesisapp.game
import com.chayzay.catequesisapp.data.ApiMessages
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.HangmanWord
import com.chayzay.catequesisapp.settings.GameFeedback
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

/** Crucigrama horizontal de 15 columnas y pistas por palabra. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrosswordScreen(classId: Int, repository: CourseRepository, accent: Color) {
    val context = LocalContext.current
    var words by remember(classId) { mutableStateOf<List<HangmanWord>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var selectedId by remember(classId) { mutableStateOf<Int?>(null) }
    var selectedCell by remember(classId) { mutableIntStateOf(0) }
    var entries by remember(classId) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var revealed by remember(classId) { mutableStateOf<Map<Int, Set<Int>>>(emptyMap()) }
    var solved by remember(classId) { mutableStateOf<Set<Int>>(emptySet()) }
    var menuOpen by remember(classId) { mutableStateOf(false) }
    var feedback by remember(classId) { mutableStateOf("") }
    var showResult by remember(classId) { mutableStateOf(false) }
    LaunchedEffect(solved.size, words?.size) {
        if (!words.isNullOrEmpty() && solved.size == words!!.size) {
            GameFeedback.finish(context, true); showResult = true
        }
    }
    LaunchedEffect(classId) {
        try { words = withContext(Dispatchers.IO) { repository.getCrosswordWords(classId) } }
        catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudo cargar el crucigrama") }
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
                val selectedAnswer = crosswordNormalize(selected.word)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Resueltas ${solved.size} / ${list.size}")
                    Box {
                        Text("⋮", modifier = Modifier.clickable { menuOpen = true }.padding(12.dp),
                            color = accent, fontWeight = FontWeight.Bold)
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Resolver casilla") },
                                enabled = selected.id !in solved && selectedCell in selectedAnswer.indices,
                                onClick = {
                                    menuOpen = false
                                    val known = revealed[selected.id].orEmpty() + selectedCell
                                    revealed = revealed + (selected.id to known)
                                    if (crosswordFilled(selectedAnswer, entries[selected.id].orEmpty(), known) == selectedAnswer) {
                                        solved = solved + selected.id
                                        entries = entries + (selected.id to selectedAnswer)
                                    }
                                })
                            DropdownMenuItem(text = { Text("Resolver palabra") }, onClick = {
                                menuOpen = false
                                entries = entries + (selected.id to selectedAnswer)
                                solved = solved + selected.id
                            })
                            DropdownMenuItem(text = { Text("Resolver crucigrama") }, onClick = {
                                menuOpen = false
                                entries = list.associate { it.id to crosswordNormalize(it.word) }
                                solved = list.map { it.id }.toSet()
                            })
                            DropdownMenuItem(text = { Text("Reiniciar") }, onClick = {
                                menuOpen = false
                                entries = emptyMap(); revealed = emptyMap(); solved = emptySet()
                                selectedCell = 0; feedback = ""; showResult = false
                            })
                        }
                    }
                }
                // Misma anchura de 15 casillas que el generador del proyecto antiguo.
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cell = maxWidth.coerceAtLeast(330.dp) / 15
                    Column(Modifier.horizontalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        list.forEachIndexed { index, word ->
                            val normalized = crosswordNormalize(word.word)
                            val answer = if (word.id in solved) normalized
                                else crosswordFilled(normalized, entries[word.id].orEmpty(), revealed[word.id].orEmpty())
                            Row {
                                repeat(15) { position ->
                                    Text(if (position < normalized.length) {
                                        answer.getOrNull(position)?.toString() ?: " "
                                    } else "", modifier = Modifier.width(cell)
                                        .background(when {
                                            position >= normalized.length -> Color.Transparent
                                            word.id in solved -> Color(0xFFBDE4BB)
                                            word.id == selected.id && position == selectedCell -> accent.copy(alpha = 0.6f)
                                            word.id == selected.id -> accent.copy(alpha = 0.35f)
                                            else -> Color.White
                                        }).combinedClickable(
                                            enabled = position < normalized.length,
                                            onClick = { selectedId = word.id; selectedCell = position },
                                            onLongClick = {
                                                selectedId = word.id; selectedCell = position
                                                Toast.makeText(context, "Pista: ${word.clue}", Toast.LENGTH_SHORT).show()
                                            }).padding(vertical = 4.dp),
                                        color = Color.DarkGray)
                                }
                            }
                            Text("${index + 1}. ${word.clue}",
                                modifier = Modifier.clickable { selectedId = word.id; selectedCell = 0 }.padding(bottom = 4.dp),
                                color = if (word.id == selected.id) accent else Color.DarkGray)
                        }
                    }
                }
                Text("${list.indexOf(selected) + 1}. ${selected.clue}", color = accent)
                OutlinedTextField(
                    value = entries[selected.id].orEmpty(),
                    onValueChange = { input ->
                        if (selected.id !in solved) {
                            entries = entries + (selected.id to crosswordNormalize(input).take(selectedAnswer.length))
                            feedback = ""
                        }
                    },
                    label = { Text("Respuesta (${selectedAnswer.length} letras)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (crosswordFilled(selectedAnswer, entries[selected.id].orEmpty(), revealed[selected.id].orEmpty()) == selectedAnswer) {
                        entries = entries + (selected.id to selectedAnswer)
                        solved = solved + selected.id
                        feedback = "¡Correcto!"
                    } else feedback = "Revisa la respuesta e inténtalo otra vez"
                }, enabled = selected.id !in solved) { Text("Comprobar") }
                if (feedback.isNotBlank()) Text(feedback)
                if (solved.size == list.size) {
                    Text("¡Completaste el crucigrama!")
                    if (showResult) GameResultDialog(true, "¡Completaste el crucigrama!") {
                        showResult = false
                    }
                    Button(onClick = { solved = emptySet(); entries = emptyMap(); revealed = emptyMap(); feedback = ""; showResult = false }) {
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

private fun crosswordFilled(answer: String, entry: String, revealed: Set<Int>): String =
    answer.indices.joinToString("") { index ->
        if (index in revealed) answer[index].toString() else entry.getOrNull(index)?.toString() ?: " "
    }
