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
import java.io.Writer
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A snapshot of text already filtered by CreatorEditorDraftStore's editor-specific
 * preview allowlists. Never serialize raw journal JSON, hidden publication state,
 * reminders or media. Stream each complete field to the caller's chosen file:
 * even multi-megabyte scripts do not need a second joined String in memory.
 */
internal object V211HeldDraftTextExport {
    fun write(draft: CreatorEditorDraftStore.HeldDraftPreview, writer: Writer) {
        require(draft.editor in setOf("project", "script", "publish"))
        require(draft.fields.isNotEmpty()) { "There is no readable text to export" }
        writer.write("Backlot earlier draft — text-only copy\n")
        writer.write("Editor: ${draft.editor}\n")
        if (draft.savedAtMillis > 0L) {
            writer.write("Saved: ${DateFormat.getDateTimeInstance().format(Date(draft.savedAtMillis))}\n")
        }
        writer.write("No publication state, reminders, linked media files or backup data are restored by this export.\n\n")
        for (field in draft.fields) {
            writer.write("=== ${field.label} ===\n")
            writer.write(field.value)
            writer.write("\n\n")
        }
    }
}

private sealed interface V211HeldDraftExportRequest {
    data class Field(val text: String) : V211HeldDraftExportRequest
    data class Complete(val draft: CreatorEditorDraftStore.HeldDraftPreview) : V211HeldDraftExportRequest
}

/**
 * C3 safety increment: readable review, selective explicit Plan staging, and
 * manual text exports. Never auto-apply, overwrite canonical work or delete data.
 */
@Composable
internal fun V211HeldDraftReviewDialog(
    drafts: List<CreatorEditorDraftStore.HeldDraftPreview>,
    onDismiss: () -> Unit,
    allowPlanFieldStaging: Boolean = false,
    onStagePlanField: (String, String, String) -> Unit = { _, _, _ -> },
    allowScriptNotesStaging: Boolean = false,
    onStageScriptNotes: (String, String, String) -> Unit = { _, _, _ -> },
    eligiblePublishOutputIds: Set<String> = emptySet(),
    onStagePublishDescription: (String, String, String, String, String) -> Unit =
        { _, _, _, _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportRequest by remember { mutableStateOf<V211HeldDraftExportRequest?>(null) }
    val saveText = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val request = exportRequest
        exportRequest = null
        if (uri != null && request != null) {
            scope.launch(Dispatchers.IO) {
                val result = runCatching {
                    val output = context.contentResolver.openOutputStream(uri, "w")
                        ?: error("Selected document could not be opened")
                    output.bufferedWriter(Charsets.UTF_8).use { writer ->
                        when (request) {
                            is V211HeldDraftExportRequest.Field -> writer.write(request.text)
                            is V211HeldDraftExportRequest.Complete ->
                                V211HeldDraftTextExport.write(request.draft, writer)
                        }
                    }
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
                    "Earlier edits were kept after a restore. Copy a field or export all readable text. Using Plan text requires confirmation and never auto-saves the project.",
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
                                if (draft.fields.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            exportRequest = V211HeldDraftExportRequest.Complete(draft)
                                            saveText.launch("Backlot_earlier_${draft.editor}_${draft.savedAtMillis}.txt")
                                        },
                                    ) { Text("SAVE DRAFT TEXT", color = MutedGold) }
                                }
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
                                                exportRequest = V211HeldDraftExportRequest.Field(field.value)
                                                saveText.launch("Backlot_" + field.label.replace(Regex("[^A-Za-z0-9_-]"), "_").take(32) + ".txt")
                                            },
                                        ) { Text("SAVE TEXT", color = MutedGold) }
                                        if (allowPlanFieldStaging &&
                                            draft.editor == "project" &&
                                            field.label in setOf("Audience", "Viewer problem", "Promise", "Angle", "Learnings")
                                        ) {
                                            TextButton(onClick = {
                                                onStagePlanField(draft.sourceId, field.label, field.value)
                                            }) { Text("USE IN PLAN", color = MutedGold) }
                                        }
                                        if (allowScriptNotesStaging &&
                                            draft.editor == "script" &&
                                            field.label == "Creator notes"
                                        ) {
                                            TextButton(onClick = {
                                                onStageScriptNotes(draft.sourceId, field.label, field.value)
                                            }) { Text("USE IN SCRIPT", color = MutedGold) }
                                        }
                                        val outputId = field.deliverableId
                                        val platform = field.deliverablePlatform
                                        val format = field.deliverableFormat
                                        if (draft.editor == "publish" &&
                                            field.label.endsWith(" · Description") &&
                                            outputId != null && outputId in eligiblePublishOutputIds &&
                                            platform != null && format != null
                                        ) {
                                            TextButton(onClick = {
                                                onStagePublishDescription(
                                                    draft.sourceId, outputId, platform, format, field.value,
                                                )
                                            }) { Text("USE IN PUBLISH", color = MutedGold) }
                                        }
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
