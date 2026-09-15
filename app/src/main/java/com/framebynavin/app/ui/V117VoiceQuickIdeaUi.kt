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
