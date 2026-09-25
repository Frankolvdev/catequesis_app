package com.chayzay.catequesisapp.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.profile.ProfileSettings

/** Personajes originales, según sexo elegido y número de aciertos o fallos. */
@Composable
fun GameCharacterFeedback(success: Boolean, count: Int = 5, modifier: Modifier = Modifier.size(120.dp),
                          animate: Boolean = true) {
    val male = ProfileSettings.load(LocalContext.current)?.gender == "MALE"
    val index = count.coerceIn(1, 5) - 1
    val scale = remember(success, index) { Animatable(1f) }
    val opacity = remember(success, index) { Animatable(1f) }
    LaunchedEffect(success, index, animate) {
        if (!animate) return@LaunchedEffect
        coroutineScope {
            launch { scale.animateTo(1.1f, tween(2500)); scale.animateTo(1f, tween(2500)) }
            launch { opacity.animateTo(0.3f, tween(2500)); opacity.animateTo(1f, tween(2500)) }
        }
    }
    val pictures = when {
        male && success -> intArrayOf(R.drawable.nino_bien1b, R.drawable.nino_bien2b,
            R.drawable.nino_bien3b, R.drawable.nino_bien4b, R.drawable.nino_bien5b)
        male -> intArrayOf(R.drawable.nino_mal1b, R.drawable.nino_mal2b,
            R.drawable.nino_mal3b, R.drawable.nino_mal4b, R.drawable.nino_mal4b)
        success -> intArrayOf(R.drawable.nina_bien1b, R.drawable.nina_bien2b,
            R.drawable.nina_bien3b, R.drawable.nina_bien4b, R.drawable.nina_bien5b)
        else -> intArrayOf(R.drawable.nina_mal1b, R.drawable.nina_mal2b,
            R.drawable.nina_mal3b, R.drawable.nina_mal4b, R.drawable.nina_mal5b)
    }
    Image(painterResource(pictures[index]),
        contentDescription = if (success) "Respuesta correcta" else "Respuesta incorrecta",
        modifier = if (animate) modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 0.1f)
            scaleX = scale.value; scaleY = scale.value; alpha = opacity.value
        } else modifier)
}

@Composable
fun GameResultDialog(success: Boolean, message: String, onAccept: () -> Unit) {
    AlertDialog(onDismissRequest = { },
        title = { Text(message) },
        text = { GameCharacterFeedback(success) },
        confirmButton = { TextButton(onClick = onAccept) { Text("Aceptar") } })
}
