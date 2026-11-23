package com.framebynavin.app.ui

import android.provider.Settings
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Point 6: one-shot Creator OS entry AFTER a successful, durable setup save.
 * This is not a startup gate, screen-router, account gate or onboarding screen.
 * Never reset a saved profile or ask for credentials.
 */
internal enum class P6CreatorEntryDestination {
    TODAY,
}

internal object P6CreatorEntryPolicy {
    const val DURATION_MS = 2_300L
    const val REDUCED_MOTION_MS = 180L

    fun eligible(
        saveSucceeded: Boolean,
        setupComplete: Boolean,
        profileComplete: Boolean,
        hasExternalLaunch: Boolean,
    ): Boolean = saveSucceeded && setupComplete && profileComplete && !hasExternalLaunch

    fun smooth(ms: Long, start: Long, end: Long): Float {
        val progress = ((ms - start).toFloat() /
            (end - start).coerceAtLeast(1L)).coerceIn(0f, 1f)
        return progress * progress * (3f - 2f * progress)
    }

    fun titleAlpha(ms: Long) = smooth(ms, 160L, 690L)
    fun subtitleAlpha(ms: Long) = smooth(ms, 620L, 1_080L)
    fun underlineProgress(ms: Long) = smooth(ms, 700L, 1_450L)
    fun settleAlpha(ms: Long) = smooth(ms, 1_160L, 1_750L)
}

/** One calm, accessible full-screen handoff; click/back may skip safely. */
@Composable
internal fun P6CreatorEntryTransition(onFinished: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) == 0f
        }.getOrDefault(false)
    }
    var finished by remember { mutableStateOf(false) }
    val latestFinished by rememberUpdatedState(onFinished)
    var elapsed by remember { mutableLongStateOf(0L) }

    fun finishOnce() {
        if (!finished) {
            finished = true
            latestFinished()
        }
    }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            delay(P6CreatorEntryPolicy.REDUCED_MOTION_MS)
        } else {
            val startedNs = SystemClock.elapsedRealtimeNanos()
            while (!finished && elapsed < P6CreatorEntryPolicy.DURATION_MS) {
                withFrameNanos {
                    elapsed = ((SystemClock.elapsedRealtimeNanos() - startedNs) / 1_000_000L)
                        .coerceAtMost(P6CreatorEntryPolicy.DURATION_MS)
                }
            }
        }
        finishOnce()
    }
    BackHandler(enabled = !finished) { finishOnce() }

    val headline = if (reducedMotion) 1f else P6CreatorEntryPolicy.titleAlpha(elapsed)
    val subtitle = if (reducedMotion) 1f else P6CreatorEntryPolicy.subtitleAlpha(elapsed)
    val underline = if (reducedMotion) 1f else P6CreatorEntryPolicy.underlineProgress(elapsed)
    val settled = if (reducedMotion) 1f else P6CreatorEntryPolicy.settleAlpha(elapsed)

    Surface(
        onClick = ::finishOnce,
        modifier = Modifier.fillMaxSize()
            .graphicsLayer {
                // Dissolve into the already-rendered Creator shell, not a blank frame.
                alpha = if (reducedMotion) 1f else
                    1f - P6CreatorEntryPolicy.smooth(
                        elapsed, 2_080L, P6CreatorEntryPolicy.DURATION_MS)
            }.testTag("p6-creator-entry-transition"),
        color = Color(0xFF0C0D11),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
                Text(
                    text = "BACKLOT",
                    color = Color(0xFFB7A5AE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.alpha(headline),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "CREATOR OS",
                    color = Color(0xFFF5F2F0),
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    letterSpacing = 1.0.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.graphicsLayer {
                        alpha = headline
                        translationY = (8f * density) * (1f - headline)
                    },
                )
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier.width(66.dp).height(2.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(underline).height(2.dp),
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFC35B65),
                    ) { }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Your studio is ready.",
                    color = Color(0xFFB4B0BA),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(subtitle),
                )
            }
            Text(
                text = "Tap anywhere to continue",
                color = Color(0xFF7F7B86),
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = 45.dp).alpha(settled),
            )
        }
    }
}
