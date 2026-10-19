package com.framebynavin.app.reminders

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Visual-only press feedback; the original Material button owns all semantics.
 * Crucially this does not wrap, debounce, suspend, delay or replace onClick.
 * Material's normal ripple remains intact. Reduced motion uses 1f.
 */
@Composable
internal fun Modifier.v201PressFeedback(
    interactionSource: MutableInteractionSource,
    motionEnabled: Boolean,
    prominent: Boolean = false,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val target = V201ReminderMotionPolicy.targetScale(pressed, prominent, motionEnabled)
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (motionEnabled) V201ReminderMotionPolicy.PRESS_MILLIS else 0,
            easing = FastOutSlowInEasing,
        ),
        label = "v201-reminder-press-scale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
