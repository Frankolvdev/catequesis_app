package com.chayzay.catequesisapp.chat

import com.chayzay.catequesisapp.auth.UserSession
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

/** El perfil original eliminaba las conversaciones del usuario al borrar su cuenta. */
object ChatAccountCleanup {
    fun removeConversations(user: UserSession) {
        val root = FirebaseDatabase.getInstance().getReference("Conversations")
        val snapshot = Tasks.await(root.get(), 15, TimeUnit.SECONDS)
        snapshot.children.filter { it.child("Users").hasChild(user.apiKey) }.forEach { conversation ->
            val key = conversation.key ?: return@forEach
            Tasks.await(root.child(key).removeValue(), 15, TimeUnit.SECONDS)
        }
    }
}
