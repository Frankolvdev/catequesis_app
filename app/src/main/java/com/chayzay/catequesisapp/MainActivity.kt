package com.chayzay.catequesisapp

import android.os.Bundle
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.chayzay.catequesisapp.profile.InitialSetupScreen
import com.chayzay.catequesisapp.profile.ProfileSettings
import com.chayzay.catequesisapp.prayer.PrayerScreen
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.chayzay.catequesisapp.data.Course
import com.chayzay.catequesisapp.data.CourseClass
import com.chayzay.catequesisapp.data.ClassTheme
import com.chayzay.catequesisapp.data.Lesson
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.CourseImageRepository
import com.chayzay.catequesisapp.ui.theme.CatequesisTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val repository = CourseRepository(getString(R.string.api_base_url), cacheDir)
        val imageRepository = CourseImageRepository(cacheDir)
        setContent {
            CatequesisTheme {
                var profile by remember { mutableStateOf(ProfileSettings.load(this@MainActivity)) }
                var showBrand by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(900)
                    showBrand = false
                }
                if (showBrand) BrandScreen()
                else if (profile == null) InitialSetupScreen { selected ->
                    selected.save(this@MainActivity)
                    profile = selected
                }
                else {
                    var section by remember { mutableStateOf("courses") }
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            if (section == "courses") CatalogScreen(repository, imageRepository, profile!!)
                            else PrayerScreen(profile!!)
                        }
                        Row(modifier = Modifier.fillMaxWidth().background(profile!!.accent)
                            .padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Row(modifier = Modifier.clickable { section = "courses" }.padding(7.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.drawable.corpus), contentDescription = null,
                                    modifier = Modifier.size(26.dp))
                                Text("Cursos", color = Color.White, modifier = Modifier.padding(start = 6.dp))
                            }
                            Row(modifier = Modifier.clickable { section = "prayer" }.padding(7.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.drawable.rezar), contentDescription = null,
                                    modifier = Modifier.size(26.dp))
                                Text("Oraciones", color = Color.White, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
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
    data class Lessons(val course: Course, val courseClass: CourseClass, val theme: ClassTheme) : CatalogPage
    data class LessonDetail(val course: Course, val courseClass: CourseClass, val theme: ClassTheme, val lesson: Lesson) : CatalogPage
}

private data class CatalogRow(val id: Int, val label: String)
private sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val rows: List<CatalogRow>) : CatalogState
    data class Error(val message: String) : CatalogState
}

