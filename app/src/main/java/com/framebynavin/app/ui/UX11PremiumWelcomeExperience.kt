package com.framebynavin.app.ui

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.R
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

/**
 * UX12-D1: approved B identity, progressive editorial wordmark, no new artwork.
 * Progress helpers are pure so CI can verify the visual ordering without devices.
 */
internal object UX12D1SignatureTimeline {
    const val MARK_START_MS = 320L
    const val MARK_END_MS = 1_100L
    const val WORDMARK_START_MS = 880L
    const val WORDMARK_END_MS = 1_580L
    const val TAGLINE_START_MS = 1_220L
    const val TAGLINE_END_MS = 1_750L
    const val EXIT_START_MS = 2_100L

    fun mark(timeMs: Long, reducedMotion: Boolean): Float =
        if (reducedMotion) 1f else UX11PremiumWelcomePolicy.eased(
            timeMs, MARK_START_MS, MARK_END_MS,
        )

    fun wordmark(timeMs: Long, reducedMotion: Boolean): Float =
        if (reducedMotion) 1f else UX11PremiumWelcomePolicy.eased(
            timeMs, WORDMARK_START_MS, WORDMARK_END_MS,
        )

    fun tagline(timeMs: Long, reducedMotion: Boolean): Float =
        if (reducedMotion) 1f else UX11PremiumWelcomePolicy.eased(
            timeMs, TAGLINE_START_MS, TAGLINE_END_MS,
        )

    fun exit(timeMs: Long, reducedMotion: Boolean): Float =
        if (reducedMotion) 1f else 1f - UX11PremiumWelcomePolicy.eased(
            timeMs, EXIT_START_MS, UX11PremiumWelcomePolicy.DURATION_MS,
        )
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
    val mark = UX12D1SignatureTimeline.mark(t, reducedMotion)
    val wordmark = UX12D1SignatureTimeline.wordmark(t, reducedMotion)
    val tagline = UX12D1SignatureTimeline.tagline(t, reducedMotion)
    val exit = UX12D1SignatureTimeline.exit(t, reducedMotion)
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

        // One scalable approved vector mark plus simple text; no large raster
        // splash asset, floating showcase cards or artificial film props.
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .graphicsLayer { alpha = exit },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.backlot_v198_brand_mark),
                contentDescription = null, // decorative; Backlot wordmark labels brand
                modifier = Modifier.size(136.dp)
                    .alpha(mark)
                    .graphicsLayer {
                        val scale = .975f + .025f * mark
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - mark) * 10.dp.toPx()
                    }
                    .testTag("ux12-d1-approved-brand-mark"),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "BACKLOT",
                modifier = Modifier.alpha(wordmark).testTag("ux12-d1-brand-wordmark"),
                color = cream,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = "BUILT FOR CREATORS",
                modifier = Modifier.alpha(tagline),
                color = soft,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                maxLines = 1,
            )
        }
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
    ux11Base(Color(0xFF08080C), Color(0xFF150B11))
    val opening = if (reduced) 1f else UX11PremiumWelcomePolicy.eased(t, 0L, 900L)
    val w = size.width
    val h = size.height
    val u = size.minDimension

    // Single quiet optical light treatment. Deliberately no repeated film
    // strips, particle fields, photographic backdrops or dashboard framing.
    ux11Light(
        Color(0xFFBF4F43), Offset(w * .17f, h * .68f), u * .83f, .22f * opening,
    )
    ux11Light(
        Color(0xFFD8A77A), Offset(w * .83f, h * .31f), u * .79f, .13f * opening,
    )
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                Color(0xFFE8BA94).copy(alpha = .19f * opening),
                Color.Transparent,
            ),
        ),
        start = Offset(w * .18f, h * .67f),
        end = Offset(w * .82f, h * .67f),
        strokeWidth = 1.dp.toPx(),
    )
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
