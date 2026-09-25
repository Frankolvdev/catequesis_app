package com.chayzay.catequesisapp.help

import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.data.ApiMessages
import com.chayzay.catequesisapp.data.CourseRepository
import com.chayzay.catequesisapp.data.ClassProgressStore
import com.chayzay.catequesisapp.data.ProgressSyncRepository
import com.chayzay.catequesisapp.auth.UserSession
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Textos recuperados de los recursos de la aplicación original. */
@Composable
fun LegacyInfoScreen(help: Boolean, repository: CourseRepository, user: UserSession? = null,
                     progressStore: ClassProgressStore? = null, syncRepository: ProgressSyncRepository? = null,
                     onUpdated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember(help) { mutableStateOf<Pair<String, String>?>(null) }
    var confirmUpdate by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val how = stringResource(R.string.legacy_textFuncAppContent)
    val approved = stringResource(R.string.legacy_textApprovedCourseContent)
    val certificates = stringResource(R.string.legacy_textCertificateInformationContent)
    val purpose = stringResource(R.string.legacy_textContentHowtoApp)
    val credits = stringResource(R.string.legacy_textDesarrolladoPor)
    val privacy = stringResource(R.string.legacy_textContentPolicyPrivacy)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (help) "Ayuda" else "Información")
        if (help) {
            Button(modifier = Modifier.fillMaxWidth(), onClick = { selected = "Cómo funciona la app" to how }) { Text("Cómo funciona la app") }
            Button(modifier = Modifier.fillMaxWidth(), onClick = { selected = "Cómo aprobar los cursos" to approved }) { Text("Cómo aprobar los cursos") }
            Button(modifier = Modifier.fillMaxWidth(), onClick = { selected = "Cómo obtener los certificados" to certificates }) { Text("Cómo obtener los certificados") }
        } else {
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                val version = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
                    catch (_: Exception) { "" }
                selected = "Versión e información" to ("Versión $version<br/><br/>" + credits)
            }) { Text("Versión e información de la app") }
            Button(modifier = Modifier.fillMaxWidth(), enabled = !loading,
                onClick = { confirmUpdate = true }) { Text("Actualizar contenido") }
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                selected = "Política de privacidad" to privacy
            }) { Text("Política de privacidad") }
            Button(modifier = Modifier.fillMaxWidth(), onClick = { selected = "Para qué sirve esta App" to purpose }) { Text("Para qué sirve esta App") }
        }
        problem?.let { Text(it) }
        if (loading) CircularProgressIndicator()
    }
    selected?.let { item -> AlertDialog(onDismissRequest = { selected = null },
        title = { Text(item.first) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AndroidView(factory = { ctx -> TextView(ctx).apply {
                    linksClickable = true
                    movementMethod = LinkMovementMethod.getInstance()
                } }, update = { view ->
                    view.text = Html.fromHtml(item.second, Html.FROM_HTML_MODE_LEGACY)
                }, modifier = Modifier.fillMaxWidth())
                if (item.first == "Política de privacidad") {
                    Button(onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.catequesis.org/policy_privacy/"))) }
                        catch (_: Exception) { problem = "No se pudo abrir la política de privacidad" }
                    }) { Text("Ir a la web") }
                }
            }
        }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } }) }
    if (confirmUpdate) AlertDialog(onDismissRequest = { confirmUpdate = false },
        title = { Text("Actualizar contenido") },
        text = { Text("¿Desea descargar nuevamente la información?") },
        confirmButton = { TextButton(onClick = {
            confirmUpdate = false
            scope.launch {
                loading = true
                problem = null
                try {
                    withContext(Dispatchers.IO) {
                        repository.refreshContent()
                        if (user != null && progressStore != null && syncRepository != null) {
                            syncRepository.sync(user, progressStore)
                        }
                    }
                    onUpdated()
                } catch (cause: Exception) {
                    problem = ApiMessages.fromException(cause, "No se pudo actualizar el contenido")
                } finally { loading = false }
            }
        }) { Text("Actualizar") } },
        dismissButton = { TextButton(onClick = { confirmUpdate = false }) { Text("Cancelar") } })
}

