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
import com.framebynavin.app.reminders.ReminderBackingMusic
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
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val preview = remember(context) { ReminderBackingMusic(context.applicationContext) }
    DisposableEffect(preview) { onDispose { preview.stop() } }
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
        if (mode == ReminderMusicMode.CUSTOM) {
            Text("Selected audio file · Saved on this device. Moving or deleting the file may break playback.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) {
                Text("Change audio file")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(
                onClick = {
                    error = null
                    preview.start(ReminderMusicChoice(mode, uri))
                },
                modifier = Modifier.testTag("v273-music-preview"),
            ) { Text("Play music preview") }
            TextButton(onClick = { preview.stop() }) { Text("Stop preview") }
        }
        Text("Voice becomes louder than the background when the reminder speaks.",
            style = MaterialTheme.typography.bodySmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
