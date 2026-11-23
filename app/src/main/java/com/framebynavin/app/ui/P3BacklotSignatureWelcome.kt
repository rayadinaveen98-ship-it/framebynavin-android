package com.framebynavin.app.ui

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Founder-approved Point 3: quiet Backlot signature.
 *
 * The three panels are the EXACT paths/gradients extracted from the approved
 * backlot_v198_brand_mark vector. The assembled final image is drawn with the
 * ORIGINAL full vector, never a reinterpreted or stretched graphic.
 *
 * No particles, stage overlays, new navigation, permissions or session writes.
 * Normal launch only. Widget/deep-link launch bypasses this via V131LaunchGate.
 */
internal object P3SignatureTimeline {
    const val DURATION_MS = 3_000L
    const val REDUCED_DURATION_MS = 350L

    /** Bounded, monotone smooth deceleration; stable under frame drops. */
    fun smooth(ms: Long, start: Long, end: Long): Float {
        val fraction = ((ms - start).toFloat() / (end - start).coerceAtLeast(1L))
            .coerceIn(0f, 1f)
        return fraction * fraction * (3f - 2f * fraction)
    }

    fun top(ms: Long): Float = smooth(ms, 100L, 620L)
    fun middle(ms: Long): Float = smooth(ms, 270L, 840L)
    fun lower(ms: Long): Float = smooth(ms, 460L, 1_030L)
    fun fullLogo(ms: Long): Float = smooth(ms, 1_040L, 1_390L)
    fun splitVisibility(ms: Long): Float = 1f - smooth(ms, 1_050L, 1_400L)
    fun wordmark(ms: Long): Float = smooth(ms, 1_380L, 1_830L)
    fun tagline(ms: Long): Float = smooth(ms, 1_770L, 2_180L)
}

@Composable
internal fun P3BacklotSignatureWelcome(onFinished: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) == 0f
        }.getOrDefault(false)
    }
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            // Show the complete static brand immediately; accessibility avoids
            // both panel motion and a forced three-second startup delay.
            delay(P3SignatureTimeline.REDUCED_DURATION_MS)
            onFinished()
        } else {
            // Keep the existing opt-in launch sound. A failing audio source must
            // never prevent the actual app from opening.
            val track = withContext(Dispatchers.Default) {
                runCatching { V199WelcomeSound.prepare(context) }.getOrNull()
            }
            val startedNs = SystemClock.elapsedRealtimeNanos()
            try {
                runCatching { track?.play() }
                while (elapsed < P3SignatureTimeline.DURATION_MS) {
                    withFrameNanos {
                        elapsed = ((SystemClock.elapsedRealtimeNanos() - startedNs) / 1_000_000L)
                            .coerceAtMost(P3SignatureTimeline.DURATION_MS)
                    }
                }
            } finally {
                runCatching { track?.stop() }
                runCatching { track?.release() }
            }
            onFinished()
        }
    }

    val top = if (reducedMotion) 1f else P3SignatureTimeline.top(elapsed)
    val middle = if (reducedMotion) 1f else P3SignatureTimeline.middle(elapsed)
    val lower = if (reducedMotion) 1f else P3SignatureTimeline.lower(elapsed)
    val assembled = if (reducedMotion) 1f else P3SignatureTimeline.fullLogo(elapsed)
    val splitVisibility = if (reducedMotion) 0f
        else P3SignatureTimeline.splitVisibility(elapsed)
    val wordmark = if (reducedMotion) 1f else P3SignatureTimeline.wordmark(elapsed)
    val tagline = if (reducedMotion) 1f else P3SignatureTimeline.tagline(elapsed)

    Box(Modifier.fillMaxSize().background(Color(0xFF0C0D11)),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(152.dp), contentAlignment = Alignment.Center) {
                // Panels are decorative only. All are in a single approved mark's
                // shared 108 x 108 vector coordinate system and align exactly.
                if (!reducedMotion) {
                    Image(
                        painter = painterResource(R.drawable.p3_backlot_top_panel),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            alpha = top * splitVisibility
                            translationX = (-17f * density) * (1f - top)
                            translationY = (-13f * density) * (1f - top)
                            scaleX = 0.86f + 0.14f * top
                            scaleY = 0.86f + 0.14f * top
                        }
                    )
                    Image(
                        painter = painterResource(R.drawable.p3_backlot_middle_panel),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            alpha = middle * splitVisibility
                            translationX = (20f * density) * (1f - middle)
                            scaleX = 0.87f + 0.13f * middle
                            scaleY = 0.87f + 0.13f * middle
                        }
                    )
                    Image(
                        painter = painterResource(R.drawable.p3_backlot_lower_panel),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            alpha = lower * splitVisibility
                            translationX = (-15f * density) * (1f - lower)
                            translationY = (14f * density) * (1f - lower)
                            scaleX = 0.88f + 0.12f * lower
                            scaleY = 0.88f + 0.12f * lower
                        }
                    )
                }
                Image(
                    painter = painterResource(R.drawable.backlot_v198_brand_mark),
                    contentDescription = "Backlot logo",
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        alpha = assembled
                    }
                )
            }
            Spacer(Modifier.height(1.dp))
            Text(
                text = "Backlot",
                modifier = Modifier.graphicsLayer { alpha = wordmark },
                color = Color(0xFFF6F4F2),
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.75).sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(11.dp))
            Text(
                text = "BUILT FOR CREATORS",
                modifier = Modifier.graphicsLayer { alpha = tagline },
                color = Color(0xFFAAA7AF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.4.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
