package com.chayzay.catequesisapp.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ExamQuestion
import com.chayzay.catequesisapp.settings.GameFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/** Preguntados no acredita la clase; admite las respuestas múltiples CLOSED del banco original. */
@Composable
fun QuizGameScreen(classId: Int, repository: CourseRepository, accent: Color, onExit: () -> Unit) {
    val context = LocalContext.current
    StopGameAudioOnDispose()
    var allQuestions by remember(classId) { mutableStateOf<List<ExamQuestion>?>(null) }
    var questions by remember(classId) { mutableStateOf<List<ExamQuestion>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var retry by remember(classId) { mutableIntStateOf(0) }
    var position by remember(classId) { mutableIntStateOf(0) }
    var correct by remember(classId) { mutableIntStateOf(0) }
    var goodStage by remember(classId) { mutableIntStateOf(0) }
    var badStage by remember(classId) { mutableIntStateOf(0) }
    var selected by remember(classId) { mutableStateOf<Set<Int>>(emptySet()) }
    var submitted by remember(classId) { mutableStateOf(false) }
    var feedbackClosing by remember(classId) { mutableStateOf(false) }
    var lastCorrect by remember(classId) { mutableStateOf(false) }
    var showEndDialog by remember(classId) { mutableStateOf(true) }
    LaunchedEffect(classId, retry) {
        error = null
        try {
            val loaded = withContext(Dispatchers.IO) { repository.getExam(classId) }
            allQuestions = loaded
            questions = loaded.shuffled().take(10)
        }
        catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudieron cargar las preguntas") }
    }
    LaunchedEffect(classId, position, submitted) {
        if (submitted) {
            delay(3000)
            if (submitted) {
                feedbackClosing = true
                delay(300)
                if (submitted) { position++; selected = emptySet(); submitted = false; feedbackClosing = false }
            }
        }
    }
    LaunchedEffect(allQuestions) {
        if (allQuestions?.isEmpty() == true) {
            android.widget.Toast.makeText(context, "No hay información", android.widget.Toast.LENGTH_SHORT).show()
            onExit()
        }
    }

    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            error != null -> {
                Text(error!!)
                Button(onClick = { retry++ }) { Text("Reintentar") }
            }
            questions == null -> CircularProgressIndicator()
            questions!!.size < 10 -> Text("No hay diez preguntas completas para esta clase.")
            position >= questions!!.size -> {
                if (showEndDialog) LegacyReplayDialog(
                    message = "Quiz terminado.\nPuntuación de: $correct/${questions!!.size}\n¿Quieres jugar de nuevo?",
                    onReplay = {
                        position = 0; correct = 0; goodStage = 0; badStage = 0
                        selected = emptySet(); submitted = false; showEndDialog = true
                        questions = allQuestions.orEmpty().shuffled().take(10)
                    },
                    onExit = { showEndDialog = false; onExit() }
                )
            }
            else -> {
                val question = questions!![position]
                // La app antigua construía como máximo cuatro opciones: primero correctas,
                // después incorrectas, y al final mezclaba esas cuatro.
                val displayedAnswers = remember(question.id, position) {
                    (question.answers.filter { it.correct } + question.answers.filterNot { it.correct })
                        .take(4).shuffled()
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mal\n${position - correct}", color = Color(0xFFD23131), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(if (question.type == "SIMPLE") "Pregunta simple" else "Pregunta cerrada", color = Color(0xFF505050), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("Bien\n$correct", color = Color(0xFF2E7B0B), fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
                LegacyQuestion(question.text, Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp))
                Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 5.dp).verticalScroll(rememberScrollState())) {
                    displayedAnswers.forEach { answer ->
                        val answerState = when {
                            submitted && answer.correct -> LegacyAnswerState.CORRECT_REVEALED
                            submitted && answer.id in selected -> LegacyAnswerState.WRONG_SELECTED
                            else -> LegacyAnswerState.NORMAL
                        }
                        LegacyAnswer(
                            text = answer.text,
                            state = answerState,
                            enabled = !submitted,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                            onClick = {
                                // Legacy: el TextView tocado se marca verde y se califica inmediatamente.
                                selected = selected + answer.id
                                val correctOptions = displayedAnswers.filter { it.correct }.map { it.id }.toSet()
                                lastCorrect = correctOptions.all { it in selected }
                                if (lastCorrect) {
                                    correct++
                                    if (goodStage < 5) { goodStage++; if (badStage > 0) badStage-- }
                                } else if (badStage < 5) {
                                    badStage++
                                    if (goodStage > 0) goodStage--
                                }
                                GameFeedback.play(context, lastCorrect)
                                feedbackClosing = false
                                submitted = true
                            }
                        )
                    }
                }
                if (submitted) QuizFeedbackPopup(feedbackClosing) {
                    GameCharacterFeedback(lastCorrect,
                        if (lastCorrect) goodStage else badStage, modifier = Modifier.size(200.dp), animate = false)
                }
            }
        }
    }
}
