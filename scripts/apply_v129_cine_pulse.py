from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def p(rel: str) -> Path:
    return ROOT / rel

def replace_once(rel: str, old: str, new: str, required: bool = True):
    path = p(rel)
    text = path.read_text()
    if old not in text:
        if required:
            raise SystemExit(f"Missing expected block in {rel}: {old[:180]!r}")
        return False
    path.write_text(text.replace(old, new, 1))
    return True

# -----------------------------------------------------------------------------
# Version
# -----------------------------------------------------------------------------
build = p("app/build.gradle.kts")
text = build.read_text()
text = text.replace('versionCode = 128', 'versionCode = 129')
text = text.replace('versionName = "2.0.0-rc4-premium-experience-v3"', 'versionName = "2.0.0-rc5-cine-pulse-live-guide"')
build.write_text(text)

# -----------------------------------------------------------------------------
# Theme model: retire Ivory, add materially different Lumen Flow personality.
# -----------------------------------------------------------------------------
theme_path = p("app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt")
theme = theme_path.read_text()
theme = theme.replace(
    'enum class FrameSurfacePersonality { SOLID, EDITORIAL, GLASS }',
    'enum class FrameSurfacePersonality { SOLID, GLASS, LUMEN }',
)
ivory = '''    IVORY_STUDIO(
        "Ivory Studio",
        "Warm paper · ink · editorial red",
        FramePalette(
            background = Color(0xFFF6F1E8), surface = Color(0xFFFFFCF7), surfaceRaised = Color(0xFFF0E9DE),
            line = Color(0xFFD8CFC0), foreground = Color(0xFF1D1A17), muted = Color(0xFF6F675E),
            primary = Color(0xFFB9232F), primaryDeep = Color(0xFFF2D8DA), secondary = Color(0xFF9A6A25),
            success = Color(0xFF3A7D55), tertiary = Color(0xFF645A4D),
            surfacePersonality = FrameSurfacePersonality.EDITORIAL, isLight = true,
        ),
    ),
'''
theme = theme.replace(ivory, '')
if 'LUMEN_FLOW(' not in theme:
    insert = '''    LUMEN_FLOW(
        "Lumen Flow",
        "Living ribbons · coral light · aqua calm",
        FramePalette(
            background = Color(0xFF070811), surface = Color(0xD1121725), surfaceRaised = Color(0xE21A2133),
            line = Color(0x665A6A8A), foreground = Color(0xFFF9F7FF), muted = Color(0xFFA8ADC1),
            primary = Color(0xFFFF5D7A), primaryDeep = Color(0xFF351225), secondary = Color(0xFFFFC76B),
            success = Color(0xFF70E2A2), tertiary = Color(0xFF52DED2),
            surfacePersonality = FrameSurfacePersonality.LUMEN,
        ),
    ),
'''
    theme = theme.replace('    AURORA_GLASS(\n', insert + '    AURORA_GLASS(\n')
theme = theme.replace(
    '    val isEditorial: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.EDITORIAL\n',
    '    val isLumen: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.LUMEN\n',
)
theme_path.write_text(theme)

