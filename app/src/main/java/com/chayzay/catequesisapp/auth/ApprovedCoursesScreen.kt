package com.chayzay.catequesisapp.auth

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.ClassProgressStore
import com.chayzay.catequesisapp.data.Course
import com.chayzay.catequesisapp.data.CourseClass
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ProgressSyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    var items by remember(user.id) { mutableStateOf<List<Course>?>(null) }
    var error by remember(user.id) { mutableStateOf<String?>(null) }
    var serverReady by remember(user.id) { mutableStateOf(false) }
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
        TextButton(onClick = onBack) { Text("‹ Perfil") }
        Text(if (certificates) "Certificados" else "Tus cursos aprobados")
        error?.let { Text(it) }
        when {
            items == null -> CircularProgressIndicator()
            items!!.isEmpty() -> Text(if (certificates)
                "No tienes cursos aprobados para obtener un certificado."
                else "Todavía no tienes cursos aprobados.")
            else -> items!!.forEach { course ->
                Button(modifier = Modifier.fillMaxWidth(),
                    enabled = !certificates || serverReady,
                    onClick = {
                        if (certificates) {
                            val host = Uri.parse(apiBaseUrl).host ?: "www.catequesis.org"
                            val uri = Uri.Builder().scheme("https").authority(host)
                                .appendPath("certificate").appendPath("certificate_course.php")
                                .appendQueryParameter("user", Base64.encodeToString(
                                    user.id.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
                                .appendQueryParameter("course", Base64.encodeToString(
                                    course.id.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
                                .build()
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            catch (_: Exception) { error = "No se pudo abrir el certificado." }
                        } else selected = course
                    }) { Text(if (certificates) "Descargar certificado: ${course.name}" else course.name) }
            }
        }
        if (certificates && !serverReady && !items.isNullOrEmpty())
            Text("Conéctate y sincroniza el progreso para descargar certificados.")
    }
    selected?.let { course ->
        AlertDialog(onDismissRequest = { selected = null },
            title = { Text(course.name) },
            text = { Column {
                when {
                    classes == null -> CircularProgressIndicator()
                    classes!!.isEmpty() -> Text("No se pudieron cargar las clases.")
                    else -> classes!!.forEach { Text("Clase ${it.number}: ${it.name}") }
                }
            } },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } })
    }
}
