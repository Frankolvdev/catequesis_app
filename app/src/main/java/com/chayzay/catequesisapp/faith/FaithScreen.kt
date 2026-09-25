package com.chayzay.catequesisapp.faith

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.profile.ProfileSettings
import com.chayzay.catequesisapp.data.ApiMessages
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class Celebration(val date: String, val description: String)
private data class FaithItem(val title: String, val summary: String, val icon: Int)

@Composable
fun FaithScreen(profile: ProfileSettings, apiBaseUrl: String, onContact: () -> Unit) {
    var page by remember { mutableStateOf("menu") }
    var year by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    val context = LocalContext.current
    BackHandler(page == "calendar") { page = "menu" }
    Column(Modifier.fillMaxSize().background(profile.baseColor)) {
        Row(Modifier.fillMaxWidth().background(profile.accent).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (page == "calendar") Text("‹", color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.clickable { page = "menu" }.padding(end = 20.dp))
            Text(if (page == "calendar") "Calendario litúrgico" else "Fe",
                color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        if (page == "calendar") {
            CalendarPage(year, onYearChange = { year = it }, apiBaseUrl = apiBaseUrl)
        } else {
            val options = listOf(
                FaithItem("Calendario litúrgico", "Celebraciones por año", R.drawable.calendar),
                FaithItem("Quiero ir a misa", "Aplicación original recomendada", R.drawable.go_to_mass),
                FaithItem("Quiero ser santo", "Escritos de san Josemaría", if (profile.gender == "MALE") R.drawable.becomesaintm else R.drawable.becomesaintf),
                FaithItem("Quiero recibir formación", "Contactar al equipo", R.drawable.retiro),
                FaithItem("Bendición del Papa", "Información sobre la bendición apostólica", R.drawable.pergamino)
            )
            LazyColumn(Modifier.fillMaxSize().background(Color.White)) {
                items(options.size) { index ->
                    val item = options[index]
                    Card(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 2.dp)) {
                        Row(Modifier.fillMaxWidth().background(Color.White).clickable {
                            if (index == 0) page = "calendar"
                            else if (index == 3) onContact()
                            else {
                                val intent = when (index) {
                                    1 -> Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.itsharedservices.hdsmyc.app"))
                                    2 -> Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=ws.ebs.stjosemaria"))
                                    else -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.vaticano.com/como-solicitar-la-bendicion-apostolica-del-papa/"))
                                }
                                try { context.startActivity(intent) }
                                catch (_: Exception) { Toast.makeText(context, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show() }
                            }
                        }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(item.icon), null, modifier = Modifier.size(48.dp))
                            Column(Modifier.padding(start = 5.dp)) {
                                Text(item.title, color = Color(0xFF7A9989), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(item.summary, color = Color(0xFF424242), fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 5.dp, top = 5.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarPage(year: Int, onYearChange: (Int) -> Unit, apiBaseUrl: String) {
    var celebrations by remember(year) { mutableStateOf<List<Celebration>?>(null) }
    var error by remember(year) { mutableStateOf<String?>(null) }
    var retry by remember(year) { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val years = (2002..2399).toList()
    LaunchedEffect(year, apiBaseUrl, retry) {
        error = null; celebrations = null
        try { celebrations = withContext(Dispatchers.IO) { loadCalendar(apiBaseUrl, year) } }
        catch (e: Exception) { error = ApiMessages.fromException(e, "No se pudo cargar el calendario") }
    }
    LaunchedEffect(year, celebrations) {
        val entries = celebrations.orEmpty()
        if (entries.isNotEmpty()) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
            val target = if (year == Calendar.getInstance().get(Calendar.YEAR))
                entries.indexOfFirst { it.date >= today }.takeIf { it >= 0 } ?: 0 else 0
            listState.scrollToItem(target)
        }
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        // fragment_recycle_view_vela.xml: bloque superior con margen 5dp, ciclo alineado a la derecha y spinner debajo.
        Column(Modifier.fillMaxWidth().padding(5.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Color(0xFF7A9989), fontSize = 24.sp, modifier = Modifier.size(20.dp)
                    .clickable(enabled = year > 2002) { onYearChange(year - 1) })
                Text("Ciclo $year", color = Color(0xFF7A9989), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
            Box(Modifier.fillMaxWidth()) {
                Text(year.toString(), color = Color.Black, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().clickable { menuOpen = true }.padding(10.dp))
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    years.forEach { y -> DropdownMenuItem(text = { Text(y.toString()) }, onClick = {
                        menuOpen = false; onYearChange(y)
                    }) }
                }
            }
        }
        when {
            error != null -> Column(Modifier.padding(16.dp)) {
                Text("No se pudo cargar el año $year: $error")
                Button(onClick = { retry++ }) { Text("Reintentar") }
            }
            celebrations == null -> CircularProgressIndicator(Modifier.padding(24.dp))
            celebrations!!.isEmpty() -> Text("No hay celebraciones para $year", Modifier.padding(16.dp))
            else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                items(celebrations!!, key = { it.date.toString() }) { item ->
                    Card(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 2.dp)) {
                        Column(Modifier.fillMaxWidth().background(Color.White).padding(10.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(formatDate(item.date), color = Color(0xFF7A9989), fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Image(painterResource(R.drawable.calendar), null, Modifier.size(16.dp))
                            }
                            Text(item.description, color = Color(0xFF424242), fontSize = 12.sp,
                                modifier = Modifier.padding(top = 10.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun loadCalendar(base: String, year: Int): List<Celebration> {
    val url = URL("${base.trimEnd('/')}/liturgical_celebration/all/es/$year")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 12000
        readTimeout = 12000
        setRequestProperty("Accept", "application/json")
    }
    try {
        if (connection.responseCode !in 200..299) throw IllegalStateException("HTTP ${connection.responseCode}")
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        if (json.has("status")) throw IllegalStateException(ApiMessages.fromServer(
            json.optString("message"), "Error del servidor"))
        return json.keys().asSequence().filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .map { date ->
                val entries = json.getJSONArray(date)
                Celebration(date, (0 until entries.length()).joinToString("\n") { "• ${entries.optString(it)}" })
            }.sortedBy { it.date }.toList()
    } finally {
        connection.disconnect()
    }
}

private fun formatDate(date: String): String {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
        if (parsed == null) date else SimpleDateFormat("EEEE d MMMM yyyy", Locale("es")).format(parsed)
    } catch (_: Exception) { date }
}