# -----------------------------------------------------------------------------
# Cine Pulse: native live vector state machine. This renderer is isolated so a
# future Rive asset can replace it without changing journey logic.
# -----------------------------------------------------------------------------
p("app/src/main/java/com/framebynavin/app/ui/V129CinePulseGuide.kt").write_text(r'''package com.framebynavin.app.ui

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.FrameTertiary
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import kotlin.math.cos
import kotlin.math.sin

enum class CinePulseState { IDLE, WALK, POINT, THINK, LISTEN, SUCCESS, CELEBRATE, REST }

/** Live, density-independent Cine Pulse mascot. No GIF/video loop: app state drives the pose. */
@Composable
internal fun CinePulseGuide(
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val motion = rememberInfiniteTransition(label = "cinePulse")
    val phase by motion.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (state == CinePulseState.WALK) 760 else 3200, easing = LinearEasing)),
        label = "cinePulsePhase",
    )
    val breathe by motion.animateFloat(
        .96f, 1.035f,
        infiniteRepeatable(tween(1850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cinePulseBreathe",
    )
    val glow by motion.animateFloat(
        .48f, .92f,
        infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "cinePulseGlow",
    )
    val bob = when (state) {
        CinePulseState.WALK -> sin(phase * Math.PI * 4).toFloat() * 2.2f
        CinePulseState.CELEBRATE -> -4.2f * sin(phase * Math.PI * 2).toFloat().coerceAtLeast(0f)
        CinePulseState.REST -> 0f
        else -> (breathe - 1f) * 18f
    }

    Canvas(modifier.graphicsLayer { translationY = bob }) {
        val w = size.width
        val h = size.height
        val u = size.minDimension / 100f
        val cx = w * .50f
        val faceY = h * .31f
        val faceW = 50f * u
        val faceH = 36f * u
        val coral = RecRed
        val aqua = FrameTertiary
        val gold = MutedGold
        val ivory = ProjectorIvory
        val shell = CinemaSurfaceRaised
        val direction = if (pointRight) 1f else -1f

        // Atmosphere: the glow follows the current app theme while the silhouette stays Cine Pulse.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(coral.copy(alpha = .22f * glow), aqua.copy(alpha = .14f * glow), gold.copy(alpha = .06f), Color.Transparent),
                center = Offset(cx, h * .43f),
                radius = w * .62f,
            ),
            radius = w * .62f,
            center = Offset(cx, h * .43f),
        )

        // Signature translucent ribbon halo. Separate arcs drift at different rates.
        val ribbonBox = Size(faceW * 1.55f, faceH * 1.65f)
        val ribbonTopLeft = Offset(cx - ribbonBox.width / 2f, faceY - ribbonBox.height / 2f)
        drawArc(
            coral.copy(alpha = .72f),
            startAngle = 198f + phase * 30f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = ribbonTopLeft,
            size = ribbonBox,
            style = Stroke(width = 6.4f * u, cap = StrokeCap.Round),
        )
        drawArc(
            aqua.copy(alpha = .72f),
            startAngle = 14f - phase * 24f,
            sweepAngle = 137f,
            useCenter = false,
            topLeft = ribbonTopLeft + Offset(0f, 1.5f * u),
            size = ribbonBox,
            style = Stroke(width = 4.6f * u, cap = StrokeCap.Round),
        )
        drawArc(
            gold.copy(alpha = .52f),
            startAngle = 322f + phase * 18f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = ribbonTopLeft + Offset(0f, 2f * u),
            size = ribbonBox,
            style = Stroke(width = 2.3f * u, cap = StrokeCap.Round),
        )

        // Head shell + premium rim.
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(shell, CinemaBlack)),
            topLeft = Offset(cx - faceW / 2f, faceY - faceH / 2f),
            size = Size(faceW, faceH),
            cornerRadius = CornerRadius(16f * u),
        )
        drawRoundRect(
            brush = Brush.linearGradient(listOf(gold.copy(alpha = .90f), coral.copy(alpha = .72f), aqua.copy(alpha = .88f))),
            topLeft = Offset(cx - faceW / 2f, faceY - faceH / 2f),
            size = Size(faceW, faceH),
            cornerRadius = CornerRadius(16f * u),
            style = Stroke(width = 2.25f * u),
        )

        val eyeY = faceY + 1f * u
        val eyeDX = 10.5f * u
        val eyeH = if (state == CinePulseState.REST) 2.2f * u else 10.5f * u
        val eyeW = 4.8f * u
        if (state == CinePulseState.CELEBRATE || state == CinePulseState.SUCCESS) {
            drawArc(ivory, 205f, 130f, false, Offset(cx - eyeDX - 5f*u, eyeY - 2f*u), Size(10f*u, 8f*u), style = Stroke(2.2f*u, cap = StrokeCap.Round))
            drawArc(ivory, 205f, 130f, false, Offset(cx + eyeDX - 5f*u, eyeY - 2f*u), Size(10f*u, 8f*u), style = Stroke(2.2f*u, cap = StrokeCap.Round))
        } else {
            drawRoundRect(ivory.copy(alpha = if (state == CinePulseState.REST) .68f else .96f), Offset(cx - eyeDX - eyeW/2, eyeY - eyeH/2), Size(eyeW, eyeH), CornerRadius(eyeW))
            drawRoundRect(ivory.copy(alpha = if (state == CinePulseState.REST) .68f else .96f), Offset(cx + eyeDX - eyeW/2, eyeY - eyeH/2), Size(eyeW, eyeH), CornerRadius(eyeW))
        }

        // Tiny signature spark above the halo.
        val sparkX = cx + 26f*u
        val sparkY = faceY - 25f*u + sin(phase * Math.PI * 2).toFloat() * 1.4f*u
        val spark = Path().apply {
            moveTo(sparkX, sparkY - 5f*u); lineTo(sparkX + 2f*u, sparkY - 1.5f*u)
            lineTo(sparkX + 5f*u, sparkY); lineTo(sparkX + 2f*u, sparkY + 1.5f*u)
            lineTo(sparkX, sparkY + 5f*u); lineTo(sparkX - 2f*u, sparkY + 1.5f*u)
            lineTo(sparkX - 5f*u, sparkY); lineTo(sparkX - 2f*u, sparkY - 1.5f*u); close()
        }
        drawPath(spark, gold.copy(alpha = .70f + .25f * glow))

        // Compact black body; the luminous core is the stable identity mark.
        val bodyTop = h * .51f
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF151722), Color(0xFF08090E))),
            topLeft = Offset(cx - 15.5f*u, bodyTop),
            size = Size(31f*u, 28f*u),
            cornerRadius = CornerRadius(12f*u),
        )
        drawCircle(coral.copy(alpha = .22f * glow), 7.5f*u, Offset(cx, bodyTop + 12.5f*u))
        drawCircle(gold.copy(alpha = .94f), 3.2f*u, Offset(cx, bodyTop + 12.5f*u))
        drawCircle(ivory.copy(alpha = .92f), 1.2f*u, Offset(cx - .7f*u, bodyTop + 11.5f*u))

        val shoulderY = bodyTop + 7f*u
        val leftShoulder = Offset(cx - 13f*u, shoulderY)
        val rightShoulder = Offset(cx + 13f*u, shoulderY)
        val arm = ivory.copy(alpha = .78f)
        val walkSwing = if (state == CinePulseState.WALK) sin(phase * Math.PI * 4).toFloat() * 7f*u else 0f

        when (state) {
            CinePulseState.POINT -> {
                val s = if (pointRight) rightShoulder else leftShoulder
                val elbow = Offset(s.x + direction * 13f*u, s.y - 2f*u)
                val hand = Offset(s.x + direction * 28f*u, s.y - 11f*u)
                drawLine(arm, s, elbow, 4f*u, StrokeCap.Round)
                drawLine(arm, elbow, hand, 4f*u, StrokeCap.Round)
                drawCircle(gold, 2.5f*u, hand)
                val rest = if (pointRight) leftShoulder else rightShoulder
                drawLine(arm.copy(alpha=.62f), rest, Offset(rest.x - direction*4f*u, rest.y + 17f*u), 3.5f*u, StrokeCap.Round)
            }
            CinePulseState.CELEBRATE -> {
                drawLine(arm, leftShoulder, Offset(cx - 28f*u, bodyTop - 9f*u), 4f*u, StrokeCap.Round)
                drawLine(arm, rightShoulder, Offset(cx + 28f*u, bodyTop - 9f*u), 4f*u, StrokeCap.Round)
                drawCircle(aqua, 2.8f*u, Offset(cx - 28f*u, bodyTop - 9f*u))
                drawCircle(coral, 2.8f*u, Offset(cx + 28f*u, bodyTop - 9f*u))
            }
            CinePulseState.THINK -> {
                drawLine(arm, leftShoulder, Offset(cx - 17f*u, bodyTop + 20f*u), 3.5f*u, StrokeCap.Round)
                drawLine(arm, rightShoulder, Offset(cx + 20f*u, faceY + 15f*u), 3.5f*u, StrokeCap.Round)
                drawCircle(gold, 2.4f*u, Offset(cx + 20f*u, faceY + 15f*u))
            }
            CinePulseState.LISTEN -> {
                drawLine(aqua.copy(alpha=.90f), leftShoulder, Offset(cx - 22f*u, bodyTop + 3f*u), 3.7f*u, StrokeCap.Round)
                drawLine(aqua.copy(alpha=.90f), rightShoulder, Offset(cx + 22f*u, bodyTop + 3f*u), 3.7f*u, StrokeCap.Round)
            }
            CinePulseState.SUCCESS -> {
                drawLine(arm, leftShoulder, Offset(cx - 17f*u, bodyTop + 18f*u), 3.5f*u, StrokeCap.Round)
                drawLine(arm, rightShoulder, Offset(cx + 18f*u, bodyTop + 4f*u), 3.5f*u, StrokeCap.Round)
                drawCircle(gold, 2.5f*u, Offset(cx + 18f*u, bodyTop + 4f*u))
            }
            else -> {
                drawLine(arm.copy(alpha=.64f), leftShoulder, Offset(cx - 18f*u - walkSwing, bodyTop + 21f*u), 3.5f*u, StrokeCap.Round)
                drawLine(arm.copy(alpha=.64f), rightShoulder, Offset(cx + 18f*u + walkSwing, bodyTop + 21f*u), 3.5f*u, StrokeCap.Round)
            }
        }

        val hipY = h * .79f
        val footY = h * .92f
        drawLine(arm.copy(alpha=.70f), Offset(cx - 7f*u, hipY), Offset(cx - 10f*u + walkSwing, footY), 4f*u, StrokeCap.Round)
        drawLine(arm.copy(alpha=.70f), Offset(cx + 7f*u, hipY), Offset(cx + 10f*u - walkSwing, footY), 4f*u, StrokeCap.Round)
        drawLine(coral.copy(alpha=.86f), Offset(cx - 14f*u + walkSwing, footY), Offset(cx - 6f*u + walkSwing, footY), 3.2f*u, StrokeCap.Round)
        drawLine(aqua.copy(alpha=.82f), Offset(cx + 6f*u - walkSwing, footY), Offset(cx + 14f*u - walkSwing, footY), 3.2f*u, StrokeCap.Round)

        if (state == CinePulseState.CELEBRATE) {
            repeat(5) { i ->
                val angle = (i * 72f + phase * 35f) * 0.017453292f
                val r = 39f*u
                drawCircle(
                    if (i % 2 == 0) coral else aqua,
                    1.4f*u,
                    Offset(cx + cos(angle)*r, faceY + sin(angle)*r),
                )
            }
        }
    }
}
''')

