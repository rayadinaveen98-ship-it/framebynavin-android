package com.framebynavin.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.framebynavin.app.data.CreatorEditorDraftStore
import com.framebynavin.app.ui.theme.*
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * C3 first safety increment: readable, copy-only review of historical work.
 * No implicit apply, overwrite, status restoration or deletion is available.
 */
@Composable
internal fun V211HeldDraftReviewDialog(
    drafts: List<CreatorEditorDraftStore.HeldDraftPreview>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportText by remember { mutableStateOf<String?>(null) }
    val saveText = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val original = exportText
        exportText = null
        if (uri != null && original != null) {
            scope.launch(Dispatchers.IO) {
                val result = runCatching {
                    val output = context.contentResolver.openOutputStream(uri, "w")
                        ?: error("Selected document could not be opened")
                    output.bufferedWriter(Charsets.UTF_8).use { it.write(original) }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (result.isSuccess) "Text saved" else "Could not save text. Earlier draft was not changed.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Earlier drafts", color = ProjectorIvory, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Earlier edits were kept after a restore. Copy only the text you need. Nothing here changes your current project.",
                    color = MutedText,
                )
                Spacer(Modifier.height(10.dp))
                if (drafts.isEmpty()) {
                    Text(
                        "An older draft exists, but could not be read safely. It has not been deleted.",
                        color = MutedGold,
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(drafts, key = { it.sourceId }) { draft ->
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    draft.editor.uppercase() + " · " +
                                        if (draft.savedAtMillis > 0L)
                                            DateFormat.getDateTimeInstance().format(Date(draft.savedAtMillis))
                                        else "Date unavailable",
                                    color = MutedGold,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("Earlier than current restore · Review only", color = MutedText)
                                Spacer(Modifier.height(5.dp))
                                if (draft.fields.isEmpty()) {
                                    Text("No readable text fields in this draft.", color = MutedText)
                                }
                                draft.fields.forEach { field ->
                                    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                        Text(field.label, color = MutedGold, fontWeight = FontWeight.Bold)
                                        // Never lay out a multi-megabyte script in a
                                        // single Compose Text; export/copy retains full bytes.
                                        Text(
                                            if (field.value.length > 400) field.value.take(400) + "…" else field.value,
                                            color = ProjectorIvory,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        TextButton(
                                            onClick = {
                                                val copied = runCatching {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                                        as ClipboardManager
                                                    clipboard.setPrimaryClip(
                                                        ClipData.newPlainText(field.label, field.value),
                                                    )
                                                }.isSuccess
                                                Toast.makeText(
                                                    context,
                                                    if (copied) "Text copied" else "Clipboard could not copy this text. Use SAVE TEXT.",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            },
                                        ) { Text("COPY " + field.label.uppercase(), color = ProjectorIvory) }
                                        TextButton(
                                            onClick = {
                                                exportText = field.value
                                                saveText.launch("Backlot_" + field.label.replace(Regex("[^A-Za-z0-9_-]"), "_").take(32) + ".txt")
                                            },
                                        ) { Text("SAVE TEXT", color = MutedGold) }
                                    }
                                }
                                HorizontalDivider(color = CinemaLine)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("DONE", color = ProjectorIvory) }
        },
        containerColor = CinemaSurfaceRaised,
    )
}
