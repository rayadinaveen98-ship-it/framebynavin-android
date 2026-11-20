package com.framebynavin.app.ui

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.V401WelcomeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * UX11-A — original launch art, source-stage only.
 *
 * Cinematic Ember Reveal, Aperture Light Bloom and Velvet Glass are real,
 * independently composed native Canvas ident styles. Existing V199/V401
 * welcomes are unchanged and can still be explicitly selected.
 *
 * No extra Activity, permission, network, bitmap blur, video, asset download,
 * authentication, reminder or project side effect. Preview does not play
 * sound or write any preference; normal launch keeps the existing signature
 * audio service and the same onFinished contract as V401.
 */
internal object UX11PremiumWelcomePolicy {
    const val DURATION_MS = 2_400L

    fun supported(style: V401WelcomeStyle): Boolean = when (style) {
        V401WelcomeStyle.CINEMATIC_EMBER,
        V401WelcomeStyle.APERTURE_LIGHT_BLOOM,
        V401WelcomeStyle.VELVET_GLASS -> true
        else -> false
    }

    fun progress(at: Long, from: Long, until: Long): Float =
        ((at - from).toFloat() / (until - from).coerceAtLeast(1L))
            .coerceIn(0f, 1f)

    fun eased(at: Long, from: Long, until: Long): Float {
        val p = progress(at, from, until)
        return p * p * (3f - 2f * p)
    }
}

@Composable
internal fun UX11PremiumWelcomeExperience(
    style: V401WelcomeStyle,
    preview: Boolean,
    onFinished: () -> Unit,
) {
    require(UX11PremiumWelcomePolicy.supported(style))
    val context = LocalContext.current.applicationContext
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    val finish by rememberUpdatedState(onFinished)
    var elapsedMs by remember(style, preview) { mutableLongStateOf(0L) }
    LaunchedEffect(style, preview) {
        val track = if (preview) null else withContext(Dispatchers.Default) {
            runCatching { V199WelcomeSound.prepare(context) }.getOrNull()
        }
        val start = SystemClock.elapsedRealtimeNanos()
        try {
            runCatching { track?.play() }
            while (elapsedMs < UX11PremiumWelcomePolicy.DURATION_MS) {
                withFrameNanos {
                    elapsedMs = ((SystemClock.elapsedRealtimeNanos() - start) / 1_000_000L)
                        .coerceAtMost(UX11PremiumWelcomePolicy.DURATION_MS)
                }
            }
        } finally {
            runCatching { track?.stop() }
            runCatching { track?.release() }
        }
        finish()
    }

    val t = elapsedMs
    val reveal = if (reducedMotion) 1f else
        UX11PremiumWelcomePolicy.eased(t, 390L, 1_190L)
    val tagline = if (reducedMotion) 1f else
        UX11PremiumWelcomePolicy.eased(t, 850L, 1_550L)
    val exit = if (reducedMotion) 1f else
        1f - UX11PremiumWelcomePolicy.eased(t, 2_100L, 2_400L)
    val cream = Color(0xFFF8F3EC)
    val soft = Color(0xFFDFCBBE)

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color(0xFF08070A))
            .testTag("ux11-premium-welcome-" + style.name.lowercase()),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = exit }) {
            if (size.minDimension <= 0f) return@Canvas
            when (style) {
                V401WelcomeStyle.CINEMATIC_EMBER -> ux11Ember(t, reducedMotion)
                V401WelcomeStyle.APERTURE_LIGHT_BLOOM -> ux11Aperture(t, reducedMotion)
                V401WelcomeStyle.VELVET_GLASS -> ux11Velvet(t, reducedMotion)
                else -> Unit
            }
        }
        // Name/logo is rendered in Compose typography, never rasterised into
        // particles. Premium art remains behind the legible brand wordmark.
        Text(
            text = "BACKLOT",
            modifier = Modifier.align(Alignment.Center)
                .offset(y = (-12).dp)
                .alpha(reveal * exit)
                .graphicsLayer {
                    scaleX = .975f + .025f * reveal
                    scaleY = .975f + .025f * reveal
                },
            color = cream,
            fontSize = 38.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
            maxLines = 1,
        )
        Text(
            text = "BUILT FOR CREATORS",
            modifier = Modifier.align(Alignment.Center)
                .offset(y = 48.dp)
                .alpha(tagline * exit),
            color = soft,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.6.sp,
            maxLines = 1,
        )
    }
}

/** All three visual treatments share a dark, high-contrast central title zone. */
private fun DrawScope.ux11Base(top: Color, bottom: Color) {
    drawRect(Brush.verticalGradient(listOf(top, bottom)))
}

