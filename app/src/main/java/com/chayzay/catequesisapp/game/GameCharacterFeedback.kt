package com.chayzay.catequesisapp.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chayzay.catequesisapp.R
import com.chayzay.catequesisapp.profile.ProfileSettings

/** Personajes originales, según sexo elegido y número de aciertos o fallos. */
@Composable
fun GameCharacterFeedback(success: Boolean, count: Int = 5, modifier: Modifier = Modifier.size(120.dp)) {
    val male = ProfileSettings.load(LocalContext.current)?.gender == "MALE"
    val index = count.coerceIn(1, 5) - 1
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
        modifier = modifier)
}
