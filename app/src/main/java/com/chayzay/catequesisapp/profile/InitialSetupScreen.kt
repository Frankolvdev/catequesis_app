package com.chayzay.catequesisapp.profile

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.R
import java.util.Calendar

/** Adaptación Compose de activity_init_config.xml con sus recursos originales. */
@Composable
fun InitialSetupScreen(onContinue: (ProfileSettings) -> Unit) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var year by remember { mutableStateOf(currentYear) }
    var gender by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val age = currentYear - year
    val accent = gender?.let { ProfileSettings(year, it).accent } ?: Color(0xFF037AD8)

    Box(modifier = Modifier.fillMaxSize()) {
        if (expanded) {
            Dialog(onDismissRequest = { expanded = false }) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = Color.White,
                    tonalElevation = 0.dp
                ) {
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
        Image(painterResource(R.drawable.background_splash_screen), contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Image(painterResource(R.drawable.logo), contentDescription = "Catequesis App",
                modifier = Modifier.size(125.dp))
            Text("Configuración inicial", color = Color.White,
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Año de nacimiento", color = Color.White,
                style = MaterialTheme.typography.titleMedium)
            Box {
                Text(year.toString(), modifier = Modifier.fillMaxWidth().background(Color.White)
                    .clickable { expanded = true }.padding(14.dp),
                    color = Color.Black)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("MALE", "FEMALE").forEach { option ->
                    val selected = gender == option
                    Column(modifier = Modifier.weight(1f)
                        .background(if (selected) Color(0xFFF0F0F0) else Color(0xAAFFFFFF))
                        .border(1.dp, if (selected) accent else Color.Transparent)
                        .clickable { gender = option; error = null }.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = when {
                            option == "MALE" && age >= 18 -> R.drawable.man
                            option == "MALE" -> R.drawable.boy
                            age >= 18 -> R.drawable.woman
                            else -> R.drawable.girl
                        }
                        Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(90.dp))
                        Text(if (option == "MALE") "Masculino" else "Femenino", color = Color.Black)
                    }
                }
            }
            if (error != null) Text(error!!, color = Color.White)
            Button(onClick = {
                when {
                    age < 8 -> error = "La edad mínima es de 8 años"
                    gender == null -> error = "Selecciona una opción"
                    else -> onContinue(ProfileSettings(year, gender!!))
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Comenzar", color = Color.White)
            }
        }
    }
}