# Frame Guide compatibility wrapper + shorter setup copy.
p("app/src/main/java/com/framebynavin/app/ui/V127GuideCompanion.kt").write_text(r'''package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

enum class FrameGuidePose { IDLE, WALK, POINT, CELEBRATE }

/** Compatibility surface: old journey calls now render the production Cine Pulse state machine. */
@Composable
internal fun FrameGuideCompanion(
    pose: FrameGuidePose,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val state = when (pose) {
        FrameGuidePose.IDLE -> CinePulseState.IDLE
        FrameGuidePose.WALK -> CinePulseState.WALK
        FrameGuidePose.POINT -> CinePulseState.POINT
        FrameGuidePose.CELEBRATE -> CinePulseState.CELEBRATE
    }
    CinePulseGuide(state = state, modifier = modifier, pointRight = pointRight)
}

@Composable
internal fun V127SetupGuideStrip(page: Int) {
    val tips = listOf(
        "Choose your main creator mode.",
        "Choose where you publish.",
        "Choose how you usually create.",
        "Pick your priorities.",
        "Optional reminder permissions.",
    )
    val poses = listOf(FrameGuidePose.POINT, FrameGuidePose.WALK, FrameGuidePose.POINT, FrameGuidePose.IDLE, FrameGuidePose.CELEBRATE)

    AnimatedContent(
        targetState = page.coerceIn(0, tips.lastIndex),
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(170)) },
        label = "setupCinePulse",
    ) { index ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, RecRed.copy(alpha = .22f)),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                FrameGuideCompanion(poses[index], Modifier.size(width = 58.dp, height = 68.dp), pointRight = true)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("CINE PULSE", color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(tips[index], color = ProjectorIvory, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
''')

