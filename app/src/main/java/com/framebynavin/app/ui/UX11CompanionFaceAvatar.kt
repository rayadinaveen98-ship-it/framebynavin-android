package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.ProjectorIvory
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * UX11-B face ONLY header avatar. These per-guide normalized windows were
 * reviewed against the approved original transparent welcome WebP masters:
 * Spark/Funny 192x192, Mira 256x256, Cute 192x192.
 *
 * Do not use these crop values for the separate full-body guide poses.
 * No face is regenerated, distorted, mirrored, or downloaded.
 */
internal data class UX11FaceWindow(
    val left: Float,
    val top: Float,
    val extent: Float,
)

internal object UX11CompanionFacePolicy {
    fun crop(identity: V144GuideIdentity): UX11FaceWindow = when (identity) {
        V144GuideIdentity.FUNNY,
        V144GuideIdentity.FRAME,
        V144GuideIdentity.NAVI ->
            UX11FaceWindow(left = 58f / 192f, top = 2f / 192f, extent = 80f / 192f)
        V144GuideIdentity.MIRA ->
            UX11FaceWindow(left = 74f / 256f, top = 4f / 256f, extent = 116f / 256f)
        V144GuideIdentity.CUTE ->
            UX11FaceWindow(left = 36f / 192f, top = 4f / 192f, extent = 120f / 192f)
    }

    fun sourceWindow(identity: V144GuideIdentity, width: Int, height: Int): Pair<IntOffset, IntSize> {
        require(width > 0 && height > 0)
        val crop = crop(identity)
        val edge = min(width, height)
        val side = (edge * crop.extent).roundToInt().coerceIn(1, edge)
        val x = (width * crop.left).roundToInt().coerceIn(0, width - side)
        val y = (height * crop.top).roundToInt().coerceIn(0, height - side)
        return IntOffset(x, y) to IntSize(side, side)
    }
}

/**
 * Drop-in icon art inside the EXISTING 48dp clickable Home overflow Surface.
 * The parent owns onClick, menu state, alert badge and accessibility.
 */
@Composable
internal fun UX11CompanionFaceAvatar(
    identity: V144GuideIdentity,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val drawable = v144GuideDrawable(identity, CinePulseState.WAVE)
    val image = remember(drawable) {
        runCatching {
            ImageBitmap.imageResource(context.resources, drawable)
        }.getOrNull()
    }
    if (image != null) {
        Canvas(
            modifier = modifier.fillMaxSize().padding(2.dp)
                .clip(CircleShape)
                .testTag("ux11-companion-face-" + identity.name.lowercase()),
        ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val (offset, sourceSize) =
                UX11CompanionFacePolicy.sourceWindow(identity, image.width, image.height)
            drawImage(
                image = image,
                srcOffset = offset,
                srcSize = sourceSize,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(
                    size.width.roundToInt().coerceAtLeast(1),
                    size.height.roundToInt().coerceAtLeast(1),
                ),
                filterQuality = FilterQuality.High,
            )
        }
    } else {
        // Avoid a crash when an older/corrupt install lacks a companion master.
        // The original menu remains fully tappable with a legible fallback.
        Text(
            identity.displayName.take(1),
            modifier = modifier.testTag("ux11-companion-face-fallback"),
            color = ProjectorIvory,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
