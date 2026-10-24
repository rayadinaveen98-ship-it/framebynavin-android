package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.provider.OpenableColumns
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private enum class Alpha3Section(val label: String) {
    OVERVIEW("Overview"), BRIEF("Brief"), RESEARCH("Sources"), ASSETS("Assets"),
    PRODUCTION("Tasks"), DELIVERABLES("Outputs"), LEARN("Notes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V19ContentWorkspaceAlpha3Dialog(
    task: CreatorTask,
    onDismiss: () -> Unit,
    onSave: (String, Long, CreatorContentWorkspace) -> Unit,
    onSaveAndScript: (String, Long, CreatorContentWorkspace) -> Unit,
    hasHeldDraft: Boolean = false,
    onReviewHeld: () -> Unit = {},
) {
    val context = LocalContext.current
    val originalRevision = task.workspace.revision
    val draftStore = remember { CreatorEditorDraftStore(context.applicationContext) }
    val recovered = remember(task.id, originalRevision) {
        draftStore.loadProject(task.id, originalRevision)
    }
    val initial = recovered ?: task.workspace
    var section by remember { mutableStateOf(Alpha3Section.OVERVIEW) }
    var audience by remember(task.id, originalRevision) { mutableStateOf(initial.audience) }
    var viewerProblem by remember(task.id, originalRevision) { mutableStateOf(initial.viewerProblem) }
    var promise by remember(task.id, originalRevision) { mutableStateOf(initial.promise) }
    var angle by remember(task.id, originalRevision) { mutableStateOf(initial.angle) }
    val hook = task.workspace.hook
    val script = task.workspace.script
    var references by remember(task.id, originalRevision) { mutableStateOf(initial.references) }
    var checklist by remember(task.id, originalRevision) { mutableStateOf(initial.checklist) }
    var assets by remember(task.id, originalRevision) { mutableStateOf(initial.assets) }
    var deliverables by remember(task.id, originalRevision) { mutableStateOf(initial.deliverables) }
    var learnings by remember(task.id, originalRevision) { mutableStateOf(initial.learnings) }
    var appliedTemplate by remember { mutableStateOf("") }
    var pendingRemovalId by remember(task.id, originalRevision) { mutableStateOf<String?>(null) }
    var assetError by remember(task.id, originalRevision) { mutableStateOf<String?>(null) }
    var openError by remember(task.id, originalRevision) { mutableStateOf<String?>(null) }

    fun draft() = CreatorContentWorkspace(
        revision = originalRevision,
        audience = audience,
        viewerProblem = viewerProblem,
        promise = promise,
        angle = angle,
        hook = hook,
        script = script,
        references = references,
        checklist = checklist,
        assets = assets,
        deliverables = deliverables,
        learnings = learnings,
        scriptStudio = task.workspace.scriptStudio,
    )

    var draftWarningShown by remember(task.id, originalRevision) { mutableStateOf(false) }
    LaunchedEffect(audience, viewerProblem, promise, angle, references, checklist, assets, deliverables, learnings) {
        delay(350)
        val snapshot = draft()
        val saveError = withContext(Dispatchers.IO) {
            runCatching {
            if (snapshot == task.workspace) draftStore.clearProject(task.id)
            else draftStore.saveProject(task.id, originalRevision, snapshot)
            }.exceptionOrNull()
        }
        if (saveError != null && !draftWarningShown) {
            draftWarningShown = true
            android.widget.Toast.makeText(
                context,
                "Could not save this draft. Reopen the project if a backup was restored.",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun moveChecklist(index: Int, delta: Int) {
        val target = index + delta
        if (index !in checklist.indices || target !in checklist.indices) return
        checklist = checklist.toMutableList().also { list ->
            val item = list.removeAt(index)
            list.add(target, item)
        }
    }

    fun moveDeliverable(index: Int, delta: Int) {
        val target = index + delta
        if (index !in deliverables.indices || target !in deliverables.indices) return
        deliverables = deliverables.toMutableList().also { list ->
            val item = list.removeAt(index)
            list.add(target, item)
        }
    }

    fun addDeliverable(platform: String, format: String, parentId: String = "") {
        deliverables = deliverables + CreatorDeliverable(
            id = UUID.randomUUID().toString(),
            platform = platform,
            format = format,
            title = task.title,
            parentDeliverableId = parentId,
        )
    }

    val assetPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val persisted = runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                true
            }.getOrDefault(false)
            if (!persisted) {
                assetError = "This file provider did not grant durable read access. Choose the file from another provider so the project does not keep a broken asset link."
            } else {
                assetError = null
                val mime = runCatching { context.contentResolver.getType(uri).orEmpty() }.getOrDefault("")
                val label = runCatching {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment.orEmpty().ifBlank { "Picked asset" } }
                val kind = when {
                    mime.startsWith("image/") -> CreatorAssetKind.IMAGE
                    mime.startsWith("video/") -> CreatorAssetKind.VIDEO
                    mime.startsWith("audio/") -> CreatorAssetKind.AUDIO
                    mime.isNotBlank() -> CreatorAssetKind.DOCUMENT
                    else -> CreatorAssetKind.OTHER
                }
                assets = assets + CreatorProjectAsset(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    location = uri.toString(),
                    kind = kind,
                )
            }
        }
    }

    WorkspaceAdaptiveFrame {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Close", tint = ProjectorIvory) }
                    Column(Modifier.weight(1f)) {
                        Text("PLAN", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(task.title, color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (hasHeldDraft) {
                        TextButton(onClick = onReviewHeld) {
                            Text("EARLIER DRAFT", color = MutedGold, fontSize = 10.sp)
                        }
                    }
                }

                if (recovered != null) {
                    Surface(Modifier.fillMaxWidth(), color = MutedGold.copy(alpha = .08f)) {
                        Text("Your unsaved plan was recovered", color = MutedGold, fontSize = 8.5.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp))
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = section.ordinal,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 12.dp,
                    containerColor = CinemaBlack,
                    divider = {},
                    indicator = {},
                ) {
                    Alpha3Section.entries.forEach { item ->
                        FilterChip(
                            selected = section == item,
                            onClick = { section = item; openError = null },
                            label = { Text(item.label, fontSize = 12.sp) },
                            modifier = Modifier.heightIn(min = 48.dp).padding(end = 7.dp),
                        )
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp).padding(bottom = 24.dp),
                ) {
                    when (section) {
                        Alpha3Section.OVERVIEW -> Alpha3Overview(
                            workspace = draft(),
                            appliedTemplate = appliedTemplate,
                            onApplyTemplate = { template ->
                                val merged = CreatorProjectTemplates.applyContentTemplate(draft(), template, task.title)
                                checklist = merged.checklist
                                deliverables = merged.deliverables
                                appliedTemplate = template.name
                            },
                        )
                        Alpha3Section.BRIEF -> {
                            A3Title("BRIEF", "Define the audience, tension, promise and angle.")
                            A3Field("AUDIENCE", audience, { audience = it }, "Who is this for?")
                            A3Field("VIEWER PROBLEM", viewerProblem, { viewerProblem = it }, "What question or need are they bringing?")
                            A3Field("PROMISE", promise, { promise = it }, "What will they get by staying?")
                            A3Field("ANGLE", angle, { angle = it }, "Why is your take different?")
                        }
                        Alpha3Section.RESEARCH -> {
                            A3Title("SOURCES", "")
                            openError?.let { Text(it, color = RecRed, fontSize = 12.sp); Spacer(Modifier.height(8.dp)) }
                            Button(onClick = { references = references + CreatorProjectReference(id = UUID.randomUUID().toString()) }) {
                                Icon(Icons.Outlined.AddLink, null); Spacer(Modifier.width(5.dp)); Text("ADD SOURCE")
                            }
                            Spacer(Modifier.height(10.dp))
                            references.forEach { ref ->
                                A3ReferenceCard(
                                    ref = ref,
                                    onChange = { changed ->
                                        openError = null
                                        references = references.map { if (it.id == changed.id) changed else it }
                                    },
                                    onOpen = { openError = CreatorWorkspaceExternalOpen.openSource(context, ref.url) },
                                    onRemove = {
                                        openError = null
                                        references = references.filterNot { it.id == ref.id }
                                    },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            if (references.isEmpty()) A3Empty("No research sources yet.")
                        }
                        Alpha3Section.ASSETS -> {
                            A3Title("ASSETS", "")
                            openError?.let { Text(it, color = RecRed, fontSize = 12.sp); Spacer(Modifier.height(8.dp)) }
                            Button(onClick = { assetPicker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf", "text/*")) }) {
                                Icon(Icons.Outlined.AttachFile, null); Spacer(Modifier.width(5.dp)); Text("PICK FROM DEVICE")
                            }
                            assetError?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = RecRed, fontSize = 8.5.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            assets.forEach { asset ->
                                A3AssetCard(
                                    asset = asset,
                                    onChange = { changed ->
                                        openError = null
                                        assets = assets.map { if (it.id == changed.id) changed else it }
                                    },
                                    onOpen = { openError = CreatorWorkspaceExternalOpen.openAsset(context, asset.location) },
                                    onRemove = {
                                        openError = null
                                        assets = assets.filterNot { it.id == asset.id }
                                    },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            if (assets.isEmpty()) A3Empty("No assets linked yet.")
                        }
                        Alpha3Section.PRODUCTION -> {
                            A3Title("PRODUCTION CHECKLIST", "Add optional tasks. Your main project progress stays unchanged.")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { checklist = checklist + CreatorChecklistItem(id = UUID.randomUUID().toString(), title = "New step") }) { Text("ADD STEP") }
                                OutlinedButton(onClick = {
                                    val template = CreatorProjectTemplates.contentTemplates.first()
                                    val merged = CreatorProjectTemplates.applyContentTemplate(draft(), template, task.title)
                                    checklist = merged.checklist
                                }) { Text("ADD STANDARD") }
                            }
                            Spacer(Modifier.height(10.dp))
                            checklist.forEachIndexed { index, item ->
                                A3ChecklistCard(
                                    item = item,
                                    canUp = index > 0,
                                    canDown = index < checklist.lastIndex,
                                    onUp = { moveChecklist(index, -1) },
                                    onDown = { moveChecklist(index, 1) },
                                    onChange = { changed -> checklist = checklist.map { if (it.id == changed.id) changed else it } },
                                    onRemove = { checklist = checklist.filterNot { it.id == item.id } },
                                )
                                Spacer(Modifier.height(7.dp))
                            }
                            if (checklist.isEmpty()) A3Empty("No checklist yet. Start with a template if you like.")
                        }
                        Alpha3Section.DELIVERABLES -> {
                            A3Title("DELIVERABLES", "Plan your posts here. Track what goes live in Prepare to Publish.")
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                OutlinedButton(onClick = { addDeliverable("YouTube", "Long video") }) { Text("+ YT") }
                                OutlinedButton(onClick = { addDeliverable("Instagram", "Reel") }) { Text("+ IG") }
                                OutlinedButton(onClick = { addDeliverable("Other", "Custom") }) { Text("+ CUSTOM") }
                            }
                            Spacer(Modifier.height(10.dp))
                            deliverables.forEachIndexed { index, d ->
                                val parent = deliverables.firstOrNull { it.id == d.parentDeliverableId }
                                A3DeliverableCard(
                                    deliverable = d,
                                    parentTitle = parent?.title?.ifBlank { parent.format },
                                    canUp = index > 0,
                                    canDown = index < deliverables.lastIndex,
                                    onUp = { moveDeliverable(index, -1) },
                                    onDown = { moveDeliverable(index, 1) },
                                    onChange = { changed -> deliverables = deliverables.map { if (it.id == changed.id) changed else it } },
                                    onDerivative = { addDeliverable(if (d.platform.equals("Instagram", true)) "YouTube" else "Instagram", if (d.platform.equals("Instagram", true)) "Short" else "Reel", d.id) },
                                    onRemove = { pendingRemovalId = d.id },
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            if (deliverables.isEmpty()) A3Empty("No deliverables yet. Apply a template or add one manually.")
                        }
                        Alpha3Section.LEARN -> {
                            A3Title("LEARN", "Keep what should carry into the next project.")
                            A3Field("PROJECT LEARNINGS", learnings, { learnings = it }, "What worked? What should change next time?", 10)
                        }
                    }
                }

                Surface(color = CinemaSurfaceRaised, tonalElevation = 8.dp) {
                    WorkspaceAdaptiveSaveActions(
                        primaryLabel = "SAVE & SCRIPT",
                        onPrimary = { onSaveAndScript(task.id, originalRevision, draft()) },
                        onSecondary = { onSave(task.id, originalRevision, draft()) },
                    )
                }
    }

    pendingRemovalId?.let { rootId ->
        val affectedIds = CreatorContentStabilization.descendantIds(deliverables, setOf(rootId))
        val protectedIds = CreatorContentStabilization.protectedRemovalIds(deliverables, setOf(rootId))
        val count = affectedIds.size
        AlertDialog(
            onDismissRequest = { pendingRemovalId = null },
            title = { Text(if (protectedIds.isNotEmpty()) "Cannot remove published work" else "Remove output?") },
            text = {
                Text(
                    if (protectedIds.isNotEmpty())
                        "This output or a linked output has a publication record. It cannot be removed from the plan."
                    else if (count > 1)
                        "This will remove $count linked outputs from your draft plan. Save Plan to make the removal permanent."
                    else
                        "This will remove the output from your draft plan. Save Plan to make the removal permanent."
                )
            },
            confirmButton = {
                if (protectedIds.isEmpty()) {
                    TextButton(onClick = {
                        deliverables = deliverables.filterNot { it.id in affectedIds }
                        pendingRemovalId = null
                    }) { Text("REMOVE") }
                } else {
                    TextButton(onClick = { pendingRemovalId = null }) { Text("OK") }
                }
            },
            dismissButton = {
                if (protectedIds.isEmpty()) TextButton(onClick = { pendingRemovalId = null }) { Text("CANCEL") }
            },
        )
    }
}

@Composable
private fun Alpha3Overview(
    workspace: CreatorContentWorkspace,
    appliedTemplate: String,
    onApplyTemplate: (CreatorContentTemplate) -> Unit,
) {
    val progress = workspace.productionProgressPercent()
    val next = workspace.nextProductionStep()?.title ?: "No checklist step queued"
    val published = workspace.deliverables.count { it.status == CreatorDeliverableStatus.PUBLISHED }
    val pending = workspace.deliverables.size - published
    A3Title("OVERVIEW", "")
    if (appliedTemplate.isNotBlank()) Text("Template merged: $appliedTemplate", color = SuccessGreen, fontSize = 9.sp)
    Spacer(Modifier.height(8.dp))
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Text("TASKS $progress%", color = ProjectorIvory, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(7.dp)); LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            if (workspace.nextProductionStep() != null) {
                Spacer(Modifier.height(10.dp))
                Text("Next · $next", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 350.dp || LocalDensity.current.fontScale >= 1.3f
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            A3Metric("Sources", workspace.references.size.toString())
                            A3Metric("Assets", workspace.assets.size.toString())
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            A3Metric("Pending", pending.toString())
                            A3Metric("Published", published.toString())
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        A3Metric("Sources", workspace.references.size.toString())
                        A3Metric("Assets", workspace.assets.size.toString())
                        A3Metric("Pending", pending.toString())
                        A3Metric("Published", published.toString())
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("TEMPLATES", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(6.dp))
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val narrow = maxWidth < 350.dp || LocalDensity.current.fontScale >= 1.3f
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            CreatorProjectTemplates.contentTemplates.chunked(if (narrow) 1 else 2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { template ->
                        OutlinedButton(
                            onClick = { onApplyTemplate(template) },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(template.name, fontSize = 12.sp) }
                    }
                    if (row.size == 1 && !narrow) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

}

@Composable private fun A3Metric(label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = ProjectorIvory, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(label, color = MutedText, fontSize = 11.sp) } }
@Composable private fun A3Title(title: String, @Suppress("UNUSED_PARAMETER") subtitle: String) { Text(title, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)) }
@Composable private fun A3Empty(text: String) { Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) { Text(text, color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(14.dp)) } }

@Composable
private fun A3ReadOnly(label: String, value: String) {
    Text(label, color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(4.dp))
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Text(value, color = ProjectorIvory, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(13.dp))
    }
}

@Composable
private fun A3Field(label: String, value: String, onChange: (String) -> Unit, placeholder: String, minLines: Int = 1) {
    Text(label, color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), placeholder = { Text(placeholder) }, minLines = minLines, shape = RoundedCornerShape(14.dp))
    Spacer(Modifier.height(11.dp))
}

@Composable
private fun A3ReferenceCard(
    ref: CreatorProjectReference,
    onChange: (CreatorProjectReference) -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(ref.label, { onChange(ref.copy(label = it)) }, Modifier.fillMaxWidth(), label = { Text("Label") }, singleLine = true)
            Spacer(Modifier.height(6.dp)); OutlinedTextField(ref.url, { onChange(ref.copy(url = it)) }, Modifier.fillMaxWidth(), label = { Text("URL") }, singleLine = true)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (CreatorWorkspaceLinkRules.isWebUrl(ref.url)) {
                    TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("OPEN")
                    }
                }
                TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) { Text("REMOVE") }
            }
        }
    }
}

@Composable
private fun A3AssetCard(
    asset: CreatorProjectAsset,
    onChange: (CreatorProjectAsset) -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.AttachFile, null, tint = MutedGold); Spacer(Modifier.width(6.dp)); Text(asset.kind.name, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(6.dp)); OutlinedTextField(asset.label, { onChange(asset.copy(label = it)) }, Modifier.fillMaxWidth(), label = { Text("Label") }, singleLine = true)
            Spacer(Modifier.height(6.dp)); Text(asset.location, color = MutedText, fontSize = 7.5.sp, maxLines = 2)
            Spacer(Modifier.height(6.dp)); OutlinedTextField(asset.notes, { onChange(asset.copy(notes = it)) }, Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 2)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (CreatorWorkspaceLinkRules.isAssetLocation(asset.location)) {
                    TextButton(onClick = onOpen, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("OPEN")
                    }
                }
                TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) { Text("REMOVE") }
            }
        }
    }
}

@Composable
private fun A3ChecklistCard(
    item: CreatorChecklistItem,
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onChange: (CreatorChecklistItem) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                val next = when (item.status) { CreatorChecklistStatus.TODO -> CreatorChecklistStatus.DONE; CreatorChecklistStatus.DONE -> CreatorChecklistStatus.SKIPPED; CreatorChecklistStatus.SKIPPED -> CreatorChecklistStatus.TODO }
                onChange(item.copy(status = next))
            }) { Icon(if (item.status == CreatorChecklistStatus.DONE) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, "Status", tint = if (item.status == CreatorChecklistStatus.DONE) SuccessGreen else MutedGold) }
            OutlinedTextField(item.title, { onChange(item.copy(title = it)) }, Modifier.weight(1f), singleLine = true)
            IconButton(onClick = onUp, enabled = canUp) { Icon(Icons.Outlined.KeyboardArrowUp, "Move up") }
            IconButton(onClick = onDown, enabled = canDown) { Icon(Icons.Outlined.KeyboardArrowDown, "Move down") }
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove") }
        }
    }
}

@Composable
private fun A3DeliverableCard(
    deliverable: CreatorDeliverable,
    parentTitle: String?,
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onChange: (CreatorDeliverable) -> Unit,
    onDerivative: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember(deliverable.id) { mutableStateOf(false) }
    val published = deliverable.status == CreatorDeliverableStatus.PUBLISHED
    val accent = when (deliverable.status) { CreatorDeliverableStatus.PLANNED -> MutedText; CreatorDeliverableStatus.READY -> MutedGold; CreatorDeliverableStatus.PUBLISHED -> SuccessGreen }
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, accent.copy(alpha = .55f))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${deliverable.platform} · ${deliverable.format}", color = ProjectorIvory, fontWeight = FontWeight.Black, fontSize = 10.sp)
                    if (!parentTitle.isNullOrBlank()) Text("Derived from $parentTitle", color = MutedGold, fontSize = 7.5.sp)
                    if (deliverable.deadlineLabel.isNotBlank()) Text("Target ${deliverable.deadlineLabel}", color = MutedText, fontSize = 7.5.sp)
                }
                Text(deliverable.status.name, color = accent, fontSize = 7.5.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, "Details") }
            }
            OutlinedTextField(
                deliverable.title,
                { onChange(deliverable.copy(title = it)) },
                Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
                enabled = !published,
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { pickDateTime(context) { label -> onChange(deliverable.copy(deadlineLabel = label)) } }, enabled = !published) { Text(if (deliverable.deadlineLabel.isBlank()) "SET TARGET" else "CHANGE TARGET", fontSize = 7.5.sp) }
                IconButton(onClick = onUp, enabled = canUp) { Icon(Icons.Outlined.KeyboardArrowUp, "Move up") }
                IconButton(onClick = onDown, enabled = canDown) { Icon(Icons.Outlined.KeyboardArrowDown, "Move down") }
                IconButton(onClick = onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (published) "Live metadata and reopen actions are locked to Publish Studio so history stays complete."
                else "Readiness and publication are managed in Publish Studio.",
                color = if (published) SuccessGreen else MutedGold,
                fontSize = 8.sp,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onDerivative) { Text("DERIVE") }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(deliverable.platform, { onChange(deliverable.copy(platform = it)) }, Modifier.weight(1f), label = { Text("Platform") }, singleLine = true, enabled = !published)
                    OutlinedTextField(deliverable.format, { onChange(deliverable.copy(format = it)) }, Modifier.weight(1f), label = { Text("Format") }, singleLine = true, enabled = !published)
                }
                Spacer(Modifier.height(6.dp)); OutlinedTextField(deliverable.description, { onChange(deliverable.copy(description = it)) }, Modifier.fillMaxWidth(), label = { Text("Caption / description") }, minLines = 3, enabled = !published)
                Spacer(Modifier.height(6.dp)); OutlinedTextField(deliverable.tags, { onChange(deliverable.copy(tags = it)) }, Modifier.fillMaxWidth(), label = { Text("Tags / hashtags") }, minLines = 2, enabled = !published)
                Spacer(Modifier.height(6.dp)); OutlinedTextField(deliverable.thumbnailConcept, { onChange(deliverable.copy(thumbnailConcept = it)) }, Modifier.fillMaxWidth(), label = { Text("Thumbnail / cover concept") }, minLines = 2, enabled = !published)
                if (deliverable.publishedUrl.isNotBlank()) {
                    Spacer(Modifier.height(6.dp)); A3ReadOnly("LIVE URL", deliverable.publishedUrl)
                }
            }
        }
    }
}

private fun pickDateTime(context: android.content.Context, onPicked: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    DatePickerDialog(context, { _, year, month, day ->
        TimePickerDialog(context, { _, hour, minute ->
            calendar.set(year, month, day, hour, minute, 0)
            val formatter = SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault())
            onPicked(formatter.format(calendar.time))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
}