@Composable
private fun CatalogScreen(
    repository: CourseRepository,
    imageRepository: CourseImageRepository,
    profile: ProfileSettings
) {
    var page by remember { mutableStateOf<CatalogPage>(CatalogPage.Courses) }
    var reload by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<CatalogState>(CatalogState.Loading) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var classes by remember { mutableStateOf<List<CourseClass>>(emptyList()) }
    var themes by remember { mutableStateOf<List<ClassTheme>>(emptyList()) }
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var courseImage by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(page) {
        courseImage = null
        val current = page
        if (current is CatalogPage.Classes) {
            courseImage = withContext(Dispatchers.IO) { imageRepository.load(current.course) }
        }
    }

    fun goBack() {
        page = when (val current = page) {
            CatalogPage.Courses -> CatalogPage.Courses
            is CatalogPage.Classes -> CatalogPage.Courses
            is CatalogPage.Themes -> CatalogPage.Classes(current.course)
            is CatalogPage.Lessons -> CatalogPage.Themes(current.course, current.courseClass)
            is CatalogPage.LessonDetail -> CatalogPage.Lessons(current.course, current.courseClass, current.theme)
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
                is CatalogPage.Lessons -> {
                    lessons = withContext(Dispatchers.IO) { repository.getLessons(current.theme.id) }
                    lessons.map { CatalogRow(it.id, "Lección ${it.number}: ${it.name}") }
                }
                is CatalogPage.LessonDetail -> emptyList()
            }
            CatalogState.Ready(rows)
        } catch (error: Exception) {
            CatalogState.Error(error.localizedMessage ?: "No se pudo conectar con el servidor")
        }
    }

    val accent = profile.accent // Matriz original ColorView según edad y género.
    Column(modifier = Modifier.fillMaxSize().background(profile.baseColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(accent).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (page != CatalogPage.Courses) {
                Text("‹", modifier = Modifier.clickable { goBack() }.padding(end = 20.dp),
                    color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
            Text("Catequesis", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        val title = when (val current = page) {
            CatalogPage.Courses -> "Cursos"
            is CatalogPage.Classes -> "Clases de ${current.course.name}"
            is CatalogPage.Themes -> current.courseClass.name
            is CatalogPage.Lessons -> current.theme.name
            is CatalogPage.LessonDetail -> current.lesson.name
        }
        Text(title, modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.titleLarge, color = Color(0xFF505050))
        if (page is CatalogPage.LessonDetail) {
            val detail = page as CatalogPage.LessonDetail
            AndroidView(
                factory = { context -> TextView(context).apply {
                    textSize = 16f
                    setTextColor(android.graphics.Color.DKGRAY)
                    movementMethod = LinkMovementMethod.getInstance()
                } },
                update = { it.text = HtmlCompat.fromHtml(detail.lesson.html, HtmlCompat.FROM_HTML_MODE_LEGACY) },
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
            )
        } else when (val result = state) {
            CatalogState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is CatalogState.Error -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No se pudo cargar: ${result.message}")
                Button(onClick = { reload++ }) { Text("Reintentar") }
            }
            is CatalogState.Ready -> if (result.rows.isEmpty()) {
                Text("No hay contenido disponible en esta sección.", modifier = Modifier.padding(20.dp))
            } else if (page is CatalogPage.Classes) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    // Fondo original del mapa de clases: path_image_solve de la API.
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        courseImage?.let { bitmap ->
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            result.rows.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.Center) {
                                    row.forEach { entry ->
                                        Text(classes.firstOrNull { it.id == entry.id }?.number?.toString() ?: "",
                                            modifier = Modifier.padding(2.dp).size(55.dp)
                                                .background(Color(0x88E1E1E1), RoundedCornerShape(3.dp))
                                                .clickable {
                                                    val selected = classes.firstOrNull { it.id == entry.id }
                                                    val current = page as? CatalogPage.Classes
                                                    if (selected != null && current != null) page = CatalogPage.Themes(current.course, selected)
                                                }.padding(15.dp),
                                            color = Color(0xFF505050), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                    // El índice original sigue accesible debajo del mapa.
                    Text("Índice", modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium)
                    result.rows.forEach { entry ->
                        Text(entry.label, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            .background(Color(0xFFF0F0F0)).clickable {
                                val selected = classes.firstOrNull { it.id == entry.id }
                                val current = page as? CatalogPage.Classes
                                if (selected != null && current != null) page = CatalogPage.Themes(current.course, selected)
                            }.padding(12.dp), color = Color(0xFF505050))
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    items(result.rows, key = { it.id }) { row ->
                        val position = result.rows.indexOf(row)
                        val icon = listOf(R.drawable.corpus, R.drawable.rings, R.drawable.maletin,
                            R.drawable.cloud, R.drawable.cloud, R.drawable.familia).getOrElse(position) { R.drawable.corpus }
                        val isCourse = page == CatalogPage.Courses
                        val itemColor = if (isCourse) accent else Color(0xFFF0F0F0)
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = if (isCourse) 20.dp else 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().background(itemColor)
                                .clickable {
                                    when (val current = page) {
                                        CatalogPage.Courses -> courses.firstOrNull { it.id == row.id }?.let { page = CatalogPage.Classes(it) }
                                        is CatalogPage.Themes -> themes.firstOrNull { it.id == row.id }?.let { page = CatalogPage.Lessons(current.course, current.courseClass, it) }
                                        is CatalogPage.Lessons -> lessons.firstOrNull { it.id == row.id }?.let { page = CatalogPage.LessonDetail(current.course, current.courseClass, current.theme, it) }
                                        else -> Unit
                                    }
                                }.padding(12.dp).height(if (isCourse) 48.dp else 40.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(row.label, modifier = Modifier.weight(1f), maxLines = 2,
                                    overflow = TextOverflow.Ellipsis, color = if (isCourse) Color.White else Color(0xFF505050),
                                    style = MaterialTheme.typography.titleMedium)
                                if (isCourse) Image(painterResource(icon), contentDescription = null,
                                    modifier = Modifier.size(36.dp))
                                else Text("›", color = Color(0xFF505050), style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
