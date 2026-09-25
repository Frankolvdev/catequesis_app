package com.chayzay.catequesisapp.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Original popup: 200 ms pause, 700 ms grow, and 300 ms shrink on dismissal. */
@Composable
fun QuizFeedbackPopup(closing: Boolean, content: @Composable () -> Unit) {
    val scale = remember { Animatable(0f) }
    val slide = remember { Animatable(0.5f) }
    LaunchedEffect(closing) {
        if (closing) {
            coroutineScope {
                launch { scale.animateTo(0f, tween(300)) }
                launch { slide.animateTo(0.1f, tween(200)) }
            }
        } else {
            delay(200)
            coroutineScope {
                launch { scale.animateTo(1f, tween(700)) }
                launch { slide.animateTo(0f, tween(100)) }
            }
        }
    }
    Popup(alignment = Alignment.BottomEnd, offset = IntOffset(-24, -110)) {
        Box(modifier = Modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, if (closing) 0.7f else 0.4f)
            scaleX = scale.value
            scaleY = scale.value
            translationY = slide.value * size.height
        }) { content() }
    }
}