# -----------------------------------------------------------------------------
# Voice orb: mic opens idle orb; orb tap explicitly starts/stops recognition.
# -----------------------------------------------------------------------------
p("app/src/main/java/com/framebynavin/app/ui/V117VoiceQuickIdeaUi.kt").write_text(r'''package com.framebynavin.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.voice.IdeaVoiceLanguage
import com.framebynavin.app.voice.IdeaVoiceTranscriber
import com.framebynavin.app.voice.IdeaVoiceTranscriberListener
import kotlinx.coroutines.delay

/** Two-step voice capture: open Frame Pulse first, tap the orb only when ready to listen. */
@Composable
internal fun V117VoiceIdeaInput(
    onTranscript: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestOnTranscript by rememberUpdatedState(onTranscript)

    var orbOpen by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf(IdeaVoiceLanguage.AUTO) }
    var listening by remember { mutableStateOf(false) }
    var partial by remember { mutableStateOf("") }
    var detectedLanguage by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var rmsDb by remember { mutableFloatStateOf(0f) }
    var transcriber by remember { mutableStateOf<IdeaVoiceTranscriber?>(null) }
    var pendingPermissionStart by remember { mutableStateOf(false) }
    var microphoneGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        microphoneGranted = granted
        if (granted && pendingPermissionStart && orbOpen) {
            error = null
            partial = ""
            transcriber?.start(language)
        } else if (!granted) {
            error = "Microphone access is needed only when you tap the orb to listen."
        }
        pendingPermissionStart = false
    }

    DisposableEffect(context) {
        val engine = IdeaVoiceTranscriber(
            context = context.applicationContext,
            listener = object : IdeaVoiceTranscriberListener {
                override fun onListeningChanged(value: Boolean) {
                    listening = value
                    if (!value) rmsDb = 0f
                }
                override fun onPartialTranscript(text: String) { partial = text }
                override fun onFinalTranscript(text: String, confidence: Float?) {
                    if (text.isNotBlank()) latestOnTranscript(text)
                    partial = ""
                }
                override fun onDetectedLanguage(languageTag: String?) { detectedLanguage = languageTag }
                override fun onVoiceError(message: String) { error = message }
                override fun onRmsChanged(rmsDbValue: Float) { rmsDb = rmsDbValue }
            },
        )
        transcriber = engine
        onDispose { engine.destroy(); transcriber = null }
    }

    LaunchedEffect(listening) {
        elapsedSeconds = 0
        while (listening) {
            delay(1_000L)
            elapsedSeconds += 1
        }
    }

    fun startVoiceFromOrb() {
        error = null
        partial = ""
        detectedLanguage = null
        rmsDb = 0f
        if (microphoneGranted) transcriber?.start(language)
        else {
            pendingPermissionStart = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun closeOrb() {
        if (listening) transcriber?.stop()
        pendingPermissionStart = false
        orbOpen = false
        partial = ""
        rmsDb = 0f
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, if (listening) RecRed.copy(alpha = .52f) else CinemaLine),
    ) {
        AnimatedContent(
            targetState = orbOpen,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(170)) },
            label = "voiceOrbOpenState",
        ) { opened ->
            if (!opened) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = { orbOpen = true; error = null },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = RecRed.copy(alpha = .16f), contentColor = RecRed),
                            modifier = Modifier.size(46.dp),
                        ) { Icon(Icons.Outlined.Mic, "Open voice capture", modifier = Modifier.size(23.dp)) }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Voice capture", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("Open Frame Pulse, then tap when you are ready.", color = MutedText, fontSize = 8.7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("Audio is never saved.", color = MutedText, fontSize = 7.7.sp)
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("FRAME PULSE", color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
                            Text(if (listening) listeningSubtitle(language, detectedLanguage) else "Idle · tap the orb to listen", color = MutedText, fontSize = 8.5.sp)
                        }
                        Text(if (listening) formatVoiceTime(elapsedSeconds) else "READY", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = ::closeOrb, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Close, "Close Frame Pulse", tint = MutedText, modifier = Modifier.size(17.dp))
                        }
                    }

                    if (!listening) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IdeaVoiceLanguage.entries.forEach { option ->
                                FilterChip(
                                    selected = language == option,
                                    onClick = { language = option; error = null },
                                    label = { Text(option.displayLabel, fontSize = 7.7.sp, fontWeight = FontWeight.Bold) },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    FramePulseOrb(
                        active = listening,
                        rmsDb = rmsDb,
                        onTap = { if (listening) transcriber?.stop() else startVoiceFromOrb() },
                        modifier = Modifier.size(166.dp),
                    )
                    Text(if (listening) "Listening" else "Tap to listen", color = ProjectorIvory, fontSize = 12.8.sp, fontWeight = FontWeight.Black)
                    Text(if (listening) "Tap again to stop" else "Nothing is listening yet", color = MutedText, fontSize = 8.4.sp)

                    if (partial.isNotBlank()) {
                        Spacer(Modifier.height(9.dp))
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(13.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
                            Text(partial, Modifier.padding(11.dp), color = ProjectorIvory.copy(alpha=.90f), fontSize = 10.2.sp, lineHeight = 14.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                        }
                    }
                    error?.let { message ->
                        Spacer(Modifier.height(7.dp))
                        Text(message, color = RecRed, fontSize = 8.5.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Speech becomes text only. The app does not save the audio.", color = MutedText, fontSize = 7.6.sp)
                }
            }
        }
    }
}

@Composable
private fun FramePulseOrb(active: Boolean, rmsDb: Float, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val raw = if (active) ((rmsDb + 2f) / 12f).coerceIn(0f, 1f) else 0f
    val amplitude by animateFloatAsState(raw, spring(dampingRatio = .74f, stiffness = 135f), label = "voiceAmplitudeV3")
    val infinite = rememberInfiniteTransition(label = "voiceOrbV3")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(if (active) 5200 else 9800, easing = LinearEasing)), label = "orbSpinV3")
    val counterSpin by infinite.animateFloat(360f, 0f, infiniteRepeatable(tween(if (active) 7600 else 12600, easing = LinearEasing)), label = "orbCounterSpinV3")
    val breathe by infinite.animateFloat(if (active) .96f else .985f, if (active) 1.055f else 1.018f, infiniteRepeatable(tween(if (active) 1250 else 2100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "orbBreatheV3")

    Canvas(modifier.clickable(onClick = onTap)) {
        val c = center
        val base = size.minDimension * (.20f + amplitude * .045f)
        val halo = base * (if (active) 2.65f + amplitude * .68f else 2.25f)
        val energy = if (active) 1f else .48f

        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    RecRed.copy(alpha = (.30f + amplitude*.20f) * energy),
                    FrameTertiary.copy(alpha = (.22f + amplitude*.12f) * energy),
                    MutedGold.copy(alpha = .12f * energy),
                    Color.Transparent,
                ), c, halo,
            ), halo, c,
        )
        drawCircle(FrameTertiary.copy(alpha = .085f * energy), base*1.88f*breathe, c)
        drawCircle(RecRed.copy(alpha = .12f * energy), base*1.58f, c)
        drawCircle(MutedGold.copy(alpha = .085f * energy), base*1.34f*(2f-breathe), c)
        drawCircle(CinemaBlack.copy(alpha=.92f), base*1.05f, c)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    ProjectorIvory.copy(alpha = if (active) .66f + amplitude*.16f else .36f),
                    FrameTertiary.copy(alpha = if (active) .68f else .38f),
                    RecRed.copy(alpha = if (active) .88f else .48f),
                    RecRedDeep.copy(alpha = .98f),
                ),
                Offset(c.x-base*.30f, c.y-base*.34f), base*1.62f,
            ),
            radius = base*.88f*breathe,
            center = c,
        )

        val ringRadius = base*1.40f
        repeat(6) { index ->
            val r = ringRadius + index*5.2f
            val direction = if (index%2==0) spin else counterSpin
            val tint = when(index%3) { 0 -> MutedGold; 1 -> FrameTertiary; else -> RecRed }
            drawArc(
                tint.copy(alpha = (.36f + amplitude*.34f)*energy),
                startAngle = direction + index*59f,
                sweepAngle = 22f + index*6f + amplitude*32f,
                useCenter = false,
                topLeft = Offset(c.x-r, c.y-r),
                size = androidx.compose.ui.geometry.Size(r*2f, r*2f),
                style = Stroke(width = 1f + amplitude*1.7f),
            )
        }
        repeat(5) { i ->
            val angle = (spin + i*72f) * 0.017453292f
            val r = base*(1.10f+i*.11f)
            val point = Offset(c.x+kotlin.math.cos(angle)*r, c.y+kotlin.math.sin(angle)*r)
            drawCircle(ProjectorIvory.copy(alpha=(.26f+amplitude*.36f)*energy), 1.1f+amplitude*1.5f, point)
        }
        drawCircle(ProjectorIvory.copy(alpha = if (active) .94f else .54f), 1.7f+amplitude*1.9f, c)
    }
}

private fun listeningSubtitle(language: IdeaVoiceLanguage, detectedLanguage: String?): String {
    val detected = when {
        detectedLanguage?.startsWith("te", true) == true -> "Telugu"
        detectedLanguage?.startsWith("en", true) == true -> "English"
        else -> null
    }
    return when {
        detected != null && language == IdeaVoiceLanguage.AUTO -> "$detected detected · keep talking"
        language == IdeaVoiceLanguage.AUTO -> "Auto Telugu + English"
        else -> language.displayLabel
    }
}

private fun formatVoiceTime(totalSeconds: Int): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
''')

