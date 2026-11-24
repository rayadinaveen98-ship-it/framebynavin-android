package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The three founder-approved premium icon families use only Compose vector drawing.
 * The microphone is decorative: no microphone/camera access, sampled audio,
 * notifications, media player, completion receipts or alarm logic.
 *
 * This renderer is reused for Creator, Personal and valid 21-day presentations.
 * In Settings its preview is static; in an actual reminder it respects Android
 * animator-off, battery saver and low-RAM restrictions.
 */
@Composable
internal fun P10PremiumReminderIcon(
    kind: ReminderCheckInKind,
    family: P10ReminderFamily,
    style: P10ReminderIconStyle,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val context = LocalContext.current
    val reducedAnimations = remember { !ValueAnimator.areAnimatorsEnabled() }
    val batterySaver = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val tuning = P10ReminderVisualPolicy.tuning(
        animationsAllowed = animated && !reducedAnimations,
        batterySaver = batterySaver,
        lowRam = lowRam,
        act = P10ReminderAct.ENTRANCE,
    )
    val t = P10SceneTime(tuning)
    val p = family.pigments()
    Canvas(modifier) {
        val u = min(size.width, size.height)
        if (u <= 0f) return@Canvas
        val c = Offset(size.width / 2f, size.height / 2f)
        val motion = if (tuning.animated) tuning.amplitude else 0f
        when (style) {
            P10ReminderIconStyle.LUMINOUS_GLASS -> drawP10Glass(c, u, p, t, motion)
            P10ReminderIconStyle.CINEMATIC_EMBLEM -> drawP10Emblem(c, u, p, t, motion)
            P10ReminderIconStyle.LIVING_ENERGY -> drawP10Energy(c, u, p, t, motion)
        }
        val glyphColor = when (style) {
            P10ReminderIconStyle.LUMINOUS_GLASS -> Color(0xFFFFF2E4)
            P10ReminderIconStyle.CINEMATIC_EMBLEM -> Color(0xFFFFD092)
            P10ReminderIconStyle.LIVING_ENERGY -> Color(0xFFFFF2F8)
        }
        when (kind) {
            ReminderCheckInKind.VOICE -> drawP10MicrophoneGlyph(c, u, glyphColor, style)
            ReminderCheckInKind.ALARM -> drawP10BellGlyph(c, u, glyphColor, style)
        }
    }
}

/** Polished transparent optical orb, restrained light and no metallic ring. */
private fun DrawScope.drawP10Glass(
    c: Offset, u: Float, p: P10ScenePigments, t: Float, motion: Float,
) {
    val breathe = 1f + .017f * motion * sin(t * 1.7f)
    val r = u * .36f * breathe
    drawCircle(
        Brush.radialGradient(
            listOf(p.light.copy(alpha = .24f), p.secondary.copy(alpha = .09f), Color.Transparent),
            center = c, radius = u * .49f,
        ), radius = u * .49f, center = c,
    )
    drawCircle(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = .36f), p.warm.copy(alpha = .20f),
                p.depth.copy(alpha = .85f), Color(0xFF100E1B).copy(alpha = .88f)),
            start = Offset(c.x - r, c.y - r),
            end = Offset(c.x + r, c.y + r),
        ), radius = r, center = c,
    )
    drawCircle(Color.White.copy(alpha = .52f), r, c,
        style = Stroke(width = u * .008f))
    drawCircle(p.light.copy(alpha = .30f), r * .92f, c,
        style = Stroke(width = u * .004f))
    drawArc(Color.White.copy(alpha = .76f), 208f, 100f, false,
        Offset(c.x - r * .84f, c.y - r * .84f), Size(r * 1.68f, r * 1.68f),
        style = Stroke(width = u * .017f, cap = StrokeCap.Round))
    drawArc(p.warm.copy(alpha = .58f), 12f, 80f, false,
        Offset(c.x - r * .90f, c.y - r * .90f), Size(r * 1.8f, r * 1.8f),
        style = Stroke(width = u * .006f, cap = StrokeCap.Round))
    val gleam = Offset(c.x - u * .13f, c.y - u * .14f)
    drawCircle(Brush.radialGradient(
        listOf(Color.White.copy(alpha = .14f), Color.Transparent),
        center = gleam, radius = u * .20f,
    ), radius = u * .20f, center = gleam)
}

