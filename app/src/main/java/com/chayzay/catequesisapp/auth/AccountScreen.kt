package com.chayzay.catequesisapp.auth

import android.util.Patterns
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (session != null) {
            Text("Sesión iniciada: ${session.displayName}")
            Text(session.email)
            Button(onClick = { editing = !editing; message = null }) { Text("Editar perfil") }
            Button(onClick = { detailsPage = true }) { Text("Foto, datos personales y contactos") }
            if (editing) {
                OutlinedTextField(editEmail, { editEmail = it.trim() }, label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(editFirstName, { editFirstName = it }, label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(editLastName, { editLastName = it }, label = { Text("Apellido") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                TextButton(onClick = { editGender = if (editGender == "MALE") "FEMALE" else "MALE" }) {
                    Text(if (editGender == "MALE") "Masculino ▾" else "Femenino ▾")
                }
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
            Button(onClick = {
                if (progressStore.approvedCourses().isEmpty()) {
                    message = "Aún no tienes aprobado ningún curso. Para aprobarlos, debes completar todos los test sacando 10/10. Puedes intentarlo las veces que quieras. Al aprobarlos la Universidad de Los Hemisferios te enviará un diploma digital a tu email y, si quieres, podrás pedir uno físico."
                } else approvedPage = false
            }) { Text("Mis cursos aprobados") }
            Button(onClick = {
                if (progressStore.approvedCourses().isEmpty()) {
                    message = "No podemos darte un certificado porque aún no tienes aprobado ningún curso. Para aprobarlos, debes completar todos los test sacando 10/10. Puedes intentarlo las veces que quieras. Al aprobarlos la Universidad de Los Hemisferios te emitirá un diploma digital o físico."
                } else approvedPage = true
            }) { Text("Mis certificados") }
            Button(onClick = { confirmReset = true }) { Text("Reiniciar cursos") }
            Button(onClick = { confirmDelete = true }) { Text("Borrar cuenta") }
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
            Text(if (registering) "Registrar usuario" else "Iniciar sesión")
            OutlinedTextField(value = email, onValueChange = { email = it.trim() },
                label = { Text("Correo electrónico") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it },
                label = { Text("Contraseña") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth())
            if (registering) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it },
                    label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lastName, onValueChange = { lastName = it },
                    label = { Text("Apellido") }, modifier = Modifier.fillMaxWidth())
                Text("Sexo")
                TextButton(onClick = { gender = if (gender == "MALE") "FEMALE" else "MALE" }) {
                    Text(if (gender == "MALE") "Masculino ▾" else "Femenino ▾")
                }
            }
            if (loading) CircularProgressIndicator()
            message?.let { Text(it) }
            Button(enabled = !loading, modifier = Modifier.fillMaxWidth(), onClick = {
                message = when {
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Escribe un correo válido"
                    password.isBlank() -> "Escribe tu contraseña"
                    registering && password.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
                    registering && (firstName.isBlank() || lastName.isBlank()) -> "Completa el nombre y el apellido"
                    else -> null
                }
                if (message == null) scope.launch {
                    loading = true
                    var accountCreated = false
                    try {
                        val user = withContext(Dispatchers.IO) {
                            if (registering) {
                                repository.register(email, password, firstName.trim(), lastName.trim(), gender)
                                accountCreated = true
                            }
                            repository.login(email, password)
                        }
                        store.save(user)
                        password = ""
                        onSessionChanged(user)
                    } catch (cause: Exception) {
                        if (accountCreated) {
                            registering = false
                            message = "La cuenta se creó, pero no pudimos iniciar sesión automáticamente. Intenta iniciar sesión."
                        } else message = ApiMessages.fromException(cause,
                            "No fue posible conectar con el servidor")
                    } finally { loading = false }
                }
            }) { Text(if (registering) "Crear cuenta" else "Iniciar sesión") }
            TextButton(onClick = { registering = !registering; message = null }) {
                Text(if (registering) "Ya tengo cuenta" else "Crear una cuenta")
            }
        }
    }
    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false },
        title = { Text("¿Deseas cerrar la sesión?") },
        confirmButton = { TextButton(onClick = {
            store.clear()
            onSessionChanged(null)
            confirmLogout = false
        }) { Text("Cerrar sesión") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } })
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text("Reiniciar cursos") },
        text = { Text("Se resetearán todas las lecciones y cursos que hayas aprobado. (Solo en este dispositivo)\n ¿Deseas continuar?") },
        confirmButton = { TextButton(onClick = {
            try {
                val hadProgress = progressStore.hasLocalProgressFiles()
                progressStore.clearLocalProgress()
                onSynced()
                message = if (hadProgress) "Los cursos fueron reiniciados." else "Los cursos ya fueron reiniciados"
            } catch (cause: Exception) {
                message = ApiMessages.fromException(cause, "No se pudo reiniciar el progreso")
            }
            confirmReset = false
        }) { Text("Reiniciar") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } })
    if (confirmDelete && session != null) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("¿Borrar la cuenta?") },
        text = { Text("Esta acción elimina tu cuenta del servidor. No podrás volver a entrar con ella.") },
        confirmButton = { TextButton(enabled = !loading, onClick = {
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
        }) { Text("Borrar cuenta") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } })
}
