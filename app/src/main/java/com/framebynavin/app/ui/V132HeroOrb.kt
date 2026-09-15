package com.framebynavin.app.ui

import android.content.Context
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.cos
import kotlin.math.sin

internal enum class V132HeroOrbState {
    IDLE,
    LISTENING,
    SPEAKING,
    PROCESSING,
    ALARM,
    SUCCESS,
}

/**
 * Hero visual renderer used by voice capture and high-attention reminder surfaces.
 *
 * Android 13+ renders the luminous body with AGSL/RuntimeShader. Older devices keep a
 * deliberately rich layered Canvas fallback. State and signal stay renderer-agnostic so the
 * interaction model never depends on GPU capability.
 */
@Composable
internal fun V132HeroOrb(
    state: V132HeroOrbState,
    signal: Float = 0f,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val target = signal.coerceIn(0f, 1f)
    val energy by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = .72f, stiffness = 90f),
        label = "heroOrbSignal",
    )
    val motion = rememberInfiniteTransition(label = "heroOrbAtmosphere")
    val spin by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(11_300, easing = LinearEasing)),
        label = "heroOrbSpin",
    )
    val counter by motion.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(17_900, easing = LinearEasing)),
        label = "heroOrbCounter",
    )
    val breathe by motion.animateFloat(
        initialValue = .985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            tween(3_350, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "heroOrbBreath",
    )

    Box(modifier = if (onTap != null) modifier.clickable(onClick = onTap) else modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { HeroOrbShaderView(it) },
                update = {
                    it.orbState = state
                    it.signal = energy
                },
            )
        } else {
            HeroOrbCanvasFallback(state, energy, spin, counter, breathe, Modifier.fillMaxSize())
        }

        // Thin optical filaments sit above both renderers and make the object read as glass/plasma
        // rather than a flat shader disc.
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val unit = size.minDimension
            val radius = unit * (.315f + energy * .018f)
            val active = state != V132HeroOrbState.IDLE
            repeat(4) { index ->
                val r = radius * (1.03f + index * .105f)
                val angle = if (index % 2 == 0) spin else counter
                val tint = when (index) {
                    0 -> Color(0xFFFF4F9B)
                    1 -> Color(0xFF58E8F2)
                    2 -> Color(0xFFFF8657)
                    else -> Color(0xFF9D63FF)
                }
                drawArc(
                    color = tint.copy(alpha = if (active) .42f + energy * .24f else .22f),
                    startAngle = angle + index * 73f,
                    sweepAngle = 38f + index * 13f + energy * 28f,
                    useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = unit * (.0045f + energy * .0025f), cap = StrokeCap.Round),
                )
            }
            repeat(10) { index ->
                val theta = Math.toRadians((spin * .32f + index * 36f).toDouble())
                val orbit = radius * (1.20f + (index % 3) * .14f)
                val p = Offset(
                    c.x + cos(theta).toFloat() * orbit,
                    c.y + sin(theta).toFloat() * orbit,
                )
                val alpha = .12f + ((index % 4) * .045f) + energy * .18f
                drawCircle(Color(0xFFFFE8CB).copy(alpha = alpha), unit * (.0027f + (index % 2) * .0018f), p)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class HeroOrbShaderView(context: Context) : View(context) {
    private val startedAt = System.nanoTime()
    private val shader = RuntimeShader(HERO_ORB_SHADER)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = this@HeroOrbShaderView.shader }

    var orbState: V132HeroOrbState = V132HeroOrbState.IDLE
        set(value) { field = value; invalidate() }
    var signal: Float = 0f
        set(value) { field = value.coerceIn(0f, 1f); invalidate() }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val seconds = (System.nanoTime() - startedAt) / 1_000_000_000f
        val baseEnergy = when (orbState) {
            V132HeroOrbState.IDLE -> .18f
            V132HeroOrbState.LISTENING -> .50f
            V132HeroOrbState.SPEAKING -> .62f
            V132HeroOrbState.PROCESSING -> .45f
            V132HeroOrbState.ALARM -> .72f
            V132HeroOrbState.SUCCESS -> .58f
        }
        shader.setFloatUniform("resolution", width.toFloat(), height.toFloat())
        shader.setFloatUniform("time", seconds)
        shader.setFloatUniform("energy", (baseEnergy + signal * .42f).coerceIn(0f, 1f))
        shader.setFloatUniform("mode", orbState.ordinal.toFloat())
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        postInvalidateOnAnimation()
    }
}

