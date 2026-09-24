package com.framebynavin.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.framebynavin.app.R

/**
 * Premium V144 guides use the approved transparent WebP artwork directly.
 * Compose only adds restrained uniform motion/mirroring and always preserves artwork aspect ratio.
 */
@Composable
internal fun V144RasterGuideCharacter(
    guide: V144GuideIdentity,
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val motion = rememberInfiniteTransition(label = "v144RasterGuideMotion")
    val breathe by motion.animateFloat(
        initialValue = .992f,
        targetValue = 1.008f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "v144RasterGuideBreath",
    )
    val sway by motion.animateFloat(
        initialValue = -.55f,
        targetValue = .55f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == CinePulseState.WALK) 900 else 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "v144RasterGuideSway",
    )

    Image(
        painter = painterResource(v144GuideDrawable(guide, state)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer {
            val direction = if (pointRight) 1f else -1f
            scaleX = direction * breathe
            scaleY = breathe
            rotationZ = when (state) {
                CinePulseState.WALK -> sway
                CinePulseState.CELEBRATE, CinePulseState.SUCCESS -> sway * .45f
                else -> 0f
            }
        },
    )
}

@DrawableRes
internal fun v144GuideDrawable(guide: V144GuideIdentity, state: CinePulseState): Int = when (guide) {
    V144GuideIdentity.FUNNY -> when (state) {
        CinePulseState.PRESENT -> R.drawable.guide_funny_present
        CinePulseState.POINT, CinePulseState.WALK, CinePulseState.NOD -> R.drawable.guide_funny_point
        CinePulseState.THINK -> R.drawable.guide_funny_thinking
        CinePulseState.SUCCESS, CinePulseState.CELEBRATE -> R.drawable.guide_funny_celebrate
        CinePulseState.IDLE, CinePulseState.LOOK, CinePulseState.WAVE,
        CinePulseState.LISTEN, CinePulseState.REST -> R.drawable.guide_funny_welcome
    }

    V144GuideIdentity.CUTE -> when (state) {
        // The welcome/listening assets are the stable neutral poses used during setup transitions.
        // Keep the older encourage raster out of first-run rendering until its visual pack is rebuilt.
        CinePulseState.IDLE, CinePulseState.WALK, CinePulseState.POINT,
        CinePulseState.PRESENT, CinePulseState.NOD, CinePulseState.WAVE -> R.drawable.guide_cute_welcome
        CinePulseState.LOOK, CinePulseState.LISTEN, CinePulseState.REST -> R.drawable.guide_cute_listening
        CinePulseState.THINK -> R.drawable.guide_cute_thinking
        CinePulseState.SUCCESS, CinePulseState.CELEBRATE -> R.drawable.guide_cute_celebrate
    }

    V144GuideIdentity.KITTY -> when (state) {
        CinePulseState.PRESENT -> R.drawable.guide_kitty_present
        CinePulseState.POINT, CinePulseState.WALK, CinePulseState.NOD -> R.drawable.guide_kitty_point
        CinePulseState.THINK -> R.drawable.guide_kitty_thinking
        CinePulseState.SUCCESS, CinePulseState.CELEBRATE -> R.drawable.guide_kitty_celebrate
        CinePulseState.IDLE, CinePulseState.LOOK, CinePulseState.WAVE,
        CinePulseState.LISTEN, CinePulseState.REST -> R.drawable.guide_kitty_welcome
    }

    V144GuideIdentity.CUTE_GIRL -> when (state) {
        CinePulseState.PRESENT -> R.drawable.guide_cute_girl_present
        CinePulseState.POINT, CinePulseState.WALK, CinePulseState.NOD -> R.drawable.guide_cute_girl_point
        CinePulseState.THINK -> R.drawable.guide_cute_girl_thinking
        CinePulseState.SUCCESS, CinePulseState.CELEBRATE -> R.drawable.guide_cute_girl_celebrate
        CinePulseState.IDLE, CinePulseState.LOOK, CinePulseState.WAVE,
        CinePulseState.LISTEN, CinePulseState.REST -> R.drawable.guide_cute_girl_welcome
    }

    // Defensive fallback; Frame/Navi are hidden from the current selectable roster.
    V144GuideIdentity.FRAME, V144GuideIdentity.NAVI -> R.drawable.guide_cute_welcome
}
