package com.framebynavin.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import com.framebynavin.app.ui.theme.BacklotScrim
import com.framebynavin.app.ui.theme.BacklotScrim
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

/** Capture the live app so the guide overlay can blur only the non-target area. */
@Composable
internal fun rememberV137GuideLayer(): GraphicsLayer = rememberGraphicsLayer()

internal fun Modifier.v137GuideCaptureAndBlur(layer: GraphicsLayer, active: Boolean): Modifier =
    if (!active) this else this.drawWithContent {
        layer.renderEffect = null
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }

/** Strong focus treatment: blurred + dimmed surroundings, completely clear selected target. */
@Composable
internal fun V137GuideSharpWindow(
    layer: GraphicsLayer,
    step: CreatorGuidedTourStep,
    modifier: Modifier = Modifier,
    targetBounds: Rect? = null,
) {
    // Never highlight an arbitrary screen percentage. If the target has not
    // laid out yet, dim the screen without claiming a false highlight.
    val profile = VisualExperiencePrefs.profile
    Canvas(
        modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val bounds = targetBounds?.let { raw ->
            val l = raw.left.coerceIn(0f, size.width)
            val t = raw.top.coerceIn(0f, size.height)
            val r = raw.right.coerceIn(0f, size.width)
            val b = raw.bottom.coerceIn(0f, size.height)
            if (r > l + 4f && b > t + 4f) Rect(l, t, r, b) else null
        }
        val themeCorner = profile.cardRadius.value * 2.2f
        val corner = if (profile.cardRadius.value <= 4f) 5f else minOf(28f, themeCorner)

        layer.renderEffect = BlurEffect(18f, 18f, TileMode.Decal)
        drawLayer(layer)
        layer.renderEffect = null
        drawRect(BacklotScrim.copy(alpha = maxOf(profile.guideScrimAlpha, if (VisualExperiencePrefs.isLight) .48f else .68f)))

        if (bounds != null) {
            val origin = Offset(bounds.left, bounds.top)
            val extent = Size(bounds.width, bounds.height)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = origin,
                size = extent,
                cornerRadius = CornerRadius(corner, corner),
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = RecRed.copy(alpha = .90f),
                topLeft = origin,
                size = extent,
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = if (profile.borderWidth.value >= 2f) 3.4f else 2.6f),
            )
        }
    }
}
