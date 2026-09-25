package com.chayzay.catequesisapp.auth

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.ClassProgressStore
import com.chayzay.catequesisapp.data.Course
import com.chayzay.catequesisapp.data.CourseClass
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ProgressSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** Lista de cursos aprobados y descarga de certificados del perfil original. */
@Composable
fun ApprovedCoursesScreen(
    user: UserSession,
    progress: ClassProgressStore,
    courses: CourseRepository,
    sync: ProgressSyncRepository,
    apiBaseUrl: String,
    certificates: Boolean,
    onSynced: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember(user.id) { mutableStateOf<List<Course>?>(null) }
    var error by remember(user.id) { mutableStateOf<String?>(null) }
    var serverReady by remember(user.id) { mutableStateOf(false) }
    var certificatePending by remember(user.id) { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Course?>(null) }
    var classes by remember(selected?.id) { mutableStateOf<List<CourseClass>?>(null) }

    LaunchedEffect(user.id) {
        try {
            sync.sync(user, progress)
            serverReady = true
            onSynced()
        } catch (_: Exception) {
            error = "No se pudo sincronizar. Se muestra el progreso guardado en este dispositivo."
        }
        try {
            items = withContext(Dispatchers.IO) {
                val approved = progress.approvedCourses()
                courses.getCourses().filter { it.id in approved }
            }
        } catch (_: Exception) {
            error = "No se pudo cargar la lista de cursos aprobados."
            items = emptyList()
        }
    }
    LaunchedEffect(selected?.id) {
        val course = selected ?: return@LaunchedEffect
        classes = null
        try {
            classes = withContext(Dispatchers.IO) { courses.getClasses(course.id) }
        } catch (_: Exception) {
            classes = emptyList()
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Image(painterResource(R.drawable.ic_baseline_arrow_back_ios_24), "Volver", Modifier.size(24.dp)) }
        Text(if (certificates) "Certificados" else "Tus cursos aprobados")
        error?.let { Text(it) }
        when {
            items == null -> CircularProgressIndicator()
            items!!.isEmpty() -> Text(if (certificates)
                "No tienes cursos aprobados para obtener un certificado."
                else "Todavía no tienes cursos aprobados.")
            else -> items!!.forEach { course ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.fillMaxWidth()) {
                            Image(painterResource(R.drawable.approve_class), null, Modifier.size(32.dp).align(Alignment.TopEnd))
                            Image(painterResource(R.drawable.ave), null, Modifier.size(96.dp).align(Alignment.Center))
                        }
                        Text(course.name, color = Color(0xFF7A9989), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Curso aprobado en todas sus lecciones", color = Color(0xFF424242), fontSize = 12.sp)
                        Button(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF446353)),
                            shape = androidx.compose.foundation.shape.RectangleShape, enabled = !certificatePending,
                            onClick = {
                        if (certificates) {
                            // CertificateCourseAdapter legacy volvía a enviar/verificar
                            // course_approved/manipulate justo al tocar cada certificado.
                            scope.launch {
                                certificatePending = true
                                error = null
                                try {
                                    sync.sync(user, progress)
                                    serverReady = true
                                    onSynced()
                                    val uri = certificateUri(apiBaseUrl, user.id, course.id)
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: Exception) {
                                    error = "No se pudo verificar el curso o abrir el certificado."
                                } finally {
                                    certificatePending = false
                                }
                            }
                        } else selected = course
                    }) { Text(if (certificates) "Descargar Certificado" else "Ver contenido", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        if (certificates && certificatePending) CircularProgressIndicator()
        if (certificates && !serverReady && !items.isNullOrEmpty())
            Text("No se pudo verificar todavía el progreso. Puedes volver a intentar al tocar el certificado.")
    }
    selected?.let { course ->
        AlertDialog(onDismissRequest = { selected = null },
            title = { Text(course.name) },
            text = { Column {
                when {
                    classes == null -> CircularProgressIndicator()
                    classes!!.isEmpty() -> Text("No se pudieron cargar las clases.")
                    else -> classes!!.forEach { Text("${it.number}) ${it.name}") }
                }
            } },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } })
    }
}
