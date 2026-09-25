package com.chayzay.catequesisapp.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

/** The original countdown dims its digits over 700 ms in the final ten seconds. */
@Composable
internal fun Modifier.legacyCountdownWarning(seconds: Int): Modifier {
    val opacity by animateFloatAsState(
        targetValue = if (seconds in 1..10) 0.3f else 1f,
        animationSpec = tween(durationMillis = 700),
        label = "Cuenta regresiva"
    )
    return this.alpha(opacity)
}

/** Correct answers fade while their green background is shown for three seconds. */
@Composable
internal fun Modifier.legacyCorrectAnswer(submitted: Boolean, correct: Boolean): Modifier {
    val opacity by animateFloatAsState(
        targetValue = if (submitted && correct) 0.3f else 1f,
        animationSpec = tween(durationMillis = 3000),
        label = "Respuesta correcta"
    )
    return this.alpha(opacity)
}