# -----------------------------------------------------------------------------
# Spotlight breath: preserve clear cutout but add a restrained living outline.
# -----------------------------------------------------------------------------
spotlight_path = p("app/src/main/java/com/framebynavin/app/ui/V20GuidedFirstRun.kt")
spotlight = spotlight_path.read_text()
spotlight = spotlight.replace('import androidx.compose.animation.core.FastOutSlowInEasing\nimport androidx.compose.animation.core.tween\n', 'import androidx.compose.animation.core.*\n')
if 'spotlightBreath' not in spotlight:
    old = '''    val placeAtTop = step == CreatorGuidedTourStep.CONTROL
    val pointRight = step.ordinal % 2 == 0
    val spot = spotlightFor(step)

    Box(Modifier.fillMaxSize()) {
'''
    new = '''    val placeAtTop = step == CreatorGuidedTourStep.CONTROL
    val pointRight = step.ordinal % 2 == 0
    val spot = spotlightFor(step)
    val spotlightMotion = rememberInfiniteTransition(label = "spotlightBreath")
    val spotlightBreath by spotlightMotion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spotlightBreathValue",
    )
    val spotX by animateFloatAsState(spot.x, tween(360, easing = FastOutSlowInEasing), label = "spotX")
    val spotY by animateFloatAsState(spot.y, tween(360, easing = FastOutSlowInEasing), label = "spotY")
    val spotW by animateFloatAsState(spot.width, tween(360, easing = FastOutSlowInEasing), label = "spotW")
    val spotH by animateFloatAsState(spot.height, tween(360, easing = FastOutSlowInEasing), label = "spotH")

    Box(Modifier.fillMaxSize()) {
'''
    if old not in spotlight:
        raise SystemExit('Spotlight insertion anchor missing')
    spotlight = spotlight.replace(old, new, 1)
    old_canvas = '''            drawRect(CinemaBlack.copy(alpha = .76f))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(size.width * spot.x, size.height * spot.y),
                size = Size(size.width * spot.width, size.height * spot.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(spot.corner, spot.corner),
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = RecRed.copy(alpha = .55f),
                topLeft = Offset(size.width * spot.x, size.height * spot.y),
                size = Size(size.width * spot.width, size.height * spot.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(spot.corner, spot.corner),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.2f),
            )
'''
    new_canvas = '''            drawRect(CinemaBlack.copy(alpha = .76f))
            val breathing = 2.5f + spotlightBreath * 3.5f
            val left = size.width * spotX - breathing
            val top = size.height * spotY - breathing
            val width = size.width * spotW + breathing * 2f
            val height = size.height * spotH + breathing * 2f
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(spot.corner + breathing, spot.corner + breathing),
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = RecRed.copy(alpha = .34f + spotlightBreath * .28f),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(spot.corner + breathing, spot.corner + breathing),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f + spotlightBreath * 1.2f),
            )
'''
    if old_canvas not in spotlight:
        raise SystemExit('Spotlight canvas anchor missing')
    spotlight = spotlight.replace(old_canvas, new_canvas, 1)
