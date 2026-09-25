package com.chayzay.catequesisapp.chat

import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.profile.ProfileSettings

/** ChatListFragmentNoUser/ChatFragmentNoUser: apariencia legacy con almacenamiento local del invitado. */
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

    Box(Modifier.fillMaxSize().background(profile.baseColor)) {
        if (selected != null) {
            val contact = selected!!
            Column(Modifier.fillMaxSize().background(Color.White)) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(history.filter { it.recipient == contact.key }) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(row.text, fontSize = 13.sp, color = Color.Black,
                                modifier = Modifier.widthIn(max = 200.dp)
                                    .background(Color(0xFFD9F3C7)).padding(8.dp))
                        }
                    }
                }
                Text("${draft.length}/500", fontSize = 9.sp, color = Color.DarkGray,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Row(Modifier.fillMaxWidth().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    AndroidView(
                        factory = { ctx -> EditText(ctx).apply { hint = "Escribe un mensaje"; textSize = 13f; maxLines = 4 } },
                        update = { view ->
                            if (view.text.toString() != draft) { view.setText(draft); view.setSelection(view.text.length) }
                            view.setOnFocusChangeListener { _, _ -> }
                            view.addTextChangedListener(object : android.text.TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
                                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                                    val value = s?.toString().orEmpty().take(500)
                                    if (value != draft) draft = value
                                }
                                override fun afterTextChanged(s: android.text.Editable?) = Unit
                            })
                        }, modifier = Modifier.weight(1f))
                    Image(painterResource(if (draft.isBlank()) R.drawable.ic_action_send1 else R.drawable.ic_action_send_2), "Enviar",
                        modifier = Modifier.size(42.dp).padding(5.dp).clickable(enabled = draft.isNotBlank()) {
                            try { store.add(contact, draft); history = store.all(); draft = ""; error = "" }
                            catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudo guardar") }
                        })
                }
                if (error.isNotBlank()) Text(error, fontSize = 12.sp, color = Color.Red, modifier = Modifier.padding(10.dp))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Text("Regístrate o inicia sesión para recibir respuestas de los catequistas.", fontSize = 12.sp, color = Color.Black,
                    modifier = Modifier.fillMaxWidth().padding(10.dp))
                val last = history.groupBy { it.recipient }.values.mapNotNull { it.lastOrNull() }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(last) { row ->
                        Row(Modifier.fillMaxWidth().background(Color.White).clickable {
                            selected = ChatContact(row.recipient, row.name, row.picture); error = ""
                        }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = row.picture, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(row.name, fontSize = 12.sp, color = Color(0xFF7A9989))
                                Text(row.text, fontSize = 10.sp, color = Color.DarkGray, maxLines = 1)
                            }
                        }
                    }
                }
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 14.dp).size(56.dp).clip(CircleShape)
                .background(profile.accent).clickable { selecting = true; error = "" }, contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.plus__64), "Nuevo mensaje", Modifier.size(32.dp))
            }
        }
    }

    if (selecting) {
        Dialog(onDismissRequest = { selecting = false }) {
            Column(Modifier.fillMaxWidth().background(Color.White)) {
                Text("Selecciona un catequista", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().background(profile.accent).padding(10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                when {
                    contacts == null -> Column(Modifier.fillMaxWidth().height(260.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) { CircularProgressIndicator(); Spacer(Modifier.height(10.dp)); Text("Buscando catequistas", fontSize = 13.sp) }
                    error.isNotBlank() -> Text(error, color = Color.Red, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().clickable { contacts = null; error = ""; retry++ }.padding(16.dp))
                    else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                        items(contacts.orEmpty()) { person ->
                            Row(Modifier.fillMaxWidth().clickable { selected = person; selecting = false; draft = ""; error = "" }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = person.picture, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(48.dp).clip(CircleShape))
                                Spacer(Modifier.width(10.dp)); Text(person.name, fontSize = 13.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
