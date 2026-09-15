package com.framebynavin.app.ui

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawLayer
import androidx.compose.ui.graphics.drawscope.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import com.framebynavin.app.data.CreatorGuidedTourStep

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

@Composable
internal fun rememberV137GuideLayer(): GraphicsLayer = rememberGraphicsLayer()

/** Capture the sharp screen once, then blur only the normal background rendering while the tour runs. */
internal fun Modifier.v137GuideCaptureAndBlur(layer: GraphicsLayer, active: Boolean): Modifier {
    val capture = drawWithContent {
        layer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(layer)
    }
    return if (active && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) capture.blur(8.dp) else capture
}

/** Re-draw only the highlighted window from the unblurred captured layer. */
@Composable
internal fun V137GuideSharpWindow(
    layer: GraphicsLayer,
    step: CreatorGuidedTourStep,
    modifier: Modifier = Modifier,
) {
    val spot = v137Spot(step)
    Canvas(modifier) {
        val left = size.width * spot.x
        val top = size.height * spot.y
        val right = left + size.width * spot.width
        val bottom = top + size.height * spot.height
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    cornerRadius = CornerRadius(spot.corner, spot.corner),
                )
            )
        }
        clipPath(path) {
            drawLayer(layer)
        }
    }
}
