package com.framebynavin.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.framebynavin.app.ui.theme.FrameTertiary
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

/** Lumen Flow has its own living composition: slow translucent light ribbons behind content. */
@Composable
internal fun V129ThemeBackdrop(modifier: Modifier = Modifier) {
    if (!VisualExperiencePrefs.isLumen) return
    val motion = rememberInfiniteTransition(label = "lumenFlow")
    val drift by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "lumenDrift")
    val breathe by motion.animateFloat(.72f, 1f, infiniteRepeatable(tween(3800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "lumenBreathe")
    Canvas(modifier) {
        drawRect(
            Brush.linearGradient(
                listOf(Color(0xFF070811), Color(0xFF0D1020), Color(0xFF080A12)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        )
        drawCircle(RecRed.copy(alpha=.11f*breathe), size.width*.48f, Offset(size.width*(.18f+.05f*drift), size.height*.18f))
        drawCircle(FrameTertiary.copy(alpha=.10f*breathe), size.width*.56f, Offset(size.width*(.88f-.06f*drift), size.height*.48f))
        drawCircle(MutedGold.copy(alpha=.055f), size.width*.38f, Offset(size.width*.42f, size.height*.88f))
        repeat(3) { index ->
            val pad = size.width * (.06f + index*.08f)
            drawArc(
                color = when(index) { 0 -> RecRed.copy(alpha=.23f); 1 -> FrameTertiary.copy(alpha=.20f); else -> MutedGold.copy(alpha=.13f) },
                startAngle = 202f + drift*36f + index*27f,
                sweepAngle = 102f + index*18f,
                useCenter = false,
                topLeft = Offset(-pad, size.height*(.10f+index*.18f)),
                size = androidx.compose.ui.geometry.Size(size.width+pad*2f, size.height*.64f),
                style = Stroke(width = size.width*(.025f-index*.004f), cap = StrokeCap.Round),
            )
        }
    }
}
