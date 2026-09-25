package com.chayzay.catequesisapp.settings

import android.content.Context
import android.media.MediaPlayer
import com.chayzay.catequesisapp.R

object GameFeedback {
    fun play(context: Context, correct: Boolean) {
        playResource(context, if (correct) R.raw.aplauso_corto else R.raw.risa)
    }
    fun timeout(context: Context) { playResource(context, R.raw.pitar) }
    /** Los juegos antiguos usaban un aplauso largo al finalizar una ronda ganada. */
    fun finish(context: Context, success: Boolean) {
        playResource(context, if (success) R.raw.aplauso else R.raw.risa)
    }
    private fun playResource(context: Context, resource: Int) {
        if (!AppPreferences(context).sound) return
        try {
            val player = MediaPlayer.create(context.applicationContext, resource) ?: return
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { media, _, _ -> media.release(); true }
            player.start()
        } catch (_: Exception) { /* El juego sigue aunque el audio del equipo falle. */ }
    }
}
