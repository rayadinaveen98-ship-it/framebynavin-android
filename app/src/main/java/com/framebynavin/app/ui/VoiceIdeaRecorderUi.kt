package com.framebynavin.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.framebynavin.app.ui.theme.BacklotButtonRadius
import com.framebynavin.app.ui.theme.BacklotCardRadius
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.RecRedDeep
import com.framebynavin.app.voice.VoiceIdeaRecorder
import com.framebynavin.app.voice.VoiceIdeaRecorderState
import com.framebynavin.app.voice.VoiceIdeaRecording
import com.framebynavin.app.voice.VoiceIdeaRecordingFiles
import kotlinx.coroutines.delay

/**
 * Reusable capture surface for a real Voice Idea recording.
 *
 * This intentionally keeps the original local audio as the source of truth. Speech-to-text is a
 * separate convenience layer and can be added/retried later without risking the recording.
 */
@Composable
fun VoiceIdeaRecorderInput(
    recording: VoiceIdeaRecording?,
    onRecordingChanged: (VoiceIdeaRecording?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recorder = remember { VoiceIdeaRecorder(context.applicationContext) }
    var recorderState by remember { mutableStateOf(recorder.state) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun syncState() {
        recorderState = recorder.state
        elapsedMillis = recorder.elapsedRecordingMillis()
    }

    fun deleteFinalizedRecording() {
        if (!VoiceIdeaRecordingFiles.remove(recording)) {
            errorMessage = "Couldn't remove the saved recording safely. The original take was kept."
            return
        }
        errorMessage = null
        onRecordingChanged(null)
    }

    fun beginRecording() {
        errorMessage = null
        // Keep an existing finalized take until the replacement is safely finalized. If recorder
        // startup, cancellation, or stop fails, the previous take remains available to the user.
        recorder.start()
            .onSuccess { syncState() }
            .onFailure { errorMessage = it.message ?: "Couldn't start recording." }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) beginRecording()
        else errorMessage = "Microphone permission is needed to record a Voice Idea."
    }

    fun requestOrStartRecording() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) beginRecording()
        else microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(recorderState) {
        while (recorderState != VoiceIdeaRecorderState.IDLE) {
            elapsedMillis = recorder.elapsedRecordingMillis()
            delay(250L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Only unfinished capture belongs to the recorder. A finalized VoiceIdeaRecording is
            // deliberately left intact for the parent to save or discard.
            recorder.release()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
        shape = RoundedCornerShape(BacklotCardRadius),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = "VOICE IDEA",
                color = RecRed,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(5.dp))

            when (recorderState) {
                VoiceIdeaRecorderState.IDLE -> {
                    if (recording == null) {
                        Text(
                            "Record the thought exactly as it came to you. The original audio stays saved locally.",
                            color = MutedText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = ::requestOrStartRecording,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            shape = RoundedCornerShape(BacklotButtonRadius),
                        ) {
                            Text("START RECORDING", fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Text(
                            "Recorded • ${formatVoiceDuration(recording.durationMillis)} • saved locally",
                            color = ProjectorIvory,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = ::requestOrStartRecording,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(BacklotButtonRadius),
                            ) {
                                Text("RE-RECORD", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = ::deleteFinalizedRecording,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(BacklotButtonRadius),
                            ) {
                                Text("REMOVE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                VoiceIdeaRecorderState.RECORDING,
                VoiceIdeaRecorderState.PAUSED -> {
                    val paused = recorderState == VoiceIdeaRecorderState.PAUSED
                    Text(
                        if (paused) "Paused • ${formatVoiceDuration(elapsedMillis)}" else "Recording • ${formatVoiceDuration(elapsedMillis)}",
                        color = if (paused) ProjectorIvory else RecRed,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                val result = if (paused) recorder.resume() else recorder.pause()
                                result.onSuccess { syncState() }
                                    .onFailure { errorMessage = it.message ?: "Couldn't update recording." }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(BacklotButtonRadius),
                        ) {
                            Text(if (paused) "RESUME" else "PAUSE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                errorMessage = null
                                recorder.stop()
                                    .onSuccess { finalized ->
                                        if (!VoiceIdeaRecordingFiles.replace(recording, finalized)) {
                                            syncState()
                                            errorMessage = "Couldn't replace the previous recording safely. The original take was kept."
                                            return@onSuccess
                                        }
                                        onRecordingChanged(finalized)
                                        syncState()
                                    }
                                    .onFailure {
                                        syncState()
                                        errorMessage = it.message ?: "Couldn't save recording safely."
                                    }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            shape = RoundedCornerShape(BacklotButtonRadius),
                        ) {
                            Text("STOP + KEEP", fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            recorder.cancel()
                            syncState()
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedText),
                        shape = RoundedCornerShape(BacklotButtonRadius),
                    ) {
                        Text("CANCEL RECORDING", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            errorMessage?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(error, color = RecRedDeep, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun formatVoiceDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis.coerceAtLeast(0L) / 1_000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
