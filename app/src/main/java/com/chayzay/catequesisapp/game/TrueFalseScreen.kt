package com.chayzay.catequesisapp.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ExamAnswer
import com.chayzay.catequesisapp.data.ExamQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Diez rondas, una respuesta aleatoria por pregunta y temporizador de veinte segundos. */
@Composable
fun TrueFalseScreen(classId: Int, repository: CourseRepository, accent: Color) {
    var source by remember(classId) { mutableStateOf<List<ExamQuestion>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var rounds by remember(classId) { mutableStateOf<List<Pair<ExamQuestion, ExamAnswer>>>(emptyList()) }
    var index by remember(classId) { mutableIntStateOf(0) }
    var correct by remember(classId) { mutableIntStateOf(0) }
    var seconds by remember(classId) { mutableIntStateOf(20) }
    LaunchedEffect(classId) {
        try {
            val questions = withContext(Dispatchers.IO) { repository.getTrueFalseQuestions(classId) }
            if (questions.isNotEmpty()) rounds = List(10) {
                val question = questions.random()
                question to question.answers.random()
            }
            source = questions
        } catch (cause: Exception) { error = cause.localizedMessage ?: "No se pudieron cargar las preguntas" }
    }
    LaunchedEffect(classId, rounds, index) {
        if (index < rounds.size) {
            seconds = 20
            repeat(20) {
                delay(1000)
                seconds--
            }
            if (index < rounds.size) index++
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Verdadero o falso", fontWeight = FontWeight.Bold, color = accent)
        when {
            error != null -> Text(error!!)
            source == null -> CircularProgressIndicator()
            source!!.isEmpty() -> Text("Esta clase no tiene preguntas disponibles para el juego.")
            index >= rounds.size -> {
                Text("Terminaste: $correct / 10")
                Button(onClick = {
                    rounds = List(10) { val question = source!!.random(); question to question.answers.random() }
                    index = 0
                    correct = 0
                }) { Text("Jugar de nuevo") }
            }
            else -> {
                val (question, answer) = rounds[index]
                Text("Pregunta ${index + 1} / 10  ·  Aciertos: $correct")
                Text("00:${seconds.toString().padStart(2, '0')}")
                Text(question.text, fontWeight = FontWeight.Bold)
                Text(answer.text)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { if (answer.correct) correct++; index++ }) { Text("Verdadero") }
                    Button(onClick = { if (!answer.correct) correct++; index++ }) { Text("Falso") }
                }
            }
        }
    }
}
