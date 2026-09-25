package com.chayzay.catequesisapp.news

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.links.HttpsLinks
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.profile.ProfileSettings
import com.chayzay.catequesisapp.settings.AppPreferences
import java.io.File
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private const val RSS_URL = "https://www.aciprensa.com/rss/news/mundo"
private data class Article(val title: String, val description: String, val date: String, val link: String)

@Composable
fun NewsScreen(profile: ProfileSettings) {
    val context = LocalContext.current
    val settings = remember(context) { AppPreferences(context) }
    var newsEnabled by remember { mutableStateOf(settings.news) }
    if (!newsEnabled) {
        Column(Modifier.fillMaxSize().background(profile.baseColor).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Las noticias están desactivadas en Ajustes.")
            Button(onClick = { settings.news = true; newsEnabled = true }) { Text("Activar noticias") }
        }
        return
    }
    var refresh by remember { mutableStateOf(0) }
    var articles by remember { mutableStateOf<List<Article>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(refresh) {
        error = null
        try {
            articles = withContext(Dispatchers.IO) { loadNews(File(context.cacheDir, "news_mundo.xml")) }
        } catch (exception: Exception) {
            error = exception.localizedMessage ?: "No se pudieron cargar las noticias"
            articles = emptyList()
        }
    }
    Column(Modifier.fillMaxSize().background(profile.baseColor)) {
        Row(Modifier.fillMaxWidth().background(profile.accent).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.feed), contentDescription = null, modifier = Modifier.size(28.dp))
            Text("Noticias", color = Color.White, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 12.dp))
        }
        Row(Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ACI Prensa · Mundo", style = MaterialTheme.typography.titleMedium, color = Color.DarkGray)
            Button(onClick = { refresh++ }) { Text("Actualizar") }
        }
        Button(modifier = Modifier.padding(horizontal = 12.dp), onClick = {
            try { context.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://x.com/Pontifex_es"))) }
            catch (_: Exception) { Toast.makeText(context, "No se pudieron abrir las publicaciones", Toast.LENGTH_SHORT).show() }
        }) { Text("Publicaciones del Papa en X") }
        when {
            articles == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            articles!!.isEmpty() -> Text(error ?: "No hay noticias disponibles.", Modifier.padding(18.dp))
            else -> {
                Column {
                    if (error != null) Text("Mostrando noticias guardadas: $error", Modifier.padding(12.dp))
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(articles!!, key = { it.link }) { article ->
                            Card(Modifier.fillMaxWidth().clickable {
                                val uri = HttpsLinks.external(article.link)
                                if (uri != null) {
                                    try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                    catch (_: Exception) { Toast.makeText(context, "No se pudo abrir la noticia por HTTPS", Toast.LENGTH_SHORT).show() }
                                }
                            }) {
                                Column(Modifier.fillMaxWidth().background(Color.White).padding(14.dp)) {
                                    Text(article.title, color = Color.DarkGray, style = MaterialTheme.typography.titleMedium)
                                    if (article.date.isNotBlank()) Text(article.date, color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall)
                                    if (article.description.isNotBlank()) Text(article.description, color = Color.DarkGray,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadNews(cache: File): List<Article> {
    val xml = try {
        val connection = (URL(RSS_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml")
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val list = parseNews(text)
            if (list.isEmpty()) throw IllegalStateException("El canal no contiene noticias")
            try { cache.writeText(text, Charsets.UTF_8) } catch (_: Exception) { }
            text
        } finally { connection.disconnect() }
    } catch (error: Exception) {
        if (!cache.exists()) throw error
        cache.readText(Charsets.UTF_8)
    }
    return parseNews(xml)
}

private fun parseNews(xml: String): List<Article> {
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(StringReader(xml))
    val result = mutableListOf<Article>()
    var insideItem = false
    var title = ""
    var description = ""
    var date = ""
    var link = ""
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                val tag = parser.name
                if (tag == "item") {
                    insideItem = true
                    title = ""; description = ""; date = ""; link = ""
                } else if (insideItem && tag in setOf("title", "description", "pubDate", "link")) {
                    val value = parser.nextText().trim().replace(Regex("<[^>]+>"), " ")
                    when (tag) {
                        "title" -> title = value
                        "description" -> description = value.take(280)
                        "pubDate" -> date = value
                        "link" -> link = value
                    }
                }
            }
            XmlPullParser.END_TAG -> if (parser.name == "item") {
                if (title.isNotBlank() && link.startsWith("http")) result.add(Article(title, description, date, link))
                insideItem = false
            }
        }
        event = parser.next()
    }
    return result.distinctBy { it.link }
}