spotlight_path.write_text(spotlight)

# -----------------------------------------------------------------------------
# Welcome threads: explicit 25% inter-thread spacing scale.
# -----------------------------------------------------------------------------
welcome_path = p("app/src/main/java/com/framebynavin/app/ui/V174CinematicWelcome.kt")
welcome = welcome_path.read_text()
if 'V129_THREAD_GAP_SCALE' not in welcome:
    welcome = welcome.replace('private const val V20_WELCOME_STRIPE_COUNT = 30\n', 'private const val V20_WELCOME_STRIPE_COUNT = 30\nprivate const val V129_THREAD_GAP_SCALE = 1.25f\n')
    welcome = welcome.replace(
        '                val startX = size.width * 0.50f + side * size.width * 0.025f\n                val endX = size.width * 0.50f + side * size.width * (0.50f + lane * 0.10f)\n',
        '                val startX = size.width * 0.50f + side * size.width * 0.025f * V129_THREAD_GAP_SCALE\n                val endX = size.width * 0.50f + side * size.width * (0.50f + lane * 0.10f * V129_THREAD_GAP_SCALE)\n',
    )
welcome_path.write_text(welcome)

# -----------------------------------------------------------------------------
# Lumen Flow backdrop gives the theme its own visual composition, not a recolor.
# -----------------------------------------------------------------------------
p("app/src/main/java/com/framebynavin/app/ui/V129ThemeBackdrop.kt").write_text(r'''package com.framebynavin.app.ui

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
''')

