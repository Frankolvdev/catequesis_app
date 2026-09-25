package com.chayzay.catequesisapp.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.profile.ProfileSettings

/** ChatListFragmentNoUser/ChatFragmentNoUser: lista pública y notas locales del invitado. */
@Composable
fun GuestChatScreen(profile: ProfileSettings, repository: ChatRepository, onOpenAccount: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { GuestChatStore(context) }
    var history by remember { mutableStateOf(store.all()) }
    var selected by remember { mutableStateOf<ChatContact?>(null) }
    var selecting by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<ChatContact>?>(null) }
    var retry by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    BackHandler(enabled = selected != null || selecting) {
        if (selected != null) selected = null else selecting = false
    }
    LaunchedEffect(selecting, retry) {
        if (selecting && contacts == null) {
            try { contacts = repository.catechistsGuest(profile); error = "" }
            catch (cause: Exception) {
                contacts = emptyList()
                error = ApiMessages.fromException(cause, "No se pudieron cargar los catequistas")
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when {
            selected != null -> {
                val contact = selected!!
                TextButton(onClick = { selected = null }) { Text("‹ Conversaciones") }
                Text(contact.name)
                Text("Como invitado se guardan en tu teléfono. Al iniciar sesión se enviarán al catequista.")
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history.filter { it.recipient == contact.key }) { row ->
                        Text("${row.text}\n${row.time}")
                    }
                }
                OutlinedTextField(draft, { if (it.length <= 500) draft = it },
                    label = { Text("Mensaje local (${draft.length}/500)") },
                    modifier = Modifier.fillMaxWidth())
                Button(enabled = draft.isNotBlank(), onClick = {
                    try {
                        store.add(contact, draft)
                        history = store.all()
                        draft = ""
                        error = "Mensaje guardado en este teléfono."
                    } catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudo guardar") }
                }) { Text("Guardar mensaje") }
            }
            selecting -> {
                TextButton(onClick = { selecting = false }) { Text("‹ Conversaciones") }
                Text("Elegir catequista")
                if (contacts == null) CircularProgressIndicator()
                if (error.isNotBlank()) TextButton(onClick = {
                    contacts = null; error = ""; retry++
                }) { Text("Reintentar") }
                if (contacts?.isEmpty() == true && error.isBlank()) Text("No hay catequistas disponibles.")
                contacts.orEmpty().forEach { person ->
                    Text(person.name, modifier = Modifier.fillMaxWidth().clickable {
                        selected = person; selecting = false; draft = ""; error = ""
                    }.padding(12.dp))
                }
            }
            else -> {
                Text("Mensajes")
                Text("Como invitado puedes guardar mensajes aquí. Para chatear en vivo, inicia sesión.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { selecting = true; error = "" }) { Text("Nuevo mensaje") }
                    Button(onClick = onOpenAccount) { Text("Iniciar sesión") }
                }
                val last = history.groupBy { it.recipient }.values.mapNotNull { it.lastOrNull() }
                if (last.isEmpty()) Text("No hay mensajes locales todavía.")
                last.forEach { row ->
                    Text("${row.name}\n${row.text}", modifier = Modifier.fillMaxWidth().clickable {
                        selected = ChatContact(row.recipient, row.name, row.picture)
                        error = ""
                    }.padding(12.dp))
                }
            }
        }
        if (error.isNotBlank()) Text(error)
    }
}
