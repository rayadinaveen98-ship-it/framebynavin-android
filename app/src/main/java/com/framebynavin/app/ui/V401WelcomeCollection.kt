package com.framebynavin.app.ui

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.R
import com.framebynavin.app.ui.theme.V401WelcomeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sin

/**
 * UX01. One 3-second launch contract for four independently selectable identities.
 * The existing V199 particle renderer remains unchanged. The same sonic signature
 * is optional on normal launches, and previews are silent and never persist choices.
 */
@Composable
internal fun V401WelcomeExperience(
    style: V401WelcomeStyle,
    preview: Boolean = false,
    onFinished: () -> Unit,
) {
    // UX11-A: keep the original four V401 routes bit-for-bit in behavior.
    // The three new premium styles have their own shorter, native ident.
    if (UX11PremiumWelcomePolicy.supported(style)) {
        UX11PremiumWelcomeExperience(
            style = style,
            preview = preview,
            onFinished = onFinished,
        )
        return
    }
    if (style == V401WelcomeStyle.PARTICLE_SIGNATURE) {
        V199ParticleWelcome(onFinished = onFinished, playSound = !preview)
        return
    }

    val context = LocalContext.current.applicationContext
    val reducedMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    var elapsedMs by remember(style) { mutableLongStateOf(0L) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(style, preview) {
        val track = if (preview) null else withContext(Dispatchers.Default) {
            runCatching { V199WelcomeSound.prepare(context) }.getOrNull()
        }
        val startedNs = SystemClock.elapsedRealtimeNanos()
        try {
            runCatching { track?.play() }
            while (elapsedMs < V199WelcomeTimeline.DURATION_MS) {
                withFrameNanos {
                    elapsedMs = ((SystemClock.elapsedRealtimeNanos() - startedNs) / 1_000_000L)
                        .coerceAtMost(V199WelcomeTimeline.DURATION_MS)
                }
            }
        } finally {
            runCatching { track?.stop() }
            runCatching { track?.release() }
        }
        currentOnFinished()
    }

    val t = elapsedMs
    val exitAlpha = 1f - V199WelcomeTimeline.progress(t, 2_800L, 3_000L)
    val titleAlpha = if (reducedMotion) 1f else V199WelcomeTimeline.progress(t, 1_180L, 2_150L) * exitAlpha
    val taglineAlpha = if (reducedMotion) 1f else V199WelcomeTimeline.progress(t, 1_850L, 2_440L) * exitAlpha

    Box(Modifier.fillMaxSize().background(V401WelcomeInk), contentAlignment = Alignment.Center) {
        if (!reducedMotion) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = exitAlpha }) {
                if (size.minDimension <= 0f) return@Canvas
                when (style) {
                    V401WelcomeStyle.LIVING_FRAME -> {
                        val progress = V199WelcomeTimeline.progress(t, 120L, 1_930L)
                        val cx = size.width * .5f
                        val cy = size.height * .475f
                        val halfWidth = min(size.width * .405f, size.height * .34f)
                        val halfHeight = min(size.height * .108f, halfWidth * .35f)
                        val left = cx - halfWidth
                        val right = cx + halfWidth
                        val top = cy - halfHeight
                        val bottom = cy + halfHeight
                        drawRoundRect(
                            color = V401Warm.copy(alpha = .08f),
                            topLeft = Offset(left - 12.dp.toPx(), top - 12.dp.toPx()),
                            size = Size(halfWidth * 2f + 24.dp.toPx(), halfHeight * 2f + 24.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                        fun segment(index: Int): Float = (progress * 4f - index).coerceIn(0f, 1f)
                        val stroke = 2.2.dp.toPx()
                        drawLine(V401Warm, Offset(left, top), Offset(left + (right - left) * segment(0), top),
                            strokeWidth = stroke, cap = StrokeCap.Round)
                        drawLine(V401Warm, Offset(right, top), Offset(right, top + (bottom - top) * segment(1)),
                            strokeWidth = stroke, cap = StrokeCap.Round)
                        drawLine(V401Warm, Offset(right, bottom), Offset(right - (right - left) * segment(2), bottom),
                            strokeWidth = stroke, cap = StrokeCap.Round)
                        drawLine(V401Warm, Offset(left, bottom), Offset(left, bottom - (bottom - top) * segment(3)),
                            strokeWidth = stroke, cap = StrokeCap.Round)
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(V401Warm.copy(alpha = .13f * progress), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = halfWidth * 1.75f,
                            ),
                            center = Offset(cx, cy),
                            radius = halfWidth * 1.75f,
                        )
                    }
                    V401WelcomeStyle.PRISMATIC_BLOOM -> {
                        val gather = V199WelcomeTimeline.progress(t, 100L, 2_000L)
                        val colors = listOf(
                            Color(0xFF4798FF), Color(0xFF9479ED), Color(0xFFFFAE80),
                            Color(0xFFFF769D), Color(0xFF8DAAFF), Color(0xFFE4B3FF),
                        )
                        for (i in colors.indices) {
                            val fromLeft = i % 2 == 0
                            val sx = if (fromLeft) -size.width * .12f else size.width * 1.12f
                            val sy = size.height * (.19f + i * .11f)
                            val ex = size.width * (.43f + i * .026f)
                            val ey = size.height * (.46f + (i - 2.5f) * .008f)
                            val progress = ((gather - i * .045f) / (1f - i * .045f)).coerceIn(0f, 1f)
                            val tipX = sx + (ex - sx) * progress
                            val tipY = sy + (ey - sy) * progress
                            val path = Path().apply {
                                moveTo(sx, sy)
                                cubicTo(
                                    size.width * (if (fromLeft) .24f else .76f),
                                    size.height * (.14f + i * .08f),
                                    size.width * .5f + (i - 2.5f) * 18.dp.toPx(),
                                    size.height * .66f,
                                    tipX, tipY,
                                )
                            }
                            drawPath(
                                path, color = colors[i].copy(alpha = .30f + .39f * progress),
                                style = Stroke(width = (1.7f + i % 3).dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFB6A5FF).copy(alpha = .23f * gather), Color.Transparent),
                                center = Offset(size.width * .5f, size.height * .46f),
                                radius = size.minDimension * .42f,
                            ),
                            center = Offset(size.width * .5f, size.height * .46f),
                            radius = size.minDimension * .42f,
                        )
                    }
                    V401WelcomeStyle.SPARK_FIRST_LIGHT -> {
                        val light = V199WelcomeTimeline.progress(t, 370L, 1_920L)
                        val source = Offset(size.width * .29f, size.height * .55f)
                        val target = Offset(size.width * .56f, size.height * .46f)
                        val cone = Path().apply {
                            moveTo(source.x, source.y)
                            lineTo(target.x - size.width * .24f * light, target.y - size.height * .15f * light)
                            lineTo(target.x + size.width * .30f * light, target.y + size.height * .16f * light)
                            close()
                        }
                        drawPath(
                            cone,
                            brush = Brush.linearGradient(
                                listOf(V401Warm.copy(alpha = .02f), V401Warm.copy(alpha = .16f * light)),
                                start = source, end = target,
                            ),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(V401Warm.copy(alpha = .18f * light), Color.Transparent),
                                center = target,
                                radius = size.minDimension * .48f,
                            ),
                            radius = size.minDimension * .48f,
                            center = target,
                        )
                        val flare = (sin(t * .004f) * .03f + .14f) * light
                        drawCircle(V401Warm.copy(alpha = flare), radius = 18.dp.toPx(), center = source)
                    }
                    V401WelcomeStyle.PARTICLE_SIGNATURE -> Unit // Existing renderer owns this style.
                    V401WelcomeStyle.CINEMATIC_EMBER,
                    V401WelcomeStyle.APERTURE_LIGHT_BLOOM,
                    V401WelcomeStyle.VELVET_GLASS -> Unit // UX11 original renderer handles these above.
                }
            }
        }

        if (style == V401WelcomeStyle.SPARK_FIRST_LIGHT) {
            val sparkAlpha = if (reducedMotion) 1f else V199WelcomeTimeline.progress(t, 180L, 970L) * exitAlpha
            Image(
                painter = painterResource(R.drawable.guide_funny_welcome),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.Center)
                    .offset(x = (-112).dp, y = 43.dp)
                    .size(120.dp)
                    .graphicsLayer {
                        alpha = sparkAlpha
                        scaleX = .96f + .04f * sparkAlpha
                        scaleY = .96f + .04f * sparkAlpha
                    },
            )
        }

        Text(
            text = "Backlot",
            modifier = Modifier.align(Alignment.Center)
                .offset(
                    x = if (style == V401WelcomeStyle.SPARK_FIRST_LIGHT) 28.dp else 0.dp,
                    y = (-18).dp,
                )
                .alpha(titleAlpha),
            color = V401Ivory,
            fontSize = if (style == V401WelcomeStyle.SPARK_FIRST_LIGHT) 36.sp else 42.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        Text(
            text = "Built for Creators",
            modifier = Modifier.align(Alignment.Center).offset(y = 66.dp).alpha(taglineAlpha),
            color = V401Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.7.sp,
        )
    }
}

private val V401WelcomeInk = Color(0xFF111227)
private val V401Ivory = Color(0xFFEAEAFF)
private val V401Muted = Color(0xFFB8B9D6)
private val V401Warm = Color(0xFFFFC59E)
