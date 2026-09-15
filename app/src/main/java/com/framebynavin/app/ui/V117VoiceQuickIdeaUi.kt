package com.framebynavin.app.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

/** Compact voice capture used by both in-app Quick Capture and the Quick Idea widget activity. */
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
    var transcriber by remember { mutableStateOf<IdeaVoiceTranscriber?>(null) }
    var pendingPermissionStart by remember { mutableStateOf(false) }
    var microphoneGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        microphoneGranted = granted
        if (granted && pendingPermissionStart) {
            error = null
            partial = ""
            transcriber?.start(language)
        } else if (!granted) {
            error = "Allow microphone access to capture an idea by voice."
        }
        pendingPermissionStart = false
    }

    DisposableEffect(context) {
        val engine = IdeaVoiceTranscriber(
            context = context.applicationContext,
            listener = object : IdeaVoiceTranscriberListener {
                override fun onListeningChanged(value: Boolean) {
                    listening = value
                    if (!value) {
                        partial = ""
                        rmsDb = 0f
                    }
                }
                override fun onPartialTranscript(text: String) { partial = text }
                override fun onFinalTranscript(text: String, confidence: Float?) {
                    if (text.isNotBlank()) latestOnTranscript(text)
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

    fun startVoice() {
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, if (listening) RecRed.copy(alpha = .48f) else CinemaLine),
    ) {
        AnimatedContent(
            targetState = listening,
            transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(170)) },
            label = "voiceOrbState",
        ) { isListening ->
            if (isListening) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("FRAME PULSE", color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
                            Text(listeningSubtitle(language, detectedLanguage), color = MutedText, fontSize = 8.5.sp)
                        }
                        Text(formatVoiceTime(elapsedSeconds), color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(5.dp))
                    FramePulseOrb(
                        rmsDb = rmsDb,
                        onStop = { transcriber?.stop() },
                        modifier = Modifier.size(154.dp),
                    )
                    Text("Listening", color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                    Text("Tap the orb to stop", color = MutedText, fontSize = 8.3.sp)

                    if (partial.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp),
                            color = CinemaSurfaceRaised,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Text(
                                partial,
                                modifier = Modifier.padding(11.dp),
                                color = ProjectorIvory.copy(alpha = .90f),
                                fontSize = 10.2.sp,
                                lineHeight = 14.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    error?.let { message ->
                        Spacer(Modifier.height(7.dp))
                        Text(message, color = RecRed, fontSize = 8.5.sp, lineHeight = 12.sp, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Speech becomes text only. FrameByNavin does not save the audio.", color = MutedText, fontSize = 7.6.sp)
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = ::startVoice,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = RecRed.copy(alpha = .16f), contentColor = RecRed),
                            modifier = Modifier.size(46.dp),
                        ) {
                            Icon(Icons.Outlined.Mic, "Start voice capture", modifier = Modifier.size(23.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Speak your idea", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (language == IdeaVoiceLanguage.AUTO) "Telugu + English · tap and talk naturally" else "${language.displayLabel} voice typing",
                                color = MutedText,
                                fontSize = 8.7.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IdeaVoiceLanguage.entries.forEach { option ->
                            FilterChip(
                                selected = language == option,
                                onClick = { language = option; error = null },
                                label = { Text(option.displayLabel, fontSize = 7.8.sp, fontWeight = FontWeight.Bold) },
                            )
                        }
                    }
                    error?.let { message ->
                        Spacer(Modifier.height(7.dp))
                        Text(message, color = RecRed, fontSize = 8.5.sp, lineHeight = 12.sp)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("Audio is not saved.", color = MutedText, fontSize = 7.7.sp)
                }
            }
        }
    }
}

@Composable
private fun FramePulseOrb(rmsDb: Float, onStop: () -> Unit, modifier: Modifier = Modifier) {
    val raw = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
    val amplitude by animateFloatAsState(raw, spring(dampingRatio = .72f, stiffness = 155f), label = "voiceAmplitudeV2")
    val infinite = rememberInfiniteTransition(label = "voiceOrbV2")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(6200, easing = LinearEasing)), label = "orbSpinV2")
    val counterSpin by infinite.animateFloat(360f, 0f, infiniteRepeatable(tween(8800, easing = LinearEasing)), label = "orbCounterSpin")
    val breathe by infinite.animateFloat(.96f, 1.045f, infiniteRepeatable(tween(1450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "orbBreatheV2")

    Canvas(modifier.clickable(onClick = onStop)) {
        val c = center
        val base = size.minDimension * (.20f + amplitude * .040f)
        val halo = base * (2.35f + amplitude * .60f)

        // Atmospheric bloom.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    RecRed.copy(alpha = .34f + amplitude * .18f),
                    FrameTertiary.copy(alpha = .18f + amplitude * .12f),
                    MutedGold.copy(alpha = .10f),
                    Color.Transparent,
                ),
                center = c,
                radius = halo,
            ),
            radius = halo,
            center = c,
        )

        // Fluid translucent shells.
        drawCircle(FrameTertiary.copy(alpha = .075f + amplitude * .07f), radius = base * 1.78f * breathe, center = c)
        drawCircle(RecRed.copy(alpha = .10f + amplitude * .10f), radius = base * 1.53f, center = c)
        drawCircle(MutedGold.copy(alpha = .07f + amplitude * .06f), radius = base * 1.30f * (2f - breathe), center = c)

        // Dark optical cavity + luminous inner field.
        drawCircle(CinemaBlack.copy(alpha = .90f), radius = base * 1.03f, center = c)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    ProjectorIvory.copy(alpha = .55f + amplitude * .18f),
                    FrameTertiary.copy(alpha = .58f),
                    RecRed.copy(alpha = .82f),
                    RecRedDeep.copy(alpha = .98f),
                ),
                center = Offset(c.x - base * .27f, c.y - base * .31f),
                radius = base * 1.55f,
            ),
            radius = base * .86f,
            center = c,
        )

        // Thin premium light filaments.
        val ringRadius = base * 1.36f
        repeat(5) { index ->
            val r = ringRadius + index * 5.5f
            val direction = if (index % 2 == 0) spin else counterSpin
            val tint = when (index % 3) {
                0 -> MutedGold
                1 -> FrameTertiary
                else -> RecRed
            }
            drawArc(
                color = tint.copy(alpha = .45f + amplitude * .28f),
                startAngle = direction + index * 71f,
                sweepAngle = 24f + index * 7f + amplitude * 28f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = androidx.compose.ui.geometry.Size(r * 2f, r * 2f),
                style = Stroke(width = 1.1f + amplitude * 1.4f),
            )
        }

        // Small floating glints give depth without particle noise.
        repeat(4) { i ->
            val angle = (spin + i * 90f) * 0.017453292f
            val r = base * (1.12f + i * .10f)
            val point = Offset(c.x + kotlin.math.cos(angle) * r, c.y + kotlin.math.sin(angle) * r)
            drawCircle(ProjectorIvory.copy(alpha = .38f + amplitude * .30f), radius = 1.2f + amplitude * 1.4f, center = point)
        }
        drawCircle(ProjectorIvory.copy(alpha = .90f), radius = 1.8f + amplitude * 1.8f, center = c)
    }
}

private fun listeningSubtitle(language: IdeaVoiceLanguage, detectedLanguage: String?): String {
    val detected = when {
        detectedLanguage?.startsWith("te", ignoreCase = true) == true -> "Telugu"
        detectedLanguage?.startsWith("en", ignoreCase = true) == true -> "English"
        else -> null
    }
    return when {
        detected != null && language == IdeaVoiceLanguage.AUTO -> "$detected detected · keep talking"
        language == IdeaVoiceLanguage.AUTO -> "Auto Telugu + English"
        else -> language.displayLabel
    }
}

private fun formatVoiceTime(totalSeconds: Int): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
