package com.chayzay.catequesisapp.chat

import android.content.Context
import com.chayzay.catequesisapp.auth.UserSession
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** En la app original, los mensajes escritos como invitado se entregaban al iniciar sesión. */
object GuestChatTransfer {
    private val mutex = Mutex()

    suspend fun sendPending(context: Context, user: UserSession, repository: ChatRepository): Int = mutex.withLock {
        val store = GuestChatStore(context.applicationContext)
        val pending = store.pendingFor(user)
        if (pending.isEmpty()) return@withLock 0
        val outbox = PendingRealtimeStore(context.applicationContext)
        val root = FirebaseDatabase.getInstance().getReference("Conversations")
        var sent = 0
        pending.forEach { note ->
            if (note.text.isBlank()) return@forEach
            // Coincide con la preferencia por conversaciones inversas del chat antiguo.
            val conversation = note.conversation.ifBlank {
                conversationFor(root, user.apiKey, note.recipient)
            }
            val datetime = note.serverDatetime.ifBlank {
                repository.send(user, note.recipient, note.text)
            }
            if (note.serverDatetime.isBlank()) store.markAccepted(note.id, datetime, conversation, user)
            outbox.enqueue(user, ChatContact(note.recipient, note.name, note.picture),
                conversation, datetime, note.text)
            store.removeSent(note.id)
            sent++
        }
        outbox.flush(root, user) { /* Los fallos de Firebase quedan en el buzón de reintentos. */ }
        sent
    }

    private suspend fun conversationFor(root: DatabaseReference, sender: String, receiver: String): String =
        suspendCancellableCoroutine { continuation ->
            val reverse = receiver + sender
            root.child(reverse).get().addOnCompleteListener { result ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (result.isSuccessful) {
                    continuation.resume(if (result.result.exists()) reverse else sender + receiver)
                } else continuation.resumeWithException(result.exception ?:
                    IllegalStateException("No se pudo consultar el chat para entregar mensajes pendientes"))
            }
        }
}
