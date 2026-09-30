package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import com.framebynavin.app.ui.theme.BacklotSurfaceRaised
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private object V148YouTubeThumbnailCache {
    private const val MAX_ENTRIES = 24
    private val memory = object : LruCache<String, Bitmap>(MAX_ENTRIES) {}

    fun get(videoId: String): Bitmap? = synchronized(memory) { memory.get(videoId) }

    fun put(videoId: String, bitmap: Bitmap) {
        synchronized(memory) { memory.put(videoId, bitmap) }
    }
}

@Composable
internal fun V148YouTubeThumbnail(
    videoId: String,
    rank: Int,
    modifier: Modifier = Modifier,
) {
    val bitmap = produceState<Bitmap?>(
        initialValue = V148YouTubeThumbnailCache.get(videoId),
        key1 = videoId,
    ) {
        if (value == null && videoId.isNotBlank()) {
            value = withContext(Dispatchers.IO) { loadV148YouTubeThumbnail(videoId) }
        }
    }.value

    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(BacklotSurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Video thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = BacklotSecondaryText,
                modifier = Modifier.size(30.dp),
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.TopStart),
            shape = RoundedCornerShape(bottomEnd = 9.dp),
            color = BacklotAccent.copy(alpha = .92f),
        ) {
            Text(
                text = "#$rank",
                color = BacklotPrimaryText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            )
        }
    }
}

private fun loadV148YouTubeThumbnail(videoId: String): Bitmap? {
    V148YouTubeThumbnailCache.get(videoId)?.let { return it }
    var connection: HttpURLConnection? = null
    return runCatching {
        connection = URL("https://i.ytimg.com/vi/$videoId/hqdefault.jpg").openConnection() as HttpURLConnection
        connection?.connectTimeout = 4_000
        connection?.readTimeout = 5_000
        connection?.instanceFollowRedirects = true
        connection?.useCaches = true
        connection?.connect()
        val responseCode = connection?.responseCode ?: return@runCatching null
        if (responseCode !in 200..299) return@runCatching null
        val decoded = connection?.inputStream?.use(BitmapFactory::decodeStream) ?: return@runCatching null
        V148YouTubeThumbnailCache.put(videoId, decoded)
        decoded
    }.getOrNull().also {
        connection?.disconnect()
    }
}
