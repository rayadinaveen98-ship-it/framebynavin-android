package com.framebynavin.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    var transcriber by remember { mutableStateOf<IdeaVoiceTranscriber?>(null) }
    var pendingPermissionStart by remember { mutableStateOf(false) }
    var microphoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
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
                    if (!value) partial = ""
                }

                override fun onPartialTranscript(text: String) {
                    partial = text
                }

                override fun onFinalTranscript(text: String, confidence: Float?) {
                    if (text.isNotBlank()) latestOnTranscript(text)
                }

                override fun onDetectedLanguage(languageTag: String?) {
                    detectedLanguage = languageTag
                }

                override fun onVoiceError(message: String) {
                    error = message
                }
            },
        )
        transcriber = engine
        onDispose {
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

    fun startVoice() {
        error = null
        partial = ""
        detectedLanguage = null
        if (microphoneGranted) {
            transcriber?.start(language)
        } else {
            pendingPermissionStart = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, if (listening) RecRed.copy(alpha = .65f) else CinemaLine),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(
                    onClick = {
                        if (listening) transcriber?.stop() else startVoice()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (listening) RecRed else CinemaSurfaceRaised,
                        contentColor = ProjectorIvory,
                    ),
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        if (listening) Icons.Outlined.StopCircle else Icons.Outlined.Mic,
                        if (listening) "Stop voice capture" else "Start voice capture",
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (listening) "Listening… ${formatVoiceTime(elapsedSeconds)}" else "Speak your idea",
                        color = ProjectorIvory,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (listening) listeningSubtitle(language, detectedLanguage)
                        else if (language == IdeaVoiceLanguage.AUTO) "Telugu + English · tap mic and talk naturally"
                        else "${language.displayLabel} voice typing",
                        color = MutedText,
                        fontSize = 8.7.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AnimatedVisibility(visible = listening) {
                Column {
                    Spacer(Modifier.height(9.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = RecRed,
                        trackColor = CinemaLine,
                    )
                    if (partial.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            partial,
                            color = ProjectorIvory.copy(alpha = .88f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (!listening) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IdeaVoiceLanguage.entries.forEach { option ->
                        FilterChip(
                            selected = language == option,
                            onClick = { language = option; error = null },
                            label = { Text(option.displayLabel, fontSize = 7.8.sp, fontWeight = FontWeight.Bold) },
                        )
                    }
                }
            }

            error?.let { message ->
                Spacer(Modifier.height(7.dp))
                Text(message, color = RecRed, fontSize = 8.5.sp, lineHeight = 12.sp)
            }

            Spacer(Modifier.height(6.dp))
            Text("Your speech becomes text in this idea. FrameByNavin does not save the audio.", color = MutedText, fontSize = 7.8.sp)
        }
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
        language == IdeaVoiceLanguage.AUTO -> "Auto Telugu + English · keep talking"
        else -> "${language.displayLabel} · keep talking"
    }
}

private fun formatVoiceTime(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
