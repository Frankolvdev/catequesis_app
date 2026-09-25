package com.chayzay.catequesisapp.auth

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.ApiMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Calendar

@Composable
fun ProfileDetailsScreen(user: UserSession, store: UserSessionStore, apiBaseUrl: String,
                         onUserChanged: (UserSession) -> Unit, onBirthYearChanged: (Int) -> Unit,
                         onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(apiBaseUrl) { ProfileDetailsRepository(apiBaseUrl) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadedPersonal by remember(user.id) { mutableStateOf(false) }
    var loadedSocial by remember(user.id) { mutableStateOf(false) }
    var countries by remember { mutableStateOf<List<ProfileOption>>(emptyList()) }
    var civilStatuses by remember { mutableStateOf<List<ProfileOption>>(emptyList()) }
    var countryMenu by remember { mutableStateOf(false) }
    var civilMenu by remember { mutableStateOf(false) }
    var country by remember(user.id) { mutableStateOf(0) }
    var civil by remember(user.id) { mutableStateOf(0) }
    var city by remember(user.id) { mutableStateOf("") }
    var address by remember(user.id) { mutableStateOf("") }
    var phone by remember(user.id) { mutableStateOf("") }
    var birthDate by remember(user.id) { mutableStateOf("") }
    var contacts by remember(user.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    val choosePhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        selectedPhoto = it
    }

    LaunchedEffect(user.id, tab) {
        error = null
        if (tab == 1 && !loadedPersonal) {
            busy = true
            try {
                val result = withContext(Dispatchers.IO) {
                    Triple(repository.countries(), repository.civilStatuses(), repository.personal(user))
                }
                countries = result.first
                civilStatuses = result.second
                country = result.third?.countryId ?: countries.firstOrNull()?.id ?: 0
                civil = result.third?.civilStatusId ?: civilStatuses.firstOrNull()?.id ?: 0
                city = result.third?.city.orEmpty()
                address = result.third?.address.orEmpty()
                phone = result.third?.phone.orEmpty()
                val prefs = context.getSharedPreferences("com.chayzay.catequesisapp_preferences", 0)
                val saved = prefs.getString("pref_key_birthdate", "").orEmpty()
                birthDate = result.third?.birthDate?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                    ?: saved.takeIf { it.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) }
                    ?.let { it.substring(6) + "-" + it.substring(3, 5) + "-" + it.substring(0, 2) }.orEmpty()
                loadedPersonal = true
            } catch (cause: Exception) {
                if (cause is CancellationException) throw cause
                error = ApiMessages.fromException(cause, "No se pudieron cargar tus datos")
            }
            finally { busy = false }
        }
        if (tab == 2 && !loadedSocial) {
            busy = true
            try {
                contacts = withContext(Dispatchers.IO) { repository.social(user) }
                loadedSocial = true
            } catch (cause: Exception) {
                if (cause is CancellationException) throw cause
                error = ApiMessages.fromException(cause, "No se pudieron cargar tus contactos")
            }
            finally { busy = false }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF037AD8)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_baseline_arrow_back_ios_24), contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(40.dp).padding(8.dp).clickable { onBack() })
            Text("Datos del usuario", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF8A65E4)), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Usuario", "Datos", "Contactos").forEachIndexed { index, title ->
                TextButton(onClick = { tab = index; error = null }, modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (busy) CircularProgressIndicator()
        error?.let { Text(it) }
        when (tab) {
            0 -> {
                Box(contentAlignment = Alignment.BottomStart, modifier = Modifier.padding(top = 10.dp)) {
                    Image(painterResource(R.drawable.background_splash_screen), null,
                        Modifier.size(120.dp).clip(CircleShape))
                    Image(painterResource(R.drawable.camera), "Cambiar foto", Modifier.size(32.dp).clip(CircleShape))
                }
                Text(if (selectedPhoto == null) "Pulsa la cámara para cambiar tu foto" else "Foto seleccionada",
                    fontSize = 13.sp)
                Button(onClick = { choosePhoto.launch("image/*") }) { Text("Elegir foto", fontSize = 13.sp) }
                Button(enabled = !busy && selectedPhoto != null, onClick = {
                    val chosen = selectedPhoto ?: return@Button
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            val updated = withContext(Dispatchers.IO) {
                                repository.savePicture(user, encodePhoto(context, chosen))
                            }
                            store.save(updated)
                            onUserChanged(updated)
                            selectedPhoto = null
                            error = "Foto actualizada"
                        } catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudo subir la foto") }
                        finally { busy = false }
                    }
                }) { Text("Guardar foto") }
            }
            1 -> if (loadedPersonal) {
                Text("Datos personales")
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
                    .border(1.dp, Color(0xFFACACAC), RoundedCornerShape(5.dp)).clickable { countryMenu = true }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(countries.firstOrNull { it.id == country }?.label ?: "Seleccionar país", modifier = Modifier.weight(1f))
                    Image(painterResource(R.drawable.arrow_down), contentDescription = "Abrir", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = countryMenu, onDismissRequest = { countryMenu = false }) {
                    countries.forEach { option -> DropdownMenuItem(text = { Text(option.label) },
                        onClick = { country = option.id; countryMenu = false }) }
                }
                OutlinedTextField(city, { city = it }, label = { Text("Ciudad") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)
                    .border(1.dp, Color(0xFFACACAC), RoundedCornerShape(5.dp)).clickable { civilMenu = true }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(civilStatuses.firstOrNull { it.id == civil }?.label ?: "Seleccionar estado civil", modifier = Modifier.weight(1f))
                    Image(painterResource(R.drawable.arrow_down), contentDescription = "Abrir", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = civilMenu, onDismissRequest = { civilMenu = false }) {
                    civilStatuses.forEach { option -> DropdownMenuItem(text = { Text(option.label) },
                        onClick = { civil = option.id; civilMenu = false }) }
                }
                OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                Text("Nacimiento: ${birthDate.takeIf { it.isNotBlank() }?.let(::isoToLegacyDate) ?: "Seleccionar fecha"}")
                Button(onClick = {
                    val now = Calendar.getInstance()
                    val parts = birthDate.split("-").mapNotNull { it.toIntOrNull() }
                    DatePickerDialog(context, { _, year, month, day ->
                        birthDate = "%04d-%02d-%02d".format(year, month + 1, day)
                    }, parts.getOrNull(0) ?: now.get(Calendar.YEAR) - 18,
                        (parts.getOrNull(1) ?: 1) - 1, parts.getOrNull(2) ?: 1).show()
                }) { Text("Elegir fecha") }
                Button(enabled = !busy, onClick = {
                    val partsDate = birthDate.split("-")
                    val year = partsDate.getOrNull(0)?.toIntOrNull()
                    val month = partsDate.getOrNull(1)?.toIntOrNull()
                    val day = partsDate.getOrNull(2)?.toIntOrNull()
                    if (year == null || month !in 1..12 || day !in 1..31) {
                        error = "Ingresa una fecha válida"
                        return@Button
                    }
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            withContext(Dispatchers.IO) {
                                repository.savePersonal(user, PersonalData(country, city.trim(), address.trim(),
                                    civil, phone.trim(), birthDate))
                            }
                            val parts = birthDate.split("-")
                            onBirthYearChanged(year)
                            context.getSharedPreferences("com.chayzay.catequesisapp_preferences", 0).edit()
                                .putString("pref_key_birthdate", "${parts[2].toInt()}/${parts[1].toInt()}/${parts[0]}").apply()
                            error = "Datos personales actualizados"
                        } catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudieron guardar los datos") }
                        finally { busy = false }
                    }
                }) { Text("Guardar datos") }
            }
            else -> if (loadedSocial) {
                Text("Contactos sociales")
                listOf("FACEBOOK", "GOOGLE+", "TWITTER", "INSTAGRAM").forEach { provider ->
                    OutlinedTextField(contacts[provider].orEmpty(),
                        { value -> contacts = contacts + (provider to value) },
                        label = { Text(provider) }, modifier = Modifier.fillMaxWidth())
                }
                Button(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        try {
                            withContext(Dispatchers.IO) { repository.saveSocial(user, contacts) }
                            error = "Contactos actualizados"
                        } catch (cause: Exception) { error = ApiMessages.fromException(cause,
                            "No se pudieron guardar todos los contactos; inténtalo de nuevo") }
                        finally { busy = false }
                    }
                }) { Text("Guardar contactos") }
            }
        }
        }
    }
}

private fun encodePhoto(context: android.content.Context, uri: Uri): String {
    val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val probe = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("No se pudo leer la foto")
    probe.use { BitmapFactory.decodeStream(it, null, size) }
    if (size.outWidth <= 0 || size.outHeight <= 0) throw IllegalStateException("La foto no es válida")
    var sample = 1
    while (size.outWidth / sample > 1024 || size.outHeight / sample > 1024) sample *= 2
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: throw IllegalStateException("No se pudo abrir la foto")
    val out = ByteArrayOutputStream()
    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)) throw IllegalStateException("No se pudo preparar la foto")
    bitmap.recycle()
    if (out.size() > 2_000_000) throw IllegalStateException("Elige una foto más pequeña")
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

private fun isoToLegacyDate(value: String): String {
    val parts = value.split("-")
    if (parts.size != 3) return value
    val year = parts[0].toIntOrNull() ?: return value
    val month = parts[1].toIntOrNull() ?: return value
    val day = parts[2].toIntOrNull() ?: return value
    return "$day/$month/$year"
}
