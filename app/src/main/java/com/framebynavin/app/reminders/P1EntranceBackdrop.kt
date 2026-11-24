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
 * Shared private gallery renderer retained under P10D entrance, main and preview.
 * No pointer handling, audio, alarm scheduling, notification permissions,
 * service ownership, completion or response; no retired visual fallback.
 *
 * All user gallery assets stay private on-device. Personal and challenge media
 * remain hidden over the keyguard. Creator media appears while locked ONLY if
 * the user explicitly opted in; no account or response behavior is changed.
 */
@Composable
internal fun P1EntranceBackdrop(
    domain: P1ReminderScope,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
    overrideMedia: P1Media? = null,
    overrideFraming: P1MediaFraming? = null,
    fallbackAct: P10ReminderAct = P10ReminderAct.ENTRANCE,
    allowCreatorMediaOverKeyguard: Boolean = false,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val keyguard = remember(context) {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }
    // Read the actual initial state before drawing: avoid a one-frame privacy
    // fallback flash when an unlocked reminder is first composed.
    var locked by remember(context) {
        mutableStateOf(keyguard?.isKeyguardLocked != false)
    }
    DisposableEffect(owner, context, keyguard) {
        val refresh = { locked = keyguard?.isKeyguardLocked != false }
        refresh()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) refresh()
            if (event == Lifecycle.Event.ON_STOP) locked = true
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val media = overrideMedia ?: remember(domain) { P1EntranceMediaStore.selected(context, domain) }
    val canShowGallery = P10CreatorLockScreenMediaPolicy.allowed(
        scope = domain, locked = locked, ownerOptedIn = allowCreatorMediaOverKeyguard,
    )
    if (!canShowGallery || media == null) {
        P1FamilyFallback(domain, fallbackAct, motionEnabled, lowPower, modifier)
        return
    }
    // Previously imported photos/GIFs may not have a poster yet. Generate a
    // small on-device preview in IO, never blocking speech or alarm delivery.
    val posterFile by produceState<File?>(media.poster, media.file.absolutePath) {
        // Do not carry a previous gallery selection's poster into a new media item.
        value = media.poster
        if (media.poster == null && media.kind != P1MediaKind.VIDEO) {
            value = withContext(Dispatchers.IO) {
                P1EntranceMediaStore.ensureStillPoster(media)
            }
        }
    }

    val framing = overrideFraming ?: P1EntranceMediaStore.framing(context, domain)
    val mediaLayer = Modifier.fillMaxSize().graphicsLayer {
        scaleX = framing.zoom
        scaleY = framing.zoom
        translationX = framing.horizontal * size.width * (framing.zoom - 1f) * 0.5f
        translationY = framing.vertical * size.height * (framing.zoom - 1f) * 0.5f
    }
    Box(modifier.fillMaxSize().clipToBounds().testTag("p1-custom-entrance-" + domain.name.lowercase())) {
        // Broken private media always falls back to a new P10 family scene.
        // Do not reintroduce the retired Director or Premiere visual.
        P1FamilyFallback(domain, fallbackAct, false, true, Modifier.fillMaxSize())
        when (media.kind) {
            P1MediaKind.IMAGE ->
                P1StillImage(media.file, mediaLayer, previewFile = posterFile)
            P1MediaKind.ANIMATED -> {
                if (Build.VERSION.SDK_INT >= 28 && motionEnabled && !lowPower) {
                    P1AnimatedImage(media.file, mediaLayer, previewFile = posterFile)
                } else P1StillImage(media.file, mediaLayer, previewFile = posterFile)
            }
            P1MediaKind.VIDEO -> {
                // Never spin up a decoder when power saver/reduced motion is active.
                if (motionEnabled && !lowPower) {
                    P1MutedVideo(
                        media, mediaLayer,
                        allowOverKeyguard = domain == P1ReminderScope.CREATOR &&
                            allowCreatorMediaOverKeyguard,
                    )
                } else {
                    if (posterFile != null)
                        P1StillImage(requireNotNull(posterFile), mediaLayer, posterFile)
                    else P1FamilyFallback(domain, fallbackAct, false, true, Modifier.fillMaxSize())
                }
            }
        }
        // Ensure text contrast without hiding the user's gallery media.
        // P10 applies a second lightweight family-colored vignette.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .36f),
            .45f to Color.Black.copy(alpha = .12f),
            1f to Color.Black.copy(alpha = .58f),
        )))
    }
}

