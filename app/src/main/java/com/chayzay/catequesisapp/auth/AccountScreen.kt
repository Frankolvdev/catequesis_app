package com.chayzay.catequesisapp.auth

import android.util.Patterns
import android.app.Activity
import com.chayzay.catequesisapp.profile.ProfileSettings
import androidx.activity.compose.BackHandler
import com.chayzay.catequesisapp.data.ClassProgressStore
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.data.ProgressSyncRepository
import com.chayzay.catequesisapp.chat.ChatAccountCleanup
import com.chayzay.catequesisapp.chat.PendingRealtimeStore
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AccountScreen(session: UserSession?, repository: AuthRepository, store: UserSessionStore,
                  progressStore: ClassProgressStore, syncRepository: ProgressSyncRepository,
                  courseRepository: CourseRepository, apiBaseUrl: String,
                  onSynced: () -> Unit, onBirthYearChanged: (Int) -> Unit,
                  onSessionChanged: (UserSession?) -> Unit) {
    var registering by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    var syncing by remember(session?.id) { mutableStateOf(false) }
    var syncMessage by remember(session?.id) { mutableStateOf<String?>(null) }
    var approvedPage by remember(session?.id) { mutableStateOf<Boolean?>(null) }
    var detailsPage by remember(session?.id) { mutableStateOf(false) }
    var editing by remember(session?.id) { mutableStateOf(false) }
    var editEmail by remember(session?.id) { mutableStateOf(session?.email.orEmpty()) }
    var editFirstName by remember(session?.id) { mutableStateOf(session?.firstName.orEmpty()) }
    var editLastName by remember(session?.id) { mutableStateOf(session?.lastName.orEmpty()) }
    var editGender by remember(session?.id) { mutableStateOf(session?.gender.orEmpty()) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val guestStore = remember(context) { ClassProgressStore(context) }
    val scope = rememberCoroutineScope()
    if (session != null && detailsPage) {
        BackHandler { detailsPage = false }
        ProfileDetailsScreen(session, store, apiBaseUrl, { onSessionChanged(it) },
            onBirthYearChanged) { detailsPage = false }
        return
    }
    if (session != null && approvedPage != null) {
        BackHandler { approvedPage = null }
        ApprovedCoursesScreen(session, progressStore, courseRepository, syncRepository,
            apiBaseUrl, approvedPage!!, onSynced) { approvedPage = null }
        return
    }
    val legacyLoginGreen = Color(0xFF7A9989)
    val screenModifier = if (session == null && !registering)
        Modifier.fillMaxSize().background(legacyLoginGreen).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)
    else Modifier.fillMaxSize().background(Color.White).verticalScroll(rememberScrollState()).padding(10.dp)
    Column(screenModifier, horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (session != null) {
            Text("Perfil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF505050),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            LegacyProfileCard(R.drawable.user_profile, "Datos del usuario",
                "Edita tus datos: correo, nombres, apellidos y más.") { editing = !editing; message = null }
            if (!editing) TextButton(onClick = { detailsPage = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Foto, datos personales y contactos", fontSize = 13.sp)
            }
            if (editing) {
                OutlinedTextField(editEmail, { editEmail = it.trim() }, label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(editFirstName, { editFirstName = it }, label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(editLastName, { editLastName = it }, label = { Text("Apellido") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                LegacyGenderSpinner(editGender, onGender = { editGender = it })
                Button(enabled = !loading, onClick = {
                    message = when {
                        !Patterns.EMAIL_ADDRESS.matcher(editEmail).matches() -> "Escribe un correo válido"
                        editFirstName.isBlank() || editLastName.isBlank() -> "Completa el nombre y el apellido"
                        else -> null
                    }
                    if (message == null) scope.launch {
                        loading = true
                        try {
                            val updated = withContext(Dispatchers.IO) {
                                repository.update(session, editEmail, editFirstName.trim(), editLastName.trim(), editGender)
                            }
                            store.save(updated)
                            onSessionChanged(updated)
                            editEmail = updated.email
                            editFirstName = updated.firstName
                            editLastName = updated.lastName
                            editGender = updated.gender
                            editing = false
                            message = "Datos actualizados"
                        } catch (cause: Exception) {
                            message = ApiMessages.fromException(cause, "No se pudo actualizar el perfil")
                        } finally { loading = false }
                    }
                }) { Text("Guardar cambios") }
            }
            if (loading) CircularProgressIndicator()
            message?.let { Text(it) }
            LegacyProfileCard(R.drawable.course_profile, "Tus cursos aprobados",
                "Visualiza la información de tus cursos aprobados.") {
                if (progressStore.approvedCourses().isEmpty()) {
                    message = "Aún no tienes aprobado ningún curso. Para aprobarlos, debes completar todos los test sacando 10/10. Puedes intentarlo las veces que quieras. Al aprobarlos la Universidad de Los Hemisferios te enviará un diploma digital a tu email y, si quieres, podrás pedir uno físico."
                } else approvedPage = false
            }
            LegacyProfileCard(R.drawable.certificate_profile, "Petición de certificado",
                "Pide tu certificado digital, una vez aprobada toda la carga.") {
                if (progressStore.approvedCourses().isEmpty()) {
                    message = "No podemos darte un certificado porque aún no tienes aprobado ningún curso. Para aprobarlos, debes completar todos los test sacando 10/10. Puedes intentarlo las veces que quieras. Al aprobarlos la Universidad de Los Hemisferios te emitirá un diploma digital o físico."
                } else approvedPage = true
            }
            LegacyProfileCard(R.drawable.reset_course_profile, "Reiniciar cursos",
                "Reiniciar toda la información de los cursos.") { confirmReset = true }
            LegacyProfileCard(R.drawable.delete_user_profile, "Borrar cuenta",
                "Borrar los datos de la cuenta incluyendo los cursos.") { confirmDelete = true }
            Button(enabled = !syncing, onClick = {
                syncing = true
                syncMessage = null
                scope.launch {
                    try {
                        val result = syncRepository.sync(session, progressStore)
                        syncMessage = "Progreso sincronizado. ${result.recovered} registros consultados; ${result.uploaded} enviados."
                        onSynced()
                    } catch (cause: Exception) {
                        syncMessage = ApiMessages.fromException(cause, "No se pudo sincronizar")
                    } finally { syncing = false }
                }
            }) { Text("Sincronizar progreso") }
            val hasGuestProgress = remember(session.id) {
                guestStore.approvedTests().isNotEmpty() || guestStore.approvedCourses().isNotEmpty()
            }
            if (hasGuestProgress) Button(enabled = !syncing, onClick = {
                syncing = true
                syncMessage = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            progressStore.mergeApprovals(guestStore.approvedTests(), guestStore.approvedCourses())
                            syncRepository.sync(session, progressStore)
                        }
                        syncMessage = "Se importó y sincronizó el progreso de invitado con esta cuenta."
                        onSynced()
                    } catch (cause: Exception) {
                        syncMessage = ApiMessages.fromException(cause, "No se pudo importar el progreso")
                    } finally { syncing = false }
                }
            }) { Text("Importar progreso local de invitado") }
            if (syncing) CircularProgressIndicator()
            syncMessage?.let { Text(it) }
            Button(onClick = { confirmLogout = true }) { Text("Cerrar sesión") }
        } else {
            if (!registering) Image(painterResource(R.drawable.logo), contentDescription = null,
                modifier = Modifier.size(148.dp).padding(bottom = 4.dp))
            else Text("Registrar usuario", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF505050), modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            OutlinedTextField(value = email, onValueChange = { email = it.trim() },
                placeholder = { Text("Correo electrónico", fontSize = 13.sp) },
                trailingIcon = { Image(painterResource(R.drawable.user), null, Modifier.size(24.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, shape = RoundedCornerShape(5.dp), colors = legacyAuthFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp))
            OutlinedTextField(value = password, onValueChange = { password = it },
                placeholder = { Text("Contraseña", fontSize = 13.sp) }, singleLine = true,
                trailingIcon = { Image(painterResource(R.drawable.pass), null, Modifier.size(24.dp)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(5.dp), colors = legacyAuthFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp))
            if (registering) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it },
                    placeholder = { Text("Nombre", fontSize = 13.sp) }, trailingIcon = { Image(painterResource(R.drawable.name_icon), null, Modifier.size(24.dp)) }, shape = RoundedCornerShape(5.dp), colors = legacyAuthFieldColors(), modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp))
                OutlinedTextField(value = lastName, onValueChange = { lastName = it },
                    placeholder = { Text("Apellido", fontSize = 13.sp) }, trailingIcon = { Image(painterResource(R.drawable.name_icon), null, Modifier.size(24.dp)) }, shape = RoundedCornerShape(5.dp), colors = legacyAuthFieldColors(), modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp))
                LegacyGenderSpinner(gender, onGender = { gender = it }, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
            if (loading) CircularProgressIndicator()
            message?.let { Text(it) }
            Button(enabled = !loading, onClick = {
                // LoginActivity legacy: correo + contraseña vacíos abre directamente el registro.
                if (!registering && email.isBlank() && password.isBlank()) {
                    registering = true
                    message = null
                    return@Button
                }
                message = when {
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Escribe un correo válido"
                    password.isBlank() -> "Escribe tu contraseña"
                    password.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
                    registering && (firstName.isBlank() || lastName.isBlank()) -> "Completa el nombre y el apellido"
                    else -> null
                }
                if (message == null) scope.launch {
                    loading = true
                    try {
                        if (registering) {
                            // UserRegistration legacy vuelve al login después de crear la cuenta;
                            // no inicia sesión automáticamente.
                            withContext(Dispatchers.IO) {
                                repository.register(email, password, firstName.trim(), lastName.trim(), gender)
                            }
                            registering = false
                            password = ""
                            message = "Cuenta creada. Ya puedes iniciar sesión."
                        } else {
                            val user = withContext(Dispatchers.IO) { repository.login(email, password) }
                            store.save(user)
                            password = ""
                            onSessionChanged(user)
                        }
                    } catch (cause: Exception) {
                        message = ApiMessages.fromException(cause, "No fue posible conectar con el servidor")
                    } finally { loading = false }
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF446353)), shape = androidx.compose.ui.graphics.RectangleShape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(42.dp)) {
                Text(if (registering) "Registrar" else "Iniciar sesión o Registrar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (!registering) {
                Button(enabled = !loading, onClick = {
                    val activity = context as? Activity
                    if (activity == null) { message = "No se pudo abrir Google"; return@Button }
                    scope.launch {
                        loading = true; message = null
                        try {
                            val profile = SocialAuthManager.google(activity)
                            val selectedGender = ProfileSettings.load(context)?.gender ?: "MALE"
                            val user = withContext(Dispatchers.IO) { repository.loginSocial(profile, selectedGender) }
                            store.save(user); onSessionChanged(user)
                        } catch (cause: Exception) {
                            message = ApiMessages.fromException(cause, "No se pudo iniciar sesión con Google")
                        } finally { loading = false }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF505050)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(48.dp)) {
                    Image(painterResource(R.drawable.icon_google), null, Modifier.size(width = 45.dp, height = 48.dp));
                    Text("Iniciar sesión con Google", fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
                Button(enabled = !loading, onClick = {
                    val activity = context as? Activity
                    if (activity == null) { message = "No se pudo abrir Facebook"; return@Button }
                    loading = true; message = null
                    try {
                        SocialAuthManager.facebook(activity, { profile ->
                            scope.launch {
                                try {
                                    val selectedGender = ProfileSettings.load(context)?.gender ?: "MALE"
                                    val user = withContext(Dispatchers.IO) { repository.loginSocial(profile, selectedGender) }
                                    store.save(user); onSessionChanged(user)
                                } catch (cause: Exception) {
                                    SocialAuthManager.facebookLogout()
                                    message = ApiMessages.fromException(cause, "No se pudo iniciar sesión con Facebook")
                                } finally { loading = false }
                            }
                        }, { error -> loading = false; message = error })
                    } catch (cause: Exception) { loading = false; message = cause.message ?: "No se pudo iniciar sesión con Facebook" }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475A96), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(48.dp)) {
                    Image(painterResource(R.drawable.icon_facebook), null, Modifier.size(width = 45.dp, height = 48.dp));
                    Text("Iniciar sesión con Facebook", fontSize = 13.sp, modifier = Modifier.weight(1f))
                }
            }
            TextButton(onClick = { registering = !registering; message = null }) {
                Text(if (registering) "Ya tengo cuenta" else "Crear una cuenta")
            }
        }
    }
    if (confirmLogout) com.chayzay.catequesisapp.ui.LegacyConfirmDialog(
        message = "¿Deseas cerrar la sesión?",
        confirmText = "Cerrar sesión",
        onConfirm = {
            try { progressStore.clearLocalProgress() } catch (_: Exception) { }
            store.clear()
            onSessionChanged(null)
            scope.launch { (context as? Activity)?.let { SocialAuthManager.signOut(it) } }
            confirmLogout = false
        },
        onDismiss = { confirmLogout = false }
    )
    if (confirmReset) com.chayzay.catequesisapp.ui.LegacyConfirmDialog(
        title = "Reiniciar cursos",
        message = "Se resetearán todas las lecciones y cursos que hayas aprobado. (Solo en este dispositivo)\n ¿Deseas continuar?",
        confirmText = "Reiniciar",
        onConfirm = {
            try {
                val hadProgress = progressStore.hasLocalProgressFiles()
                progressStore.clearLocalProgress()
                onSynced()
                message = if (hadProgress) "Los cursos fueron reiniciados." else "Los cursos ya fueron reiniciados"
            } catch (cause: Exception) {
                message = ApiMessages.fromException(cause, "No se pudo reiniciar el progreso")
            }
            confirmReset = false
        },
        onDismiss = { confirmReset = false }
    )
    if (confirmDelete && session != null) com.chayzay.catequesisapp.ui.LegacyConfirmDialog(
        title = "¿Borrar la cuenta?",
        message = "Esta acción elimina tu cuenta del servidor. No podrás volver a entrar con ella.",
        confirmText = "Borrar cuenta",
        confirmEnabled = !loading,
        onConfirm = {
            confirmDelete = false
            scope.launch {
                loading = true
                try {
                    withContext(Dispatchers.IO) { repository.delete(session) }
                    store.clear()
                    onSessionChanged(null)
                    try { progressStore.clearLocalProgress() } catch (_: Exception) { }
                    PendingRealtimeStore(context).clearForUser(session)
                    try {
                        withContext(Dispatchers.IO) { ChatAccountCleanup.removeConversations(session) }
                        message = "Cuenta eliminada"
                    } catch (_: Exception) {
                        message = "Cuenta eliminada; no se pudieron limpiar las conversaciones del chat. Contacta al administrador."
                    }
                } catch (cause: Exception) {
                    message = ApiMessages.fromException(cause, "No se pudo borrar la cuenta")
                } finally { loading = false }
            }
        },
        onDismiss = { confirmDelete = false }
    )
}


@Composable
private fun legacyAuthFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
    focusedBorderColor = Color(0xFFBBBBBB), unfocusedBorderColor = Color(0xFFBBBBBB),
    focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
    focusedPlaceholderColor = Color(0xFFB7B7B7), unfocusedPlaceholderColor = Color(0xFFB7B7B7)
)

@Composable
private fun LegacyGenderSpinner(value: String, onGender: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()
            .border(1.dp, Color(0xFFACACAC), RoundedCornerShape(5.dp))
            .background(Color.White, RoundedCornerShape(5.dp))
            .clickable { open = true }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (value == "FEMALE") "Femenino" else "Masculino", color = Color.Black, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Image(painterResource(R.drawable.arrow_down), contentDescription = "Abrir", modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Masculino", fontSize = 13.sp) }, onClick = { onGender("MALE"); open = false })
            DropdownMenuItem(text = { Text("Femenino", fontSize = 13.sp) }, onClick = { onGender("FEMALE"); open = false })
        }
    }
}

@Composable
private fun LegacyProfileCard(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(48.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(title, color = Color(0xFF7A9989), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFF424242), fontSize = 12.sp)
            }
        }
    }
}
