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
    val amplitude by animateFloatAsState(raw, spring(dampingRatio = .58f, stiffness = 260f), label = "voiceAmplitude")
    val infinite = rememberInfiniteTransition(label = "voiceOrb")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "orbSpin")
    val breathe by infinite.animateFloat(.92f, 1.06f, infiniteRepeatable(tween(1150, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "orbBreathe")

    Canvas(modifier.clickable(onClick = onStop)) {
        val c = center
        val base = size.minDimension * (.205f + amplitude * .035f)
        val halo = base * (2.05f + amplitude * .55f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    RecRed.copy(alpha = .30f + amplitude * .22f),
                    MutedGold.copy(alpha = .13f + amplitude * .10f),
                    Color.Transparent,
                ),
                center = c,
                radius = halo,
            ),
            radius = halo,
            center = c,
        )

        drawCircle(RecRed.copy(alpha = .14f + amplitude * .16f), radius = base * 1.55f * breathe, center = c)
        drawCircle(MutedGold.copy(alpha = .09f + amplitude * .10f), radius = base * 1.23f, center = c)
        drawCircle(CinemaBlack.copy(alpha = .92f), radius = base, center = c)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(ProjectorIvory.copy(alpha = .40f + amplitude * .25f), RecRed.copy(alpha = .75f), RecRedDeep.copy(alpha = .96f)),
                center = Offset(c.x - base * .24f, c.y - base * .30f),
                radius = base * 1.4f,
            ),
            radius = base * .82f,
            center = c,
        )

        val ringRadius = base * 1.38f
        repeat(3) { index ->
            drawArc(
                color = if (index % 2 == 0) MutedGold.copy(alpha = .72f) else RecRed.copy(alpha = .86f),
                startAngle = spin * (if (index % 2 == 0) 1f else -1f) + index * 112f,
                sweepAngle = 48f + amplitude * 46f,
                useCenter = false,
                topLeft = Offset(c.x - ringRadius - index * 5f, c.y - ringRadius - index * 5f),
                size = androidx.compose.ui.geometry.Size((ringRadius + index * 5f) * 2f, (ringRadius + index * 5f) * 2f),
                style = Stroke(width = 2.2f + amplitude * 2.5f),
            )
        }
        drawCircle(ProjectorIvory.copy(alpha = .82f), radius = 2.2f + amplitude * 2.3f, center = c)
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
