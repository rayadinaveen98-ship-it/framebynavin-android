package com.framebynavin.app.ui

import android.Manifest
import android.animation.ValueAnimator
import android.app.ActivityManager
import android.os.PowerManager
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

    var dotFieldOpen by remember { mutableStateOf(false) }
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
        if (granted && pendingPermissionStart && dotFieldOpen) {
            error = null
            partial = ""
            transcriber?.start(language)
        } else if (!granted) {
            error = "Microphone access is needed only when you tap the dots to listen."
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

    fun startVoiceFromField() {
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

    fun closeDotField() {
        if (listening) transcriber?.stop()
        pendingPermissionStart = false
        dotFieldOpen = false
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
            targetState = dotFieldOpen,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(170)) },
            label = "voiceDotFieldOpenState",
        ) { opened ->
            if (!opened) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = { dotFieldOpen = true; error = null },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = RecRed.copy(alpha = .16f), contentColor = RecRed),
                            modifier = Modifier.size(46.dp),
                        ) { Icon(Icons.Outlined.Mic, "Open voice capture", modifier = Modifier.size(23.dp)) }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Voice capture", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("Open the dot field to dictate.", color = MutedText, fontSize = 8.7.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            Text("VOICE INPUT", color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
                            Text(if (listening) listeningSubtitle(language, detectedLanguage) else "Ready · tap the dots to listen", color = MutedText, fontSize = 8.5.sp)
                        }
                        Text(if (listening) formatVoiceTime(elapsedSeconds) else "READY", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = ::closeDotField, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.Close, "Close voice input", tint = MutedText, modifier = Modifier.size(17.dp))
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
                    FC13ReactiveDotField(
                        active = listening,
                        rmsDb = rmsDb,
                        onTap = { if (listening) transcriber?.stop() else startVoiceFromField() },
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
                    Text("Your words become text. No audio recording is saved.", color = MutedText, fontSize = 7.6.sp)
                }
            }
        }
    }
}

/**
 * FC13: real microphone RMS drives a field of individual dots, not a rotating
 * orb. No recorder, external analyzer, or audio persistence is added.
 * Idle is calm; speaking pushes the dots outward and creates a subtle wave.
 */
@Composable
internal fun FC13ReactiveDotField(
    active: Boolean,
    rmsDb: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Shared by Dictate and Spark conversation. The OS animation and device
    // power settings control presentation only, never microphone/listening.
    val motionAllowed = remember {
        ValueAnimator.areAnimatorsEnabled()
    }
    val lowPower = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true ||
            context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val reactiveMotion = motionAllowed && !lowPower
    val target = if (active) ((rmsDb + 2f) / 12f).coerceIn(0f, 1f) else 0f
    val amplitude by animateFloatAsState(
        if (reactiveMotion) target else 0f,
        spring(dampingRatio = .8f, stiffness = 170f),
        label = "fc13RealMicIntensity",
    )
    val motion = if (reactiveMotion) {
        val transition = rememberInfiniteTransition(label = "fc13DotMotion")
        val phase by transition.animateFloat(
            0f, 6.2831853f,
            infiniteRepeatable(
                tween(if (active) 2400 else 7600, easing = LinearEasing),
            ),
            label = "fc13WavePhase",
        )
        phase
    } else 0f
    Canvas(
        modifier
            .semantics {
                contentDescription = if (active) "Stop voice listening" else "Start voice listening"
            }
            .clickable(
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = onTap,
            ),
    ) {
        val c = center
        val spacing = size.minDimension / 14.5f
        val intensity = if (active && reactiveMotion) amplitude else 0f
        val bloom = spacing * (6.15f + 0.45f * intensity)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    RecRed.copy(alpha = .055f + intensity * .11f),
                    RecRedDeep.copy(alpha = .055f + intensity * .065f),
                    Color.Transparent,
                ),
                center = c,
                radius = bloom,
            ),
            radius = bloom,
            center = c,
        )
        // Low amplitude = precise grid. Real speech energy expands local dots
        // and produces a visible traveling wave, without photosensitivity-
        // inducing flashes or rapidly changing full-screen colors.
        for (y in -6..6) {
            for (x in -6..6) {
                val horizontal = x.toFloat()
                val vertical = y.toFloat()
                val distance = kotlin.math.sqrt(
                    horizontal * horizontal + vertical * vertical,
                )
                if (distance > 6.15f) continue
                val attenuation = (1f - distance / 8f).coerceIn(.15f, 1f)
                val traveling = (
                    kotlin.math.sin(distance * 1.35f - motion * 1.2f) + 1f
                ) * .5f
                val reaction = intensity * traveling * attenuation
                val offset = reaction * spacing * .24f
                val xSign = if (distance > .001f) horizontal / distance else 0f
                val ySign = if (distance > .001f) vertical / distance else 0f
                val dotCenter = Offset(
                    c.x + horizontal * spacing + xSign * offset,
                    c.y + vertical * spacing + ySign * offset,
                )
                val light = (attenuation * (.45f + reaction * .55f))
                    .coerceIn(.20f, 1f)
                val radius = (spacing * (.125f + reaction * .145f))
                    .coerceAtMost(spacing * .33f)
                val dotColor = if ((x + y) % 4 == 0) ProjectorIvory else RecRed
                drawCircle(
                    color = dotColor.copy(alpha = light),
                    radius = radius,
                    center = dotCenter,
                )
            }
        }
        if (active) {
            drawCircle(
                color = RecRed.copy(alpha = .13f + intensity * .20f),
                radius = spacing * (3.4f + intensity * 1.55f),
                center = c,
                style = Stroke(width = 1f + intensity * 1.5f),
            )
        }
        drawCircle(
            color = ProjectorIvory.copy(alpha = .85f),
            radius = spacing * (.14f + intensity * .11f),
            center = c,
        )
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