# -----------------------------------------------------------------------------
# Android Calendar Provider integration.
# -----------------------------------------------------------------------------
p("app/src/main/java/com/framebynavin/app/ui/V129GoogleCalendarSettings.kt").write_text(r'''package com.framebynavin.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.*
import java.util.TimeZone

data class V129CalendarOption(val id: Long, val name: String, val account: String)

internal object CreatorCalendarSync {
    private const val PREFS = "framebynavin_google_calendar"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_CALENDAR_ID = "calendar_id"
    private const val EVENT_PREFIX = "event_"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun isEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
    fun setEnabled(context: Context, enabled: Boolean) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    fun selectedCalendarId(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_CALENDAR_ID, -1L)
    fun selectCalendar(context: Context, id: Long) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_CALENDAR_ID, id).apply()

    fun googleCalendars(context: Context): List<V129CalendarOption> {
        if (!hasPermission(context)) return emptyList()
        val result = mutableListOf<V129CalendarOption>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
        )
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.ACCOUNT_TYPE}=? AND ${CalendarContract.Calendars.VISIBLE}=1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=?",
            arrayOf("com.google", CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                result += V129CalendarOption(cursor.getLong(idI), cursor.getString(nameI).orEmpty(), cursor.getString(accountI).orEmpty())
            }
        }
        return result
    }

    fun syncIfEnabled(context: Context, tasks: List<CreatorTask>): Int {
        if (!isEnabled(context) || !hasPermission(context)) return 0
        val calendarId = selectedCalendarId(context)
        if (calendarId <= 0L) return 0
        return sync(context, tasks, calendarId)
    }

    fun sync(context: Context, tasks: List<CreatorTask>, calendarId: Long): Int {
        if (!hasPermission(context) || calendarId <= 0L) return 0
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var count = 0
        tasks.filter { it.dueAtMillis > 0L && it.status != TaskStatus.SKIPPED }.forEach { task ->
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "Creator project · ${task.title}")
                put(CalendarContract.Events.DESCRIPTION, listOf(task.platform, task.contentType, task.notes).filter { it.isNotBlank() }.joinToString(" · "))
                put(CalendarContract.Events.DTSTART, task.dueAtMillis)
                put(CalendarContract.Events.DTEND, task.dueAtMillis + 30L * 60_000L)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 0)
            }
            val key = EVENT_PREFIX + task.id
            val existing = prefs.getLong(key, -1L)
            if (existing > 0L) {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existing)
                if (context.contentResolver.update(uri, values, null, null) > 0) count++
            } else {
                val created = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                val id = created?.let(ContentUris::parseId) ?: -1L
                if (id > 0L) {
                    prefs.edit().putLong(key, id).apply()
                    count++
                }
            }
        }
        return count
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V129GoogleCalendarSettings(tasks: List<CreatorTask>) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var enabled by remember { mutableStateOf(CreatorCalendarSync.isEnabled(context)) }
    var selectedId by remember { mutableLongStateOf(CreatorCalendarSync.selectedCalendarId(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    val granted = remember(revision) { CreatorCalendarSync.hasPermission(context) }
    val calendars = remember(revision, granted) { CreatorCalendarSync.googleCalendars(context) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        revision++
        message = if (result.values.all { it }) "Calendar access ready." else "Calendar access was not granted."
    }

    LaunchedEffect(calendars, selectedId) {
        if (granted && calendars.isNotEmpty() && calendars.none { it.id == selectedId }) {
            selectedId = calendars.first().id
            CreatorCalendarSync.selectCalendar(context, selectedId)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("GOOGLE CALENDAR", color = MutedGold, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(5.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(38.dp), RoundedCornerShape(12.dp), RecRed.copy(alpha=.12f)) {
                        Box(contentAlignment=Alignment.Center) { Icon(Icons.Outlined.CalendarMonth, null, tint=RecRed, modifier=Modifier.size(20.dp)) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Project deadlines in Google Calendar", color=ProjectorIvory, fontSize=11.5.sp, fontWeight=FontWeight.Bold)
                        Text("Your in-app reminders stay separate.", color=MutedText, fontSize=8.5.sp)
                    }
                    if (granted && calendars.isNotEmpty()) {
                        Switch(
                            checked=enabled,
                            onCheckedChange={ value -> enabled=value; CreatorCalendarSync.setEnabled(context,value); if (value) message="Sync is on." },
                            colors=SwitchDefaults.colors(checkedTrackColor=RecRed),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (!granted) {
                    Button(
                        onClick={ permissions.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) },
                        modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(containerColor=RecRed),
                        shape=RoundedCornerShape(13.dp),
                    ) { Text("CONNECT GOOGLE CALENDAR", fontSize=9.sp, fontWeight=FontWeight.Black) }
                } else if (calendars.isEmpty()) {
                    Text("No writable Google Calendar is synced on this phone. Add or enable one in Android Calendar, then return here.", color=MutedText, fontSize=8.8.sp, lineHeight=13.sp)
                } else {
                    Text("Calendar", color=ProjectorIvory, fontSize=9.5.sp, fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        calendars.forEach { option ->
                            FilterChip(
                                selected=selectedId==option.id,
                                onClick={ selectedId=option.id; CreatorCalendarSync.selectCalendar(context, option.id); message=null },
                                label={ Text(option.name.ifBlank { option.account }, fontSize=8.sp, maxLines=1) },
                            )
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    OutlinedButton(
                        onClick={
                            CreatorCalendarSync.selectCalendar(context, selectedId)
                            val count=CreatorCalendarSync.sync(context, tasks, selectedId)
                            message = if (count > 0) "$count project deadline${if (count==1) "" else "s"} synced." else "Nothing new to sync."
                        },
                        modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(13.dp),
                        border=BorderStroke(1.dp, CinemaLine),
                    ) { Text("SYNC NOW", color=ProjectorIvory, fontSize=9.sp, fontWeight=FontWeight.Black) }
                }
                message?.let { Spacer(Modifier.height(7.dp)); Text(it, color=if (it.contains("not granted")) RecRed else MutedGold, fontSize=8.4.sp) }
            }
        }
    }
}
''')

# Calendar manifest permissions.
manifest_path = p("app/src/main/AndroidManifest.xml")
manifest = manifest_path.read_text()
if 'android.permission.READ_CALENDAR' not in manifest:
    manifest = manifest.replace(
        '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n',
        '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n    <uses-permission android:name="android.permission.READ_CALENDAR" />\n    <uses-permission android:name="android.permission.WRITE_CALENDAR" />\n',
    )
manifest_path.write_text(manifest)

# -----------------------------------------------------------------------------
# App shell: Lumen backdrop, dynamic dock, calendar settings, automatic sync.
# -----------------------------------------------------------------------------
app_path = p("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")
app = app_path.read_text()
app = app.replace('import kotlinx.coroutines.delay\n', 'import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.delay\nimport kotlinx.coroutines.withContext\n')
if 'CreatorCalendarSync.syncIfEnabled' not in app:
    anchor = '''    LaunchedEffect(controlExpanded) {
        if (controlExpanded) {
            delay(180L)
            showControl = true
            controlExpanded = false
        }
    }
'''
    block = anchor + '''
    LaunchedEffect(vm.tasks) {
        if (CreatorCalendarSync.isEnabled(context)) {
            withContext(Dispatchers.IO) { CreatorCalendarSync.syncIfEnabled(context.applicationContext, vm.tasks) }
        }
    }
'''
    if anchor not in app:
        raise SystemExit('Calendar auto-sync anchor missing')
    app = app.replace(anchor, block, 1)