/** P10 family fallback, preserving media/privacy gates and renderer ownership. */
@Composable
private fun P1FamilyFallback(
    domain: P1ReminderScope,
    act: P10ReminderAct,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val family = remember(context, domain) { P10ReminderFamilyStore.selected(context, domain) }
    P10LivingReminderBackdrop(
        family = family, act = act,
        animationsAllowed = motionEnabled, batterySaver = lowPower, lowRam = false,
        modifier = modifier,
    )
}

@Composable
private fun P1StillImage(
    file: File, modifier: Modifier, previewFile: File? = null,
) {
    // Import produces a <=720px JPEG. Its bounded synchronous decode prevents
    // the unrelated family background from flashing for 1-2 seconds while
    // high-resolution decoding proceeds off the UI thread.
    val instantPoster = remember(previewFile?.absolutePath, previewFile?.lastModified()) {
        previewFile?.takeIf { it.isFile && it.length() in 1..1_500_000L }
            ?.let { runCatching { BitmapFactory.decodeFile(it.absolutePath) }.getOrNull() }
    }
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
    val displayed = bitmap ?: instantPoster
    if (displayed != null) {
        Image(
            bitmap = displayed.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Composable
private fun P1AnimatedImage(file: File, modifier: Modifier, previewFile: File? = null) {
    if (Build.VERSION.SDK_INT < 28) {
        P1StillImage(file, modifier, previewFile)
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
        // Show the small imported still immediately, without an extra decoder
        // for the original animation while ImageDecoder prepares its drawable.
        P1StillImage(previewFile ?: file, modifier, previewFile)
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
private fun P1MutedVideo(
    media: P1Media,
    modifier: Modifier,
    allowOverKeyguard: Boolean = false,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val keyguard = remember(context) {
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }
    var playerError by remember(media.file.absolutePath) { mutableStateOf(false) }
    var firstFrameReady by remember(media.file.absolutePath) { mutableStateOf(false) }
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
        if (media.poster != null)
            P1StillImage(media.poster, modifier, previewFile = media.poster)
        return
    }
    DisposableEffect(player, owner, allowOverKeyguard) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrameReady = true }
            override fun onPlayerError(error: PlaybackException) {
                playerError = true
                firstFrameReady = false
            }
        }
        fun mayPlay(): Boolean {
            val active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            val overKeyguard = allowOverKeyguard &&
                keyguard?.isKeyguardLocked == true &&
                owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            return active || overKeyguard
        }
        val lifecycle = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME ->
                    if (mayPlay()) player.play()
                Lifecycle.Event.ON_PAUSE ->
                    if (!mayPlay()) player.pause()
                Lifecycle.Event.ON_STOP -> player.pause()
                else -> Unit
            }
        }
        player.addListener(listener)
        owner.lifecycle.addObserver(lifecycle)
        if (mayPlay()) player.play()
        onDispose {
            owner.lifecycle.removeObserver(lifecycle)
            player.removeListener(listener)
            player.release()
        }
    }
    Box(modifier) {
        if (!playerError) {
            // Keep the texture attached/visible so Media3 can actually render
            // its first frame. Cover its initial black surface with our poster.
            AndroidView(
                factory = {
                    (LayoutInflater.from(it).inflate(R.layout.p1_entrance_video_texture, null)
                        as PlayerView).apply { this.player = player }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Already captured during import. Poster stays on TOP of the prepared
        // video surface until the first real frame is rendered (or on error).
        // No overlapping audio—the selected video is always mute.
        if ((!firstFrameReady || playerError) && media.poster != null) {
            P1StillImage(media.poster, Modifier.fillMaxSize(), previewFile = media.poster)
        }
    }
}
