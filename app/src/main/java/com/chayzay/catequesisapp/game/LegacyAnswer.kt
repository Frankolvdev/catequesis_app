package com.chayzay.catequesisapp.game

import android.graphics.Color as AndroidColor
import android.view.Gravity
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.chayzay.catequesisapp.R

/** TextView de respuesta idéntico al que creaban GameQuizActivity/TestClassFragment. */
@Composable
fun LegacyAnswer(
    text: String,
    state: LegacyAnswerState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = 13f
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setTextColor(AndroidColor.rgb(0x50, 0x50, 0x50))
                isClickable = true
            }
        },
        update = { view ->
            view.text = text
            view.isEnabled = enabled
            view.isClickable = enabled
            when (state) {
                LegacyAnswerState.NORMAL -> {
                    view.setBackgroundResource(R.drawable.response_background_white)
                    view.setTextColor(AndroidColor.rgb(0x50, 0x50, 0x50))
                    view.alpha = 1f
                }
                LegacyAnswerState.SELECTED_CORRECT -> {
                    view.setBackgroundResource(R.drawable.response_background_green)
                    view.setTextColor(AndroidColor.WHITE)
                    view.alpha = 1f
                }
                LegacyAnswerState.CORRECT_REVEALED -> {
                    view.setBackgroundResource(R.drawable.response_background_green)
                    view.setTextColor(AndroidColor.WHITE)
                    view.alpha = 0.3f
                }
                LegacyAnswerState.WRONG_SELECTED -> {
                    view.setBackgroundResource(R.drawable.response_background_red)
                    view.setTextColor(AndroidColor.WHITE)
                    view.alpha = 1f
                }
            }
            view.setOnClickListener { if (enabled) onClick() }
        }
    )
}

enum class LegacyAnswerState { NORMAL, SELECTED_CORRECT, CORRECT_REVEALED, WRONG_SELECTED }

@Composable
fun LegacyQuestion(text: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                textSize = 14f
                setTextColor(AndroidColor.BLACK)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(20.dpToPx(context), 20.dpToPx(context), 20.dpToPx(context), 20.dpToPx(context))
                setBackgroundResource(R.drawable.question_background)
            }
        },
        update = { it.text = text }
    )
}

private fun Int.dpToPx(context: android.content.Context): Int =
    (this * context.resources.displayMetrics.density).toInt()