app = app.replace(
    '    Box(Modifier.fillMaxSize().background(CinemaBlack)) {\n',
    '    Box(Modifier.fillMaxSize().background(CinemaBlack)) {\n        V129ThemeBackdrop(Modifier.matchParentSize())\n',
    1,
)
app = app.replace(
    '                settings = settings,\n                weeklyAutoPlanEnabled = vm.weeklyAutoPlanEnabled,\n                permissions = permissions,\n',
    '                settings = settings,\n                tasks = vm.tasks,\n                weeklyAutoPlanEnabled = vm.weeklyAutoPlanEnabled,\n                permissions = permissions,\n',
    1,
)
app = app.replace(
    'private fun PSettingsScreen(\n    settings: CreatorOsSettings,\n    weeklyAutoPlanEnabled: Boolean,\n',
    'private fun PSettingsScreen(\n    settings: CreatorOsSettings,\n    tasks: List<CreatorTask>,\n    weeklyAutoPlanEnabled: Boolean,\n',
    1,
)
if 'V129GoogleCalendarSettings(tasks)' not in app:
    app = app.replace(
        '            Spacer(Modifier.height(22.dp))\n            V127AppearanceSettings()\n',
        '            Spacer(Modifier.height(22.dp))\n            V129GoogleCalendarSettings(tasks)\n\n            Spacer(Modifier.height(22.dp))\n            V127AppearanceSettings()\n',
        1,
    )
# New custom theme changes composition of the dock as well as palette.
app = app.replace(
    '        Color(0xF2161618),\n',
    '        if (VisualExperiencePrefs.isLumen) CinemaSurface.copy(alpha = .88f) else Color(0xF2161618),\n',
    1,
)
app = app.replace(
    '        color = if (active) Color(0xFF282326) else Color.Transparent,\n',
    '        color = if (active) { if (VisualExperiencePrefs.isLumen) FrameTertiary.copy(alpha=.16f) else CinemaSurfaceRaised } else Color.Transparent,\n',
    1,
)
app_path.write_text(app)

# -----------------------------------------------------------------------------
# Appearance card copy + Lumen preview strip.
# -----------------------------------------------------------------------------
appearance_path = p("app/src/main/java/com/framebynavin/app/ui/V127AppearanceSettings.kt")
appearance = appearance_path.read_text()
appearance = appearance.replace('import androidx.compose.foundation.background\n', 'import androidx.compose.foundation.background\nimport androidx.compose.ui.graphics.Brush\n')
appearance = appearance.replace('Text("Choose your FrameByNavin atmosphere.", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)', 'Text("Choose your visual atmosphere.", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)')
if 'theme == FrameTheme.LUMEN_FLOW' not in appearance:
    appearance = appearance.replace(
        '                        Spacer(Modifier.weight(1f))\n                        Text(theme.displayName, color = palette.foreground, fontSize = 11.sp, fontWeight = FontWeight.Black)\n',
        '                        Spacer(Modifier.weight(1f))\n                        if (theme == FrameTheme.LUMEN_FLOW) {\n                            Box(Modifier.fillMaxWidth().height(14.dp).background(Brush.horizontalGradient(listOf(palette.primary, palette.tertiary, palette.secondary)), RoundedCornerShape(100.dp)))\n                            Spacer(Modifier.height(6.dp))\n                        }\n                        Text(theme.displayName, color = palette.foreground, fontSize = 11.sp, fontWeight = FontWeight.Black)\n',
        1,
    )
appearance_path.write_text(appearance)

# -----------------------------------------------------------------------------
# Widgets: remove retired Ivory and give Lumen its own background.
# -----------------------------------------------------------------------------
widget_path = p("app/src/main/java/com/framebynavin/app/widget/CreatorWidgetUpdater.kt")
widget = widget_path.read_text()
widget = widget.replace('        FrameTheme.IVORY_STUDIO -> R.drawable.widget_bg_ivory\n', '        FrameTheme.LUMEN_FLOW -> R.drawable.widget_bg_lumen\n')
widget_path.write_text(widget)
p("app/src/main/res/drawable/widget_bg_lumen.xml").write_text('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="315" android:startColor="#241126" android:centerColor="#111827" android:endColor="#0D2A2B" android:type="linear" />
    <corners android:radius="24dp" />
    <stroke android:width="1dp" android:color="#665A6A8A" />
</shape>
''')

# -----------------------------------------------------------------------------
# Theme regression test now reflects the dark-first six-theme v129 contract.
# -----------------------------------------------------------------------------
test_path = p("app/src/test/java/com/framebynavin/app/ui/theme/VisualExperienceV127Test.kt")
if test_path.exists():
    test_path.write_text(r'''package com.framebynavin.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualExperienceV127Test {
    @Test
    fun `v129 ships six distinct selectable themes without ivory`() {
        assertEquals(6, FrameTheme.entries.size)
        assertEquals(6, FrameTheme.entries.map { it.displayName }.distinct().size)
        assertEquals(6, FrameTheme.entries.map { it.palette.primary }.distinct().size)
        assertTrue(FrameTheme.entries.none { it.name == "IVORY_STUDIO" })
    }

    @Test
    fun `every palette preserves readable semantic separation`() {
        FrameTheme.entries.forEach { theme ->
            assertNotEquals(theme.palette.background, theme.palette.foreground)
            assertNotEquals(theme.palette.surface, theme.palette.primary)
            assertNotEquals(theme.palette.primary, theme.palette.secondary)
        }
    }

    @Test
    fun `v129 theme system is dark first and lumen is structurally distinct`() {
        FrameTheme.entries.forEach { assertFalse(it.palette.isLight) }
        assertEquals(FrameSurfacePersonality.LUMEN, FrameTheme.LUMEN_FLOW.palette.surfacePersonality)
        assertEquals(FrameSurfacePersonality.GLASS, FrameTheme.AURORA_GLASS.palette.surfacePersonality)
    }
}
''')

print("v129 Cine Pulse integration patch applied")
