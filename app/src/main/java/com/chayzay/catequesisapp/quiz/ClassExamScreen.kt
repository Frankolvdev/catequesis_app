package com.chayzay.catequesisapp.quiz
import com.chayzay.catequesisapp.data.ApiMessages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.material3.Surface
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.data.ClassProgressStore
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ExamQuestion
import com.chayzay.catequesisapp.profile.ProfileSettings
import com.chayzay.catequesisapp.settings.GameFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@Composable
fun ClassExamScreen(classId: Int, repository: CourseRepository, store: ClassProgressStore,
                    profile: ProfileSettings, onPassed: () -> Unit) {
    val context = LocalContext.current
    var questions by remember(classId) { mutableStateOf<List<ExamQuestion>?>(null) }
    var error by remember(classId) { mutableStateOf<String?>(null) }
    var retryLoad by remember(classId) { mutableStateOf(0) }
    var position by remember(classId) { mutableStateOf(0) }
    var correctCount by remember(classId) { mutableStateOf(0) }
    var wrongCount by remember(classId) { mutableStateOf(0) }
    var goodStage by remember(classId) { mutableStateOf(0) }
    var badStage by remember(classId) { mutableStateOf(0) }
    var selected by remember(classId) { mutableStateOf<Set<Int>>(emptySet()) }
    var submitted by remember(classId) { mutableStateOf(false) }
    var finished by remember(classId) { mutableStateOf(false) }
    var lastCorrect by remember(classId) { mutableStateOf(false) }
    LaunchedEffect(classId, retryLoad) {
        error = null
        questions = null
        try { questions = withContext(Dispatchers.IO) { repository.getExam(classId) } }
        catch (e: Exception) { error = ApiMessages.fromException(e, "No se pudo cargar el examen") }
    }
    LaunchedEffect(classId, position, submitted) {
        if (submitted) {
            delay(3000)
            if (submitted) {
                if (position == 9) {
                    if (correctCount == 10) try {
                        store.markPassed(classId); onPassed()
                    } catch (cause: Exception) {
                        error = ApiMessages.fromException(cause, "No se pudo guardar la clase")
                        return@LaunchedEffect
                    }
                    finished = true
                } else { position++; selected = emptySet(); submitted = false }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mal\n$wrongCount", color = Color(0xFF9D2626))
            Text("${position.coerceAtMost(10)}/10", color = Color.DarkGray)
            Text("Bien\n$correctCount", color = Color(0xFF176C35))
        }
        when {
            error != null -> {
                Text("No se pudo cargar: $error", modifier = Modifier.padding(12.dp))
                Button(onClick = { retryLoad++ }) { Text("Reintentar carga") }
            }
            questions == null -> CircularProgressIndicator(Modifier.padding(20.dp))
            questions!!.size < 10 -> Text("No hay diez preguntas completas para esta clase.",
                modifier = Modifier.padding(12.dp))
            finished -> {
                Spacer(Modifier.height(30.dp))
                Text(if (correctCount == 10) "¡Felicidades! 10/10. Clase aprobada."
                    else "Examen terminado: $correctCount/10. ¿Por qué no lo intentas de nuevo?",
                    style = MaterialTheme.typography.titleLarge, color = Color.DarkGray)
                if (correctCount != 10) Button(onClick = {
                    questions = questions!!.shuffled()
                    position = 0; correctCount = 0; wrongCount = 0
                    goodStage = 0; badStage = 0
                    selected = emptySet(); submitted = false; finished = false
                }) { Text("Intentar de nuevo") }
            }
            else -> {
                val question = questions!![position]
                Text(if (question.type == "SIMPLE") "Pregunta simple" else "Pregunta cerrada",
                    color = Color.DarkGray, modifier = Modifier.padding(top = 12.dp))
                Card(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text(question.text, modifier = Modifier.fillMaxWidth().background(Color.White)
                        .padding(18.dp), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.answers.forEach { answer ->
                        val color = when {
                            submitted && answer.correct -> Color(0xFFCDECCF)
                            submitted && answer.id in selected -> Color(0xFFF1C7C7)
                            else -> Color.White
                        }
                        Row(Modifier.fillMaxWidth().background(color)
                            .clickable(enabled = !submitted) {
                                selected = if (answer.id in selected) selected - answer.id else selected + answer.id
                            }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = answer.id in selected,
                                onCheckedChange = if (submitted) null else { checked ->
                                    selected = if (checked) selected + answer.id else selected - answer.id
                                })
                            Text(answer.text, color = Color.DarkGray,
                                modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    if (submitted) {
                        Text(if (lastCorrect) "Correcto" else "Respuesta incorrecta: revisa las opciones verdes.",
                            color = if (lastCorrect) Color(0xFF176C35) else Color(0xFF9D2626))
                        val image = feedbackImage(profile.gender, lastCorrect,
                            if (lastCorrect) goodStage else badStage)
                        Popup(alignment = Alignment.BottomEnd, offset = IntOffset(-24, -110)) {
                            Surface { Image(painterResource(image), contentDescription = null,
                                modifier = Modifier.size(120.dp)) }
                        }
                    }
                }
                Button(enabled = submitted || selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(), onClick = {
                        if (!submitted) {
                            lastCorrect = selected == question.answers.filter { it.correct }.map { it.id }.toSet()
                            if (lastCorrect) {
                                correctCount++
                                if (goodStage < 5) { goodStage++; if (badStage > 0) badStage-- }
                            } else {
                                wrongCount++
                                if (badStage < 5) { badStage++; if (goodStage > 0) goodStage-- }
                            }
                            GameFeedback.play(context, lastCorrect)
                            submitted = true
                        } else if (position == 9) {
                            if (correctCount == 10) {
                                try { store.markPassed(classId); onPassed() }
                                catch (e: Exception) { error = ApiMessages.fromException(e, "No se pudo guardar la clase"); return@Button }
                            }
                            finished = true
                        } else {
                            position++
                            selected = emptySet()
                            submitted = false
                        }
                    }) { Text(if (submitted) "Siguiente" else "Calificar") }
            }
        }
    }
}

private fun feedbackImage(gender: String, success: Boolean, count: Int): Int {
    val index = count.coerceIn(1, 5) - 1
    return if (gender == "MALE") {
        (if (success) listOf(R.drawable.nino_bien1b, R.drawable.nino_bien2b,
            R.drawable.nino_bien3b, R.drawable.nino_bien4b, R.drawable.nino_bien5b)
        else listOf(R.drawable.nino_mal1b, R.drawable.nino_mal2b,
            R.drawable.nino_mal3b, R.drawable.nino_mal4b, R.drawable.nino_mal4b))[index]
    } else {
        (if (success) listOf(R.drawable.nina_bien1b, R.drawable.nina_bien2b,
            R.drawable.nina_bien3b, R.drawable.nina_bien4b, R.drawable.nina_bien5b)
        else listOf(R.drawable.nina_mal1b, R.drawable.nina_mal2b,
            R.drawable.nina_mal3b, R.drawable.nina_mal4b, R.drawable.nina_mal5b))[index]
    }
}
