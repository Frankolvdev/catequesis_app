package com.chayzay.catequesisapp.profile

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.R
import java.util.Calendar
import kotlinx.coroutines.launch

/** Adaptación Compose de activity_init_config.xml con sus recursos originales. */
@Composable
fun InitialSetupScreen(onContinue: suspend (ProfileSettings) -> Boolean) {
    val context = LocalContext.current
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var year by remember { mutableStateOf(currentYear) }
    var gender by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val age = currentYear - year
    val accent = gender?.let { ProfileSettings(year, it).accent } ?: Color(0xFF037AD8)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.background_splash_screen), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.logo), contentDescription = "Catequesis App",
                modifier = Modifier.padding(top = 4.dp).size(120.dp))
            Text("Configuración inicial", color = Color.White, fontSize = 20.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            Text("Año de nacimiento", color = Color.White, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 5.dp))
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 4.dp)) {
                Text(year.toString(), modifier = Modifier.fillMaxWidth().background(Color.White)
                    .clickable { expanded = true }.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = Color.Black, fontSize = 13.sp)
            }
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                listOf("MALE", "FEMALE").forEach { option ->
                    val selected = gender == option
                    Column(modifier = Modifier.weight(1f).fillMaxSize().padding(5.dp)
                        .background(if (selected) Color(0xFFF0F0F0) else Color(0xAAFFFFFF))
                        .clickable { gender = option; error = null },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        val icon = when {
                            option == "MALE" && age >= 18 -> R.drawable.man
                            option == "MALE" -> R.drawable.boy
                            age >= 18 -> R.drawable.woman
                            else -> R.drawable.girl
                        }
                        Image(painterResource(icon), contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxWidth(), contentScale = ContentScale.Fit)
                        Text(if (option == "MALE") "Masculino" else "Femenino", color = Color.Black, fontSize = 13.sp)
                    }
                }
            }
            if (error != null) Text(error!!, color = Color.White, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
            Button(onClick = {
                when {
                    age < 8 -> error = "La edad mínima es de 8 años"
                    gender == null -> error = "Selecciona una opción"
                    !hasInternetConnection(context) -> error = "No hay conexión a internet"
                    else -> {
                        loading = true; error = null
                        scope.launch {
                            val completed = onContinue(ProfileSettings(year, gender!!))
                            if (!completed) { loading = false; error = "No fue posible descargar el contenido, inténtalo nuevamente." }
                        }
                    }
                }
            }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D749B), disabledContainerColor = Color(0xFF4D749B).copy(alpha = 0.5f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth().padding(5.dp)) {
                Text(if (loading) "Descargando contenido…" else "Comenzar", color = Color.White,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (expanded) {
            Dialog(onDismissRequest = { expanded = false }) {
                androidx.compose.material3.Surface(color = Color.White, tonalElevation = 0.dp) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp).background(Color.White)) {
                        items((1940..currentYear).toList().reversed()) { option ->
                            Text(option.toString(), modifier = Modifier.fillMaxWidth()
                                .clickable { year = option; expanded = false; error = null }
                                .padding(horizontal = 24.dp, vertical = 14.dp), color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}


/** InitConfig.java comprobaba NetworkConnection antes de guardar la configuración inicial. */
private fun hasInternetConnection(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
