package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
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

/** Capture the real app once so the guide overlay can render a blurred copy only outside its target. */
@Composable
internal fun rememberV137GuideLayer(): GraphicsLayer = rememberGraphicsLayer()

internal fun Modifier.v137GuideCaptureAndBlur(layer: GraphicsLayer, active: Boolean): Modifier =
    if (!active) this else this.drawWithContent {
        // Keep the live UI sharp underneath while recording an identical frame for the overlay.
        layer.renderEffect = null
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }

/**
 * V146 focus treatment: draw a blurred snapshot over the app, dim it, then punch a completely clear
 * window through both layers. The selected control therefore remains sharp while everything else
 * visibly recedes instead of merely becoming a little darker.
 */
@Composable
internal fun V137GuideSharpWindow(
    layer: GraphicsLayer,
    step: CreatorGuidedTourStep,
    modifier: Modifier = Modifier,
) {
    val spot = v137Spot(step)
    val profile = VisualExperiencePrefs.profile
    Canvas(
        modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val left = size.width * spot.x
        val top = size.height * spot.y
        val right = left + size.width * spot.width
        val bottom = top + size.height * spot.height
        val themeCorner = profile.cardRadius.value * 2.2f
        val corner = when {
            profile.cardRadius.value <= 4f -> 5f
            else -> minOf(spot.corner, themeCorner)
        }

        // Render the recorded UI as a real blurred copy. V20's animated coach adds the second,
        // darker focus layer and speech, so this blur remains intentionally readable underneath.
        layer.renderEffect = BlurEffect(18f, 18f, TileMode.Decal)
        drawLayer(layer)
        layer.renderEffect = null
        drawRect(Color.Black.copy(alpha = maxOf(profile.guideScrimAlpha, .58f)))

        // Clear the blur + dim over the active control.
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(corner, corner),
            blendMode = BlendMode.Clear,
        )

        drawRoundRect(
            color = RecRed.copy(alpha = .90f),
            topLeft = Offset(left, top),
            size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = if (profile.borderWidth.value >= 2f) 3.4f else 2.6f),
        )
    }
}
