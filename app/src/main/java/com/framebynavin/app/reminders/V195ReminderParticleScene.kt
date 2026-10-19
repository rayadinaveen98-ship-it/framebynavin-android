package com.framebynavin.app.reminders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotSecondaryAccent
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos

/** Decorative: the alarm/voice service owns playback and deadlines. */
@Composable
internal fun V195ReminderParticleScene(
    kind: ReminderCheckInKind,
    revealed: Boolean,
    lowPower: Boolean,
    onReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glyph = if (kind == ReminderCheckInKind.ALARM) V195ReminderGlyph.CLOCK
        else V195ReminderGlyph.MICROPHONE
    val dots = remember(glyph, lowPower) {
        V195ParticleGeometry.build(
            glyph, if (lowPower) V195ParticleGeometry.LOW_POWER_DOTS
                else V195ParticleGeometry.DEFAULT_DOTS,
        )
    }
    var formed by remember(glyph) { mutableStateOf(false) }
    LaunchedEffect(glyph) {
        formed = false
        kotlinx.coroutines.delay(60)
        formed = true
    }
    val shapeProgress by animateFloatAsState(
        targetValue = if (formed) 1f else 0f,
        animationSpec = tween(durationMillis = 760),
        label = "v195-shape-formation",
    )
    val revealProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "v195-reveal-depth",
    )
    val clock = rememberInfiniteTransition(label = "v195-particle-motion")
    val time by clock.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "v195-particle-time",
    )
    val color = if (kind == ReminderCheckInKind.ALARM) BacklotAccent
        else BacklotSecondaryAccent
    val interactions = remember { MutableInteractionSource() }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Show reminder controls" }
            .clickable(interactionSource = interactions, indication = null, onClick = onReveal),
    ) {
        val symbolRadius = min(size.width * .41f, size.height * .30f)
        val center = Offset(size.width * .50f, size.height * .35f)
        val depth = 1f - .25f * revealProgress
        val dotRadius = min(2.3.dp.toPx(), symbolRadius * .022f)
        val tint = color.copy(alpha = 1f - .89f * revealProgress)
        for (dot in dots) {
            val dx = sin((time * dot.speed + dot.phase).toDouble()).toFloat() * .048f
            val dy = cos((time * (dot.speed * .73f) + dot.phase * 1.31f).toDouble()).toFloat() * .048f
            val x = dot.scatterX + (dot.x + dx - dot.scatterX) * shapeProgress
            val y = dot.scatterY + (dot.y + dy - dot.scatterY) * shapeProgress
            drawCircle(
                color = tint,
                radius = dotRadius,
                center = Offset(center.x + x * symbolRadius * depth,
                    center.y + y * symbolRadius * depth),
            )
        }
    }
}
