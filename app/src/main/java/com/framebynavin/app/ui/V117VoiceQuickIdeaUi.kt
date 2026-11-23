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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
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

/** P8B single-step dictate. The final transcript goes to the owner's ONE idea field. */
@Composable
internal fun V117VoiceIdeaInput(
    onTranscript: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestOnTranscript by rememberUpdatedState(onTranscript)

    var language by remember { mutableStateOf(IdeaVoiceLanguage.AUTO) }
    var listening by remember { mutableStateOf(false) }
    var partial by remember { mutableStateOf("") }
    var detectedLanguage by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var rmsDb by remember { mutableFloatStateOf(0f) }
    var justAdded by remember { mutableStateOf(false) }
    var transcriber by remember { mutableStateOf<IdeaVoiceTranscriber?>(null) }
    var pendingPermissionStart by remember { mutableStateOf(false) }
    var microphoneGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context,
            Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        microphoneGranted = granted
        if (granted && pendingPermissionStart) {
            error = null
            partial = ""
            transcriber?.start(language)
        } else if (!granted) {
            error = "Microphone access is needed to dictate an idea."
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
                override fun onPartialTranscript(text: String) {
                    partial = text
                }
                override fun onFinalTranscript(text: String, confidence: Float?) {
                    if (text.isNotBlank()) {
                        latestOnTranscript(text)
                        justAdded = true
                    }
                    partial = ""
                }
                override fun onDetectedLanguage(languageTag: String?) {
                    detectedLanguage = languageTag
                }
                override fun onVoiceError(message: String) {
                    error = message
                    justAdded = false
                }
                override fun onRmsChanged(rmsDbValue: Float) {
                    rmsDb = rmsDbValue
                }
            },
        )
        transcriber = engine
        onDispose {
            pendingPermissionStart = false
            engine.destroy()
            transcriber = null
        }
    }

    LaunchedEffect(listening) {
        elapsedSeconds = 0
        while (listening) {
            delay(1_000L)
            elapsedSeconds += 1
        }
    }
    LaunchedEffect(justAdded) {
        if (justAdded) {
            delay(1700L)
            justAdded = false
        }
    }

    fun startVoiceFromOrb() {
        error = null
        partial = ""
        justAdded = false
        detectedLanguage = null
        rmsDb = 0f
        if (microphoneGranted) {
            transcriber?.start(language)
        } else {
            pendingPermissionStart = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("p8b-dictation-panel"),
        shape = RoundedCornerShape(19.dp),
        color = CinemaSurface,
        border = BorderStroke(
            1.dp, if (listening) RecRed.copy(alpha = .62f) else CinemaLine,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SPEAK TO TEXT", color = RecRed, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Text(
                        if (listening) listeningSubtitle(language, detectedLanguage)
                        else "Your voice becomes the idea above.",
                        color = MutedText, fontSize = 11.sp, lineHeight = 15.sp,
                    )
                }
                Text(if (listening) formatVoiceTime(elapsedSeconds) else "READY",
                    color = ProjectorIvory, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold)
            }

            if (!listening) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IdeaVoiceLanguage.entries.forEach { option ->
                        FilterChip(
                            selected = language == option,
                            onClick = { language = option; error = null },
                            modifier = Modifier.padding(end = 6.dp),
                            label = {
                                Text(option.displayLabel, fontSize = 11.sp)
                            },
                        )
                    }
                }
            }
            P8BLivingVoiceOrb(
                listening = listening,
                rmsDb = rmsDb,
                onTap = {
                    if (listening) transcriber?.stop()
                    else if (!pendingPermissionStart) startVoiceFromOrb()
                },
                modifier = Modifier.size(196.dp).testTag("p8b-living-voice-orb"),
            )
            Text(
                when {
                    error != null -> "Voice needs attention"
                    listening -> "Listening · tap the orb to stop"
                    justAdded -> "Added to your idea"
                    pendingPermissionStart -> "Waiting for microphone permission"
                    else -> "Tap the breathing orb to speak"
                },
                color = if (error == null) ProjectorIvory else RecRed,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )

            // No second editable or bordered transcript box. The partial
            // text is a small non-editable listening hint; final words flow
            // through onTranscript into the parent field only.
            if (partial.isNotBlank()) {
                Text("Hearing: " + partial, color = MutedText,
                    fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("p8b-live-words-hint"))
            }
            error?.let { message ->
                Text(message, color = RecRed, fontSize = 11.sp,
                    lineHeight = 16.sp, textAlign = TextAlign.Center)
            }
            Text(
                "Dictation writes text into your idea. It does not save audio.",
                color = MutedText, fontSize = 10.sp, lineHeight = 14.sp,
                textAlign = TextAlign.Center,
            )
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
