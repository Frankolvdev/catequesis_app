package com.chayzay.catequesisapp.course

import android.graphics.Color as AndroidColor
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.core.text.HtmlCompat
import com.chayzay.catequesisapp.data.ClassTheme
import com.chayzay.catequesisapp.data.Lesson
import com.chayzay.catequesisapp.data.LessonExtras
import com.chayzay.catequesisapp.links.HttpsLinks
import com.chayzay.catequesisapp.settings.AppPreferences
import kotlinx.coroutines.launch

/** Estructura de ThemeClassFragment: ideas, desarrollo y tres anexos por lección. */
@Composable
fun ThemeReadingScreen(theme: ClassTheme, lessons: List<Lesson>, extras: Map<Int, LessonExtras>?,
                       extrasError: String?) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("Ideas a desarrollar", color = Color.Black, style = MaterialTheme.typography.titleMedium)
                Text("${theme.number}. ${theme.name}", modifier = Modifier.padding(top = 12.dp),
                    color = Color(0xFF505050), style = MaterialTheme.typography.titleMedium)
                lessons.forEachIndexed { index, lesson ->
                    Text("${theme.number}.${lesson.number}. ${lesson.name}",
                        modifier = Modifier.fillMaxWidth().clickable {
                            scope.launch { state.animateScrollToItem(index + 2) }
                        }.padding(top = 12.dp, start = 16.dp), color = Color(0xFF505050),
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item { Text("Desarrollo", color = Color.Black,
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        items(lessons, key = { it.id }) { lesson ->
            Column(Modifier.fillMaxWidth()) {
                Text("${theme.number}.${lesson.number}. ${lesson.name}", color = Color.Black,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
                HtmlBlock(lesson.html)
                val data = extras?.get(lesson.id)
                if (data != null) {
                    ExpandableExtra("Ampliación", data.extension, lesson.id)
                    ExpandableExtra("Anécdotas", data.anecdotes, lesson.id)
                    ExpandableExtra("Catecismo", data.catechism, lesson.id)
                } else if (extras == null && extrasError == null) {
                    CircularProgressIndicator(Modifier.padding(8.dp))
                }
            }
        }
        if (lessons.isEmpty()) item { Text("No hay lecciones para este tema.") }
        if (extrasError != null) item {
            Text("No se pudieron cargar los anexos: $extrasError", color = Color(0xFF8B2626),
                modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun ExpandableExtra(title: String, content: List<String>, lessonId: Int) {
    val context = LocalContext.current
    var expanded by remember(lessonId, title) { mutableStateOf(AppPreferences(context).extra(title)) }
    Text("$title  ${if (expanded) "−" else "+"}",
        color = Color(0xFF505050), style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
            .padding(start = 10.dp, top = 12.dp, bottom = 4.dp))
    if (expanded && content.isNotEmpty()) Column(Modifier.fillMaxWidth()
        .background(Color(0x66FFFFFF)).padding(10.dp)) {
        content.forEach { HtmlBlock(it) }
    }
}

@Composable
private fun HtmlBlock(html: String) {
    val context = LocalContext.current
    val multiplier = when (AppPreferences(context).font) { 1 -> 0.85f; 3 -> 1.2f; 4 -> 1.4f; else -> 1f }
    AndroidView(factory = { ctx -> TextView(ctx).apply {
        textSize = 16f * multiplier
        setTextColor(AndroidColor.DKGRAY)
        movementMethod = LinkMovementMethod.getInstance()
    } }, update = { view ->
        view.textSize = 16f * multiplier
        view.text = HtmlCompat.fromHtml(HttpsLinks.html(html), HtmlCompat.FROM_HTML_MODE_LEGACY)
    }, modifier = Modifier.fillMaxWidth())
}