/** Physical, cinema-hardware-inspired medallion with restrained brass detail. */
private fun DrawScope.drawP10Emblem(
    c: Offset, u: Float, p: P10ScenePigments, t: Float, motion: Float,
) {
    val gleam = .06f * motion * sin(t * 1.1f)
    drawCircle(Brush.radialGradient(
        listOf(p.warm.copy(alpha = .23f), p.dark.copy(alpha = 0f)),
        center = c, radius = u * .50f,
    ), radius = u * .50f, center = c)
    drawCircle(Brush.linearGradient(
        listOf(Color(0xFFFAE2AE), Color(0xFFB67E40), Color(0xFF563118), Color(0xFFE1AD6A)),
        start = Offset(c.x - u * .40f, c.y - u * .40f),
        end = Offset(c.x + u * .43f, c.y + u * .36f),
    ), radius = u * .37f, center = c)
    drawCircle(Color(0xFF321A17), radius = u * .317f, center = c)
    drawCircle(Brush.radialGradient(
        listOf(p.depth.copy(alpha = .85f), Color(0xFF201017), Color(0xFF100C12)),
        center = Offset(c.x + u * gleam, c.y - u * .10f),
        radius = u * .36f,
    ), radius = u * .304f, center = c)
    drawCircle(Color(0xFFF4C887).copy(alpha = .84f), radius = u * .302f,
        center = c, style = Stroke(width = u * .007f))
    drawCircle(Color(0xFF64371C), radius = u * .353f,
        center = c, style = Stroke(width = u * .008f))
    repeat(12) { index ->
        val angle = 2.0 * PI * index / 12.0
        val pos = Offset(
            c.x + u * .341f * cos(angle).toFloat(),
            c.y + u * .341f * sin(angle).toFloat(),
        )
        drawCircle(Color(0xFF3B1D13), radius = u * .016f, center = pos)
        drawCircle(Color(0xFFFFD7A0).copy(alpha = .40f), radius = u * .006f,
            center = pos.copy(x = pos.x - u * .003f, y = pos.y - u * .003f))
    }
    drawArc(Color(0xFFFFEDC6).copy(alpha = .52f), 205f, 92f, false,
        Offset(c.x - u * .365f, c.y - u * .365f),
        Size(u * .73f, u * .73f),
        style = Stroke(width = u * .008f, cap = StrokeCap.Round))
}

/** Signature default: two independent moving arcs + live radial resonance. */
private fun DrawScope.drawP10Energy(
    c: Offset, u: Float, p: P10ScenePigments, t: Float, motion: Float,
) {
    val breath = .018f * motion * sin(t * 1.85f)
    drawCircle(Brush.radialGradient(
        listOf(p.secondary.copy(alpha = .24f), p.light.copy(alpha = .10f), Color.Transparent),
        center = c, radius = u * .50f,
    ), radius = u * .50f, center = c)
    drawCircle(Brush.radialGradient(
        listOf(p.depth.copy(alpha = .87f), p.dark.copy(alpha = .94f), Color(0xFF120B20)),
        center = c, radius = u * .37f,
    ), radius = u * (.315f + breath), center = c)
    drawCircle(p.secondary.copy(alpha = .52f), radius = u * (.325f + breath),
        center = c, style = Stroke(width = u * .010f))
    val phase = t * 18f * motion
    repeat(3) { i ->
        val r = u * (.35f + i * .035f + breath * .5f)
        drawArc(
            color = when (i) {
                0 -> p.light
                1 -> p.secondary
                else -> p.warm
            }.copy(alpha = if (i == 0) .94f else .70f),
            startAngle = 15f + i * 84f + phase * if (i == 1) -1f else 1f,
            sweepAngle = 106f + 20f * motion * sin(t + i),
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(width = u * (if (i == 0) .020f else .011f),
                cap = StrokeCap.Round),
        )
    }
    repeat(6) { i ->
        val angle = (2.0 * PI * i / 6.0).toFloat() + t * .14f * motion
        val r = u * (.375f + .009f * motion * sin(t * 1.6f + i))
        val point = Offset(c.x + r * cos(angle), c.y + r * sin(angle))
        drawCircle(p.warm.copy(alpha = .38f), radius = u * .016f, center = point)
        drawCircle(p.warm.copy(alpha = .9f), radius = u * .0048f, center = point)
    }
    drawCircle(p.light.copy(alpha = .15f), radius = u * (.44f + breath),
        center = c, style = Stroke(width = u * .004f))
}

