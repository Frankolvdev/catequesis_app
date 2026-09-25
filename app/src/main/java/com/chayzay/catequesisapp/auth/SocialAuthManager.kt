package com.chayzay.catequesisapp.auth

import android.app.Activity
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.chayzay.catequesisapp.BuildConfig
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONObject

/** Equivalente moderno de los accesos GOOGLE/FACEBOOK de LoginActivity legacy. */
object SocialAuthManager {
    private val facebookCallbacks: CallbackManager = CallbackManager.Factory.create()
    private var facebookInstalled = false
    private var facebookSuccess: ((SocialProfile) -> Unit)? = null
    private var facebookFailure: ((String) -> Unit)? = null

    suspend fun google(activity: Activity): SocialProfile {
        check(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) { "Falta configurar GOOGLE_WEB_CLIENT_ID" }
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val result = CredentialManager.create(activity).getCredential(
            activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential = result.credential as? CustomCredential
            ?: error("Google no devolvió una credencial válida")
        check(credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google devolvió un tipo de credencial inesperado"
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        return SocialProfile(
            uid = googleSubject(google.idToken).ifBlank { google.id },
            provider = "GOOGLE",
            email = google.id,
            firstName = google.givenName.orEmpty(),
            lastName = google.familyName.orEmpty(),
            picture = google.profilePictureUri?.toString().orEmpty(),
            displayName = google.displayName.orEmpty()
        )
    }

    private fun googleSubject(idToken: String): String = try {
        val payload = idToken.split(".").getOrNull(1) ?: return ""
        val decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
        JSONObject(String(decoded, Charsets.UTF_8)).optString("sub")
    } catch (_: Exception) { "" }

    fun facebook(activity: Activity, onSuccess: (SocialProfile) -> Unit, onFailure: (String) -> Unit) {
        check(BuildConfig.FACEBOOK_APP_ID.isNotBlank()) { "Falta configurar FACEBOOK_APP_ID" }
        check(BuildConfig.FACEBOOK_CLIENT_TOKEN.isNotBlank()) { "Falta configurar FACEBOOK_CLIENT_TOKEN de Meta" }
        facebookSuccess = onSuccess
        facebookFailure = onFailure
        if (!facebookInstalled) {
            LoginManager.getInstance().registerCallback(facebookCallbacks, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val token = result.accessToken
                    val request = GraphRequest.newMeRequest(token) { json, _ ->
                        if (json == null) { facebookFailure?.invoke("Facebook no devolvió los datos del usuario"); return@newMeRequest }
                        val id = json.optString("id")
                        facebookSuccess?.invoke(SocialProfile(
                            uid = id, provider = "FACEBOOK", email = json.optString("email"),
                            firstName = json.optString("first_name"), lastName = json.optString("last_name"),
                            picture = if (id.isBlank()) "" else "https://graph.facebook.com/$id/picture?type=large",
                            displayName = listOf(json.optString("first_name"), json.optString("last_name")).filter { it.isNotBlank() }.joinToString(" ")
                        ))
                    }
                    request.parameters = android.os.Bundle().apply { putString("fields", "id,first_name,last_name,email") }
                    request.executeAsync()
                }
                override fun onCancel() { facebookFailure?.invoke("Inicio de sesión con Facebook cancelado") }
                override fun onError(error: FacebookException) { facebookFailure?.invoke(error.message ?: "Error al iniciar sesión con Facebook") }
            })
            facebookInstalled = true
        }
        LoginManager.getInstance().logInWithReadPermissions(activity, listOf("public_profile", "email"))
    }

    suspend fun signOut(activity: Activity) {
        try { CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest()) } catch (_: Exception) { }
        try { LoginManager.getInstance().logOut() } catch (_: Exception) { }
    }

    fun facebookLogout() { try { LoginManager.getInstance().logOut() } catch (_: Exception) { } }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean =
        facebookCallbacks.onActivityResult(requestCode, resultCode, data)
}

data class SocialProfile(
    val uid: String, val provider: String, val email: String, val firstName: String,
    val lastName: String, val picture: String, val displayName: String = ""
)
