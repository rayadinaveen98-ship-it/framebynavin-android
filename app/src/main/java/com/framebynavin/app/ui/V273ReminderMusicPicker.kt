package com.framebynavin.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.framebynavin.app.reminders.ReminderVoiceMusicPreview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.framebynavin.app.reminders.CreatorMusicLocalStore
import com.framebynavin.app.reminders.ReminderMusicChoice
import com.framebynavin.app.reminders.ReminderMusicMode

/**
 * Picker is available ONLY when a user opens the app's reminder editor.
 * SAF grants long-lived read permission; no READ_MEDIA_AUDIO or gallery sweep.
 * Preview music is stopped when the editor is dismissed/recomposed away.
 */
@Composable
internal fun V273ReminderMusicPicker(
    mode: ReminderMusicMode,
    uri: String,
    onChanged: (ReminderMusicMode, String) -> Unit,
    modifier: Modifier = Modifier,
    allowSetCreatorDefault: Boolean = false,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var confirmAll by remember { mutableStateOf(false) }
    var globalStatus by remember { mutableStateOf<String?>(null) }
    var previewStatus by remember { mutableStateOf<String?>(null) }
    val preview = remember(context) { ReminderVoiceMusicPreview(context.applicationContext) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(preview, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) {
                preview.stop()
                previewStatus = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            preview.stop()
        }
    }
    LaunchedEffect(mode, uri) {
        preview.stop()
        previewStatus = null
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { chosen ->
        if (chosen == null) return@rememberLauncherForActivityResult
        if (chosen.scheme != "content" || chosen.toString().length > 2048) {
            error = "Select a local audio document from Files."
            return@rememberLauncherForActivityResult
        }
        val allowed = runCatching {
            context.contentResolver.takePersistableUriPermission(
                chosen, Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
        if (allowed) {
            error = null
            preview.stop()
            onChanged(ReminderMusicMode.CUSTOM, chosen.toString())
        } else {
            error = "Unable to keep access to this file. Try a different audio file."
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("BACKGROUND MUSIC", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == ReminderMusicMode.PIANO,
                onClick = { preview.stop(); onChanged(ReminderMusicMode.PIANO, "") },
                label = { Text("Soft piano") },
                modifier = Modifier.testTag("v273-music-piano"),
            )
            FilterChip(
                selected = mode == ReminderMusicMode.CUSTOM,
                onClick = { picker.launch(arrayOf("audio/*")) },
                label = { Text("Choose music") },
                modifier = Modifier.testTag("v273-music-custom"),
            )
            FilterChip(
                selected = mode == ReminderMusicMode.OFF,
                onClick = { preview.stop(); onChanged(ReminderMusicMode.OFF, "") },
                label = { Text("No music") },
                modifier = Modifier.testTag("v273-music-off"),
            )
        }
        if (allowSetCreatorDefault) {
            TextButton(
                modifier = Modifier.testTag("v276-use-music-all-projects"),
                onClick = { confirmAll = true; globalStatus = null },
            ) { Text("Use this music for all Creator projects") }
            Text(
                "Replaces every Creator project's music selection. Personal reminders stay unchanged.",
                style = MaterialTheme.typography.bodySmall,
            )
            globalStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        if (mode == ReminderMusicMode.CUSTOM) {
            Text("Selected audio file · Saved on this device. Moving or deleting the file may break playback.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                Text("Change audio file")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = {
                    error = null
                    preview.playMusic(ReminderMusicChoice(mode, uri))
                    previewStatus = "Background preview plays for at most 30 seconds."
                },
                enabled = mode != ReminderMusicMode.OFF,
                modifier = Modifier.testTag("v273-music-preview"),
            ) { Text("Play background music") }
            OutlinedButton(
                onClick = {
                    error = null
                    previewStatus = "Playing one sample announcement with your selected music."
                    preview.playVoiceAndMusic(ReminderMusicChoice(mode, uri)) {
                        previewStatus = it
                    }
                },
                modifier = Modifier.testTag("v277-voice-music-test"),
            ) { Text("Test voice + music") }
            TextButton(
                onClick = {
                    preview.stop()
                    previewStatus = "Preview stopped."
                },
                modifier = Modifier.testTag("v277-preview-stop"),
            ) { Text("Stop preview") }
        }
        Text("Test uses a sample voice line, not your private reminder text. " +
            "Playback stops automatically or when you leave this screen.",
            style = MaterialTheme.typography.bodySmall)
        previewStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Use for every Creator project?") },
            text = { Text(
                "This changes the default soundtrack for all current and future Creator projects, " +
                    "replacing earlier per-project music choices. This device only. Personal reminders and other project data are unchanged."
            ) },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("v276-confirm-all-creator-music"),
                    onClick = {
                        val selected = ReminderMusicChoice(mode, uri)
                        runCatching { CreatorMusicLocalStore(context).setDefaultForAll(selected) }
                            .onSuccess { globalStatus = "Default music applied to all Creator projects." }
                            .onFailure { error = "Could not save music. Choose a valid local file and try again." }
                        confirmAll = false
                    },
                ) { Text("APPLY TO ALL") }
            },
            dismissButton = {
                TextButton(onClick = { confirmAll = false }) { Text("CANCEL") }
            },
        )
    }
}
