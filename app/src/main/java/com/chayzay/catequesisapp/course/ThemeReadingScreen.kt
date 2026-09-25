package com.chayzay.catequesisapp.course

import android.graphics.Color as AndroidColor
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.view.MotionEvent
import android.widget.TextView
import androidx.compose.foundation.background
import com.chayzay.catequesisapp.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
fun LessonDetailReading(lesson: Lesson, onOpenChat: () -> Unit) {
    var focus by remember(lesson.id) { mutableStateOf<ReadingFocus?>(null) }
    var requested by remember(lesson.id) { mutableStateOf<ReadingFocus?>(null) }
    var requestNumber by remember(lesson.id) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onOpenChat) { Image(painterResource(R.drawable.ic_action_chat), "Chat", Modifier.size(32.dp)) }
        LessonVoiceControls(lessonVoicePassages(listOf(lesson)), { focus = it }, requested, requestNumber)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp)) {
            HtmlBlock(lesson.html, focus?.paragraph) {
                requested = ReadingFocus(lesson.id, "main", it)
                requestNumber++
            }
        }
    }
}

/** Estructura de ThemeClassFragment: ideas, desarrollo y tres anexos por lección. */
@Composable
fun ThemeReadingScreen(theme: ClassTheme, lessons: List<Lesson>, extras: Map<Int, LessonExtras>?,
                       extrasError: String?, onOpenChat: () -> Unit) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val expanded = remember(theme.id) { mutableStateMapOf<String, Boolean>() }
    var focus by remember(theme.id) { mutableStateOf<ReadingFocus?>(null) }
    var requested by remember(theme.id) { mutableStateOf<ReadingFocus?>(null) }
    var requestNumber by remember(theme.id) { mutableIntStateOf(0) }
    val preferences = remember(context) { AppPreferences(context) }
    fun isExpanded(lessonId: Int, title: String): Boolean =
        expanded["$lessonId:$title"] ?: preferences.extra(title)
    val passages = remember(lessons, extras, expanded.toMap()) { buildList {
        lessons.forEach { lesson ->
            addAll(lessonVoicePassages(listOf(lesson)))
            val data = extras?.get(lesson.id) ?: return@forEach
            listOf("Ampliación" to data.extension, "Anécdotas" to data.anecdotes,
                "Catecismo" to data.catechism).forEach { (title, content) ->
                if (isExpanded(lesson.id, title)) content.forEachIndexed { block, html ->
                    readingParagraphs(html).forEachIndexed { paragraph, text ->
                        add(VoicePassage(ReadingFocus(lesson.id, "$title:$block", paragraph), text))
                    }
                }
            }
        }
    } }
    androidx.compose.runtime.LaunchedEffect(focus?.lessonId) {
        val index = lessons.indexOfFirst { it.id == focus?.lessonId }
        if (index >= 0) state.animateScrollToItem(index + 2)
    }
    fun readFrom(start: ReadingFocus) { requested = start; requestNumber++ }
    Column(Modifier.fillMaxSize()) {
    TextButton(onClick = onOpenChat) { Image(painterResource(R.drawable.ic_action_chat), "Chat", Modifier.size(32.dp)) }
    LessonVoiceControls(passages, { focus = it }, requested, requestNumber)
    LazyColumn(state = state, modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
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
                HtmlBlock(lesson.html, focus?.takeIf { it.lessonId == lesson.id && it.section == "main" }?.paragraph) {
                    readFrom(ReadingFocus(lesson.id, "main", it))
                }
                val data = extras?.get(lesson.id)
                if (data != null) {
                    ExpandableExtra("Ampliación", data.extension, lesson.id, isExpanded(lesson.id, "Ampliación"),
                        { expanded["${lesson.id}:Ampliación"] = it }, focus, ::readFrom)
                    ExpandableExtra("Anécdotas", data.anecdotes, lesson.id, isExpanded(lesson.id, "Anécdotas"),
                        { expanded["${lesson.id}:Anécdotas"] = it }, focus, ::readFrom)
                    ExpandableExtra("Catecismo", data.catechism, lesson.id, isExpanded(lesson.id, "Catecismo"),
                        { expanded["${lesson.id}:Catecismo"] = it }, focus, ::readFrom)
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
}

@Composable
private fun ExpandableExtra(title: String, content: List<String>, lessonId: Int,
    expanded: Boolean, onExpanded: (Boolean) -> Unit, focus: ReadingFocus?, onRead: (ReadingFocus) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onExpanded(!expanded) }
        .padding(start = 10.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color(0xFF505050), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f))
        Image(painterResource(if (expanded) R.drawable.close else R.drawable.expand),
            if (expanded) "Contraer" else "Expandir", Modifier.size(width = 48.dp, height = 30.dp))
    }
    if (expanded && content.isNotEmpty()) Column(Modifier.fillMaxWidth()
        .background(Color(0x66FFFFFF)).padding(10.dp)) {
        content.forEachIndexed { block, html ->
            HtmlBlock(html, focus?.takeIf { it.lessonId == lessonId && it.section == "$title:$block" }?.paragraph) {
                onRead(ReadingFocus(lessonId, "$title:$block", it))
            }
        }
    }
}

@Composable
private fun HtmlBlock(html: String, highlighted: Int? = null, onRead: ((Int) -> Unit)? = null) {
    val context = LocalContext.current
    val multiplier = when (AppPreferences(context).font) { 1 -> 0.85f; 3 -> 1.2f; 4 -> 1.4f; else -> 1f }
    AndroidView(factory = { ctx -> TextView(ctx).apply {
        textSize = 16f * multiplier
        setTextColor(AndroidColor.DKGRAY)
        movementMethod = LinkMovementMethod.getInstance()
        var downY = 0f
        setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) downY = event.y
            false
        }
        setOnLongClickListener {
            val textLayout = this.layout
            if (textLayout == null || onRead == null) false else {
                val line = textLayout.getLineForVertical((downY - totalPaddingTop + scrollY).toInt().coerceAtLeast(0))
                val offset = textLayout.getLineStart(line)
                val paragraph = text.toString().take(offset).count { it == '\n' }
                val parts = text.toString().split('\n')
                val normalized = parts.take(paragraph).count { it.trim().isNotEmpty() }
                onRead(normalized.coerceIn(0, (parts.count { it.trim().isNotEmpty() } - 1).coerceAtLeast(0)))
                true
            }
        }
    } }, update = { view ->
        view.textSize = 16f * multiplier
        val formatted = SpannableStringBuilder(HtmlCompat.fromHtml(HttpsLinks.html(html), HtmlCompat.FROM_HTML_MODE_LEGACY))
        if (highlighted != null) {
            var start = 0
            var number = 0
            val value = formatted.toString()
            while (start < value.length) {
                val end = value.indexOf('\n', start).let { if (it == -1) value.length else it }
                if (value.substring(start, end).trim().isNotEmpty()) {
                    if (number == highlighted) {
                        formatted.setSpan(BackgroundColorSpan(AndroidColor.YELLOW), start, end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        break
                    }
                    number++
                }
                start = end + 1
            }
        }
        view.text = formatted
    }, modifier = Modifier.fillMaxWidth())
}
