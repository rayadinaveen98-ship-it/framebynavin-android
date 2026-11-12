package com.framebynavin.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * CW4 Output Craft Desk: one project, independent output-specific scripts and
 * explicit cut-version tracking. No automatic publishing and no media encoding.
 */
@Composable
internal fun CW4OutputCraftDesk(
    task: CreatorTask,
    onBack: () -> Unit,
    onOpenMainScript: () -> Unit,
    onSaveDerivativeScript: (Long, CreatorOutputScript) -> Unit,
    onAddEditVersion: (Long, String, String, String, String) -> Unit,
    onReviewEditVersion: (Long, String, CreatorEditReviewStatus) -> Unit,
) {
    val context = LocalContext.current
    val workspace = task.workspace
    val revision = workspace.revision
    val outputs = workspace.deliverables
    val editable = task.status != TaskStatus.DONE && task.status != TaskStatus.SKIPPED &&
        task.archivedAtMillis == 0L
    val journal = remember(task.id) { CreatorOutputCraftDraftStore(context.applicationContext) }
    var selectedId by rememberSaveable(task.id) { mutableStateOf(outputs.firstOrNull()?.id.orEmpty()) }
    val selected = outputs.firstOrNull { it.id == selectedId } ?: outputs.firstOrNull()
    val outputId = selected?.id.orEmpty()
    val script = CreatorOutputCraftPolicy.scriptFor(workspace, outputId)

    val restored = remember(task.id, outputId, revision, editable) {
        if (editable && outputId.isNotBlank()) journal.load(task.id, outputId, revision) else null
    }
    var hook by rememberSaveable(task.id, outputId, revision) {
        mutableStateOf(restored?.hook ?: script?.hook.orEmpty())
    }
    var narration by rememberSaveable(task.id, outputId, revision) {
        mutableStateOf(restored?.narration ?: script?.narration.orEmpty())
    }
    var visualNotes by rememberSaveable(task.id, outputId, revision) {
        mutableStateOf(restored?.visualNotes ?: script?.visualNotes.orEmpty())
    }
    var creatorNotes by rememberSaveable(task.id, outputId, revision) {
        mutableStateOf(restored?.creatorNotes ?: script?.creatorNotes.orEmpty())
    }
    var parentSourceRevision by rememberSaveable(task.id, outputId, revision) {
        mutableLongStateOf(restored?.startingParentRevision ?: script?.adaptedFromScriptRevision ?: 0L)
    }
    var copiedParent by rememberSaveable(task.id, outputId, revision) {
        mutableStateOf(restored?.usedParentSource ?: false)
    }
    var versionLabel by rememberSaveable(task.id, outputId, revision) {
        mutableStateOf(restored?.versionLabel.orEmpty())
    }
    var versionUrl by rememberSaveable(task.id, outputId, revision) {
        mutableStateOf(restored?.versionUrl.orEmpty())
    }
    var versionNotes by rememberSaveable(task.id, outputId, revision) {
        mutableStateOf(restored?.versionNotes.orEmpty())
    }
    var draftWarning by remember(task.id, outputId, revision) { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val allowed = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.isSuccess
            if (allowed) versionUrl = uri.toString()
            else draftWarning = true
        }
    }

    val draft = CreatorOutputCraftDraft(
        hook = hook, narration = narration, visualNotes = visualNotes,
        creatorNotes = creatorNotes, startingParentRevision = parentSourceRevision,
        usedParentSource = copiedParent, versionLabel = versionLabel,
        versionUrl = versionUrl, versionNotes = versionNotes,
    )
    val changedScript = script?.let {
        it.hook != hook.trim() || it.narration != narration.trimEnd() ||
            it.visualNotes != visualNotes.trimEnd() || it.creatorNotes != creatorNotes.trimEnd()
    } ?: (hook.isNotBlank() || narration.isNotBlank() ||
        visualNotes.isNotBlank() || creatorNotes.isNotBlank())

    LaunchedEffect(task.id, outputId, revision, draft, editable) {
        if (!editable || outputId.isBlank()) return@LaunchedEffect
        delay(400)
        val error = withContext(Dispatchers.IO) {
            runCatching {
                if (draft != CreatorOutputCraftDraft(
                        hook = script?.hook.orEmpty(), narration = script?.narration.orEmpty(),
                        visualNotes = script?.visualNotes.orEmpty(), creatorNotes = script?.creatorNotes.orEmpty(),
                        startingParentRevision = script?.adaptedFromScriptRevision ?: 0L,
                        usedParentSource = false
                    )
                ) journal.save(task.id, outputId, revision, draft)
                else journal.clear(task.id, outputId)
            }.exceptionOrNull()
        }
        if (error != null) draftWarning = true
    }

    WorkspaceAdaptiveFrame {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp).testTag("cw4-output-desk"),
        ) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to project", tint = ProjectorIvory)
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text("CREATOR WORKSPACE", color = MutedGold, fontSize = 11.sp)
                    Text("Output Craft", color = ProjectorIvory,
                        fontSize = 23.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Text("One project · Separate scripts and cut histories for each output.",
                color = MutedText, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(18.dp))

            if (outputs.isEmpty()) {
                Surface(shape = RoundedCornerShape(18.dp), color = CinemaSurfaceRaised) {
                    Text("No outputs yet. Create your main video and a derivative Short/Reel in Plan first.",
                        modifier = Modifier.fillMaxWidth().padding(18.dp), color = MutedText)
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Text("CHOOSE AN OUTPUT", color = ProjectorIvory,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    outputs.forEach { output ->
                        FilterChip(
                            selected = output.id == outputId,
                            onClick = { selectedId = output.id },
                            label = { Text(
                                output.platform + " · " + output.format,
                                maxLines = 1,
                            ) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (selected != null) {
                    Surface(shape = RoundedCornerShape(18.dp), color = CinemaSurfaceRaised,
                        border = BorderStroke(1.dp, CinemaLine)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(selected.title.ifBlank { selected.format }, color = ProjectorIvory,
                                fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("OUTPUT · ${selected.status.name}  ·  " +
                                if (selected.parentDeliverableId.isBlank()) "MAIN" else "DERIVATIVE",
                                color = MutedGold, fontSize = 11.sp)
                            if (selected.status == CreatorDeliverableStatus.PUBLISHED) {
                                Spacer(Modifier.height(7.dp))
                                Text("Live output: new script/cut notes here do not alter the published release.",
                                    color = MutedText, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(19.dp))
                    if (selected.parentDeliverableId.isBlank()) {
                        Text("MAIN SCRIPT", color = ProjectorIvory, fontSize = 15.sp,
                            fontWeight = FontWeight.Bold)
                        Text("The original narration belongs in Script Studio. Derivative outputs can have independent scripts below.",
                            color = MutedText, fontSize = 12.sp)
                        Spacer(Modifier.height(9.dp))
                        OutlinedButton(onClick = onOpenMainScript,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("OPEN MAIN SCRIPT STUDIO")
                        }
                    } else {
                        Text("INDEPENDENT DERIVATIVE SCRIPT", color = ProjectorIvory,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Your Reel/Short narration is separate. Editing it never changes the main video.",
                            color = MutedText, fontSize = 12.sp, lineHeight = 17.sp)
                        if (script == null && hook.isBlank() && narration.isBlank() && editable) {
                            Spacer(Modifier.height(7.dp))
                            OutlinedButton(
                                onClick = {
                                    val parentScript = CreatorOutputCraftPolicy.scriptFor(
                                        workspace, selected.parentDeliverableId
                                    )
                                    hook = parentScript?.hook ?: workspace.scriptStudio?.selectedHook().orEmpty()
                                    narration = parentScript?.narration ?: workspace.scriptStudio?.compiledNarration().orEmpty()
                                    parentSourceRevision = parentScript?.revision ?: workspace.scriptStudio?.revision ?: 0L
                                    copiedParent = true
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text("COPY PARENT SCRIPT AS STARTING DRAFT") }
                            Text("Copies once. Future changes stay independent.",
                                color = MutedText, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = hook, onValueChange = { hook = it },
                            enabled = editable, modifier = Modifier.fillMaxWidth().testTag("cw4-hook"),
                            label = { Text("Hook / opening") }, minLines = 2)
                        Spacer(Modifier.height(9.dp))
                        OutlinedTextField(value = narration, onValueChange = { narration = it },
                            enabled = editable, modifier = Modifier.fillMaxWidth().testTag("cw4-narration"),
                            label = { Text("Independent narration") }, minLines = 5, maxLines = 16)
                        Spacer(Modifier.height(9.dp))
                        OutlinedTextField(value = visualNotes, onValueChange = { visualNotes = it },
                            enabled = editable, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Visual and crop notes") }, minLines = 2)
                        Spacer(Modifier.height(9.dp))
                        OutlinedTextField(value = creatorNotes, onValueChange = { creatorNotes = it },
                            enabled = editable, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Creative notes") }, minLines = 2)
                        Spacer(Modifier.height(10.dp))
                        if (script != null) {
                            Text("SAVED SCRIPT REVISIONS · ${script.history.size}",
                                color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            script.history.takeLast(5).asReversed().forEach { snapshot ->
                                Text("v${snapshot.revision} · ${snapshot.atMillis} · " +
                                    snapshot.narration.take(78), color = MutedText, fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                        if (editable) {
                            Button(
                                onClick = {
                                    onSaveDerivativeScript(
                                        revision,
                                        CreatorOutputScript(
                                            deliverableId = selected.id, revision = script?.revision ?: 0L,
                                            hook = hook, narration = narration, visualNotes = visualNotes,
                                            creatorNotes = creatorNotes,
                                            adaptedFromDeliverableId = if (copiedParent)
                                                selected.parentDeliverableId else script?.adaptedFromDeliverableId.orEmpty(),
                                            adaptedFromScriptRevision = if (copiedParent)
                                                parentSourceRevision else script?.adaptedFromScriptRevision ?: 0L,
                                        ),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("cw4-save-script"),
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                                enabled = changedScript && (hook.isNotBlank() || narration.isNotBlank()),
                            ) { Text("SAVE DERIVATIVE SCRIPT") }
                        }
                    }
                    Spacer(Modifier.height(26.dp))
                    HorizontalDivider(color = CinemaLine)
                    Spacer(Modifier.height(16.dp))
                    Text("EDIT VERSIONS", color = ProjectorIvory,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Record rough cuts, review passes and final cuts. Backlot stores notes and references—not video files.",
                        color = MutedText, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(11.dp))
                    val versions = CreatorOutputCraftPolicy.versionsFor(workspace, selected.id)
                    if (versions.isEmpty()) Text("No cuts recorded for this output yet.",
                        color = MutedText, fontSize = 12.sp)
                    versions.asReversed().forEach { version ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
                            shape = RoundedCornerShape(14.dp), color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(13.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(version.label, color = ProjectorIvory,
                                        modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                    Text(version.status.name, color = MutedGold, fontSize = 11.sp)
                                }
                                if (version.notes.isNotBlank()) Text(version.notes,
                                    color = MutedText, fontSize = 12.sp)
                                if (version.sourceLocation.isNotBlank()) Text(
                                    "Reference: " + version.sourceLocation.take(90),
                                    color = MutedText, fontSize = 11.sp,
                                )
                                if (editable && version.status != CreatorEditReviewStatus.APPROVED) {
                                    TextButton(onClick = {
                                        onReviewEditVersion(
                                            revision, version.id,
                                            if (version.status == CreatorEditReviewStatus.DRAFT)
                                                CreatorEditReviewStatus.REVIEW
                                            else CreatorEditReviewStatus.APPROVED,
                                        )
                                    }) {
                                        Text(
                                            if (version.status == CreatorEditReviewStatus.DRAFT)
                                                "MARK FOR REVIEW" else "APPROVE THIS CUT",
                                            color = RecRed,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (editable) {
                        Spacer(Modifier.height(10.dp))
                        Text("LOG NEW CUT", color = ProjectorIvory, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = versionLabel, onValueChange = { versionLabel = it },
                            modifier = Modifier.fillMaxWidth().testTag("cw4-new-cut-label"),
                            label = { Text("Version name (e.g., Rough Cut v2)") }, singleLine = true)
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(value = versionUrl, onValueChange = { versionUrl = it },
                            modifier = Modifier.fillMaxWidth().testTag("cw4-new-cut-url"),
                            label = { Text("HTTPS reference or Android document URI (optional)") },
                            singleLine = true)
                        OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text("SELECT FILE FROM THIS DEVICE")
                        }
                        Text("Links may not work after a device change or backup restore.",
                            color = MutedText, fontSize = 11.sp)
                        Spacer(Modifier.height(7.dp))
                        OutlinedTextField(value = versionNotes, onValueChange = { versionNotes = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Changes and review notes") }, minLines = 2)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onAddEditVersion(
                                revision, selected.id, versionLabel, versionUrl, versionNotes
                            ) },
                            enabled = versionLabel.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("cw4-save-cut"),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        ) { Text("SAVE NEW EDIT VERSION") }
                    }
                    if (draftWarning) Text(
                        "Local draft or document permission could not be retained. Review and save your output work.",
                        color = RecRed, fontSize = 12.sp)
                    Spacer(Modifier.height(25.dp))
                }
            }
        }
    }
}
