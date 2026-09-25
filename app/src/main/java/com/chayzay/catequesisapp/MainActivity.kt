package com.chayzay.catequesisapp

import android.os.Bundle
import android.content.Intent
import com.chayzay.catequesisapp.auth.SocialAuthManager
import android.net.Uri
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.chayzay.catequesisapp.course.LessonDetailReading
import com.chayzay.catequesisapp.game.HangmanScreen
import com.chayzay.catequesisapp.game.TrueFalseScreen
import com.chayzay.catequesisapp.game.MatchScreen
import com.chayzay.catequesisapp.game.EnigmaScreen
import com.chayzay.catequesisapp.game.CrosswordScreen
import com.chayzay.catequesisapp.game.ImageActivitiesScreen
import com.chayzay.catequesisapp.game.GameHubScreen
import com.chayzay.catequesisapp.game.QuizGameScreen
import com.chayzay.catequesisapp.game.WhiteBoardScreen
import com.chayzay.catequesisapp.auth.AccountScreen
import com.chayzay.catequesisapp.auth.AuthRepository
import com.chayzay.catequesisapp.auth.UserSessionStore
import com.chayzay.catequesisapp.auth.certificateUri
import com.chayzay.catequesisapp.chat.ChatRepository
import com.chayzay.catequesisapp.chat.ChatScreen
import com.chayzay.catequesisapp.chat.GuestChatScreen
import com.chayzay.catequesisapp.chat.GuestChatStore
import com.chayzay.catequesisapp.chat.GuestChatTransfer
import com.chayzay.catequesisapp.contact.ContactScreen
import com.chayzay.catequesisapp.help.HelpUsScreen
import com.chayzay.catequesisapp.help.LegacyInfoScreen
import com.chayzay.catequesisapp.settings.AppPreferences
import com.chayzay.catequesisapp.settings.SettingsScreen
import com.chayzay.catequesisapp.links.HttpsLinks
import com.chayzay.catequesisapp.data.LessonExtras
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.chayzay.catequesisapp.data.Course
import com.chayzay.catequesisapp.data.CourseClass
import com.chayzay.catequesisapp.data.ClassTheme
import com.chayzay.catequesisapp.data.Lesson
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.CourseImageRepository
import com.chayzay.catequesisapp.data.ClassProgressStore
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.data.ProgressSyncRepository
import com.chayzay.catequesisapp.auth.UserSession
import com.chayzay.catequesisapp.data.ClassGoal
import com.chayzay.catequesisapp.data.ClassActivity
import com.chayzay.catequesisapp.data.OnlineActivity
import com.chayzay.catequesisapp.ui.theme.CatequesisTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    @Deprecated("Facebook SDK callback bridge")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        SocialAuthManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val repository = CourseRepository(getString(R.string.api_base_url), filesDir, cacheDir)
        val imageRepository = CourseImageRepository(filesDir, cacheDir)
        val sessionStore = UserSessionStore(this)
        val authRepository = AuthRepository(getString(R.string.api_base_url))
        val chatRepository = ChatRepository(getString(R.string.api_base_url))
        val syncRepository = ProgressSyncRepository(getString(R.string.api_base_url))
        setContent {
            var fontOption by remember { mutableIntStateOf(AppPreferences(this@MainActivity).font) }
            CatequesisTheme(fontOption = fontOption) {
                var profile by remember { mutableStateOf(ProfileSettings.load(this@MainActivity)) }
                var session by remember { mutableStateOf(sessionStore.load()) }
                val progressStore = remember(session?.id) { ClassProgressStore(this@MainActivity, session?.id) }
                var syncVersion by remember { mutableStateOf(0) }
                var catalogContentVersion by remember { mutableStateOf(0) }
                LaunchedEffect(session?.id) {
                    val current = session ?: return@LaunchedEffect
                    try {
                        // La app original enviaba las aprobaciones hechas como invitado al iniciar sesión.
                        // Conservar primero una copia por cuenta permite reintentar si falla la red.
                        withContext(Dispatchers.IO) {
                            val guest = ClassProgressStore(this@MainActivity)
                            progressStore.mergeApprovals(guest.approvedTests(), guest.approvedCourses())
                        }
                        syncRepository.sync(current, progressStore)
                        syncVersion++
                    } catch (_: Exception) { /* Reintentar en Cuenta sin perder datos locales. */ }
                }
                LaunchedEffect(session?.apiKey) {
                    val current = session ?: return@LaunchedEffect
                    if (GuestChatStore(this@MainActivity).pendingFor(current).isNotEmpty()) {
                        try {
                            val count = GuestChatTransfer.sendPending(this@MainActivity, current, chatRepository)
                            if (count > 0) Toast.makeText(this@MainActivity,
                                "$count mensaje(s) de invitado enviados", Toast.LENGTH_LONG).show()
                        } catch (cause: Exception) {
                            if (cause is CancellationException) throw cause
                            Toast.makeText(this@MainActivity,
                                "Hay mensajes de invitado pendientes. Abre Chat para reintentarlo.",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
                var showBrand by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    // Legacy SplashScreen: MILISECONDS_COUNT = 3000.
                    // Mantener la portada visible el mismo tiempo que la app original.
                    delay(3000)
                    showBrand = false
                }
                if (showBrand) BrandScreen()
                else if (profile == null) InitialSetupScreen { selected ->
                    // Legacy: INIT_CONFIG solo se marcaba cuando terminaban correctamente
                    // las 17 descargas iniciales. No entrar al catálogo con una descarga fallida.
                    try {
                        withContext(Dispatchers.IO) { repository.refreshContent() }
                        selected.save(this@MainActivity)
                        profile = selected
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                else {
                    var section by remember { mutableStateOf("courses") }
                    val catalogPage = remember(session?.id, catalogContentVersion) {
                        mutableStateOf<CatalogPage>(CatalogPage.Courses)
                    }
                    var contactSubject by remember { mutableStateOf(6) }
                    var contactReturnSection by remember { mutableStateOf("courses") }
                    var atCatalogRoot by remember { mutableStateOf(true) }
                    fun openContact(subject: Int) {
                        contactReturnSection = section
                        contactSubject = subject
                        section = "contact"
                    }
                    BackHandler(enabled = section != "courses") {
                        section = if (section == "contact") contactReturnSection else "courses"
                    }
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when (section) {
                                "courses" -> key(session?.id, catalogContentVersion) {
                                    CatalogScreen(repository, imageRepository, progressStore, profile!!,
                                        session, syncRepository, syncVersion, catalogPage, { syncVersion++ },
                                        {
                                            if (session != null && !hasLegacyInternet(this@MainActivity))
                                                Toast.makeText(this@MainActivity, "No hay conexión a internet", Toast.LENGTH_SHORT).show()
                                            else section = "account"
                                        }, {
                                            try { progressStore.clearLocalProgress() } catch (_: Exception) { }
                                            sessionStore.clear()
                                            session = null
                                            syncVersion++
                                            section = "courses"
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                                try { SocialAuthManager.signOut(this@MainActivity) } catch (_: Exception) { }
                                            }
                                        }, { openContact(6) },
                                        { section = "help_us" }, { section = "help" },
                                        { section = "information" }, { section = "settings" },
                                        { section = "chat" }) { atCatalogRoot = it }
                                }
                                "faith" -> FaithScreen(profile!!, getString(R.string.api_base_url)) { openContact(0) }
                                "news" -> NewsScreen(profile!!)
                                "chat" -> if (session == null) GuestChatScreen(profile!!, chatRepository) {
                                    section = "account"
                                } else ChatScreen(session!!, profile!!, chatRepository)
                                "account" -> AccountScreen(session, authRepository, sessionStore, progressStore, syncRepository, repository, getString(R.string.api_base_url), { syncVersion++ }, { year ->
                                    profile = profile?.copy(birthYear = year)?.also { it.save(this@MainActivity) }
                                }) { signedIn ->
                                    session = signedIn
                                    if (signedIn != null && signedIn.gender in listOf("MALE", "FEMALE") &&
                                        profile?.gender != signedIn.gender) {
                                        profile = profile?.copy(gender = signedIn.gender)?.also { it.save(this@MainActivity) }
                                    }
                                }
                                "contact" -> ContactScreen(profile!!, session,
                                    getString(R.string.api_base_url), contactSubject) {
                                    section = contactReturnSection
                                }
                                "help_us" -> HelpUsScreen(session, progressStore, repository,
                                    syncRepository, getString(R.string.api_base_url)) {
                                    section = "courses"
                                }
                                "help" -> LegacyInfoScreen(help = true, repository = repository) { section = "courses" }
                                "information" -> LegacyInfoScreen(help = false, repository = repository,
                                    user = session, progressStore = progressStore, syncRepository = syncRepository) {
                                    catalogContentVersion++
                                    syncVersion++
                                    section = "courses"
                                }
                                "settings" -> SettingsScreen(repository, imageRepository) { fontOption = it }
                                else -> PrayerScreen(profile!!) { openContact(it) }
                            }
                        }
                        if (section != "courses" || atCatalogRoot) Row(
                            modifier = Modifier.fillMaxWidth().background(profile!!.accent)
                                .padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(Modifier.weight(1f).clickable { openLegacyOnlineSection(this@MainActivity) { section = "news" } }.padding(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(painterResource(R.mipmap.document), contentDescription = null,
                                    colorFilter = ColorFilter.tint(Color.White), modifier = Modifier.size(25.dp))
                                Text("Noticias", color = Color.White, fontSize = 11.sp, maxLines = 1)
                            }
                            Column(Modifier.weight(1f).clickable { openLegacyOnlineSection(this@MainActivity) { section = "faith" } }.padding(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(painterResource(R.mipmap.vela), contentDescription = null,
                                    colorFilter = ColorFilter.tint(Color.White), modifier = Modifier.size(25.dp))
                                Text("Fe", color = Color.White, fontSize = 11.sp, maxLines = 1)
                            }
                            Column(Modifier.weight(1f).clickable { openLegacyOnlineSection(this@MainActivity) { section = "prayer" } }.padding(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(painterResource(R.mipmap.prayer), contentDescription = null,
                                    colorFilter = ColorFilter.tint(Color.White), modifier = Modifier.size(25.dp))
                                Text("Oraciones", color = Color.White, fontSize = 11.sp, maxLines = 1)
                            }
                            Column(Modifier.weight(1f).clickable { openLegacyOnlineSection(this@MainActivity) { section = "chat" } }.padding(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(painterResource(R.mipmap.chat), contentDescription = null,
                                    colorFilter = ColorFilter.tint(Color.White), modifier = Modifier.size(25.dp))
                                Text("Chat", color = Color.White, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun hasLegacyInternet(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun openLegacyOnlineSection(context: Context, open: () -> Unit) {
    if (hasLegacyInternet(context)) open()
    else Toast.makeText(context, "No hay conexión a internet", Toast.LENGTH_SHORT).show()
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
            modifier = Modifier.align(Alignment.Center).size(175.dp)
        )
        Image(
            painter = painterResource(R.drawable.logouh),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 7.dp).width(160.dp).height(80.dp)
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
    user: UserSession?,
    syncRepository: ProgressSyncRepository,
    syncVersion: Int,
    pageState: MutableState<CatalogPage>,
    onSynced: () -> Unit,
    onOpenAccount: () -> Unit,
    onLogout: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenHelpUs: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenInformation: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChat: () -> Unit,
    onRootChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val syncScope = rememberCoroutineScope()
    var page by pageState
    var accountMenuOpen by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var gloriaTitle by remember { mutableStateOf<String?>(null) }
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
                    lessonExtrasError = ApiMessages.fromException(error, "Error del servidor")
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

    LaunchedEffect(page, courseApproved, user?.id) {
        if (page is CatalogPage.Classes && courseApproved && user != null) {
            try { syncRepository.sync(user, progressStore) }
            catch (_: Exception) { /* El progreso aprobado permanece guardado localmente. */ }
        }
    }
    LaunchedEffect(page, reload, syncVersion) {
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
            CatalogState.Error(ApiMessages.fromException(error, "No se pudo conectar con el servidor"))
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
                Image(
                    painter = painterResource(R.drawable.ic_baseline_arrow_back_ios_24),
                    contentDescription = "Volver",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(horizontal = 16.dp)
                        .size(24.dp)
                        .clickable { goBack() }
                )
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
                CatalogPage.Courses -> user?.displayName ?: if (profile.gender == "MALE") "Invitado" else "Invitada"
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
            Box(Modifier.align(Alignment.CenterEnd)) {
                Text("⋮", color = Color.White, fontSize = 24.sp,
                    modifier = Modifier.clickable { accountMenuOpen = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp))
                DropdownMenu(expanded = accountMenuOpen,
                    onDismissRequest = { accountMenuOpen = false }) {
                    if (user == null) {
                        DropdownMenuItem(text = { Text("Iniciar sesión") },
                            onClick = { accountMenuOpen = false; onOpenAccount() })
                    } else {
                        DropdownMenuItem(text = { Text("Cerrar sesión") },
                            onClick = { accountMenuOpen = false; confirmLogout = true })
                    }
                    DropdownMenuItem(text = { Text("Perfil") }, onClick = {
                        accountMenuOpen = false
                        if (user == null) {
                            Toast.makeText(context, "Solo es posible acceder si se registra :)", Toast.LENGTH_SHORT).show()
                        } else if (!hasLegacyInternet(context)) {
                            Toast.makeText(context, "No hay conexión a internet", Toast.LENGTH_SHORT).show()
                        } else onOpenAccount()
                    })
                    DropdownMenuItem(text = { Text("Contactar") },
                        onClick = { accountMenuOpen = false; onOpenContact() })
                    DropdownMenuItem(text = { Text("Información") },
                        onClick = { accountMenuOpen = false; onOpenInformation() })
                    DropdownMenuItem(text = { Text("Ajustes") },
                        onClick = { accountMenuOpen = false; onOpenSettings() })
                    DropdownMenuItem(text = { Text("Ayuda") },
                        onClick = { accountMenuOpen = false; onOpenHelp() })
                    DropdownMenuItem(text = { Text("Ayúdanos") },
                        onClick = { accountMenuOpen = false; onOpenHelpUs() })
                }
            }
        }
        if (confirmLogout) AlertDialog(
            onDismissRequest = { confirmLogout = false },
            text = { Text("¿Deseas cerrar la sesión?") },
            confirmButton = { TextButton(onClick = {
                confirmLogout = false
                onLogout()
            }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } }
        )
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
        if (page is CatalogPage.Classes) Text(title, modifier = Modifier.fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 10.dp),
            style = MaterialTheme.typography.titleMedium, color = Color(0xFF505050))
        if (page is CatalogPage.GameHub) {
            val hub = page as CatalogPage.GameHub
            GameHubScreen(hub.course, hub.courseClass.id, repository, imageRepository, accent) { key ->
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
            QuizGameScreen(game.courseClass.id, repository, accent) {
                page = CatalogPage.GameHub(game.course, game.courseClass)
            }
        } else if (page is CatalogPage.Board) {
            WhiteBoardScreen()
        } else if (page is CatalogPage.ImageGame) {
            val game = page as CatalogPage.ImageGame
            ImageActivitiesScreen(game.courseClass.id, game.type, repository, accent) {
                page = CatalogPage.GameHub(game.course, game.courseClass)
            }
        } else if (page is CatalogPage.Crossword) {
            val game = page as CatalogPage.Crossword
            CrosswordScreen(game.courseClass.id, repository, accent)
        } else if (page is CatalogPage.Enigma) {
            val game = page as CatalogPage.Enigma
            EnigmaScreen(game.courseClass.id, repository, accent) {
                page = CatalogPage.GameHub(game.course, game.courseClass)
            }
        } else if (page is CatalogPage.Match) {
            val game = page as CatalogPage.Match
            MatchScreen(game.courseClass.id, repository, accent) {
                page = CatalogPage.GameHub(game.course, game.courseClass)
            }
        } else if (page is CatalogPage.TrueFalse) {
            val game = page as CatalogPage.TrueFalse
            TrueFalseScreen(game.courseClass.id, repository, accent) {
                page = CatalogPage.GameHub(game.course, game.courseClass)
            }
        } else if (page is CatalogPage.Hangman) {
            val game = page as CatalogPage.Hangman
            HangmanScreen(game.courseClass.id, repository, accent) {
                page = CatalogPage.GameHub(game.course, game.courseClass)
            }
        } else if (page is CatalogPage.Exam) {
            val exam = page as CatalogPage.Exam
            ClassExamScreen(exam.courseClass.id, repository, progressStore, profile) {
                progressRefresh++
                if (user != null) syncScope.launch {
                    try { syncRepository.sync(user, progressStore); onSynced() }
                    catch (_: Exception) { /* Se mantiene local; reintentar en Cuenta. */ }
                }
            }
        } else if (page is CatalogPage.LessonDetail) {
            val detail = page as CatalogPage.LessonDetail
            LessonDetailReading(detail.lesson, onOpenChat)
        } else if (page is CatalogPage.Lessons && state is CatalogState.Ready) {
            val current = page as CatalogPage.Lessons
            ThemeReadingScreen(current.theme, lessons, lessonExtras, lessonExtrasError, onOpenChat)
        } else when (val result = state) {
            CatalogState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is CatalogState.Error -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("No se pudo cargar: ${result.message}")
                Button(onClick = { reload++ }) { Text("Reintentar") }
            }
            is CatalogState.Ready -> if (page is CatalogPage.Themes) {
                val summary = page as CatalogPage.Themes
                var lessonIndex by remember(summary, themes) { mutableStateOf<Map<Int, List<Lesson>>?>(null) }
                var indexError by remember(summary, themes) { mutableStateOf<String?>(null) }
                LaunchedEffect(summary, themes) {
                    try {
                        lessonIndex = withContext(Dispatchers.IO) {
                            themes.associate { theme -> theme.id to repository.getLessons(theme.id) }
                        }
                    } catch (cause: Exception) {
                        indexError = ApiMessages.fromException(cause, "No se pudo cargar el índice de lecciones")
                    }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("Resumen", modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                        color = Color.Black, fontSize = 13.sp)
                    themes.forEach { theme ->
                        Text("${theme.number}. ${theme.name}",
                            modifier = Modifier.fillMaxWidth().clickable {
                                page = CatalogPage.Lessons(summary.course, summary.courseClass, theme)
                            }.padding(start = 15.dp, end = 15.dp, top = 10.dp),
                            color = Color(0xFF505050), fontSize = 13.sp)
                        lessonIndex?.get(theme.id)?.forEach { lesson ->
                            Text("${theme.number}.${lesson.number}. ${lesson.name}",
                                modifier = Modifier.fillMaxWidth().clickable {
                                    page = CatalogPage.LessonDetail(summary.course, summary.courseClass, theme, lesson)
                                }.padding(start = 30.dp, end = 15.dp, top = 5.dp),
                                color = Color(0xFF505050), fontSize = 13.sp,
                                textDecoration = TextDecoration.Underline)
                        }
                    }
                    if (lessonIndex == null && indexError == null) CircularProgressIndicator()
                    indexError?.let { Text(it) }
                }
            } else if (page is CatalogPage.Goals) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("Las metas para la semana son:", color = Color.Black, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 10.dp))
                    if (goals.isEmpty()) Text("No hay metas para esta clase.", modifier = Modifier.padding(4.dp))
                    goals.forEach { goal ->
                        Text("${goal.number}. ${goal.content}", color = Color(0xFF505050), fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp))
                    }
                }
            } else if (page is CatalogPage.Activities) {
                val activitiesPage = page as CatalogPage.Activities
                val activityPreferences = remember(context) { AppPreferences(context) }
                var showReal by remember(page) { mutableStateOf(activityPreferences.showRealActivities) }
                var showOffline by remember(page) { mutableStateOf(activityPreferences.showOfflineActivities) }
                var showOnline by remember(page) { mutableStateOf(activityPreferences.showOnlineActivities) }
                var showInstructions by remember(page) { mutableStateOf(false) }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    LegacyActivityHeader("Actividades reales", showReal) { showReal = !showReal }
                    if (showReal) Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        realActivities.forEach { activity -> LegacyActivityTextRow(activity.content) }
                    }
                    LegacyActivityHeader("Actividades virtuales off-line", showOffline) { showOffline = !showOffline }
                    if (showOffline) Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        LegacyOfflineActivityRow(R.drawable.image_adivina, "Juegos con imágenes") { page = CatalogPage.GameHub(activitiesPage.course, activitiesPage.courseClass) }
                        LegacyOfflineActivityRow(R.drawable.crucigrama, "Juegos con palabras") { page = CatalogPage.GameHub(activitiesPage.course, activitiesPage.courseClass) }
                        LegacyOfflineActivityRow(R.drawable.course_approved, "Juegos de trivia") { page = CatalogPage.GameHub(activitiesPage.course, activitiesPage.courseClass) }
                        LegacyOfflineActivityRow(R.drawable.pizarra, "Pizarra para tu clase") { page = CatalogPage.GameHub(activitiesPage.course, activitiesPage.courseClass) }
                        LegacyOfflineActivityRow(R.drawable.selfie, "Selfie\nAutoanalizarse") { page = CatalogPage.GameHub(activitiesPage.course, activitiesPage.courseClass) }
                        LegacyOfflineActivityRow(R.drawable.help_game, stringResource(R.string.textExplicacionJuego)) { showInstructions = true }
                    }
                    LegacyActivityHeader("Actividades virtuales on-line", showOnline) { showOnline = !showOnline }
                    if (showOnline) Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        onlineActivities.forEach { activity ->
                            Column(Modifier.fillMaxWidth().padding(5.dp).background(Color(0x77000000)).clickable {
                                val link = HttpsLinks.external(activity.link)
                                if (link != null) try { context.startActivity(Intent(Intent.ACTION_VIEW, link)) }
                                catch (_: Exception) { Toast.makeText(context, "No se pudo abrir el enlace por HTTPS", Toast.LENGTH_SHORT).show() }
                            }.padding(10.dp)) {
                                Row(Modifier.fillMaxWidth()) {
                                    Text(activity.title, fontSize = 13.sp, color = Color(0xFF505050), modifier = Modifier.weight(1f))
                                    Text(activity.type, fontSize = 13.sp, color = Color.Black)
                                }
                                Text("Ir a la actividad", fontSize = 13.sp, color = Color(0xFF505050), modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
                if (showInstructions) {
                    val instructions = listOf(
                        R.string.textExpGameOne to R.string.textExpGameOneSummary, R.string.textExpGameTwo to R.string.textExpGameTwoSummary,
                        R.string.textExpGameThree to R.string.textExpGameThreeSummary, R.string.textExpGameFour to R.string.textExpGameFourSummary,
                        R.string.textExpGameFive to R.string.textExpGameFiveSummary, R.string.textExpGameSix to R.string.textExpGameSixSummary,
                        R.string.textExpGameSeven to R.string.textExpGameSevenSummary, R.string.textExpGameEight to R.string.textExpGameEightSummary,
                        R.string.textExpGameNine to R.string.textExpGameNineSummary)
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showInstructions = false }) {
                        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
                            Column(Modifier.fillMaxSize()) {
                                Row(Modifier.fillMaxWidth().background(accent).padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Image(painterResource(R.drawable.ic_baseline_arrow_back_ios_24), "Volver", modifier = Modifier.size(24.dp).clickable { showInstructions = false }, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White))
                                    Text(stringResource(R.string.textExplicacionJuego), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 3.dp)) {
                                    instructions.forEach { (t, d) ->
                                        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp).background(Color.White).padding(10.dp)) {
                                            Text(stringResource(t), color = Color(0xFF7A9989), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(stringResource(d), color = Color(0xFF505050), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (result.rows.isEmpty()) {
                Text("No hay contenido disponible en esta sección.", modifier = Modifier.padding(20.dp))
            } else if (page is CatalogPage.Classes) {
                var certificatePending by remember(page) { mutableStateOf(false) }
                var certificateError by remember(page) { mutableStateOf<String?>(null) }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    if (courseApproved) {
                        // ClassCourseActivity legacy muestra una única leyenda accionable distinta
                        // según exista o no una sesión iniciada.
                        val approvedMessage = if (user == null)
                            "Felicidades has completado el curso\nSi deseas obtener un certificado digital debes iniciar sesión."
                        else
                            "Tocar para visualizar certificado digital.\n (Requiere internet)"
                        Text(approvedMessage,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(enabled = !certificatePending) {
                                if (user == null) onOpenAccount() else {
                                    val selectedCourse = (page as? CatalogPage.Classes)?.course ?: return@clickable
                                    syncScope.launch {
                                        certificatePending = true
                                        certificateError = null
                                        try {
                                            syncRepository.sync(user, progressStore)
                                            onSynced()
                                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                                certificateUri(context.getString(R.string.api_base_url), user.id, selectedCourse.id)))
                                        } catch (error: Exception) {
                                            certificateError = ApiMessages.fromException(error,
                                                "No se pudo verificar el curso o abrir el certificado")
                                        } finally { certificatePending = false }
                                    }
                                }
                            },
                            color = Color(0xFF505050), style = MaterialTheme.typography.titleMedium)
                        certificateError?.let { Text(it, color = Color(0xFF8B2626)) }
                    }
                    // En la app anterior el fondo ocupaba todo el GridLayout: cinco columnas,
                    // filas de 30 unidades para un ancho de 200. Se escala junto con el mapa.
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).padding(10.dp),
                        contentAlignment = Alignment.Center) {
                        val mapWidth = 200.dp
                        val rowCount = (classes.size + 4) / 5
                        val mapHeight = 30.dp * rowCount.coerceAtLeast(1)
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
                    Text("ÍNDICE", modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)
                        .background(Color(0x77FFFFFF)).padding(10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 14.sp, color = Color(0xFF505050))
                    result.rows.forEach { entry ->
                        val flags = remember(entry.id, progressRefresh) { progressStore.flags(entry.id) }
                        Text(entry.label, modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 2.dp)
                            .background(Color(0x77FFFFFF)).combinedClickable(onClick = {
                                val selected = classes.firstOrNull { it.id == entry.id }
                                val current = page as? CatalogPage.Classes
                                if (selected != null && current != null) {
                                    progressStore.setVisited(selected.id, true)
                                    progressRefresh++
                                    page = CatalogPage.Themes(current.course, selected)
                                }
                            }, onLongClick = {
                                progressStore.setVisited(entry.id, false)
                                progressRefresh++
                            }).padding(10.dp), color = if (flags.visited) Color.Black else Color(0xFF505050),
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
                        Card(modifier = Modifier.fillMaxWidth().padding(start = if (isCourse) 20.dp else 12.dp,
                            end = if (isCourse) 20.dp else 12.dp, top = 2.dp, bottom = 3.dp),
                            shape = RoundedCornerShape(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().background(itemColor)
                                .clickable {
                                    when (val current = page) {
                                        CatalogPage.Courses -> courses.firstOrNull { it.id == row.id }?.let { course ->
                                            if (position == 3) gloriaTitle = course.name
                                            else page = CatalogPage.Classes(course)
                                        }
                                        is CatalogPage.Themes -> themes.firstOrNull { it.id == row.id }?.let { page = CatalogPage.Lessons(current.course, current.courseClass, it) }
                                        is CatalogPage.Lessons -> lessons.firstOrNull { it.id == row.id }?.let { page = CatalogPage.LessonDetail(current.course, current.courseClass, current.theme, it) }
                                        else -> Unit
                                    }
                                }.padding(if (isCourse) 10.dp else 12.dp).heightIn(min = if (isCourse) 40.dp else 40.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(row.label, modifier = Modifier.weight(1f), maxLines = 2,
                                    overflow = TextOverflow.Ellipsis, color = if (isCourse) Color.White else Color(0xFF505050),
                                    fontSize = if (isCourse) 13.sp else 16.sp)
                                if (isCourse) Image(painterResource(icon), contentDescription = null,
                                    modifier = Modifier.size(32.dp))

                            }
                        }
                    }
                }
            }
        }
    }
    gloriaTitle?.let { title ->
        val reveal = remember(title) { Animatable(0f) }
        LaunchedEffect(title) { reveal.animateTo(1f, tween(durationMillis = 3000)) }
        Dialog(onDismissRequest = { gloriaTitle = null }) {
            Surface(color = Color.White, tonalElevation = 0.dp, shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (reveal.value < 1f) {
                        Image(painterResource(R.drawable.destello), contentDescription = null,
                            modifier = Modifier.fillMaxWidth().padding(25.dp).graphicsLayer {
                                rotationZ = 360f * reveal.value
                                scaleX = 1f + 49f * reveal.value
                                scaleY = 1f + 49f * reveal.value
                                alpha = 1f - reveal.value
                            })
                    } else {
                        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().background(accent).padding(10.dp))
                        Text("ATENCIÓN: A este curso sólo se puede acceder después de muerto. Lo dictará el mismo Dios a los que hayan aprobado los cursos anteriores. Inténtelo más tarde, una vez que hayas cumplido tu misión en la vida.",
                            color = Color(0xFF505050), fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 30.dp, bottom = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyActivityHeader(title: String, expanded: Boolean, onClick: () -> Unit) {
    Row(Modifier.padding(start = 4.dp, end = 15.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.Black, fontSize = 13.sp, modifier = Modifier.width(200.dp))
        Image(painterResource(if (expanded) R.drawable.close else R.drawable.expand), null, Modifier.padding(start = 10.dp).width(48.dp).height(30.dp).clickable(onClick = onClick))
    }
}
@Composable
private fun LegacyActivityTextRow(text: String) {
    Text(text, fontSize = 13.sp, color = Color(0xFF505050), modifier = Modifier.fillMaxWidth().padding(5.dp).background(Color(0x77000000)).padding(10.dp))
}
@Composable
private fun LegacyOfflineActivityRow(icon: Int, text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(5.dp).background(Color(0x77000000)).clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(icon), null, Modifier.size(48.dp)); Text(text, color = Color(0xFF505050), fontSize = 13.sp, modifier = Modifier.padding(start = 5.dp))
    }
}
