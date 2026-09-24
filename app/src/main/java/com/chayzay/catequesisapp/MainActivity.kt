package com.chayzay.catequesisapp

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.chayzay.catequesisapp.profile.InitialSetupScreen
import com.chayzay.catequesisapp.profile.ProfileSettings
import com.chayzay.catequesisapp.prayer.PrayerScreen
import com.chayzay.catequesisapp.faith.FaithScreen
import com.chayzay.catequesisapp.news.NewsScreen
import com.chayzay.catequesisapp.quiz.ClassExamScreen
import com.chayzay.catequesisapp.course.ThemeReadingScreen
import com.chayzay.catequesisapp.game.HangmanScreen
import com.chayzay.catequesisapp.game.TrueFalseScreen
import com.chayzay.catequesisapp.game.MatchScreen
import com.chayzay.catequesisapp.game.EnigmaScreen
import com.chayzay.catequesisapp.game.CrosswordScreen
import com.chayzay.catequesisapp.game.ImageActivitiesScreen
import com.chayzay.catequesisapp.game.GameHubScreen
import com.chayzay.catequesisapp.game.QuizGameScreen
import com.chayzay.catequesisapp.game.WhiteBoardScreen
import com.chayzay.catequesisapp.data.LessonExtras
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import com.chayzay.catequesisapp.data.ClassProgressStore
import com.chayzay.catequesisapp.data.ClassGoal
import com.chayzay.catequesisapp.data.ClassActivity
import com.chayzay.catequesisapp.data.OnlineActivity
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
        val progressStore = ClassProgressStore(this)
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
                    var atCatalogRoot by remember { mutableStateOf(true) }
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when (section) {
                                "courses" -> CatalogScreen(repository, imageRepository, progressStore, profile!!) { atCatalogRoot = it }
                                "faith" -> FaithScreen(profile!!, getString(R.string.api_base_url))
                                "news" -> NewsScreen(profile!!)
                                else -> PrayerScreen(profile!!)
                            }
                        }
                        if (section != "courses" || atCatalogRoot) Row(modifier = Modifier.fillMaxWidth().background(profile!!.accent)
                            .padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Row(modifier = Modifier.clickable { section = "news" }.padding(7.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.mipmap.document), contentDescription = null,
                                    modifier = Modifier.size(26.dp))
                                Text("Noticias", color = Color.White, modifier = Modifier.padding(start = 4.dp))
                            }
                            Row(modifier = Modifier.clickable { section = "courses" }.padding(7.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.drawable.corpus), contentDescription = null,
                                    modifier = Modifier.size(26.dp))
                                Text("Cursos", color = Color.White, modifier = Modifier.padding(start = 6.dp))
                            }
                            Row(modifier = Modifier.clickable { section = "faith" }.padding(7.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.mipmap.vela), contentDescription = null,
                                    modifier = Modifier.size(26.dp))
                                Text("Fe", color = Color.White, modifier = Modifier.padding(start = 6.dp))
                            }
                            Row(modifier = Modifier.clickable { section = "prayer" }.padding(7.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Image(painterResource(R.mipmap.prayer), contentDescription = null,
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
    data class Goals(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class Activities(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class Exam(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class Hangman(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class TrueFalse(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class Match(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class Enigma(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class Crossword(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class ImageGame(val course: Course, val courseClass: CourseClass, val type: String) : CatalogPage
    data class GameHub(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class QuizGame(val course: Course, val courseClass: CourseClass) : CatalogPage
    data class Board(val course: Course, val courseClass: CourseClass) : CatalogPage
}

private data class CatalogRow(val id: Int, val label: String)
private sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val rows: List<CatalogRow>) : CatalogState
    data class Error(val message: String) : CatalogState
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CatalogScreen(
    repository: CourseRepository,
    imageRepository: CourseImageRepository,
    progressStore: ClassProgressStore,
    profile: ProfileSettings,
    onRootChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var page by remember { mutableStateOf<CatalogPage>(CatalogPage.Courses) }
    var reload by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<CatalogState>(CatalogState.Loading) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var classes by remember { mutableStateOf<List<CourseClass>>(emptyList()) }
    var courseApproved by remember { mutableStateOf(false) }
    var themes by remember { mutableStateOf<List<ClassTheme>>(emptyList()) }
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var lessonExtras by remember { mutableStateOf<Map<Int, LessonExtras>?>(null) }
    var lessonExtrasError by remember { mutableStateOf<String?>(null) }
    var goals by remember { mutableStateOf<List<ClassGoal>>(emptyList()) }
    var realActivities by remember { mutableStateOf<List<ClassActivity>>(emptyList()) }
    var onlineActivities by remember { mutableStateOf<List<OnlineActivity>>(emptyList()) }
    var courseImage by remember { mutableStateOf<Bitmap?>(null) }
    var progressRefresh by remember { mutableStateOf(0) }
    LaunchedEffect(page) { onRootChanged(page == CatalogPage.Courses) }
    LaunchedEffect(page) {
        courseImage = null
        val current = page
        if (current is CatalogPage.Classes) {
            courseImage = withContext(Dispatchers.IO) { imageRepository.load(current.course) }
        }
    }
    LaunchedEffect(page, lessons) {
        val current = page
        if (current is CatalogPage.Lessons) {
            lessonExtras = null
            lessonExtrasError = null
            if (lessons.isNotEmpty() && lessons.all { it.themeId == current.theme.id }) {
                try {
                    lessonExtras = withContext(Dispatchers.IO) {
                        repository.getLessonExtras(lessons.map { it.id }.toSet())
                    }
                } catch (error: Exception) {
                    lessonExtrasError = error.localizedMessage ?: "Error del servidor"
                }
            }
        }
    }

    fun goBack() {
        page = when (val current = page) {
            CatalogPage.Courses -> CatalogPage.Courses
            is CatalogPage.Classes -> CatalogPage.Courses
            is CatalogPage.Themes -> CatalogPage.Classes(current.course)
            is CatalogPage.Lessons -> CatalogPage.Themes(current.course, current.courseClass)
            is CatalogPage.LessonDetail -> CatalogPage.Lessons(current.course, current.courseClass, current.theme)
            is CatalogPage.Goals -> CatalogPage.Themes(current.course, current.courseClass)
            is CatalogPage.Activities -> CatalogPage.Themes(current.course, current.courseClass)
            is CatalogPage.Exam -> CatalogPage.Themes(current.course, current.courseClass)
            is CatalogPage.Hangman -> CatalogPage.GameHub(current.course, current.courseClass)
            is CatalogPage.TrueFalse -> CatalogPage.GameHub(current.course, current.courseClass)
            is CatalogPage.Match -> CatalogPage.GameHub(current.course, current.courseClass)
            is CatalogPage.Enigma -> CatalogPage.GameHub(current.course, current.courseClass)
            is CatalogPage.Crossword -> CatalogPage.GameHub(current.course, current.courseClass)
            is CatalogPage.ImageGame -> CatalogPage.GameHub(current.course, current.courseClass)
            is CatalogPage.GameHub -> CatalogPage.Themes(current.course, current.courseClass)
            is CatalogPage.QuizGame -> CatalogPage.GameHub(current.course, current.courseClass)
            is CatalogPage.Board -> CatalogPage.GameHub(current.course, current.courseClass)
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
                    courseApproved = false
                    classes = withContext(Dispatchers.IO) { repository.getClasses(current.course.id) }
                    courseApproved = withContext(Dispatchers.IO) {
                        progressStore.approveCourseIfComplete(current.course.id, classes.map { it.id }) ||
                            progressStore.isCourseApproved(current.course.id)
                    }
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
                is CatalogPage.Goals -> {
                    goals = withContext(Dispatchers.IO) { repository.getGoals(current.courseClass.id) }
                    goals.map { CatalogRow(it.id, "${it.number}. ${it.content}") }
                }
                is CatalogPage.Activities -> {
                    realActivities = withContext(Dispatchers.IO) { repository.getRealActivities(current.courseClass.id) }
                    onlineActivities = withContext(Dispatchers.IO) { repository.getOnlineActivities(current.courseClass.id) }
                    realActivities.map { CatalogRow(it.id, it.content) } +
                        onlineActivities.map { CatalogRow(it.id, it.title) }
                }
                is CatalogPage.Exam -> emptyList()
                is CatalogPage.Hangman -> emptyList()
                is CatalogPage.TrueFalse -> emptyList()
                is CatalogPage.Match -> emptyList()
                is CatalogPage.Enigma -> emptyList()
                is CatalogPage.Crossword -> emptyList()
                is CatalogPage.ImageGame -> emptyList()
                is CatalogPage.GameHub -> emptyList()
                is CatalogPage.QuizGame -> emptyList()
                is CatalogPage.Board -> emptyList()
            }
            CatalogState.Ready(rows)
        } catch (error: Exception) {
            CatalogState.Error(error.localizedMessage ?: "No se pudo conectar con el servidor")
        }
    }

    val accent = profile.accent // Matriz original ColorView según edad y género.
    Column(modifier = Modifier.fillMaxSize().background(profile.baseColor)) {
        Box(modifier = Modifier.fillMaxWidth().height(56.dp).background(accent)) {
            if (page == CatalogPage.Courses) {
                Image(painterResource(if (profile.gender == "MALE") {
                    if (profile.age >= 18) R.drawable.man else R.drawable.boy
                } else {
                    if (profile.age >= 18) R.drawable.woman else R.drawable.girl
                }), contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(35.dp))
            } else {
                Text("‹", modifier = Modifier.align(Alignment.CenterStart).clickable { goBack() }
                    .padding(horizontal = 16.dp), color = Color.White,
                    style = MaterialTheme.typography.headlineMedium)
            }
            if (page is CatalogPage.Themes || page is CatalogPage.Lessons || page is CatalogPage.LessonDetail ||
                page is CatalogPage.Goals || page is CatalogPage.Activities || page is CatalogPage.Exam) {
                val selectedClass = when (val current = page) {
                    is CatalogPage.Themes -> current.courseClass
                    is CatalogPage.Lessons -> current.courseClass
                    is CatalogPage.LessonDetail -> current.courseClass
                    is CatalogPage.Goals -> current.courseClass
                    is CatalogPage.Activities -> current.courseClass
                    is CatalogPage.Exam -> current.courseClass
                is CatalogPage.Hangman -> current.courseClass
                is CatalogPage.TrueFalse -> current.courseClass
                is CatalogPage.Match -> current.courseClass
                is CatalogPage.Enigma -> current.courseClass
                is CatalogPage.Crossword -> current.courseClass
                is CatalogPage.ImageGame -> current.courseClass
                    else -> null
                }
                val selectedCourse = when (val current = page) {
                    is CatalogPage.Themes -> current.course
                    is CatalogPage.Lessons -> current.course
                    is CatalogPage.LessonDetail -> current.course
                    is CatalogPage.Goals -> current.course
                    is CatalogPage.Activities -> current.course
                    is CatalogPage.Exam -> current.course
                is CatalogPage.Hangman -> current.course
                is CatalogPage.TrueFalse -> current.course
                is CatalogPage.Match -> current.course
                is CatalogPage.Enigma -> current.course
                is CatalogPage.Crossword -> current.course
                is CatalogPage.ImageGame -> current.course
                    else -> null
                }
                Column(Modifier.align(Alignment.Center).padding(start = 48.dp, end = 12.dp)) {
                    Text(selectedCourse?.name.orEmpty(), color = Color.White,
                        style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Text("Clase ${selectedClass?.number}: ${selectedClass?.name.orEmpty()}",
                        color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            } else Text(when (page) {
                CatalogPage.Courses -> "Invitado"
                is CatalogPage.Classes -> "Clases"
                is CatalogPage.GameHub -> "Actividades virtuales off-line"
                is CatalogPage.Hangman -> "El ahorcado"
                is CatalogPage.Crossword -> "Crucigramas"
                is CatalogPage.Enigma -> "Enigma"
                is CatalogPage.QuizGame -> "Preguntados (Quiz)"
                is CatalogPage.Match -> "Hacer el match"
                is CatalogPage.TrueFalse -> "Verdadero o Falso"
                is CatalogPage.ImageGame -> when ((page as CatalogPage.ImageGame).type) {
                    "IMAGES" -> "Imágenes"
                    "IMAGES_TEXT" -> "Imagen con texto"
                    else -> "Juego \"adivina\""
                }
                is CatalogPage.Board -> "Pizarra"
                else -> "Catequesis"
            }, modifier = Modifier.align(Alignment.Center), color = Color.White,
                style = MaterialTheme.typography.titleMedium)
        }
        if (page is CatalogPage.Themes || page is CatalogPage.Lessons || page is CatalogPage.LessonDetail ||
            page is CatalogPage.Goals || page is CatalogPage.Activities || page is CatalogPage.Exam) {
            val currentClass = when (val current = page) {
                is CatalogPage.Themes -> current.courseClass
                is CatalogPage.Lessons -> current.courseClass
                is CatalogPage.LessonDetail -> current.courseClass
                is CatalogPage.Goals -> current.courseClass
                is CatalogPage.Activities -> current.courseClass
                is CatalogPage.Exam -> current.courseClass
                is CatalogPage.Hangman -> current.courseClass
                is CatalogPage.TrueFalse -> current.courseClass
                is CatalogPage.Match -> current.courseClass
                is CatalogPage.Enigma -> current.courseClass
                is CatalogPage.Crossword -> current.courseClass
                is CatalogPage.ImageGame -> current.courseClass
                else -> null
            }
            val currentCourse = when (val current = page) {
                is CatalogPage.Themes -> current.course
                is CatalogPage.Lessons -> current.course
                is CatalogPage.LessonDetail -> current.course
                is CatalogPage.Goals -> current.course
                is CatalogPage.Activities -> current.course
                is CatalogPage.Exam -> current.course
                is CatalogPage.Hangman -> current.course
                is CatalogPage.TrueFalse -> current.course
                is CatalogPage.Match -> current.course
                is CatalogPage.Enigma -> current.course
                is CatalogPage.Crossword -> current.course
                is CatalogPage.ImageGame -> current.course
                else -> null
            }
            if (currentCourse != null && currentClass != null) {
                Row(Modifier.fillMaxWidth().background(accent).horizontalScroll(rememberScrollState())) {
                    Text("0", modifier = Modifier.clickable { page = CatalogPage.Themes(currentCourse, currentClass) }
                        .padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White)
                    themes.forEach { theme ->
                        Text(theme.number.toString(), modifier = Modifier.clickable {
                            page = CatalogPage.Lessons(currentCourse, currentClass, theme)
                        }.padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White, maxLines = 1)
                    }
                    Text("Metas", modifier = Modifier.clickable { page = CatalogPage.Goals(currentCourse, currentClass) }
                        .padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White)
                    Text("Actividades", modifier = Modifier.clickable { page = CatalogPage.Activities(currentCourse, currentClass) }
                        .padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White)
                    Text("Juegos", modifier = Modifier.clickable { page = CatalogPage.GameHub(currentCourse, currentClass) }
                        .padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White)
                    Text("Examen", modifier = Modifier.clickable { page = CatalogPage.Exam(currentCourse, currentClass) }
                        .padding(horizontal = 16.dp, vertical = 12.dp), color = Color.White)
                }
            }
        }
        val title = when (val current = page) {
            CatalogPage.Courses -> "Cursos"
            is CatalogPage.Classes -> "Clases de ${current.course.name}"
            is CatalogPage.Themes -> current.courseClass.name
            is CatalogPage.Lessons -> current.theme.name
            is CatalogPage.LessonDetail -> current.lesson.name
            is CatalogPage.Goals -> "Metas de la clase"
            is CatalogPage.Activities -> "Actividades de la clase"
            is CatalogPage.Exam -> "Examen de la clase"
            is CatalogPage.Hangman -> "Ahorcado"
            is CatalogPage.TrueFalse -> "Verdadero o falso"
            is CatalogPage.Match -> "Relacionar respuestas"
            is CatalogPage.Enigma -> "Enigma"
            is CatalogPage.Crossword -> "Crucigrama"
            is CatalogPage.GameHub -> "Actividades virtuales off-line"
            is CatalogPage.QuizGame -> "Preguntados (Quiz)"
            is CatalogPage.Board -> "Pizarra"
            is CatalogPage.ImageGame -> when (current.type) {
                "IMAGES" -> "Imágenes"
                "IMAGES_TEXT" -> "Imagen y texto"
                else -> "Adivina la imagen"
            }
        }
        val gamePage = page is CatalogPage.GameHub || page is CatalogPage.QuizGame ||
            page is CatalogPage.Board || page is CatalogPage.Hangman || page is CatalogPage.TrueFalse ||
            page is CatalogPage.Match || page is CatalogPage.Enigma || page is CatalogPage.Crossword ||
            page is CatalogPage.ImageGame
        if (page !is CatalogPage.Lessons && !gamePage) Text(title, modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.titleLarge, color = Color(0xFF505050))
        if (page is CatalogPage.GameHub) {
            val hub = page as CatalogPage.GameHub
            GameHubScreen(accent) { key ->
                page = when (key) {
                    "hangman" -> CatalogPage.Hangman(hub.course, hub.courseClass)
                    "crossword" -> CatalogPage.Crossword(hub.course, hub.courseClass)
                    "enigma" -> CatalogPage.Enigma(hub.course, hub.courseClass)
                    "match" -> CatalogPage.Match(hub.course, hub.courseClass)
                    "truefalse" -> CatalogPage.TrueFalse(hub.course, hub.courseClass)
                    "quiz" -> CatalogPage.QuizGame(hub.course, hub.courseClass)
                    "board" -> CatalogPage.Board(hub.course, hub.courseClass)
                    "selfie" -> {
                        try { context.startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)) }
                        catch (_: Exception) { Toast.makeText(context, "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show() }
                        page
                    }
                    else -> CatalogPage.ImageGame(hub.course, hub.courseClass, key)
                }
            }
        } else if (page is CatalogPage.QuizGame) {
            val game = page as CatalogPage.QuizGame
            QuizGameScreen(game.courseClass.id, repository, accent)
        } else if (page is CatalogPage.Board) {
            WhiteBoardScreen()
        } else if (page is CatalogPage.ImageGame) {
            val game = page as CatalogPage.ImageGame
            ImageActivitiesScreen(game.courseClass.id, game.type, repository, accent)
        } else if (page is CatalogPage.Crossword) {
            val game = page as CatalogPage.Crossword
            CrosswordScreen(game.courseClass.id, repository, accent)
        } else if (page is CatalogPage.Enigma) {
            val game = page as CatalogPage.Enigma
            EnigmaScreen(game.courseClass.id, repository, accent)
        } else if (page is CatalogPage.Match) {
            val game = page as CatalogPage.Match
            MatchScreen(game.courseClass.id, repository, accent)
        } else if (page is CatalogPage.TrueFalse) {
            val game = page as CatalogPage.TrueFalse
            TrueFalseScreen(game.courseClass.id, repository, accent)
        } else if (page is CatalogPage.Hangman) {
            val game = page as CatalogPage.Hangman
            HangmanScreen(game.courseClass.id, repository, accent)
        } else if (page is CatalogPage.Exam) {
            val exam = page as CatalogPage.Exam
            ClassExamScreen(exam.courseClass.id, repository, progressStore, profile) { progressRefresh++ }
        } else if (page is CatalogPage.LessonDetail) {
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
        } else if (page is CatalogPage.Lessons && state is CatalogState.Ready) {
            val current = page as CatalogPage.Lessons
            ThemeReadingScreen(current.theme, lessons, lessonExtras, lessonExtrasError)
        } else when (val result = state) {
            CatalogState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is CatalogState.Error -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No se pudo cargar: ${result.message}")
                Button(onClick = { reload++ }) { Text("Reintentar") }
            }
            is CatalogState.Ready -> if (page is CatalogPage.Goals) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Las metas para la semana son:", color = Color.Black,
                        style = MaterialTheme.typography.titleMedium)
                    if (goals.isEmpty()) Text("No hay metas para esta clase.")
                    goals.forEach { goal ->
                        Text("${goal.number}. ${goal.content}", color = Color(0xFF505050),
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else if (page is CatalogPage.Activities) {
                var showReal by remember(page) { mutableStateOf(true) }
                var showOnline by remember(page) { mutableStateOf(true) }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Actividades reales  ${if (showReal) "−" else "+"}",
                        modifier = Modifier.fillMaxWidth().clickable { showReal = !showReal }.padding(8.dp),
                        color = Color.Black, style = MaterialTheme.typography.titleMedium)
                    if (showReal) {
                        if (realActivities.isEmpty()) Text("No hay actividades reales para esta clase.")
                        realActivities.forEach { activity ->
                            Text(activity.content, modifier = Modifier.fillMaxWidth()
                                .background(Color(0x77FFFFFF)).padding(12.dp), color = Color(0xFF505050))
                        }
                    }
                    Text("Actividades en línea  ${if (showOnline) "−" else "+"}",
                        modifier = Modifier.fillMaxWidth().clickable { showOnline = !showOnline }.padding(8.dp),
                        color = Color.Black, style = MaterialTheme.typography.titleMedium)
                    if (showOnline) {
                        if (onlineActivities.isEmpty()) Text("No hay actividades en línea para esta clase.")
                        onlineActivities.forEach { activity ->
                            Text(activity.title, modifier = Modifier.fillMaxWidth()
                                .background(Color(0x77FFFFFF)).clickable {
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activity.link))) }
                                    catch (_: Exception) { Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show() }
                                }.padding(12.dp), color = Color(0xFF505050))
                        }
                    }
                }
            } else if (result.rows.isEmpty()) {
                Text("No hay contenido disponible en esta sección.", modifier = Modifier.padding(20.dp))
            } else if (page is CatalogPage.Classes) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    if (courseApproved) Text("¡Felicidades! Curso aprobado.",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        color = Color(0xFF505050), style = MaterialTheme.typography.titleMedium)
                    // En la app anterior el fondo ocupaba todo el GridLayout: cinco columnas,
                    // filas de 30 unidades para un ancho de 200. Se escala junto con el mapa.
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center) {
                        val mapWidth = maxWidth
                        val rowCount = (classes.size + 4) / 5
                        val mapHeight = mapWidth * (rowCount.coerceAtLeast(1) * 30f / 200f)
                        Box(modifier = Modifier.width(mapWidth).height(mapHeight)) {
                            courseImage?.let { bitmap ->
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                                    contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                            }
                            Column(modifier = Modifier.fillMaxSize()) {
                                classes.chunked(5).forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        row.forEach { selected ->
                                            val flags = remember(selected.id, progressRefresh) { progressStore.flags(selected.id) }
                                            Box(modifier = Modifier.width(mapWidth / 5).height(mapHeight / rowCount.coerceAtLeast(1))
                                                .background(if (flags.completed) Color(0x55FFFFFF) else Color(0xFFE1E1E1))
                                                .border(1.dp, Color(0xFF0A0A0A))
                                                .combinedClickable(onClick = {
                                                    val current = page as? CatalogPage.Classes
                                                    if (current != null) {
                                                        progressStore.setVisited(selected.id, true)
                                                        progressRefresh++
                                                        page = CatalogPage.Themes(current.course, selected)
                                                    }
                                                }, onLongClick = {
                                                    progressStore.setVisited(selected.id, false)
                                                    progressRefresh++
                                                }), contentAlignment = Alignment.Center) {
                                                Text(selected.number.toString(), color = if (flags.visited) Color.Black else Color(0xFF777777),
                                                    style = MaterialTheme.typography.bodyMedium)
                                                if (flags.completed) Image(painterResource(R.drawable.approve_class),
                                                    contentDescription = "Clase aprobada",
                                                    modifier = Modifier.align(Alignment.BottomCenter).size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // El índice original sigue accesible debajo del mapa.
                    Text("Índice", modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium)
                    result.rows.forEach { entry ->
                        val flags = remember(entry.id, progressRefresh) { progressStore.flags(entry.id) }
                        Text(entry.label, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            .background(Color(0xFFF0F0F0)).clickable {
                                val selected = classes.firstOrNull { it.id == entry.id }
                                val current = page as? CatalogPage.Classes
                                if (selected != null && current != null) {
                                    progressStore.setVisited(selected.id, true)
                                    progressRefresh++
                                    page = CatalogPage.Themes(current.course, selected)
                                }
                            }.padding(12.dp), color = if (flags.visited) Color.Black else Color(0xFF777777),
                            fontWeight = if (flags.visited) FontWeight.Bold else FontWeight.Normal)
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
