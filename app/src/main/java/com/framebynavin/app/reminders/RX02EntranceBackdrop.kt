package com.framebynavin.app.reminders

import android.app.KeyguardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.framebynavin.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RX02: render-only BACKGROUND for the existing RX01 double-tap entrance.
 * No pointer handling, audio, alarm scheduling, notification permissions,
 * service ownership, completion, response or MAIN-phase rendering.
 *
 * All user gallery assets remain private on-device. When Android is locked,
 * return the original neutral fluid backdrop; never render personal gallery
 * media on top of the keyguard (Creator Alerts can open over lock screen).
 */
@Composable
internal fun RX02EntranceBackdrop(
    domain: RX01ReminderDomain,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
    overrideMedia: RX02Media? = null,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var locked by remember { mutableStateOf(true) }
    DisposableEffect(owner, context) {
        val manager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val refresh = { locked = manager?.isKeyguardLocked != false }
        refresh()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) refresh()
            if (event == Lifecycle.Event.ON_STOP) locked = true
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val media = overrideMedia ?: remember(domain) { RX02EntranceMediaStore.selected(context, domain) }
    if (locked || media == null) {
        RX01FluidReminderBackdrop(domain, RX01SurfacePhase.ENTRANCE,
            motionEnabled, lowPower, modifier)
        return
    }

    Box(modifier.fillMaxSize().testTag("rx02-custom-entrance-" + domain.name.lowercase())) {
        when (media.kind) {
            RX02MediaKind.IMAGE -> RX02StillImage(media.file, Modifier.fillMaxSize())
            RX02MediaKind.ANIMATED -> {
                if (Build.VERSION.SDK_INT >= 28 && motionEnabled && !lowPower) {
                    RX02AnimatedImage(media.file, Modifier.fillMaxSize())
                } else RX02StillImage(media.file, Modifier.fillMaxSize())
            }
            RX02MediaKind.VIDEO -> {
                // Never spin up a decoder when power saver/reduced motion is active.
                if (motionEnabled && !lowPower) {
                    RX02MutedVideo(media, Modifier.fillMaxSize())
                } else {
                    if (media.poster != null)
                        RX02StillImage(media.poster, Modifier.fillMaxSize())
                    else RX01FluidReminderBackdrop(domain, RX01SurfacePhase.ENTRANCE,
                        false, true, Modifier.fillMaxSize())
                }
            }
        }
        // Light custom content must not wash out the original white/gold
        // microphone, alarm glyph, domain label and double-tap prompt.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .45f),
            .42f to Color.Black.copy(alpha = .24f),
            1f to Color.Black.copy(alpha = .67f),
        )))
    }
}

@Composable
private fun RX02StillImage(file: File, modifier: Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                var sample = 1
                while (bounds.outWidth / sample > 1440 ||
                    bounds.outHeight / sample > 2560) sample *= 2
                BitmapFactory.decodeFile(file.absolutePath,
                    BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(Color(0xFF12101A)))
    }
}

@Composable
private fun RX02AnimatedImage(file: File, modifier: Modifier) {
    if (Build.VERSION.SDK_INT < 28) {
        RX02StillImage(file, modifier)
        return
    }
    val drawable by produceState<Drawable?>(null, file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(file)) { decoder, info, _ ->
                    val larger = maxOf(info.size.width, info.size.height)
                    decoder.setTargetSampleSize((larger / 1440).coerceAtLeast(1))
                }
            }.getOrNull()
        }
    }
    if (drawable == null) {
        RX02StillImage(file, modifier)
        return
    }
    DisposableEffect(drawable) {
        (drawable as? AnimatedImageDrawable)?.start()
        onDispose { (drawable as? AnimatedImageDrawable)?.stop() }
    }
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageDrawable(drawable)
            }
        },
        update = { it.setImageDrawable(drawable) },
        modifier = modifier,
    )
}

@Composable
private fun RX02MutedVideo(media: RX02Media, modifier: Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var playerError by remember(media.file.absolutePath) { mutableStateOf(false) }
    val player = remember(media.file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build()
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(media.file)))
            prepare()
        }
    }
    DisposableEffect(player, owner) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { playerError = true }
        }
        val lifecycle = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player.play()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> player.pause()
                else -> Unit
            }
        }
        player.addListener(listener)
        owner.lifecycle.addObserver(lifecycle)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) player.play()
        onDispose {
            owner.lifecycle.removeObserver(lifecycle)
            player.removeListener(listener)
            player.release()
        }
    }
    if (playerError) {
        if (media.poster != null) RX02StillImage(media.poster, modifier)
        else Box(modifier.background(Color(0xFF12101A)))
    } else {
        AndroidView(
            factory = {
                (LayoutInflater.from(it).inflate(R.layout.rx02_entrance_video_texture, null)
                    as PlayerView).apply { this.player = player }
            },
            update = { it.player = player },
            modifier = modifier,
        )
    }
}
