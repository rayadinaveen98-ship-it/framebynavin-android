package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorQuickCaptureEngine
import com.framebynavin.app.data.IdeaAudioSyncState
import com.framebynavin.app.data.IdeaCaptureType
import com.framebynavin.app.data.IdeaVaultLabels
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.voice.VoiceIdeaRecording
import com.framebynavin.app.voice.VoiceIdeaText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun V14QuickCaptureDialog(
    onDismiss: () -> Unit,
    onSave: (CreatorIdea) -> Unit,
) {
    val context = LocalContext.current
    val creatorProfile = remember { CreatorOsSettingsStore(context.applicationContext).snapshot().creatorProfile }
    var title by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var voiceRecording by remember { mutableStateOf<VoiceIdeaRecording?>(null) }
    val cleanupRecording = remember { mutableStateOf<VoiceIdeaRecording?>(null) }
    val recordingCommitted = remember { mutableStateOf(false) }
    val suggestion = remember(title, notes, creatorProfile) {
        CreatorQuickCaptureEngine.suggest("$title $notes", creatorProfile)
    }

    fun updateRecording(recording: VoiceIdeaRecording?) {
        voiceRecording = recording
        cleanupRecording.value = recording
        recordingCommitted.value = false
    }

    fun discardRecording() {
        cleanupRecording.value?.localPath
            ?.takeIf { it.isNotBlank() }
            ?.let { path -> runCatching { File(path).delete() } }
        voiceRecording = null
        cleanupRecording.value = null
        recordingCommitted.value = false
    }

    fun dismissQuickCapture() {
        discardRecording()
        onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!recordingCommitted.value) {
                cleanupRecording.value?.localPath
                    ?.takeIf { it.isNotBlank() }
                    ?.let { path -> runCatching { File(path).delete() } }
            }
        }
    }

    Dialog(
        onDismissRequest = ::dismissQuickCapture,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::dismissQuickCapture) {
                        Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text("QUICK CAPTURE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Text("Save it before it disappears", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Idea") },
                        placeholder = { Text("A scene, hook, topic or idea worth exploring") },
                        minLines = 2,
                        maxLines = 6,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    V117VoiceIdeaInput(
                        onTranscript = { spoken -> title = VoiceIdeaText.append(title, spoken) },
                    )
                    Spacer(Modifier.height(12.dp))
                    VoiceIdeaRecorderInput(
                        recording = voiceRecording,
                        onRecordingChanged = ::updateRecording,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                        label = { Text("Notes · optional") },
                        placeholder = { Text("Angle, scene, hook or anything worth remembering…") },
                        maxLines = 5,
                        shape = RoundedCornerShape(16.dp),
                    )

                    Spacer(Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Bolt, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("IDEA VAULT", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                                Text(IdeaVaultLabels.category(suggestion.category), color = ProjectorIvory, fontSize = 10.8.sp, fontWeight = FontWeight.Bold)
                                Text("${suggestion.platformHint} · ${suggestion.formatHint}", color = MutedText, fontSize = 8.4.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        if (voiceRecording != null) {
                            "The original recording will be saved with this Voice Idea. You can transcribe or refine it later."
                        } else {
                            "You can refine or turn it into a project later."
                        },
                        color = MutedText,
                        fontSize = 8.5.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Surface(color = CinemaSurfaceRaised, tonalElevation = 8.dp) {
                    Button(
                        onClick = {
                            val finalizedRecording = voiceRecording
                            val isVoiceIdea = finalizedRecording != null
                            val finalTitle = title.trim().ifBlank {
                                if (isVoiceIdea) v14DefaultVoiceIdeaTitle() else ""
                            }
                            val baseIdea = CreatorQuickCaptureEngine.toIdea(
                                title = finalTitle,
                                notes = notes,
                                profile = creatorProfile,
                            )
                            val savedIdea = if (finalizedRecording == null) {
                                baseIdea
                            } else {
                                baseIdea.copy(
                                    captureType = IdeaCaptureType.VOICE,
                                    audioLocalPath = finalizedRecording.localPath,
                                    audioDurationMillis = finalizedRecording.durationMillis,
                                    audioMimeType = finalizedRecording.mimeType,
                                    audioSyncState = IdeaAudioSyncState.LOCAL_ONLY,
                                )
                            }

                            recordingCommitted.value = finalizedRecording != null
                            onSave(savedIdea)
                            voiceRecording = null
                            cleanupRecording.value = null
                        },
                        enabled = title.isNotBlank() || voiceRecording != null,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text(
                            if (voiceRecording != null) "SAVE VOICE IDEA" else "SAVE IDEA",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun v14DefaultVoiceIdeaTitle(): String =
    "Voice idea • ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(System.currentTimeMillis())}"
