package com.chayzay.catequesisapp.chat

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.auth.UserSession
import com.chayzay.catequesisapp.profile.ProfileSettings
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch

private data class Conversation(val contact: ChatContact, val last: String)
private data class ChatLine(val key: String, val sender: String, val text: String)

/** Conserva Conversations/{clave1+clave2}/Users y Messages/{datetime} del proyecto anterior. */
@Composable
fun ChatScreen(user: UserSession, profile: ProfileSettings, repository: ChatRepository) {
    val root = remember { FirebaseDatabase.getInstance().getReference("Conversations") }
    var conversations by remember(user.apiKey) { mutableStateOf<List<Conversation>>(emptyList()) }
    var selected by remember(user.apiKey) { mutableStateOf<ChatContact?>(null) }
    var contacts by remember(user.apiKey) { mutableStateOf<List<ChatContact>>(emptyList()) }
    var choosing by remember { mutableStateOf(false) }
    var loading by remember(user.apiKey) { mutableStateOf(true) }
    var error by remember(user.apiKey) { mutableStateOf("") }

    DisposableEffect(root, user.apiKey) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                conversations = snapshot.children.mapNotNull { chat ->
                    val users = chat.child("Users")
                    if (!users.hasChild(user.apiKey)) return@mapNotNull null
                    val other = users.children.firstOrNull { it.key != user.apiKey } ?: return@mapNotNull null
                    val key = other.key ?: return@mapNotNull null
                    val last = chat.child("Messages").children.lastOrNull()?.child("message")?.getValue(String::class.java).orEmpty()
                    Conversation(ChatContact(key, other.child("name_user").getValue(String::class.java) ?: "Catequista",
                        other.child("picture").getValue(String::class.java).orEmpty()), last)
                }.sortedBy { it.contact.name }
                loading = false
                error = ""
            }
            override fun onCancelled(databaseError: DatabaseError) {
                loading = false
                error = "No se pudieron cargar las conversaciones: ${databaseError.message}"
            }
        }
        root.addValueEventListener(listener)
        onDispose { root.removeEventListener(listener) }
    }

    LaunchedEffect(choosing, user.apiKey) {
        if (choosing) {
            loading = true
            try { contacts = repository.catechists(user, profile); error = "" }
            catch (e: Exception) { error = e.message ?: "No se pudieron cargar los catequistas" }
            finally { loading = false }
        }
    }

    if (selected != null) {
        val contact = selected!!
        ConversationScreen(user, contact, root, repository, { selected = null })
        return
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (choosing) "Catequistas" else "Mensajes")
            Button(onClick = { choosing = !choosing; error = "" }) { Text(if (choosing) "Volver" else "Nuevo chat") }
        }
        if (loading) CircularProgressIndicator(Modifier.padding(12.dp))
        if (error.isNotBlank()) Text(error, color = Color.Red)
        val entries = if (choosing) contacts.map { Conversation(it, "") } else conversations
        if (!loading && entries.isEmpty() && error.isEmpty())
            Text(if (choosing) "No hay catequistas disponibles" else "Todavía no tienes conversaciones",
                modifier = Modifier.padding(top = 20.dp))
        LazyColumn {
            items(entries, key = { it.contact.key }) { entry ->
                Column(Modifier.fillMaxWidth().clickable { selected = entry.contact }.padding(vertical = 14.dp)) {
                    Text(entry.contact.name)
                    if (entry.last.isNotBlank()) Text(entry.last, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ConversationScreen(user: UserSession, contact: ChatContact, root: DatabaseReference,
                               repository: ChatRepository, onBack: () -> Unit) {
    var thread by remember(user.apiKey, contact.key) { mutableStateOf<DatabaseReference?>(null) }
    var lines by remember(user.apiKey, contact.key) { mutableStateOf<List<ChatLine>>(emptyList()) }
    var loading by remember(user.apiKey, contact.key) { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    DisposableEffect(root, user.apiKey, contact.key) {
        val first = root.child(user.apiKey + contact.key)
        val reverse = root.child(contact.key + user.apiKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // La conversación inversa prevalece igual que en la aplicación original.
                thread = if (snapshot.hasChild(reverse.key!!)) reverse else first
                loading = false
            }
            override fun onCancelled(databaseError: DatabaseError) {
                loading = false
                error = databaseError.message
            }
        }
        root.addListenerForSingleValueEvent(listener)
        onDispose { thread = null }
    }
    DisposableEffect(thread) {
        val messages = thread?.child("Messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lines = snapshot.children.mapNotNull { row ->
                    val key = row.key ?: return@mapNotNull null
                    ChatLine(key, row.child("user_send").getValue(String::class.java).orEmpty(),
                        row.child("message").getValue(String::class.java).orEmpty())
                }
            }
            override fun onCancelled(databaseError: DatabaseError) { error = databaseError.message }
        }
        messages?.addValueEventListener(listener)
        onDispose { messages?.removeEventListener(listener) }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onBack) { Text("Volver") }
            Text(contact.name, modifier = Modifier.padding(10.dp))
        }
        if (loading) CircularProgressIndicator()
        if (error.isNotBlank()) Text(error, color = Color.Red)
        LazyColumn(Modifier.weight(1f), reverseLayout = true) {
            items(lines.asReversed(), key = { it.key }) { line ->
                val mine = line.sender == user.apiKey
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Text(line.text, modifier = Modifier.background(if (mine) Color(0xFFE0F1FF) else Color(0xFFF1F1F1))
                        .padding(10.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            OutlinedTextField(value = draft, onValueChange = { draft = it.take(1000) },
                label = { Text("Mensaje") }, modifier = Modifier.weight(1f), maxLines = 4)
            Button(enabled = !sending && !loading && draft.isNotBlank() && thread != null,
                onClick = {
                    val message = draft.trim()
                    val destination = thread ?: return@Button
                    sending = true
                    error = ""
                    scope.launch {
                        try {
                            val datetime = repository.send(user, contact.key, message)
                            // El servidor guarda primero el mensaje; Firebase distribuye su copia en tiempo real.
                            destination.child("Users").child(user.apiKey).setValue(
                                mapOf("name_user" to user.displayName, "picture" to user.picture))
                            destination.child("Users").child(contact.key).setValue(
                                mapOf("name_user" to contact.name, "picture" to contact.picture))
                            destination.child("Messages").child(datetime).setValue(
                                mapOf("user_send" to user.apiKey, "message" to message, "check" to false))
                                .addOnSuccessListener { if (draft == message) draft = "" }
                                .addOnFailureListener { error = "Guardado en el servidor, pero Firebase no pudo mostrarlo: ${it.message}" }
                        } catch (e: Exception) { error = e.message ?: "No se pudo enviar el mensaje" }
                        finally { sending = false }
                    }
                }, modifier = Modifier.padding(start = 6.dp)) { Text("Enviar") }
        }
    }
}
