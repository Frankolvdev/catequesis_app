package com.chayzay.catequesisapp.news

import android.content.Intent
import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.links.HttpsLinks
import com.chayzay.catequesisapp.profile.ProfileSettings
import com.chayzay.catequesisapp.settings.AppPreferences
import java.io.File
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

private const val RSS_URL = "https://www.aciprensa.com/rss/news/mundo"
private data class Article(val title: String, val description: String, val date: String, val link: String)
private enum class NewsMode { RSS, X }

@Composable
fun NewsScreen(profile: ProfileSettings) {
    val context = LocalContext.current
    val settings = remember(context) { AppPreferences(context) }
    var newsEnabled by remember { mutableStateOf(settings.news) }
    if (!newsEnabled) {
        Column(Modifier.fillMaxSize().background(profile.baseColor), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Las noticias están desactivadas en Ajustes.", fontSize = 13.sp)
            Button(modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp), onClick = { settings.news = true; newsEnabled = true }) {
                Text("Activar noticias", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    var mode by remember { mutableStateOf(NewsMode.RSS) }
    var refresh by remember { mutableStateOf(0) }
    var articles by remember { mutableStateOf<List<Article>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mode, refresh) {
        error = null
        if (mode == NewsMode.RSS) {
            try {
                articles = withContext(Dispatchers.IO) { loadNews(File(context.cacheDir, "news_mundo.xml")) }
            } catch (exception: Exception) {
                error = exception.localizedMessage ?: "No se pudieron cargar las noticias"
                articles = emptyList()
            }
        } else {
            // El timeline de X se renderiza con el widget web oficial. No requiere API ni Bearer Token.
        }
    }

    Column(Modifier.fillMaxSize().background(profile.baseColor)) {
        Row(Modifier.fillMaxWidth().background(profile.accent).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.feed), contentDescription = null, modifier = Modifier.size(28.dp))
            Text("Noticias", color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp))
        }
        // El fragment legacy no tenía botones Material aquí: era una fila de 5dp con
        // texto de 13sp y un icono de 32dp (Twitter/feed) para alternar la fuente.
        Row(Modifier.fillMaxWidth().padding(5.dp).clickable {
            mode = if (mode == NewsMode.RSS) NewsMode.X else NewsMode.RSS
        }, verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (mode == NewsMode.RSS) "Últimos tweets del Papa" else "Volver a las Noticias",
                color = Color.Black, fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.weight(1f).padding(end = 5.dp)
            )
            Image(
                painterResource(if (mode == NewsMode.RSS) R.drawable.twitter_icon else R.drawable.feed),
                contentDescription = null, modifier = Modifier.size(32.dp)
            )
        }

        if (mode == NewsMode.RSS) {
            when {
                articles == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                articles!!.isEmpty() -> Text(error ?: "No hay noticias disponibles.", Modifier.padding(18.dp))
                else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    items(articles!!, key = { it.link }) { article ->
                        Card(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 2.dp).clickable {
                            HttpsLinks.external(article.link)?.let { uri -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } }
                        }) {
                            Column(Modifier.fillMaxWidth().background(Color.White).padding(10.dp)) {
                                Image(painterResource(R.drawable.feed), contentDescription = null, modifier = Modifier.size(16.dp).align(Alignment.End))
                                Text(article.title, color = Color(0xFF7A9989), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (article.description.isNotBlank()) Text(legacyRssDescription(article.description), color = Color(0xFF424242), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                                if (article.date.isNotBlank()) Text(legacyRssDate(article.date), color = Color(0xFF7A9989), fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            }
                        }
                    }
                }
            }
        } else {
            PapaTimeline(Modifier.fillMaxSize())
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PapaTimeline(modifier: Modifier = Modifier) {
    val html = remember {
        """
        <!doctype html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
                html, body { margin:0; padding:0; background:#ffffff; }
                .twitter-timeline { width:100% !important; }
            </style>
        </head>
        <body>
            <a class="twitter-timeline"
               data-lang="es"
               data-theme="light"
               data-chrome="noheader nofooter noborders transparent"
               data-dnt="true"
               href="https://twitter.com/Pontifex_es?ref_src=twsrc%5Etfw">Tweets de @Pontifex_es</a>
            <script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier.background(Color.White),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                // X usa cookies y recursos de varios subdominios para construir el timeline.
                // Sin cookies de terceros WebView puede dejar únicamente el texto del enlace.
                val timelineWebView = this
                CookieManager.getInstance().apply {
                    setAcceptCookie(true)
                    setAcceptThirdPartyCookies(timelineWebView, true)
                }

                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL("https://twitter.com/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            if (webView.url == null) {
                webView.loadDataWithBaseURL("https://twitter.com/", html, "text/html", "UTF-8", null)
            }
        }
    )
}

private fun loadNews(cache: File): List<Article> {
    val xml = try {
        val connection = (URL(RSS_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000; readTimeout = 12000
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
    var insideItem = false; var title = ""; var description = ""; var date = ""; var link = ""
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                val tag = parser.name
                if (tag == "item") { insideItem = true; title = ""; description = ""; date = ""; link = "" }
                else if (insideItem && tag in setOf("title", "description", "pubDate", "link")) {
                    val value = parser.nextText().trim().replace(Regex("<[^>]+>"), " ")
                    when (tag) { "title" -> title = value; "description" -> description = value.take(280); "pubDate" -> date = value; "link" -> link = value }
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

private fun legacyRssDescription(value: String): String {
    val plain = value.trim()
    val cut = if (plain.length > 80) plain.substring(0, 80) else plain
    return "$cut ..."
}

private fun legacyRssDate(value: String): String {
    if (value.isBlank()) return value
    val inputs = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )
    for (pattern in inputs) {
        try {
            val input = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = true }
            val date = input.parse(value) ?: continue
            return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        } catch (_: Exception) { }
    }
    return value
}
