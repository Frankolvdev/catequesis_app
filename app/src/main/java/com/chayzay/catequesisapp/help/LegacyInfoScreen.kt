package com.chayzay.catequesisapp.help

import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.auth.UserSession
import com.chayzay.catequesisapp.data.*
import com.chayzay.catequesisapp.profile.ProfileSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class LegacyDetail(val title: String, val html: String, val kind: String)

@Composable
fun LegacyInfoScreen(help: Boolean, repository: CourseRepository, user: UserSession? = null,
                     progressStore: ClassProgressStore? = null, syncRepository: ProgressSyncRepository? = null,
                     onUpdated: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = remember { ProfileSettings.load(context) }
    val accent = profile?.accent ?: Color(0xFF037AD8)
    val base = profile?.baseColor ?: Color(0xFF90CAF8)
    var detail by remember(help) { mutableStateOf<LegacyDetail?>(null) }
    var confirmUpdate by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val how = stringResource(R.string.legacy_textFuncAppContent)
    val approved = stringResource(R.string.legacy_textApprovedCourseContent)
    val certificates = stringResource(R.string.legacy_textCertificateInformationContent)
    val purpose = stringResource(R.string.legacy_textContentHowtoApp)
    val credits = stringResource(R.string.legacy_textDesarrolladoPor)
    val privacy = stringResource(R.string.legacy_textContentPolicyPrivacy)

    BackHandler(enabled = detail != null) { detail = null }

    if (detail != null) {
        val item = detail!!
        LegacyDetailPage(item, accent, base) {
            detail = null
        }
        return
    }

    Column(Modifier.fillMaxSize().background(base)) {
        LegacyInfoToolbar(if (help) "Ayuda" else "Información", accent, false) {}
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (help) {
                LegacyCard(R.drawable.hand_touch, "Cómo funciona la app", "") { detail = LegacyDetail("Cómo funciona la app", how, "text") }
                LegacyCard(R.drawable.course_approved, "Cómo aprobar los cursos", "") { detail = LegacyDetail("Cómo aprobar los cursos", approved, "text") }
                LegacyCard(R.drawable.certificate_approved, "Cómo obtener los certificados digitales o físicos", "") { detail = LegacyDetail("Cómo obtener los certificados digitales o físicos", certificates, "text") }
            } else {
                LegacyCard(R.drawable.info, "Versión e información de la app", "Contiene la versión actual de la App y derechos de autor.") {
                    val version = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
                    detail = LegacyDetail("Versión", "Catequesis App, Versión $version<br/><br/>$credits", "version")
                }
                LegacyCard(R.drawable.update_content, "Actualizar contenido", "Actualiza el contenido de la aplicación para estar al día.") { confirmUpdate = true }
                LegacyCard(R.drawable.privacypolicy, "Política de privacidad", "") { detail = LegacyDetail("Política de privacidad", privacy, "privacy") }
                LegacyCard(R.drawable.what_for, "Para qué sirve esta App", "") { detail = LegacyDetail("Para qué sirve esta App", purpose, "text") }
            }
            problem?.let { Text(it, color = Color(0xFF333333), fontSize = 12.sp, modifier = Modifier.padding(10.dp)) }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(12.dp))
        }
    }

    if (confirmUpdate) AlertDialog(onDismissRequest = { confirmUpdate = false },
        text = { Text("¿Desea descargar nuevamente la información?") },
        confirmButton = { TextButton(onClick = {
            confirmUpdate = false
            scope.launch {
                loading = true; problem = null
                try {
                    withContext(Dispatchers.IO) {
                        repository.refreshContent()
                        if (user != null && progressStore != null && syncRepository != null) syncRepository.sync(user, progressStore)
                    }
                    onUpdated()
                } catch (cause: Exception) { problem = ApiMessages.fromException(cause, "No se pudo actualizar el contenido") }
                finally { loading = false }
            }
        }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = { confirmUpdate = false }) { Text("Cancelar") } })
}

@Composable private fun LegacyInfoToolbar(title: String, accent: Color, back: Boolean, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).background(accent).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back) Text("‹", color = Color.White, fontSize = 38.sp, modifier = Modifier.width(44.dp).clickable { onBack() }, maxLines = 1)
        Text(title, color = Color.White, fontSize = 20.sp, maxLines = 1, modifier = Modifier.weight(1f))
    }
}

@Composable private fun LegacyCard(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 2.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icon), null, Modifier.size(48.dp))
            Column(Modifier.weight(1f).padding(start = 5.dp)) {
                Text(title, color = Color(0xFF7A9989), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (subtitle.isNotEmpty()) Text(subtitle, color = Color(0xFF333333), fontSize = 12.sp, modifier = Modifier.padding(start = 5.dp, top = 5.dp))
            }
        }
    }
}

@Composable private fun LegacyDetailPage(item: LegacyDetail, accent: Color, base: Color, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().background(base)) {
        LegacyInfoToolbar(item.title, accent, true, onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.escudo), null, Modifier.fillMaxWidth().height(150.dp))
            AndroidView(factory = { ctx -> TextView(ctx).apply {
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.rgb(51,51,51))
                textSize = if (item.kind == "version") 14f else 13f
                linksClickable = true
                movementMethod = LinkMovementMethod.getInstance()
            } }, update = { it.text = Html.fromHtml(item.html, Html.FROM_HTML_MODE_LEGACY) },
                modifier = Modifier.fillMaxWidth().padding(top = if (item.kind == "text") 5.dp else 20.dp, bottom = if (item.kind == "privacy") 0.dp else 50.dp))
            if (item.kind == "privacy") {
                Button(onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.catequesis.org/policy_privacy/"))) } catch (_: Exception) { }
                }, modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 50.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("Ir a la web de Catequesis App", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
