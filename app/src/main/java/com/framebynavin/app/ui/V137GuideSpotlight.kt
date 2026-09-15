package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.framebynavin.app.data.CreatorGuidedTourStep
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

private data class V137Spot(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val corner: Float,
)

private fun v137Spot(step: CreatorGuidedTourStep): V137Spot = when (step) {
    CreatorGuidedTourStep.TODAY -> V137Spot(.045f, .23f, .91f, .38f, 34f)
    CreatorGuidedTourStep.IDEAS -> V137Spot(.78f, .035f, .17f, .13f, 50f)
    CreatorGuidedTourStep.PROJECT -> V137Spot(.05f, .18f, .90f, .30f, 34f)
    CreatorGuidedTourStep.WORKSPACE -> V137Spot(.04f, .22f, .92f, .43f, 34f)
    CreatorGuidedTourStep.INSIGHTS -> V137Spot(.045f, .17f, .91f, .27f, 34f)
    CreatorGuidedTourStep.CONTROL -> V137Spot(.78f, .74f, .17f, .14f, 54f)
}

/** Compatibility token retained for existing guide call sites. v139 intentionally uses no GPU blur. */
@Composable
internal fun rememberV137GuideLayer(): GraphicsLayer = rememberGraphicsLayer()

internal fun Modifier.v137GuideCaptureAndBlur(layer: GraphicsLayer, active: Boolean): Modifier = this

/**
 * Sharp-window guide focus. The highlighted UI is never blurred or covered. The surrounding scrim,
 * outline strength and corner treatment inherit the active visual language.
 */
@Composable
internal fun V137GuideSharpWindow(
    layer: GraphicsLayer,
    step: CreatorGuidedTourStep,
    modifier: Modifier = Modifier,
) {
    val spot = v137Spot(step)
    val profile = VisualExperiencePrefs.profile
    Canvas(modifier) {
        val left = size.width * spot.x
        val top = size.height * spot.y
        val right = left + size.width * spot.width
        val bottom = top + size.height * spot.height
        val scrim = Color.Black.copy(alpha = profile.guideScrimAlpha)
        val themeCorner = profile.cardRadius.value * 2.2f
        val corner = when {
            profile.cardRadius.value <= 4f -> 5f
            else -> minOf(spot.corner, themeCorner)
        }

        drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, top.coerceAtLeast(0f)))
        drawRect(
            scrim,
            topLeft = Offset(0f, bottom.coerceAtMost(size.height)),
            size = Size(size.width, (size.height - bottom).coerceAtLeast(0f)),
        )
        drawRect(
            scrim,
            topLeft = Offset(0f, top),
            size = Size(left.coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
        )
        drawRect(
            scrim,
            topLeft = Offset(right.coerceAtMost(size.width), top),
            size = Size((size.width - right).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
        )

        drawRoundRect(
            color = RecRed.copy(alpha = .82f),
            topLeft = Offset(left, top),
            size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = if (profile.borderWidth.value >= 2f) 2.2f else 1.5f),
        )
    }
}
