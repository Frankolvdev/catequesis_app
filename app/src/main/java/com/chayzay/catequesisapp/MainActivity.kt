package com.chayzay.catequesisapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
                if (showBrand) BrandScreen() else CoursesScreen(repository)
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

private sealed interface CoursesState {
    data object Loading : CoursesState
    data class Ready(val courses: List<Course>) : CoursesState
    data class Error(val message: String) : CoursesState
}

@Composable
private fun CoursesScreen(repository: CourseRepository) {
    var reload by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<CoursesState>(CoursesState.Loading) }
    LaunchedEffect(reload) {
        state = CoursesState.Loading
        state = try {
            CoursesState.Ready(withContext(Dispatchers.IO) { repository.getCourses() })
        } catch (error: Exception) {
            CoursesState.Error(error.localizedMessage ?: "No se pudo conectar con el servidor")
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF6F8FF)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Catequesis", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Cursos", style = MaterialTheme.typography.titleLarge)
        when (val result = state) {
            CoursesState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is CoursesState.Error -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No se pudieron cargar los cursos: ${result.message}")
                Button(onClick = { reload++ }) { Text("Reintentar") }
            }
            is CoursesState.Ready -> if (result.courses.isEmpty()) {
                Text("No hay cursos disponibles por ahora.")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(result.courses, key = { it.id }) { course ->
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(course.name, modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