private fun DrawScope.ux11Light(
    color: Color,
    center: Offset,
    radius: Float,
    opacity: Float,
) {
    if (radius <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = opacity.coerceIn(0f, 1f)),
                .4f to color.copy(alpha = (opacity * .35f).coerceIn(0f, 1f)),
                1f to color.copy(alpha = 0f),
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.ux11Ember(t: Long, reduced: Boolean) {
    ux11Base(Color(0xFF09060A), Color(0xFF13080C))
    val time = if (reduced) .7f else t / 1_000f
    val warm = Color(0xFFFF603E)
    val amber = Color(0xFFFFB264)
    val w = size.width
    val h = size.height
    val u = size.minDimension
    val opening = if (reduced) 1f else UX11PremiumWelcomePolicy.eased(t, 80L, 1_100L)

    // Two broad, slow atmospheric light wells behind an ink-black label zone.
    ux11Light(warm, Offset(w * (.14f + .035f * sin(time)), h * .80f),
        u * .98f, .31f * opening)
    ux11Light(amber, Offset(w * (.88f + .02f * cos(time * .85f)), h * .20f),
        u * .82f, .22f * opening)

    // Restrained projector-like light streaks converge from the edges.
    repeat(5) { index ->
        val dx = (index - 2) * u * .075f
        val start = Offset(w * .5f + dx - w * .42f, h * .88f)
        val end = Offset(w * .5f + dx * .16f, h * .42f)
        drawLine(
            color = if (index % 2 == 0) warm.copy(alpha = .095f * opening)
                else amber.copy(alpha = .075f * opening),
            start = start,
            end = end,
            strokeWidth = (1.5f + (index % 3)).dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    // Thin original cinematic frame detail; never behind text at full opacity.
    val borderAlpha = .25f * opening
    val half = min(w * .40f, 190.dp.toPx())
    val y = h * .51f
    drawLine(amber.copy(alpha = borderAlpha), Offset(w*.5f-half, y+80.dp.toPx()),
        Offset(w*.5f-half*.42f, y+80.dp.toPx()), 1.dp.toPx())
    drawLine(amber.copy(alpha = borderAlpha), Offset(w*.5f+half*.42f, y+80.dp.toPx()),
        Offset(w*.5f+half, y+80.dp.toPx()), 1.dp.toPx())
}

private fun DrawScope.ux11Aperture(t: Long, reduced: Boolean) {
    ux11Base(Color(0xFF080B14), Color(0xFF0D0915))
    val w = size.width
    val h = size.height
    val u = size.minDimension
    val center = Offset(w * .5f, h * .495f)
    val p = if (reduced) .9f else UX11PremiumWelcomePolicy.eased(t, 120L, 1_450L)
    val turn = if (reduced) .3f else t / 2_400f
    val copper = Color(0xFFFFAD79)
    val rose = Color(0xFFE55C75)
    ux11Light(rose, Offset(center.x + u * .30f, center.y - u*.08f), u*.93f, .17f*p)
    ux11Light(copper, Offset(center.x - u * .32f, center.y + u*.07f), u*.70f, .16f*p)

    // Aperture made of staggered precision arcs rather than a stock camera
    // graphic. The thin concentric bands preserve a readable center for text.
    val outer = u * .60f
    for (i in 0 until 6) {
        val r = outer - i * u * .057f
        drawArc(
            color = (if (i % 2 == 0) copper else rose)
                .copy(alpha = (.13f + .11f * p) * (1f - i*.06f)),
            startAngle = i * 60f + turn * (if (i % 2 == 0) 7f else -7f),
            sweepAngle = (105f + 40f * p).coerceAtMost(160f),
            useCenter = false,
            topLeft = Offset(center.x-r, center.y-r),
            size = Size(r*2f,r*2f),
            style = Stroke(width = if (i==0) 1.6.dp.toPx() else 1.dp.toPx(),
                cap=StrokeCap.Round),
        )
    }
    // Film-lens optics, subtle and dark enough behind the brand.
    drawCircle(Color(0xFF080A11).copy(alpha=.64f),
        radius=u*.27f, center=center)
    drawCircle(Brush.radialGradient(
        listOf(Color(0xFFB46A77).copy(alpha=.12f*p), Color.Transparent),
        center=center, radius=u*.42f), radius=u*.42f,center=center)
    drawCircle(copper.copy(alpha=.21f*p), radius=u*.34f,
        center=center, style=Stroke(width=1.dp.toPx()))
}

private fun DrawScope.ux11Velvet(t: Long, reduced: Boolean) {
    ux11Base(Color(0xFF070B11), Color(0xFF140E17))
    val w = size.width
    val h = size.height
    val u = size.minDimension
    val p = if (reduced) 1f else UX11PremiumWelcomePolicy.eased(t, 70L, 1_300L)
    val phase = if (reduced) .6f else t/1_200f
    val violet = Color(0xFF8A75C7)
    val titanium = Color(0xFFD9A5A5)
    ux11Light(violet, Offset(w*.15f, h*.2f),u*.83f,.17f*p)
    ux11Light(titanium, Offset(w*.88f,h*.84f),u*.95f,.13f*p)

    // Translucent flowing glass ribbons; curves run outside the central brand
    // anchor and never form a hard, distracting pill/card behind the wordmark.
    repeat(5) { index ->
        val side = if (index % 2 == 0) -1f else 1f
        val sweep = ((index+1)*.09f + .026f*sin(phase+index)) * w
        val x = if (side < 0) -w*.12f+sweep else w*1.12f-sweep
        val path=Path().apply {
            moveTo(x,h*.01f)
            cubicTo(x+side*w*.25f,h*.20f,
                x-side*w*.17f,h*.76f,x+side*w*.14f,h*1.06f)
        }
        drawPath(path,
            color=(if(index%2==0) violet else titanium)
                .copy(alpha=(.10f+.04f*(index%3))*p),
            style=Stroke(width=(5+index*2).dp.toPx(),cap=StrokeCap.Round))
    }
    drawLine(
        brush=Brush.horizontalGradient(listOf(
            Color.Transparent,
            titanium.copy(alpha=.19f*p),
            Color.Transparent)),
        start=Offset(w*.12f,h*.61f),
        end=Offset(w*.88f,h*.61f),
        strokeWidth=1.dp.toPx(),
    )
}
