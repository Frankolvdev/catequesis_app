package com.chayzay.catequesisapp.game
import com.chayzay.catequesisapp.data.ApiMessages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.chayzay.catequesisapp.data.ExamAnswer
import com.chayzay.catequesisapp.data.ExamQuestion
import com.chayzay.catequesisapp.settings.GameFeedback
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val gameRed = Color(0xFFD23131)
private val gameGreen = Color(0xFF2E7B0B)

/** Disposición original: fallos/tiempo/aciertos, pregunta al centro, dos botones abajo. */
@Composable
fun TrueFalseScreen(
    classId: Int,
    repository: CourseRepository,
    accent: Color,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    StopGameAudioOnDispose()
    var source by remember(classId) { mutableStateOf<List<ExamQuestion>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var rounds by remember(classId) { mutableStateOf<List<Pair<ExamQuestion, ExamAnswer>>>(emptyList()) }
    var index by remember(classId) { mutableIntStateOf(0) }
    var correct by remember(classId) { mutableIntStateOf(0) }
    var wrong by remember(classId) { mutableIntStateOf(0) }
    var seconds by remember(classId) { mutableIntStateOf(20) }
    var showEndDialog by remember(classId) { mutableStateOf(true) }
    LaunchedEffect(classId) {
        try {
            val questions = withContext(Dispatchers.IO) { repository.getTrueFalseQuestions(classId) }
            if (questions.isNotEmpty()) rounds = List(10) { val question = questions.random(); question to question.answers.random() }
            source = questions
        } catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudieron cargar las preguntas") }
    }
    LaunchedEffect(classId, rounds, index) {
        if (index < rounds.size) {
            seconds = 20
            repeat(20) { delay(1000); seconds-- }
            if (index < rounds.size) { GameFeedback.timeout(context); wrong++; index++ }
        }
    }
    LaunchedEffect(source) {
        if (source?.isEmpty() == true) {
            android.widget.Toast.makeText(context, "No hay información", android.widget.Toast.LENGTH_SHORT).show()
            onExit()
        }
    }

    Column(Modifier.fillMaxSize()) {
        when {
            error != null -> Text(error!!, modifier = Modifier.padding(20.dp))
            source == null -> CircularProgressIndicator()
            source!!.isEmpty() -> Text("Esta clase no tiene preguntas disponibles para el juego.", modifier = Modifier.padding(20.dp))
            index >= rounds.size -> {
                if (showEndDialog) LegacyReplayDialog(
                    message = "Puntuación de: $correct/10\n¿Quieres jugar de nuevo?",
                    onReplay = {
                        rounds = List(10) { val question = source!!.random(); question to question.answers.random() }
                        index = 0; correct = 0; wrong = 0; showEndDialog = true
                    },
                    onExit = onExit
                )
            }
            else -> {
                val (question, answer) = rounds[index]
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text("Mal\n$wrong", color = gameRed, fontWeight = FontWeight.Bold)
                    Text("00:${seconds.toString().padStart(2, '0')}", modifier = Modifier.legacyCountdownWarning(seconds),
                        color = if (seconds <= 10) gameRed else Color(0xFF505050), fontWeight = FontWeight.Bold)
                    Text("Bien\n$correct", color = gameGreen, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)
                        .background(Color(0xFFE1E1E1), RoundedCornerShape(5.dp))
                        .border(5.dp, Color(0xFF838383), RoundedCornerShape(5.dp)).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(question.text, fontWeight = FontWeight.Bold)
                        Text(answer.text)
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Button(onClick = { GameFeedback.play(context, !answer.correct); if (!answer.correct) correct++ else wrong++; index++ },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = gameRed)) { Text("Falso") }
                    Button(onClick = { GameFeedback.play(context, answer.correct); if (answer.correct) correct++ else wrong++; index++ },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = gameGreen)) { Text("Verdadero") }
                }
            }
        }
    }
}
