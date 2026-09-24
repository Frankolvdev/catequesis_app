package com.chayzay.catequesisapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.chayzay.catequesisapp.data.Course
import com.chayzay.catequesisapp.data.CourseClass
import com.chayzay.catequesisapp.data.ClassTheme
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.ui.theme.CatequesisTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val repository = CourseRepository(getString(R.string.api_base_url))
        setContent {
            CatequesisTheme {
                var showBrand by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(900)
                    showBrand = false
                }
                if (showBrand) BrandScreen() else CatalogScreen(repository)
            }
        }
    }
}

@Composable
private fun BrandScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.background_splash_screen),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Catequesis App",
            modifier = Modifier.align(Alignment.Center).height(196.dp)
        )
        Image(
            painter = painterResource(R.drawable.logouh),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 24.dp).height(48.dp)
        )
    }
}

private sealed interface CatalogPage {
    data object Courses : CatalogPage
    data class Classes(val course: Course) : CatalogPage
    data class Themes(val course: Course, val courseClass: CourseClass) : CatalogPage
}

private data class CatalogRow(val id: Int, val label: String)
private sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val rows: List<CatalogRow>) : CatalogState
    data class Error(val message: String) : CatalogState
}

@Composable
private fun CatalogScreen(repository: CourseRepository) {
    var page by remember { mutableStateOf<CatalogPage>(CatalogPage.Courses) }
    var reload by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<CatalogState>(CatalogState.Loading) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var classes by remember { mutableStateOf<List<CourseClass>>(emptyList()) }
    var themes by remember { mutableStateOf<List<ClassTheme>>(emptyList()) }

    fun goBack() {
        page = when (val current = page) {
            CatalogPage.Courses -> CatalogPage.Courses
            is CatalogPage.Classes -> CatalogPage.Courses
            is CatalogPage.Themes -> CatalogPage.Classes(current.course)
        }
    }
    BackHandler(enabled = page != CatalogPage.Courses) { goBack() }

    LaunchedEffect(page, reload) {
        state = CatalogState.Loading
        state = try {
            val rows = when (val current = page) {
                CatalogPage.Courses -> {
                    courses = withContext(Dispatchers.IO) { repository.getCourses() }
                    courses.map { CatalogRow(it.id, it.name) }
                }
                is CatalogPage.Classes -> {
                    classes = withContext(Dispatchers.IO) { repository.getClasses(current.course.id) }
                    classes.map { CatalogRow(it.id, "Clase ${it.number}: ${it.name}") }
                }
                is CatalogPage.Themes -> {
                    themes = withContext(Dispatchers.IO) { repository.getThemes(current.courseClass.id) }
                    themes.map { CatalogRow(it.id, "Tema ${it.number}: ${it.name}") }
                }
            }
            CatalogState.Ready(rows)
        } catch (error: Exception) {
            CatalogState.Error(error.localizedMessage ?: "No se pudo conectar con el servidor")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF6F8FF)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Catequesis", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (page != CatalogPage.Courses) {
            Button(onClick = { goBack() }) { Text("Atrás") }
        }
        val title = when (val current = page) {
            CatalogPage.Courses -> "Cursos"
            is CatalogPage.Classes -> current.course.name
            is CatalogPage.Themes -> current.courseClass.name
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        when (val result = state) {
            CatalogState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is CatalogState.Error -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No se pudo cargar: ${result.message}")
                Button(onClick = { reload++ }) { Text("Reintentar") }
            }
            is CatalogState.Ready -> if (result.rows.isEmpty()) {
                Text("No hay contenido disponible en esta sección.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(result.rows, key = { it.id }) { row ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                row.label,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    when (val current = page) {
                                        CatalogPage.Courses -> {
                                            courses.firstOrNull { it.id == row.id }?.let { page = CatalogPage.Classes(it) }
                                        }
                                        is CatalogPage.Classes -> {
                                            classes.firstOrNull { it.id == row.id }?.let { page = CatalogPage.Themes(current.course, it) }
                                        }
                                        is CatalogPage.Themes -> { /* El detalle del tema llegará en la siguiente entrega. */ }
                                    }
                                }.padding(20.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
