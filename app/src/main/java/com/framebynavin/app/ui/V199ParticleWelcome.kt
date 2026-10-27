package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * V199 replaces the red-bar / 4,600ms studio-ident with a single 3,000ms timeline.
 * One batched Android Canvas drawPoints call per color/radius bucket: not 4,000 Compose nodes.
 * The actual word is a sampled text mask, not an image hidden below random confetti.
 */
internal object V199WelcomeTimeline {
    const val DURATION_MS = 3_000L
    const val AWAKEN_END_MS = 350L
    const val CONVERGE_END_MS = 1_900L
    const val TITLE_LOCK_END_MS = 2_450L
    const val HOLD_END_MS = 2_800L

    fun progress(ms: Long, start: Long, end: Long): Float =
        ((ms - start).toFloat() / (end - start).coerceAtLeast(1L)).coerceIn(0f, 1f)

    fun converge(ms: Long, stagger: Float): Float {
        val begin = (AWAKEN_END_MS - 80L + stagger * 400L).toLong()
        val progress = progress(ms, begin, CONVERGE_END_MS)
        return progress * progress * (3f - 2f * progress)
    }
}

@Composable
internal fun V199ParticleWelcome(onFinished: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val renderer = remember { V199ParticleWordRenderer() }
    val reducedMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        // Audio is prepared before the visual clock, never used as a second timer.
        val track = withContext(Dispatchers.Default) {
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
        onFinished()
    }

    val t = elapsedMs
    val textAlpha = V199WelcomeTimeline.progress(t, 1_900L, 2_250L) *
        (1f - V199WelcomeTimeline.progress(t, 2_800L, 3_000L))
    Box(Modifier.fillMaxSize().background(Color(0xFF111227)), contentAlignment = Alignment.Center) {
        if (reducedMotion) {
            Text(
                "Backlot",
                color = Color(0xFFEAEAFF),
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                renderer.draw(drawContext.canvas.nativeCanvas, size.width, size.height, t)
            }
        }
        Text(
            text = "Built for Creators",
            modifier = Modifier.align(Alignment.Center).offset(y = 78.dp)
                .alpha(if (reducedMotion) 1f else textAlpha),
            color = Color(0xFFB8B9D6),
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.7.sp,
            fontSize = 11.sp,
        )
    }
}

private class V199ParticleWordRenderer {
    private data class Particle(
        val sx: Float, val sy: Float,
        val bendX: Float, val bendY: Float,
        val tx: Float, val ty: Float,
        val stagger: Float, val group: Int,
    )

    private var width = 0f
    private var height = 0f
    private var points: List<Particle> = emptyList()
    private val groupCount = 12
    private var packed = Array(groupCount) { FloatArray(1) }
    private val counts = IntArray(groupCount)
    private val palette = intArrayOf(
        AndroidColor.rgb(68, 147, 255), // saturated blue
        AndroidColor.rgb(153, 122, 247), // lavender
        AndroidColor.rgb(255, 179, 136), // peach
        AndroidColor.rgb(255, 112, 156), // coral pink
    )
    private val brushes = Array(groupCount) { index ->
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette[index / 3]
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }

    fun draw(canvas: AndroidCanvas, w: Float, h: Float, elapsedMs: Long) {
        if (w <= 1f || h <= 1f) return
        if (w != width || h != height) rebuild(w, h)
        java.util.Arrays.fill(counts, 0)
        val exit = 1f - V199WelcomeTimeline.progress(elapsedMs, 2_800L, 3_000L)
        val ignition = V199WelcomeTimeline.progress(elapsedMs, 0, 350)
        val globalAlpha = (exit * ignition).coerceIn(0f, 1f)
        if (globalAlpha <= 0f) return
        for (p in points) {
            val u = V199WelcomeTimeline.converge(elapsedMs, p.stagger)
            val reverse = 1f - u
            val x = reverse * reverse * p.sx + 2f * reverse * u * p.bendX + u * u * p.tx
            val y = reverse * reverse * p.sy + 2f * reverse * u * p.bendY + u * u * p.ty
            val bucket = p.group
            val offset = counts[bucket] * 2
            packed[bucket][offset] = x
            packed[bucket][offset + 1] = y
            counts[bucket]++
        }
        val radiusBase = min(w, h)
        for (group in 0 until groupCount) {
            val total = counts[group] * 2
            if (total == 0) continue
            val paint = brushes[group]
            paint.strokeWidth = radiusBase * (0.0022f + (group % 3) * 0.0017f)
            paint.alpha = (255f * globalAlpha).toInt().coerceIn(0, 255)
            canvas.drawPoints(packed[group], 0, total, paint)
        }
    }

    private fun rebuild(w: Float, h: Float) {
        width = w
        height = h
        val random = Random(199_198L)
        val typePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textSize = min(w * 0.17f, h * 0.108f)
        }
        val word = "Backlot"
        val maxWordWidth = w * 0.85f
        if (typePaint.measureText(word) > maxWordWidth) {
            typePaint.textSize *= maxWordWidth / typePaint.measureText(word)
        }
        val wordWidth = typePaint.measureText(word)
        val maskWidth = (wordWidth + 20f).toInt().coerceAtLeast(10)
        val maskHeight = (typePaint.textSize * 1.6f).toInt().coerceAtLeast(10)
        val mask = Bitmap.createBitmap(maskWidth, maskHeight, Bitmap.Config.ARGB_8888)
        try {
            val maskCanvas = AndroidCanvas(mask)
            val baseline = typePaint.textSize * 1.18f
            maskCanvas.drawText(word, 10f, baseline, typePaint)
            val pixels = IntArray(maskWidth * maskHeight)
            mask.getPixels(pixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)
            val glyphCells = ArrayList<Int>(maskWidth * maskHeight / 4)
            for (i in pixels.indices) {
                if ((pixels[i] ushr 24) >= 136) glyphCells.add(i)
            }
            val total = when {
                w < 540f -> 2_300
                w < 900f -> 3_100
                else -> 4_000
            }
            val startX = (w - maskWidth) * .5f
            val startY = h * .445f - maskHeight * .5f
            val built = ArrayList<Particle>(total)
            repeat(total) { i ->
                val cell = if (glyphCells.isEmpty()) (maskWidth * (maskHeight / 2) + maskWidth / 2)
                    else glyphCells[random.nextInt(glyphCells.size)]
                val targetX = startX + (cell % maskWidth) + random.nextFloat() * .85f
                val targetY = startY + (cell / maskWidth) + random.nextFloat() * .85f
                val fromX = (-w * .10f) + random.nextFloat() * w * 1.20f
                val fromY = (-h * .08f) + random.nextFloat() * h * 1.16f
                val turn = if (i % 2 == 0) 1f else -1f
                val bendX = (fromX + targetX) * .5f + turn * (h * (.06f + random.nextFloat() * .14f))
                val bendY = (fromY + targetY) * .5f - turn * (w * (.04f + random.nextFloat() * .12f))
                val colorGroup = i % 4
                val sizeGroup = random.nextInt(3)
                built.add(Particle(fromX, fromY, bendX, bendY, targetX, targetY,
                    random.nextFloat(), colorGroup * 3 + sizeGroup))
            }
            points = built
            val groupSizes = IntArray(groupCount)
            for (p in points) groupSizes[p.group]++
            packed = Array(groupCount) { FloatArray(groupSizes[it] * 2) }
        } finally {
            mask.recycle()
        }
    }
}
