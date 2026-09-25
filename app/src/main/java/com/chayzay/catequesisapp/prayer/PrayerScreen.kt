package com.chayzay.catequesisapp.prayer

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.profile.ProfileSettings
import java.util.Calendar

private data class PrayerItem(val title: String, val subtitle: String, val icon: Int)
private val prayerMenu = listOf(
    PrayerItem("Pide oraciones al Papa", "Si quieres que el Santo Padre rece por una intención tuya, pídelo aquí.", R.drawable.papafrancisco),
    PrayerItem("Necesito oraciones por una intención mía", "Escribe aquí la intención por la que quieres que recemos.", R.drawable.church),
    PrayerItem("Textos para orar", "Homilías del Papa completas, libro «Hablar con Dios» y otros textos de santos.", R.drawable.rezar),
    PrayerItem("Devocionario", "Encuentra aquí el texto de las oraciones más típicas", R.drawable.holy_bible)
)
private val prayerLinks = listOf(
    PrayerItem("Homilías del papa completas", "Textos del Vaticano", R.drawable.caricatura_papa1),
    PrayerItem("Hablar con Dios de (Francisco Fernández Carvajal)", "Meditaciones para cada día", R.drawable.book),
    PrayerItem("Libros de San Josemaría", "Obras en línea", R.drawable.books)
)
private val devotionals = listOf(
    "Adoro te devote" to "page_prayer_bless_dev.html",
    "Espíritu Santo" to "page_prayer_ep_sto.html",
    "Oraciones para la Virgen" to "page_prayer_mary.html",
    "Oraciones varias" to "page_prayer_basic.html",
    "Responso" to "page_prayer_answer.html",
    "Santo Rosario" to "page_prayer_holy_rosary.html",
    "Virtudes, frutos y mandamientos" to "page_prayer_virt_frut_mand.html"
)

@Composable
fun PrayerScreen(profile: ProfileSettings, onContact: (Int) -> Unit) {
    var page by remember { mutableStateOf("menu") }
    var selected by remember { mutableStateOf(0) }
    val context = LocalContext.current
    BackHandler(page != "menu") {
        page = if (page == "devotional_page") "devotionals" else "menu"
    }
    Column(Modifier.fillMaxSize().background(profile.baseColor)) {
        Row(Modifier.fillMaxWidth().background(profile.accent).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (page != "menu") {
                Icon(painterResource(R.drawable.ic_baseline_arrow_back_ios_24), contentDescription = "Volver", tint = Color.White,
                    modifier = Modifier.size(24.dp).clickable {
                        page = if (page == "devotional_page") "devotionals" else "menu"
                    }.padding(end = 4.dp))
            }
            Text(when (page) {
                "links" -> "Textos para orar"
                "devotionals" -> "Devocionario"
                "devotional_page" -> devotionals[selected].first
                else -> "Oraciones"
            }, color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        when (page) {
            "menu" -> PrayerList(prayerMenu) { index ->
                if (index == 0 || index == 1) onContact(if (index == 0) 3 else 4)
                else page = if (index == 2) "links" else "devotionals"
            }
            "links" -> PrayerList(prayerLinks) { index ->
                val url = when (index) {
                    0 -> "https://w2.vatican.va/content/francesco/es/homilies/${Calendar.getInstance().get(Calendar.YEAR)}.index.html"
                    1 -> "https://hablarcondios.org/meditacion-diaria/"
                    else -> "https://escriva.org/es/"
                }
                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                catch (_: Exception) { Toast.makeText(context, "No se puede abrir el enlace", Toast.LENGTH_SHORT).show() }
            }
            "devotionals" -> PrayerList(devotionals.map { PrayerItem(it.first, "", R.drawable.howtoapp_content) }) { index ->
                selected = index
                page = "devotional_page"
            }
            "devotional_page" -> AndroidView(
                factory = { ctx -> WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    webViewClient = WebViewClient()
                } },
                update = { view ->
                    val asset = "file:///android_asset/html/es/${devotionals[selected].second}"
                    if (view.url != asset) view.loadUrl(asset)
                }, modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PrayerList(items: List<PrayerItem>, onSelect: (Int) -> Unit) {
    // row_recycle_view_fragment.xml: 10dp laterales, 3dp arriba, 2dp abajo, padding 10dp.
    LazyColumn(modifier = Modifier.fillMaxSize().background(Color.White)) {
        itemsIndexed(items) { index, item ->
            Card(shape = RoundedCornerShape(2.dp), modifier = Modifier.fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 2.dp)) {
                Row(modifier = Modifier.fillMaxWidth().clickable { onSelect(index) }
                    .background(Color.White).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(item.icon), contentDescription = null, modifier = Modifier.size(48.dp))
                    Column(modifier = Modifier.padding(start = 5.dp)) {
                        Text(item.title, fontWeight = FontWeight.Bold, color = Color(0xFF7A9989), fontSize = 14.sp)
                        if (item.subtitle.isNotBlank()) Text(item.subtitle, color = Color(0xFF424242),
                            fontSize = 12.sp, modifier = Modifier.padding(start = 5.dp, top = 5.dp))
                    }
                }
            }
        }
    }
}
