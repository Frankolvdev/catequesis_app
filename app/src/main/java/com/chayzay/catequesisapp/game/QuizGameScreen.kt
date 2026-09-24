package com.chayzay.catequesisapp.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ExamQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Trivia independiente del examen que acredita la clase. */
@Composable
fun QuizGameScreen(classId: Int, repository: CourseRepository, accent: Color) {
    var questions by remember(classId) { mutableStateOf<List<ExamQuestion>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var position by remember(classId) { mutableIntStateOf(0) }
    var correct by remember(classId) { mutableIntStateOf(0) }
    LaunchedEffect(classId) {
        try { questions = withContext(Dispatchers.IO) { repository.getExam(classId) } }
        catch (cause: Exception) { error = cause.localizedMessage ?: "No se pudieron cargar las preguntas" }
    }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            error != null -> Text(error!!)
            questions == null -> CircularProgressIndicator()
            questions!!.isEmpty() -> Text("Esta clase no tiene preguntas para el juego.")
            position >= questions!!.size -> {
                Text("Juego terminado: $correct / ${questions!!.size}", color = accent)
                Button(onClick = { position = 0; correct = 0; questions = questions!!.shuffled() }) { Text("Jugar de nuevo") }
            }
            else -> {
                val question = questions!![position]
                Text("${position + 1} / ${questions!!.size}  ·  Aciertos: $correct", color = accent)
                Text(question.text)
                question.answers.forEach { answer ->
                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        if (answer.correct) correct++
                        position++
                    }) { Text(answer.text) }
                }
            }
        }
    }
}
