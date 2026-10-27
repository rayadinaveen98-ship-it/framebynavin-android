package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.framebynavin.app.data.QuickCaptureDraft
import com.framebynavin.app.data.QuickCaptureDraftStore
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.IdeaVaultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * V161 keeps Quick Capture one-screen and text-first. Speech and original-audio inputs
 * use their existing tested implementations, revealed on demand. The callback signals
 * acceptance so a rejected save never takes ownership of (or discards) a recording.
 */
@Composable
internal fun V14QuickCaptureDialog(
    onDismiss: () -> Unit,
    onSave: (CreatorIdea) -> Boolean,
    onSaveDurably: (suspend (CreatorIdea, Long) -> Boolean)? = null,
) {
    val context = LocalContext.current
    val creatorProfile = remember { CreatorOsSettingsStore(context.applicationContext).snapshot().creatorProfile }
    val draftStore = remember { QuickCaptureDraftStore(context) }
    val recovered = remember { draftStore.load() }
    val draftId = remember { recovered?.id ?: java.util.UUID.randomUUID().toString() }
    val draftGeneration = remember { recovered?.generation ?: CreatorDataGate.generation(context) }
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(recovered?.title.orEmpty()) }
    var notes by remember { mutableStateOf(recovered?.notes.orEmpty()) }
    var draftError by remember { mutableStateOf<String?>(null) }
    var confirmClose by remember { mutableStateOf(false) }
    var recorderActive by remember { mutableStateOf(false) }
    var checkingRecovery by remember { mutableStateOf(recovered != null) }
    var showDictation by rememberSaveable { mutableStateOf(false) }
    var showRecording by rememberSaveable { mutableStateOf(recovered?.recording != null) }
    var showNotes by rememberSaveable { mutableStateOf(recovered?.notes?.isNotBlank() == true) }
    var showSuggestion by rememberSaveable { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var voiceRecording by remember { mutableStateOf(recovered?.recording) }
    val suggestion = remember(title, notes, creatorProfile) {
        CreatorQuickCaptureEngine.suggest("$title $notes", creatorProfile)
    }
    fun currentDraft() = QuickCaptureDraft(draftId, draftGeneration, title, notes, voiceRecording)
    fun preserveDraft(): Boolean = runCatching { draftStore.save(currentDraft()) }
        .onSuccess { draftError = null }.onFailure { draftError = it.message }.isSuccess

    // Recover a crash after the vault commit but before the draft was cleared without a second save.
    LaunchedEffect(Unit) {
        if (recovered == null) return@LaunchedEffect
        try {
            val alreadySaved = withContext(Dispatchers.IO) {
                CreatorDataGate.readyTransaction(context) {
                    IdeaVaultStore(context).load().any { it.id == draftId }
                }
            }
            if (alreadySaved) {
                draftStore.clear(currentDraft())
                onDismiss()
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Throwable) { draftError = "Could not check saved ideas. Reopen capture before saving." }
        finally { checkingRecovery = false }
    }

    fun updateRecording(recording: VoiceIdeaRecording?) {
        voiceRecording = recording
        saveFailed = false
        preserveDraft()
    }

    fun dismissQuickCapture() {
        if (submitting || checkingRecovery) return
        if (recorderActive) {
            draftError = "Stop and keep your recording before closing."
        } else if (currentDraft().hasContent) confirmClose = true
        else onDismiss()
    }


    CreatorAdaptiveFullScreen(onDismissRequest = ::dismissQuickCapture) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = ::dismissQuickCapture, modifier = Modifier.testTag("quick-capture-close")) {
                        Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text("QUICK CAPTURE", color = RecRed, fontSize = 10.sp,
                            fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("What's your idea?", color = ProjectorIvory,
                            fontSize = 23.sp, fontWeight = FontWeight.Black)
                    }
                }

                Column(
                    Modifier.weight(1f).padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(12.dp))
                    if (recovered != null) Text("Your unfinished idea was recovered.", color = MutedGold, fontSize = 12.sp)
                    draftError?.let { Text(it, color = RecRed, fontSize = 12.sp) }
                    OutlinedTextField(
                        value = title,
                        enabled = !submitting && !checkingRecovery,
                        onValueChange = { title = it; saveFailed = false; preserveDraft() },
                        modifier = Modifier.fillMaxWidth().testTag("quick-capture-title"),
                        label = { Text("Your idea") },
                        placeholder = { Text("A scene, a title, a thought…") },
                        minLines = 3,
                        maxLines = 7,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Save it now. You can develop it later in Idea Vault.",
                        color = MutedText, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(15.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showDictation = !showDictation },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("quick-capture-speak"),
                            border = BorderStroke(1.dp, if (showDictation) MutedGold else CinemaLine),
                        ) {
                            Icon(Icons.Outlined.RecordVoiceOver, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Speak text", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { showRecording = !showRecording },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("quick-capture-record"),
                            border = BorderStroke(1.dp, if (showRecording || voiceRecording != null) MutedGold else CinemaLine),
                        ) {
                            Icon(Icons.Outlined.Mic, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Record voice", fontSize = 11.sp)
                        }
                    }
                    AnimatedVisibility(showDictation) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Text("Speak and turn your words into text", color = MutedGold, fontSize = 12.sp)
                            V117VoiceIdeaInput(
                                onTranscript = { spoken ->
                                    title = VoiceIdeaText.append(title, spoken)
                                    saveFailed = false
                                    preserveDraft()
                                },
                            )
                        }
                    }
                    AnimatedVisibility(showRecording || voiceRecording != null) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Text("Keep an original audio note with your idea",
                                color = MutedGold, fontSize = 12.sp)
                            VoiceIdeaRecorderInput(
                                recording = voiceRecording,
                                onRecordingChanged = ::updateRecording,
                                onActiveChanged = { recorderActive = it },
                                enabled = !submitting && !checkingRecovery,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    Surface(
                        onClick = { showNotes = !showNotes },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 49.dp).testTag("quick-capture-notes-toggle"),
                        shape = RoundedCornerShape(14.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.NoteAdd, null, tint = MutedGold,
                                modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(if (showNotes) "Hide extra notes" else "Add notes (optional)",
                                color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f))
                            Icon(if (showNotes) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                null, tint = MutedText)
                        }
                    }
                    AnimatedVisibility(showNotes) {
                        Column {
                            Spacer(Modifier.height(9.dp))
                            OutlinedTextField(
                                value = notes,
                                enabled = !submitting && !checkingRecovery,
                                onValueChange = { notes = it; saveFailed = false; preserveDraft() },
                                modifier = Modifier.fillMaxWidth().testTag("quick-capture-notes"),
                                label = { Text("Notes") },
                                placeholder = { Text("Anything else worth remembering?") },
                                minLines = 3,
                                maxLines = 6,
                                shape = RoundedCornerShape(16.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = { showSuggestion = !showSuggestion },
                        modifier = Modifier.fillMaxWidth().testTag("quick-capture-suggestion-toggle"),
                    ) {
                        Text(if (showSuggestion) "Hide suggested organization" else "How will this be organized?",
                            color = MutedGold, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(if (showSuggestion) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            null, tint = MutedGold, modifier = Modifier.size(17.dp))
                    }
                    AnimatedVisibility(showSuggestion) {
                        Column {
                            Text(
                                "Suggested: ${IdeaVaultLabels.category(suggestion.category)}",
                                color = ProjectorIvory, fontSize = 12.sp,
                            )
                            Text("${suggestion.platformHint} · ${suggestion.formatHint}",
                                color = MutedText, fontSize = 12.sp)
                            Text("You can change these later in Idea Vault.",
                                color = MutedText, fontSize = 11.sp)
                        }
                    }

                    if (voiceRecording != null) {
                        Spacer(Modifier.height(10.dp))
                        Text("Your original recording will be kept with this idea.",
                            color = MutedGold, fontSize = 12.sp)
                    }
                    if (saveFailed) {
                        Spacer(Modifier.height(9.dp))
                        Text("Couldn't save yet. Your idea is still here — please try again.",
                            color = RecRed, fontSize = 12.sp, modifier = Modifier.testTag("quick-capture-save-error"))
                    }
                    Spacer(Modifier.height(18.dp))
                }

                Surface(color = CinemaSurfaceRaised, tonalElevation = 8.dp) {
                    Button(
                        onClick = {
                            if (submitting) return@Button
                            submitting = true
                            val finalized = voiceRecording
                            val finalTitle = title.trim().ifBlank {
                                if (finalized != null) v14DefaultVoiceIdeaTitle() else ""
                            }
                            val baseIdea = CreatorQuickCaptureEngine.toIdea(
                                title = finalTitle, notes = notes, profile = creatorProfile,
                            )
                            val idea = finalized?.let { recording ->
                                baseIdea.copy(
                                    captureType = IdeaCaptureType.VOICE,
                                    audioLocalPath = recording.localPath,
                                    audioDurationMillis = recording.durationMillis,
                                    audioMimeType = recording.mimeType,
                                    audioSyncState = IdeaAudioSyncState.LOCAL_ONLY,
                                )
                            } ?: baseIdea

                            if (!preserveDraft()) { submitting = false; return@Button }
                            val candidate = idea.copy(id = draftId)
                            scope.launch {
                                try {
                                    CreatorDataGate.checkGeneration(context, draftGeneration)
                                    val accepted = onSaveDurably?.invoke(candidate, draftGeneration)
                                        ?: runCatching { onSave(candidate) }.getOrDefault(false)
                                    if (accepted) {
                                        // No delete: audio now belongs to the durable vault item.
                                        draftStore.clear(currentDraft())
                                        voiceRecording = null
                                    } else saveFailed = true
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (error: Throwable) { saveFailed = true; draftError = error.message }
                                finally { submitting = false }
                            }
                        },
                        enabled = (title.isNotBlank() || voiceRecording != null) && !submitting && !recorderActive && !checkingRecovery,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                            .heightIn(min = 54.dp).testTag("quick-capture-save"),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text(
                            if (voiceRecording != null) "SAVE VOICE IDEA" else "SAVE TO IDEA VAULT",
                            fontWeight = FontWeight.Black, fontSize = 12.sp,
                        )
                    }
                }
    }
    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { if (!submitting) confirmClose = false },
            title = { Text("Keep this idea for later?") },
            text = { Text("Your text and completed recording can stay on this phone until you return.") },
            confirmButton = {
                TextButton(onClick = {
                    if (preserveDraft()) { confirmClose = false; onDismiss() }
                }, enabled = !submitting, modifier = Modifier.testTag("quick-capture-keep-draft")) { Text("KEEP DRAFT & CLOSE") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmClose = false }, enabled = !submitting) { Text("KEEP EDITING") }
                    TextButton(onClick = {
                        submitting = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    CreatorDataGate.readyTransaction(context) {
                                        val paths = IdeaVaultStore(context).load().map { it.audioLocalPath }.toSet()
                                        draftStore.discard(currentDraft(), paths)
                                    }
                                }
                                confirmClose = false
                                onDismiss()
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (error: Throwable) { draftError = error.message; confirmClose = false }
                            finally { submitting = false }
                        }
                    }, enabled = !submitting, modifier = Modifier.testTag("quick-capture-discard-draft")) { Text("DISCARD") }
                }
            },
        )
    }

}

private fun v14DefaultVoiceIdeaTitle(): String =
    "Voice idea • ${SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(System.currentTimeMillis())}"
