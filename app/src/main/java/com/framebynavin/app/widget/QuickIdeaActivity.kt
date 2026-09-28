package com.framebynavin.app.widget

import androidx.activity.compose.BackHandler
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorWriteConflict
import com.framebynavin.app.data.IdeaAudioSyncState
import com.framebynavin.app.data.IdeaCaptureType
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.ui.V117VoiceIdeaInput
import com.framebynavin.app.ui.VoiceIdeaRecorderInput
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.voice.VoiceIdeaRecording
import com.framebynavin.app.voice.VoiceIdeaText
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickIdeaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        VisualExperiencePrefs.initialize(applicationContext)
        setContent {
            FrameByNavinTheme {
                QuickIdeaScreen(onClose = ::finish)
            }
        }
    }
}

@Composable
private fun QuickIdeaScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { IdeaVaultStore(context.applicationContext) }
    var title by rememberSaveable { mutableStateOf("") }
    var voiceRecording by remember { mutableStateOf<VoiceIdeaRecording?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun discardFinalizedRecording() {
        voiceRecording?.localPath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).delete() }
        }
        voiceRecording = null
    }

    fun closeDiscardingDraft() {
        if (saving) return
        discardFinalizedRecording()
        onClose()
    }

    BackHandler(enabled = !saving) { closeDiscardingDraft() }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(BacklotPagePadding),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BACKLOT", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Quick Idea", color = ProjectorIvory, style = MaterialTheme.typography.headlineLarge)
                }
                IconButton(onClick = ::closeDiscardingDraft, enabled = !saving) {
                    Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory)
                }
            }

            Spacer(Modifier.height(BacklotSectionGap))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Catch it before it disappears.", color = ProjectorIvory, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Type it, dictate it, or keep the original audio. Everything lands in Idea Vault so you can shape it later.",
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(BacklotSectionGap))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = { Text("Movie, scene, thought, hook…", color = MutedText) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ProjectorIvory,
                        unfocusedTextColor = ProjectorIvory,
                        focusedBorderColor = RecRed,
                        unfocusedBorderColor = CinemaLine,
                        cursorColor = RecRed,
                    ),
                    shape = RoundedCornerShape(BacklotCardRadius),
                )

                Spacer(Modifier.height(12.dp))
                VoiceIdeaRecorderInput(
                    recording = voiceRecording,
                    onRecordingChanged = { voiceRecording = it },
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "DICTATE TEXT (OPTIONAL)",
                    color = MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = .8.sp,
                )
                Spacer(Modifier.height(6.dp))
                V117VoiceIdeaInput(
                    onTranscript = { spoken -> title = VoiceIdeaText.append(title, spoken) },
                )

                saveError?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = RecRed, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val finalizedRecording = voiceRecording
                    if ((cleanTitle.isBlank() && finalizedRecording == null) || saving) return@Button

                    saving = true
                    saveError = null
                    val epoch = CreatorDataGate.generation(context.applicationContext)
                    val isVoice = finalizedRecording != null
                    val idea = CreatorIdea(
                        id = UUID.randomUUID().toString(),
                        title = cleanTitle.ifBlank { defaultVoiceIdeaTitle() },
                        captureType = if (isVoice) IdeaCaptureType.VOICE else IdeaCaptureType.TEXT,
                        audioLocalPath = finalizedRecording?.localPath.orEmpty(),
                        audioDurationMillis = finalizedRecording?.durationMillis ?: 0L,
                        audioMimeType = finalizedRecording?.mimeType.orEmpty(),
                        audioSyncState = if (isVoice) IdeaAudioSyncState.LOCAL_ONLY else IdeaAudioSyncState.NONE,
                    )
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) { store.capture(idea, epoch) }
                        }.onSuccess {
                            // Ownership of the finalized audio now belongs to the saved Idea Vault item.
                            voiceRecording = null
                            Toast.makeText(
                                context,
                                if (isVoice) "Voice Idea saved" else "Saved to Idea Vault",
                                Toast.LENGTH_SHORT,
                            ).show()
                            onClose()
                        }.onFailure { error ->
                            if (error is CancellationException) throw error
                            saving = false
                            saveError = if (error is CreatorWriteConflict) error.message
                                else "Couldn't save idea. Your draft and recording are still here. Try again."
                        }
                    }
                },
                enabled = (title.isNotBlank() || voiceRecording != null) && !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RecRed,
                    disabledContainerColor = RecRedDeep,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .55f),
                ),
                shape = RoundedCornerShape(BacklotButtonRadius),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        if (voiceRecording != null) "SAVE VOICE IDEA" else "SAVE IDEA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

private fun defaultVoiceIdeaTitle(): String =
    "Voice idea • ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date())}"
