package com.chayzay.catequesisapp.game
import android.widget.Toast
import com.chayzay.catequesisapp.data.ApiMessages

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
import com.chayzay.catequesisapp.data.ExamQuestion
import com.chayzay.catequesisapp.settings.GameFeedback
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Cuatro parejas SIMPLE, respuesta correcta, verificación explícita y un minuto por ronda. */
@Composable
fun MatchScreen(classId: Int, repository: CourseRepository, accent: Color, onExit: () -> Unit) {
    val context = LocalContext.current
    StopGameAudioOnDispose()
    var source by remember(classId) { mutableStateOf<List<ExamQuestion>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var round by remember(classId) { mutableIntStateOf(0) }
    var showResult by remember(classId, round) { mutableStateOf(false) }
    var pairs by remember(classId) { mutableStateOf<List<ExamQuestion>>(emptyList()) }
    var answerOrder by remember(classId) { mutableStateOf<List<ExamQuestion>>(emptyList()) }
    var solved by remember(classId) { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedQuestion by remember(classId) { mutableStateOf<Int?>(null) }
    var selectedAnswer by remember(classId) { mutableStateOf<Int?>(null) }
    var mistakes by remember(classId) { mutableIntStateOf(0) }
    var seconds by remember(classId) { mutableIntStateOf(60) }
    LaunchedEffect(classId) {
        try {
            source = withContext(Dispatchers.IO) { repository.getTrueFalseQuestions(classId) }
                .filter { it.type == "SIMPLE" && it.answers.any { answer -> answer.correct } }
        } catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudieron cargar las preguntas") }
    }
    LaunchedEffect(source, round) {
        val available = source.orEmpty()
        if (available.size >= 4) {
            pairs = available.shuffled().take(4)
            answerOrder = pairs.shuffled()
            solved = emptySet()
            selectedQuestion = null
            selectedAnswer = null
            mistakes = 0
            seconds = 60
        }
    }
    LaunchedEffect(classId, round, pairs) {
        if (pairs.size == 4) repeat(60) {
            delay(1000)
            if (solved.size == 4) return@LaunchedEffect
            seconds--
        }
    }
    LaunchedEffect(solved.size, seconds == 0) {
        if (pairs.size == 4 && (solved.size == 4 || seconds == 0)) {
            GameFeedback.finish(context, solved.size == 4)
            showResult = true
        }
    }
    LaunchedEffect(source) {
        if (source?.isEmpty() == true) {
            android.widget.Toast.makeText(context, "No hay información", android.widget.Toast.LENGTH_SHORT).show()
            onExit()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            error != null -> Text(error!!)
            source == null -> CircularProgressIndicator()
            source!!.size < 4 -> Text("Esta clase no tiene cuatro parejas de preguntas para este juego.")
            pairs.size == 4 -> {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Preguntas", modifier = Modifier.fillMaxWidth().background(accent).padding(5.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        pairs.filter { it.id !in solved }.forEach { question ->
                            Text(question.text, modifier = Modifier.fillMaxWidth()
                                .background(if (selectedQuestion == question.id) Color(0xFF96BC85) else Color.White)
                                .clickable(enabled = seconds > 0) { selectedQuestion = question.id }
                                .padding(5.dp), color = Color(0xFF505050), fontSize = 13.sp)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Respuestas", modifier = Modifier.fillMaxWidth().background(accent).padding(5.dp), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        answerOrder.filter { it.id !in solved }.forEach { question ->
                            Text(question.answers.first { it.correct }.text, modifier = Modifier.fillMaxWidth()
                                .background(if (selectedAnswer == question.id) Color(0xFFD48656) else Color.White)
                                .clickable(enabled = seconds > 0) { selectedAnswer = question.id }
                                .padding(5.dp), color = Color(0xFF505050), fontSize = 13.sp)
                        }
                    }
                }
                if (showResult) GameResultDialog(solved.size == 4,
                    if (solved.size == 4) "Has ganado felicidades." else "Has perdido :(") {
                    showResult = false
                }
                // Pie original de activity_game_match.xml: temporizador a la izquierda,
                // aciertos/fallos al centro y Comprobar a la derecha.
                Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (seconds == 0) "Has perdido :(" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}",
                        modifier = Modifier.legacyCountdownWarning(seconds).weight(1f),
                        color = if (seconds in 1..10 || seconds == 0) Color(0xFFB71C1C) else Color(0xFF505050),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Aciertos ${solved.size}/4", modifier = Modifier.padding(5.dp), color = Color(0xFF2E7B0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Fallos $mistakes", modifier = Modifier.padding(5.dp), color = Color(0xFFD23131), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = {
                        if (seconds == 0 || solved.size == 4) {
                            showResult = false
                            round++
                        } else {
                            val question = selectedQuestion
                            val answer = selectedAnswer
                            if (question == null || answer == null) {
                                Toast.makeText(context, "Selecciona una pregunta.", Toast.LENGTH_SHORT).show()
                            } else {
                                if (question == answer) solved = solved + question
                                else mistakes++
                                selectedQuestion = null
                                selectedAnswer = null
                            }
                        }
                    }, modifier = Modifier.padding(start = 5.dp)) {
                        Text(if (seconds == 0 || solved.size == 4) "Reiniciar" else "Comprobar", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