private fun DrawScope.drawP10MicrophoneGlyph(
    c: Offset, u: Float, color: Color, style: P10ReminderIconStyle,
) {
    val stroke = u * (if (style == P10ReminderIconStyle.CINEMATIC_EMBLEM) .014f else .012f)
    drawRoundRect(
        color = color, topLeft = Offset(c.x - u * .055f, c.y - u * .195f),
        size = Size(u * .11f, u * .245f),
        cornerRadius = CornerRadius(u * .055f),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawArc(color, 0f, 180f, false,
        topLeft = Offset(c.x - u * .107f, c.y - u * .085f),
        size = Size(u * .214f, u * .225f),
        style = Stroke(width = stroke, cap = StrokeCap.Round))
    drawLine(color, Offset(c.x, c.y + u * .139f),
        Offset(c.x, c.y + u * .226f), stroke, cap = StrokeCap.Round)
    drawLine(color, Offset(c.x - u * .067f, c.y + u * .226f),
        Offset(c.x + u * .067f, c.y + u * .226f), stroke, cap = StrokeCap.Round)
    val waves = color.copy(alpha = .60f)
    drawArc(waves, 128f, 103f, false,
        Offset(c.x - u * .225f, c.y - u * .124f),
        Size(u * .20f, u * .305f),
        style = Stroke(width = stroke * .65f, cap = StrokeCap.Round))
    drawArc(waves, -51f, 103f, false,
        Offset(c.x + u * .025f, c.y - u * .124f),
        Size(u * .20f, u * .305f),
        style = Stroke(width = stroke * .65f, cap = StrokeCap.Round))
}

/** Always a bell, never the old stopwatch; consistent across all domains. */
private fun DrawScope.drawP10BellGlyph(
    c: Offset, u: Float, color: Color, style: P10ReminderIconStyle,
) {
    val stroke = u * (if (style == P10ReminderIconStyle.CINEMATIC_EMBLEM) .014f else .012f)
    val bell = Path().apply {
        moveTo(c.x - u * .145f, c.y + u * .145f)
        cubicTo(c.x - u * .106f, c.y + u * .080f,
            c.x - u * .098f, c.y + u * .020f,
            c.x - u * .098f, c.y - u * .077f)
        cubicTo(c.x - u * .098f, c.y - u * .215f,
            c.x + u * .098f, c.y - u * .215f,
            c.x + u * .098f, c.y - u * .077f)
        cubicTo(c.x + u * .098f, c.y + u * .020f,
            c.x + u * .106f, c.y + u * .080f,
            c.x + u * .145f, c.y + u * .145f)
        close()
    }
    drawPath(bell, color.copy(alpha = .94f), style = Stroke(width = stroke,
        cap = StrokeCap.Round))
    drawArc(color, 18f, 144f, false,
        Offset(c.x - u * .047f, c.y + u * .113f),
        Size(u * .094f, u * .096f),
        style = Stroke(width = stroke, cap = StrokeCap.Round))
    drawLine(color, Offset(c.x, c.y - u * .188f),
        Offset(c.x, c.y - u * .217f), stroke, cap = StrokeCap.Round)
    drawArc(color.copy(alpha = .65f), 183f, 72f, false,
        Offset(c.x - u * .229f, c.y - u * .164f),
        Size(u * .21f, u * .28f),
        style = Stroke(width = stroke * .7f, cap = StrokeCap.Round))
    drawArc(color.copy(alpha = .65f), -72f, 72f, false,
        Offset(c.x + u * .019f, c.y - u * .164f),
        Size(u * .21f, u * .28f),
        style = Stroke(width = stroke * .7f, cap = StrokeCap.Round))
}
