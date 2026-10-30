package com.framebynavin.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * V217 staged, live-app artwork integration for FRAME / NAVI / CUTE.
 *
 * The three approved transparent illustrations are now visible through the
 * original V144GuideCharacter entry point: existing first-run onboarding,
 * Settings and creator companion call sites all share this renderer.
 *
 * The 9 independently-authored pose packs DO NOT exist yet. Until production
 * art passes its own gate, provide semantic STATE TRANSITIONS with subtle
 * posture changes of the single original illustration rather than misleading
 * users or reviewers with duplicated "pose files".
 *
 * No persistent identity changes; Spark/FUNNY still routes through its original
 * untouched V144RasterGuideCharacter code and original five WebPs.
 */
@Composable
internal fun V217LiveCompanionCharacter(
    guide: V144GuideIdentity,
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    require(guide != V144GuideIdentity.FUNNY) {
        "Original Spark must stay on V144RasterGuideCharacter"
    }
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    // The selected identity is never inferred from state, visual style, or a
    // stored data migration. Preview artwork uses only the historical enum.
    val angle = if (motionEnabled) when (state) {
        CinePulseState.POINT -> if (pointRight) 3f else -3f
        CinePulseState.LOOK, CinePulseState.THINK -> if (pointRight) -3f else 3f
        CinePulseState.LISTEN, CinePulseState.NOD -> if (pointRight) 2f else -2f
        CinePulseState.PRESENT -> if (pointRight) 1.5f else -1.5f
        CinePulseState.SUCCESS, CinePulseState.CELEBRATE -> if (pointRight) -2f else 2f
        CinePulseState.WALK -> if (pointRight) 2.5f else -2.5f
        CinePulseState.IDLE, CinePulseState.WAVE, CinePulseState.REST -> 0f
    } else 0f
    val targetScale = if (motionEnabled) when (state) {
        CinePulseState.SUCCESS, CinePulseState.CELEBRATE -> 1.035f
        CinePulseState.WALK, CinePulseState.PRESENT, CinePulseState.WAVE -> 1.012f
        CinePulseState.REST -> .988f
        else -> 1f
    } else 1f

    // Single short transition on an actual state change, never a permanently
    // running animation. Respect global animator-disabled / reduced motion.
    val rotation = animateFloatAsState(
        targetValue = angle,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "v217StateRotation",
    ).value
    val scale = animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "v217StateScale",
    ).value
    Image(
        painter = painterResource(v217ConceptDrawable(guide)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer {
            rotationZ = rotation
            scaleX = scale * if (pointRight) 1f else -1f
            scaleY = scale
        },
    )
}
