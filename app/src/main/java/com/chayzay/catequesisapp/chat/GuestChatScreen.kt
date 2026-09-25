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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
                            AndroidView(factory = { ctx ->
                                android.widget.TextView(ctx).apply {
                                    setTextColor(android.graphics.Color.BLACK)
                                    maxWidth = (200 * resources.displayMetrics.density).toInt()
                                    val pad = (5 * resources.displayMetrics.density).toInt()
                                    setPadding(pad, pad, pad, pad)
                                    setBackgroundResource(R.drawable.bubble_in)
                                }
                            }, update = { it.text = row.text }, modifier = Modifier.padding(bottom = 10.dp))
                        }
                    }
                }
                Text("${draft.length}/500", fontSize = 9.sp, color = Color.DarkGray, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(start = 5.dp, end = 15.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                Row(Modifier.fillMaxWidth().padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    AndroidView(
                        factory = { ctx -> EditText(ctx).apply {
                            hint = "Escribir mensaje"; textSize = 13f
                            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                            filters = arrayOf(android.text.InputFilter.LengthFilter(500))
                        } },
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
                        modifier = Modifier.weight(0.2f).clickable(enabled = draft.isNotBlank()) {
                            try { store.add(contact, draft); history = store.all(); draft = ""; error = "" }
                            catch (cause: Exception) { error = ApiMessages.fromException(cause, "No se pudo guardar") }
                        })
                }
                if (error.isNotBlank()) Text(error, fontSize = 12.sp, color = Color.Red, modifier = Modifier.padding(10.dp))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Text("Conviene que te registres como usuario para que tus preguntas puedan ser contestadas más tarde y guardes las conversaciones con tus amigos.", fontSize = 12.sp, color = Color.Black,
                    modifier = Modifier.fillMaxWidth().padding(10.dp))
                val last = history.groupBy { it.recipient }.values.mapNotNull { it.lastOrNull() }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(last) { row ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 2.dp)
                                .clickable { selected = ChatContact(row.recipient, row.name, row.picture); error = "" },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                LegacyChatImage(row.picture, Modifier.size(48.dp).clip(CircleShape))
                                Column(Modifier.weight(1f).padding(start = 5.dp)) {
                                    Text(row.name, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = Color(0xFF7A9989), modifier = Modifier.padding(bottom = 5.dp))
                                    Text(if (row.text.length < 40) row.text else row.text.take(40) + " ...",
                                        fontSize = 10.sp, color = Color.Black, maxLines = 1, modifier = Modifier.padding(start = 5.dp))
                                    Text(row.time, fontSize = 8.sp, color = Color.Black, textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        modifier = Modifier.fillMaxWidth().padding(start = 5.dp, top = 5.dp))
                                }
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
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.7f).background(Color.White)) {
                Text("Selecciona al catequista", color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().background(profile.accent).padding(10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                when {
                    contacts == null -> Column(Modifier.fillMaxWidth().height(260.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) { CircularProgressIndicator(); Spacer(Modifier.height(10.dp)); Text("Buscando usuarios ...", fontSize = 13.sp) }
                    error.isNotBlank() -> Text(error, color = Color.Red, fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().clickable { contacts = null; error = ""; retry++ }.padding(16.dp))
                    else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                        items(contacts.orEmpty()) { person ->
                            Row(Modifier.fillMaxWidth().padding(5.dp).clickable { selected = person; selecting = false; draft = ""; error = "" },
                                verticalAlignment = Alignment.CenterVertically) {
                                LegacyChatImage(person.picture, Modifier.size(48.dp))
                                Text(person.name, fontSize = 14.sp, color = Color.Black,
                                    modifier = Modifier.padding(start = 5.dp, bottom = 15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
