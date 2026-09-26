package com.chayzay.catequesisapp.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Equivalente ligero al Glide del legacy, sin añadir una dependencia nueva. */
@Composable
internal fun LegacyChatImage(url: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        bitmap = if (url.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                val safeUrl = url.trim().replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
                val connection = (URL(safeUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                }
                try { connection.inputStream.use(BitmapFactory::decodeStream) } finally { connection.disconnect() }
            }.getOrNull()
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(loaded.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        // Glide legacy no declaraba placeholder: el ImageView queda vacío hasta cargar.
        Spacer(modifier = modifier)
    }
}
