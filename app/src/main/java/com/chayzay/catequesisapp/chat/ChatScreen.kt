package com.chayzay.catequesisapp.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import android.app.ProgressDialog
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.chayzay.catequesisapp.R
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
import com.chayzay.catequesisapp.data.ApiMessages
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private data class Conversation(val contact: ChatContact, val last: String, val datetime: String = "")
private data class ChatLine(val key: String, val sender: String, val text: String)

/** Conserva Conversations/{clave1+clave2}/Users y Messages/{datetime} del proyecto anterior. */
@Composable
fun ChatScreen(user: UserSession, profile: ProfileSettings, repository: ChatRepository) {
    val root = remember { FirebaseDatabase.getInstance().getReference("Conversations") }
    val context = LocalContext.current
    val guestStore = remember(context) { GuestChatStore(context) }
    val scope = rememberCoroutineScope()
    // El legacy transfería silenciosamente los mensajes de invitado al iniciar sesión;
    // no añadía banners ni controles extra dentro de la lista de conversaciones.
    LaunchedEffect(user.apiKey) {
        if (guestStore.pendingFor(user).isNotEmpty()) {
            try { GuestChatTransfer.sendPending(context, user, repository) }
            catch (cause: Exception) { if (cause is CancellationException) throw cause }
        }
    }
    var conversations by remember(user.apiKey) { mutableStateOf<List<Conversation>>(emptyList()) }
    var selected by remember(user.apiKey) { mutableStateOf<ChatContact?>(null) }
    var contacts by remember(user.apiKey) { mutableStateOf<List<ChatContact>>(emptyList()) }
    var choosing by remember { mutableStateOf(false) }
    var loading by remember(user.apiKey) { mutableStateOf(true) }
    var error by remember(user.apiKey) { mutableStateOf("") }

    BackHandler(enabled = selected != null || choosing) {
        if (selected != null) selected = null else choosing = false
    }

    DisposableEffect(root, user.apiKey) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                conversations = snapshot.children.mapNotNull { chat ->
                    val users = chat.child("Users")
                    if (!users.hasChild(user.apiKey)) return@mapNotNull null
                    val other = users.children.firstOrNull { it.key != user.apiKey } ?: return@mapNotNull null
                    val key = other.key ?: return@mapNotNull null
                    val lastRow = chat.child("Messages").children.lastOrNull()
                    val last = lastRow?.child("message")?.getValue(String::class.java).orEmpty()
                    Conversation(ChatContact(key, other.child("name_user").getValue(String::class.java) ?: "Catequista",
                        other.child("picture").getValue(String::class.java).orEmpty()), last, lastRow?.key.orEmpty())
                }.sortedBy { it.contact.name }
                loading = false
                error = ""
            }
            override fun onCancelled(databaseError: DatabaseError) {
                loading = false
                error = "No se pudieron cargar las conversaciones. Revisa la conexión."
            }
        }
        root.addValueEventListener(listener)
        onDispose { root.removeEventListener(listener) }
    }

    // Una lectura de Realtime Database puede quedar esperando indefinidamente si el
    // Firebase antiguo está inaccesible. El legacy podía dejar el ProgressDialog abierto
    // para siempre; aquí liberamos la UI sin impedir abrir el selector de catequistas.
    LaunchedEffect(root, user.apiKey, loading, choosing) {
        if (loading && !choosing) {
            delay(10_000)
            if (loading && !choosing) {
                loading = false
                error = "El chat en tiempo real no respondió. Puedes intentar de nuevo o elegir un catequista."
            }
        }
    }

    LaunchedEffect(choosing, user.apiKey) {
        if (choosing) {
            loading = true
            try { contacts = repository.catechists(user, profile); error = "" }
            catch (e: Exception) { error = ApiMessages.fromException(e, "No se pudieron cargar los catequistas") }
            finally { loading = false }
        }
    }

    if (selected != null) {
        val contact = selected!!
        ConversationScreen(user, contact, root, repository, onBack = { selected = null })
        return
    }
    Box(Modifier.fillMaxSize().background(profile.baseColor)) {
        Column(Modifier.fillMaxSize()) {
            LegacyChatProgressDialog(show = loading && !choosing, message = "Cargando datos")
            if (error.isNotBlank() && !choosing) Text(error, color = Color.Red, modifier = Modifier.padding(10.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(conversations, key = { it.contact.key }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 2.dp)
                            .clickable { selected = entry.contact },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            LegacyChatImage(entry.contact.picture, Modifier.size(48.dp).clip(CircleShape))
                            Column(Modifier.weight(1f).padding(start = 5.dp)) {
                                Text(entry.contact.name, color = Color(0xFF7A9989), fontSize = 12.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
                                if (entry.last.isNotBlank()) Text(if (entry.last.length < 40) entry.last else entry.last.take(40) + " ...",
                                    maxLines = 1, fontSize = 10.sp, color = Color.Black, modifier = Modifier.padding(start = 5.dp))
                                if (entry.datetime.isNotBlank()) {
                                    val shownDate = entry.datetime.toLongOrNull()?.let { seconds ->
                                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss a", java.util.Locale.getDefault())
                                            .format(java.util.Date(seconds * 1000L))
                                    } ?: entry.datetime
                                    Text(shownDate, fontSize = 8.sp, color = Color.Black,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.fillMaxWidth().padding(start = 5.dp, top = 5.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 14.dp).size(56.dp).clip(CircleShape)
            .background(profile.accent).clickable { choosing = true; error = "" }, contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.plus__64), "Nuevo mensaje", Modifier.size(32.dp))
        }
    }
    if (choosing) {
        Dialog(onDismissRequest = { choosing = false }) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.7f).background(Color.White)) {
                if (loading) {
                    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(); Text("Buscando usuarios ...", fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                } else {
                    Text("Selecciona al catequista", color = Color.White, fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().background(profile.accent).padding(10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    if (error.isNotBlank()) Text(error, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(contacts, key = { it.key }) { person ->
                            Row(Modifier.fillMaxWidth().padding(5.dp).clickable { selected = person; choosing = false; error = "" },
                                verticalAlignment = Alignment.CenterVertically) {
                                LegacyChatImage(person.picture, Modifier.size(48.dp))
                                Text(person.name, fontSize = 14.sp, color = Color.Black, modifier = Modifier.padding(start = 5.dp, bottom = 15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegacyChatProgressDialog(show: Boolean, message: String) {
    val context = LocalContext.current
    DisposableEffect(show, message, context) {
        val dialog = if (show) ProgressDialog(context).apply {
            setMessage(message)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        } else null
        onDispose { dialog?.dismiss() }
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
    val context = LocalContext.current

    DisposableEffect(root, user.apiKey, contact.key) {
        val first = root.child(user.apiKey + contact.key)
        val reverse = root.child(contact.key + user.apiKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Igual que ChatFragment legacy: usa la conversación inversa si ya existe;
                // si no existe ninguna, crea inmediatamente Users para ambos participantes.
                thread = when {
                    snapshot.hasChild(reverse.key!!) -> reverse
                    snapshot.hasChild(first.key!!) -> first
                    else -> first.also { created ->
                        created.child("Users").child(user.apiKey).setValue(
                            mapOf("name_user" to user.displayName, "picture" to user.picture)
                        )
                        created.child("Users").child(contact.key).setValue(
                            mapOf("name_user" to contact.name, "picture" to contact.picture)
                        )
                    }
                }
                loading = false
            }
            override fun onCancelled(databaseError: DatabaseError) {
                loading = false
                error = "No se pudo cargar esta conversación."
            }
        }
        root.addListenerForSingleValueEvent(listener)
        onDispose { thread = null }
    }

    // Firebase no ofrece timeout para este listener. Tras 10 s usamos el mismo token
    // determinista del legacy para que la pantalla no quede bloqueada en “Cargando datos”.
    // El POST message/register_message sigue funcionando y setValue queda en cola si
    // Firebase recupera conexión posteriormente.
    LaunchedEffect(user.apiKey, contact.key, loading) {
        if (loading) {
            delay(10_000)
            if (loading) {
                thread = root.child(user.apiKey + contact.key)
                loading = false
                error = "El chat en tiempo real no respondió. Puedes enviar; Firebase sincronizará cuando vuelva a estar disponible."
            }
        }
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
            override fun onCancelled(databaseError: DatabaseError) {
                error = "No se pudieron cargar los mensajes."
            }
        }
        messages?.addValueEventListener(listener)
        onDispose { messages?.removeEventListener(listener) }
    }

    Column(Modifier.fillMaxSize()) {
        LegacyChatProgressDialog(show = loading, message = "Cargando datos")
        LegacyChatProgressDialog(show = sending, message = "Enviando")
        if (error.isNotBlank()) Text(error, color = Color.Red)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().background(Color.White).padding(horizontal = 8.dp), reverseLayout = true) {
            items(lines.asReversed(), key = { it.key }) { line ->
                val mine = line.sender == user.apiKey
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    AndroidView(factory = { ctx ->
                        android.widget.TextView(ctx).apply {
                            setTextColor(android.graphics.Color.BLACK)
                            maxWidth = (200 * resources.displayMetrics.density).toInt()
                            val pad = (5 * resources.displayMetrics.density).toInt()
                            setPadding(pad, pad, pad, pad)
                        }
                    }, update = { view ->
                        view.text = line.text
                        view.setBackgroundResource(if (mine) R.drawable.bubble_in else R.drawable.bubble_out)
                    }, modifier = Modifier.padding(bottom = 10.dp))
                }
            }
        }
        Text("${draft.length}/500", fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(start = 5.dp, end = 15.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Row(Modifier.fillMaxWidth().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            AndroidView(
                factory = { ctx ->
                    android.widget.EditText(ctx).apply {
                        hint = "Escribir mensaje"
                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                        inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                        filters = arrayOf(android.text.InputFilter.LengthFilter(500))
                        addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                val value = s?.toString().orEmpty()
                                if (draft != value) draft = value
                            }
                            override fun afterTextChanged(s: android.text.Editable?) = Unit
                        })
                    }
                },
                update = { view ->
                    if (view.text.toString() != draft) {
                        view.setText(draft)
                        view.setSelection(view.text.length)
                    }
                },
                modifier = Modifier.weight(0.8f)
            )
            Image(painterResource(if (draft.isNotBlank()) R.drawable.ic_action_send_2 else R.drawable.ic_action_send1),
                contentDescription = "Enviar", modifier = Modifier.weight(0.2f).size(48.dp)
                    .clickable(enabled = !sending && !loading && draft.isNotBlank() && thread != null) {
                    val message = draft.trim()
                    val destination = thread ?: return@clickable
                    sending = true
                    error = ""
                    scope.launch {
                        try {
                            val datetime = repository.send(user, contact.key, message)
                            // ChatFragment/Message legacy: tras aceptar PHP, escribe directamente
                            // en Conversations/{token}/Messages/{datetime} y limpia el editor.
                            destination.child("Messages").child(datetime).setValue(
                                mapOf("user_send" to user.apiKey, "message" to message, "read" to false)
                            )
                            draft = ""
                            Toast.makeText(context, "Mensaje enviado", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { error = ApiMessages.fromException(e, "No se pudo enviar el mensaje") }
                        finally { sending = false }
                    }
                })
        }
    }
}