@Composable
private fun HeroOrbCanvasFallback(
    state: V132HeroOrbState,
    energy: Float,
    spin: Float,
    counter: Float,
    breathe: Float,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val c = center
        val unit = size.minDimension
        val stateBoost = when (state) {
            V132HeroOrbState.IDLE -> .50f
            V132HeroOrbState.ALARM -> 1f
            V132HeroOrbState.SUCCESS -> .86f
            else -> .78f
        }
        val core = unit * (.235f + energy * .022f) * breathe
        val halo = core * (1.92f + energy * .22f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFFFF4A92).copy(alpha = .28f * stateBoost),
                    Color(0xFF6939FF).copy(alpha = .18f * stateBoost),
                    Color(0xFF48E8F3).copy(alpha = .12f * stateBoost),
                    Color.Transparent,
                ), c, halo,
            ),
            radius = halo,
            center = c,
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color(0xFFDCF9FF).copy(alpha = .98f),
                    Color(0xFF58E8F2).copy(alpha = .88f),
                    Color(0xFF7549D9).copy(alpha = .78f),
                    Color(0xFFFF4A92).copy(alpha = .86f),
                    Color(0xFF100919),
                    Color(0xFF020308),
                ),
                center = Offset(c.x - core * .30f, c.y - core * .36f),
                radius = core * 1.65f,
            ),
            radius = core,
            center = c,
        )
        repeat(5) { index ->
            val r = core * (1.08f + index * .12f)
            val tint = listOf(
                Color(0xFFFF4A92), Color(0xFFFF8556), Color(0xFF58E8F2), Color(0xFF9A61FF), Color(0xFFFFC678)
            )[index]
            drawArc(
                color = tint.copy(alpha = .34f + stateBoost * .18f),
                startAngle = (if (index % 2 == 0) spin else counter) + index * 61f,
                sweepAngle = 45f + index * 11f + energy * 22f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(unit * .008f, cap = StrokeCap.Round),
            )
        }
        drawCircle(Color.White.copy(alpha = .82f), unit * .010f, Offset(c.x - core * .30f, c.y - core * .38f))
        drawCircle(Color.White.copy(alpha = .28f), unit * .028f, Offset(c.x - core * .28f, c.y - core * .36f))
    }
}

private const val HERO_ORB_SHADER = """
uniform float2 resolution;
uniform float time;
uniform float energy;
uniform float mode;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

half4 main(float2 fragCoord) {
    float scale = min(resolution.x, resolution.y);
    float2 p = (fragCoord - resolution * 0.5) / scale;
    float r = length(p);
    float a = atan(p.y, p.x);

    float breathing = sin(time * 1.75) * 0.006 + sin(time * 0.63) * 0.004;
    float shell = 0.235 + breathing + energy * 0.010;
    float warp = sin(a * 3.0 + time * 0.73) * 0.010 + sin(a * 5.0 - time * 0.41) * 0.006;
    float rr = r + warp * (0.35 + energy * 0.65);

    float body = 1.0 - smoothstep(shell - 0.006, shell + 0.010, rr);
    float rim = smoothstep(shell - 0.030, shell - 0.004, rr) * (1.0 - smoothstep(shell + 0.002, shell + 0.018, rr));
    float inner = 1.0 - smoothstep(shell * 0.90, shell, rr);

    float plasmaA = 0.5 + 0.5 * sin(a * 2.0 - time * 0.78 + r * 24.0);
    float plasmaB = 0.5 + 0.5 * sin(a * -3.0 + time * 0.51 + r * 31.0);
    float plasmaC = 0.5 + 0.5 * sin((p.x - p.y) * 21.0 + time * 0.34);

    float3 pink = float3(1.00, 0.13, 0.47);
    float3 coral = float3(1.00, 0.36, 0.20);
    float3 cyan = float3(0.12, 0.88, 1.00);
    float3 violet = float3(0.45, 0.18, 1.00);
    float3 gold = float3(1.00, 0.67, 0.28);

    float3 plasma = mix(pink, cyan, plasmaA);
    plasma = mix(plasma, violet, plasmaB * 0.52);
    plasma = mix(plasma, coral, plasmaC * 0.42);
    plasma += gold * max(0.0, sin(a * 4.0 + time * 0.29)) * 0.22;

    float2 lightPoint = float2(-0.08, -0.10);
    float spec = 1.0 - smoothstep(0.0, 0.075, length(p - lightPoint));
    float glass = 0.30 + 0.70 * (1.0 - rr / max(shell, 0.001));
    float3 core = float3(0.006, 0.008, 0.025);
    float3 color = mix(core, plasma * (0.50 + energy * 0.75), inner * (0.66 + glass * 0.34));
    color += rim * (cyan + pink + coral) * (0.42 + energy * 0.55);
    color += spec * float3(0.82, 0.96, 1.0) * (0.75 + energy * 0.55);

    float ribbon1 = 1.0 - smoothstep(0.010, 0.024, abs(rr - (shell + 0.045 + sin(a * 2.0 + time * 0.38) * 0.017)));
    float ribbon2 = 1.0 - smoothstep(0.010, 0.026, abs(rr - (shell + 0.085 + sin(a * 3.0 - time * 0.31) * 0.022)));
    ribbon1 *= 0.5 + 0.5 * sin(a * 1.6 - time * 0.27);
    ribbon2 *= 0.5 + 0.5 * sin(a * 2.1 + time * 0.21);
    color += ribbon1 * mix(pink, coral, plasmaB) * (0.55 + energy * 0.45);
    color += ribbon2 * mix(cyan, violet, plasmaA) * (0.48 + energy * 0.38);

    float bloom = (1.0 - smoothstep(shell, shell + 0.24, r)) * (1.0 - body) * (0.12 + energy * 0.13);
    color += bloom * mix(pink, cyan, plasmaA) * 0.75;

    float grain = hash21(floor(fragCoord / 5.0) + floor(time * 2.0));
    float star = step(0.994, grain) * (1.0 - smoothstep(shell + 0.02, shell + 0.22, r));
    color += star * float3(1.0, 0.78, 0.55) * 0.8;

    float alpha = max(body, max(ribbon1 * 0.82, ribbon2 * 0.72));
    alpha = max(alpha, bloom * 2.2);
    alpha = max(alpha, star * 0.7);
    return half4(half3(color), half(clamp(alpha, 0.0, 1.0)));
}
"""
