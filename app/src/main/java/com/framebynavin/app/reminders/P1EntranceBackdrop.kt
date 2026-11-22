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
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.framebynavin.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * P1: render-only BACKGROUND for the existing RX01 double-tap entrance.
 * No pointer handling, audio, alarm scheduling, notification permissions,
 * service ownership, completion, response or MAIN-phase rendering.
 *
 * All user gallery assets remain private on-device. When Android is locked,
 * return the original neutral fluid backdrop; never render personal gallery
 * media on top of the keyguard (Creator Alerts can open over lock screen).
 */
@Composable
internal fun P1EntranceBackdrop(
    domain: P1ReminderScope,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
    overrideMedia: P1Media? = null,
    overrideFraming: P1MediaFraming? = null,
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
    val media = overrideMedia ?: remember(domain) { P1EntranceMediaStore.selected(context, domain) }
    if (locked || media == null) {
        P1OriginalBackdrop(motionEnabled, lowPower, modifier)
        return
    }

    val framing = overrideFraming ?: P1EntranceMediaStore.framing(context, domain)
    val mediaLayer = Modifier.fillMaxSize().graphicsLayer {
        scaleX = framing.zoom
        scaleY = framing.zoom
        translationX = framing.horizontal * size.width * (framing.zoom - 1f) * 0.5f
        translationY = framing.vertical * size.height * (framing.zoom - 1f) * 0.5f
    }
    Box(modifier.fillMaxSize().clipToBounds().testTag("p1-custom-entrance-" + domain.name.lowercase())) {
        // Failed image/video decode must show the trustworthy original scene,
        // not a blank or inaccessible alert. No second RX01 foreground layer.
        P1OriginalBackdrop(false, true, Modifier.fillMaxSize())
        when (media.kind) {
            P1MediaKind.IMAGE -> P1StillImage(media.file, mediaLayer)
            P1MediaKind.ANIMATED -> {
                if (Build.VERSION.SDK_INT >= 28 && motionEnabled && !lowPower) {
                    P1AnimatedImage(media.file, mediaLayer)
                } else P1StillImage(media.file, mediaLayer)
            }
            P1MediaKind.VIDEO -> {
                // Never spin up a decoder when power saver/reduced motion is active.
                if (motionEnabled && !lowPower) {
                    P1MutedVideo(media, mediaLayer)
                } else {
                    if (media.poster != null)
                        P1StillImage(media.poster, mediaLayer)
                    else P1OriginalBackdrop(false, true, Modifier.fillMaxSize())
                }
            }
        }
        // Light custom content must not wash out the original white/gold
        // microphone, alarm glyph, domain label and double-tap prompt.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .62f),
            .42f to Color.Black.copy(alpha = .65f),
            1f to Color.Black.copy(alpha = .78f),
        )))
    }
}

/** Neutral fallback behind any media and when a media decoder cannot open it. */
@Composable
private fun P1OriginalBackdrop(motionEnabled: Boolean, lowPower: Boolean, modifier: Modifier) {
    Box(modifier.background(Brush.verticalGradient(
        listOf(Color(0xFF080408), Color(0xFF25070F), Color(0xFF10050A))
    )))
}

@Composable
private fun P1StillImage(file: File, modifier: Modifier) {
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
    }
}

@Composable
private fun P1AnimatedImage(file: File, modifier: Modifier) {
    if (Build.VERSION.SDK_INT < 28) {
        P1StillImage(file, modifier)
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
        P1StillImage(file, modifier)
        return
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(drawable, owner) {
        val animated = drawable as? AnimatedImageDrawable
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> animated?.start()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> animated?.stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            animated?.start()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            animated?.stop()
        }
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

@OptIn(UnstableApi::class)
@Composable
private fun P1MutedVideo(media: P1Media, modifier: Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var playerError by remember(media.file.absolutePath) { mutableStateOf(false) }
    val player = remember(media.file.absolutePath) {
        runCatching {
            ExoPlayer.Builder(context).build().apply {
                volume = 0f
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true).build()
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(media.file)))
                prepare()
            }
        }.getOrNull()
    }
    if (player == null) {
        if (media.poster != null) P1StillImage(media.poster, modifier)
        return
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
        if (media.poster != null) P1StillImage(media.poster, modifier)
    } else {
        AndroidView(
            factory = {
                (LayoutInflater.from(it).inflate(R.layout.p1_entrance_video_texture, null)
                    as PlayerView).apply { this.player = player }
            },
            update = { it.player = player },
            modifier = modifier,
        )
    }
}

/**
 * P1-only overlay for the existing V337 Creator particle entrance.
 * No touch handlers, new navigation, reminder receipts, sound or action callbacks.
 * The V337 one-tap reveal stays unchanged; gallery media never displays while locked.
 */
@Composable
internal fun P1CreatorEntranceUnderlay(
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
    lowPower: Boolean,
    content: @Composable (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val media = remember(context) {
        P1EntranceMediaStore.selected(context, P1ReminderScope.CREATOR)
    }
    var unlocked by remember { mutableStateOf(false) }
    DisposableEffect(owner, context) {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val refresh = { unlocked = keyguard?.isKeyguardLocked == false }
        refresh()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) refresh()
            if (event == Lifecycle.Event.ON_STOP) unlocked = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val custom = unlocked && media != null
    Box(modifier.fillMaxSize()) {
        if (custom) {
            P1EntranceBackdrop(
                domain = P1ReminderScope.CREATOR,
                motionEnabled = motionEnabled,
                lowPower = lowPower,
                modifier = Modifier.fillMaxSize(),
                overrideMedia = media,
            )
        }
        content(custom)
    }
}
