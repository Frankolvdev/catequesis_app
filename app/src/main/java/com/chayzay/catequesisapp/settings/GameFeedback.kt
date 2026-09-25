package com.chayzay.catequesisapp.settings

import android.content.Context
import android.media.MediaPlayer
import com.chayzay.catequesisapp.R

object GameFeedback {
    private var activePlayer: MediaPlayer? = null

    /** The original game stops the current sound when its result dialog closes. */
    fun stop() {
        val player = activePlayer ?: return
        activePlayer = null
        runCatching { if (player.isPlaying) player.stop() }
        runCatching { player.release() }
    }

    fun play(context: Context, correct: Boolean) {
        playResource(context, if (correct) R.raw.aplauso_corto else R.raw.risa)
    }
    fun timeout(context: Context) { playResource(context, R.raw.pitar) }
    /** Los juegos antiguos usaban un aplauso largo al finalizar una ronda ganada. */
    fun finish(context: Context, success: Boolean) {
        playResource(context, if (success) R.raw.aplauso else R.raw.risa)
    }
    private fun playResource(context: Context, resource: Int) {
        stop()
        if (!AppPreferences(context).sound) return
        try {
            val player = MediaPlayer.create(context.applicationContext, resource) ?: return
            activePlayer = player
            player.setOnCompletionListener { media ->
                if (activePlayer === media) { activePlayer = null; media.release() }
            }
            player.setOnErrorListener { media, _, _ ->
                if (activePlayer === media) { activePlayer = null; media.release() }
                true
            }
            player.start()
        } catch (_: Exception) { stop() /* El juego sigue aunque el audio del equipo falle. */ }
    }
}
