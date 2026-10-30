package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

private enum class Alpha5PublicationAction { PUBLISH, UPDATE, REOPEN }

/**
 * The first Publish recovery may target an existing PLANNED output only.
 * Never resolve a destination by its on-screen index. Duplicate IDs, changed
 * platform/format, READY and PUBLISHED outputs all fail closed.
 */
internal fun V211CanStageHistoricalPublishDescription(
    outputs: List<CreatorDeliverable>,
    deliverableId: String,
    platform: String,
    format: String,
): Boolean {
    if (deliverableId.isBlank() || platform.isBlank() || format.isBlank()) return false
    val output = outputs.singleOrNull { it.id == deliverableId } ?: return false
    return output.platform == platform && output.format == format &&
        output.status == CreatorDeliverableStatus.PLANNED
}

internal data class V211PublishDescriptionRecovery(
    val nonce: Long,
    val projectId: String,
    val workspaceRevision: Long,
    val deliverableId: String,
    val platform: String,
    val format: String,
    val text: String,
)

@Composable
internal fun V19PublishStudioAlpha5Dialog(
    task: CreatorTask,
    onDismiss: () -> Unit,
    onSave: (String, Long, CreatorContentWorkspace) -> Unit,
    hasHeldDraft: Boolean = false,
    onReviewHeld: () -> Unit = {},
    pendingDescriptionRecovery: V211PublishDescriptionRecovery? = null,
    onDescriptionRecoveryHandled: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    val originalRevision = task.workspace.revision
    val draftStore = remember { CreatorEditorDraftStore(context.applicationContext) }
    val recovered = remember(task.id, originalRevision) {
        draftStore.loadPublish(task.id, originalRevision)
    }
    val canonicalEditorState = remember(task.id, originalRevision) {
        task.workspace.deliverables.map(CreatorPublishWorkflow::ensureGate)
    }
    var deliverables by remember(task.id, originalRevision) {
        mutableStateOf((recovered ?: canonicalEditorState).map(CreatorPublishWorkflow::ensureGate))
    }
    var selectedId by remember(task.id, originalRevision) {
        mutableStateOf(deliverables.firstOrNull()?.id.orEmpty())
    }
    // Owner-approved historical text changes the unsaved editor only.
    // Do not invoke replace(): that method can recalculate publish readiness.
    LaunchedEffect(pendingDescriptionRecovery?.nonce) {
        val request = pendingDescriptionRecovery ?: return@LaunchedEffect
        if (request.projectId == task.id &&
            request.workspaceRevision == originalRevision &&
            V211CanStageHistoricalPublishDescription(
                task.workspace.deliverables, request.deliverableId, request.platform, request.format,
            ) &&
            V211CanStageHistoricalPublishDescription(
                deliverables, request.deliverableId, request.platform, request.format,
            )
        ) {
            deliverables = deliverables.map {
                if (it.id == request.deliverableId) it.copy(description = request.text) else it
            }
            selectedId = request.deliverableId
        }
        onDescriptionRecoveryHandled(request.nonce)
    }
    var action by remember { mutableStateOf<Alpha5PublicationAction?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    // Explicitly selected title only. Never use a random/unselected draft title.
    val scriptTitle = CreatorScriptPublishBridge.selectedTitle(task.workspace.scriptStudio)
    var pendingScriptTitleForId by remember(task.id, originalRevision) { mutableStateOf<String?>(null) }

    var draftWarningShown by remember(task.id, originalRevision) { mutableStateOf(false) }
    LaunchedEffect(deliverables) {
        delay(350)
        val snapshot = deliverables
        val saveError = withContext(Dispatchers.IO) {
            runCatching {
            if (snapshot == canonicalEditorState) draftStore.clearPublish(task.id)
            else draftStore.savePublish(task.id, originalRevision, snapshot)
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

    fun replace(changed: CreatorDeliverable) {
        val normalized = if (changed.status == CreatorDeliverableStatus.PUBLISHED) changed else changed.copy(
            status = if (CreatorPublishWorkflow.isReadyToPublish(changed)) CreatorDeliverableStatus.READY
            else CreatorDeliverableStatus.PLANNED,
        )
        deliverables = deliverables.map { if (it.id == normalized.id) normalized else it }
    }

    fun addDeliverable(platform: String, format: String) {
        val created = CreatorPublishWorkflow.ensureGate(
            CreatorDeliverable(
                id = UUID.randomUUID().toString(),
                platform = platform,
                format = format,
                title = task.title,
            )
        )
        deliverables = deliverables + created
        selectedId = created.id
    }

    fun addDerivative(parent: CreatorDeliverable, platform: String, format: String) {
        if (deliverables.any {
                it.parentDeliverableId == parent.id &&
                    it.platform.equals(platform, true) && it.format.equals(format, true)
            }) return
        val created = CreatorPublishWorkflow.createDerivative(parent, platform, format)
        deliverables = deliverables + created
        selectedId = created.id
    }

    val selected = deliverables.firstOrNull { it.id == selectedId }
    val unrecordedPublishedEdits = deliverables.any(CreatorPublishWorkflow::hasUnrecordedPublishedChanges)

    WorkspaceAdaptiveFrame {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                    Column(Modifier.weight(1f)) {
                        Text("PUBLISH", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Text("Your unsaved publishing details were recovered", color = MutedGold, fontSize = 8.5.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp))
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp).padding(bottom = 28.dp),
                ) {
                    Alpha5PublishSummary(deliverables)
                    Spacer(Modifier.height(16.dp))
                    Alpha5Title("OUTPUTS", "")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        deliverables.forEach { item ->
                            FilterChip(
                                selected = selectedId == item.id,
                                onClick = { selectedId = item.id },
                                label = { Text("${item.platform} · ${item.format}", fontSize = 12.sp) },
                                modifier = Modifier.heightIn(min = 44.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(onClick = { addDeliverable("YouTube", "Long video") }) { Text("+ YT LONG", fontSize = 8.sp) }
                        OutlinedButton(onClick = { addDeliverable("Instagram", "Reel") }) { Text("+ REEL", fontSize = 8.sp) }
                    }

                    Spacer(Modifier.height(20.dp))
                    if (selected == null) {
                        Alpha5Empty("No posts planned yet. Add one to get started.")
                    } else {
                        Alpha5DeliverableEditor(
                            deliverable = selected,
                            parentTitle = deliverables.firstOrNull { it.id == selected.parentDeliverableId }?.title,
                            onChange = { replace(it) },
                            scriptTitle = scriptTitle,
                            onUseScriptTitle = { pendingScriptTitleForId = selected.id },
                            onPublish = { action = Alpha5PublicationAction.PUBLISH; actionError = null },
                            onUpdate = { action = Alpha5PublicationAction.UPDATE; actionError = null },
                            onReopen = { action = Alpha5PublicationAction.REOPEN; actionError = null },
                            onYouTubeShort = { addDerivative(selected, "YouTube", "Short") },
                            onInstagramReel = { addDerivative(selected, "Instagram", "Reel") },
                        )
                    }
                    actionError?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = RecRed, fontSize = 9.sp)
                    }
                }

                Surface(color = CinemaSurfaceRaised, tonalElevation = 8.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
                        if (unrecordedPublishedEdits) {
                            Text(
                                "This published post has unsaved changes. Record an update or reopen it.",
                                color = RecRed,
                                fontSize = 8.5.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Button(
                            onClick = {
                                onSave(
                                    task.id,
                                    originalRevision,
                                    task.workspace.copy(revision = originalRevision, deliverables = deliverables),
                                )
                            },
                            enabled = !unrecordedPublishedEdits,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        ) { Text("SAVE PUBLISH DETAILS", fontWeight = FontWeight.Black) }
                    }
                }
    }

    val titleTarget = deliverables.firstOrNull { it.id == pendingScriptTitleForId }
    if (pendingScriptTitleForId != null) {
        AlertDialog(
            onDismissRequest = { pendingScriptTitleForId = null },
            title = { Text("Use script title?") },
            text = {
                Column {
                    Text(
                        if (titleTarget != null) "For ${titleTarget.platform} · ${titleTarget.format}"
                        else "This output is no longer available.",
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("SCRIPT TITLE", color = MutedGold, fontSize = 12.sp)
                    Text(scriptTitle)
                    if (titleTarget?.title?.isNotBlank() == true) {
                        Spacer(Modifier.height(10.dp))
                        Text("CURRENT TITLE", color = MutedGold, fontSize = 12.sp)
                        Text(titleTarget.title)
                    }
                }
            },
            confirmButton = {
                if (titleTarget != null && CreatorScriptPublishBridge.canUseTitle(titleTarget, scriptTitle)) {
                    TextButton(onClick = {
                        // Re-validate at the actual click, even if the output changed meanwhile.
                        val currentTarget = deliverables.firstOrNull { it.id == pendingScriptTitleForId }
                        if (currentTarget != null && CreatorScriptPublishBridge.canUseTitle(currentTarget, scriptTitle)) {
                            replace(CreatorScriptPublishBridge.useTitle(currentTarget, scriptTitle))
                        }
                        pendingScriptTitleForId = null
                    }) { Text("USE TITLE") }
                } else {
                    TextButton(onClick = { pendingScriptTitleForId = null }) { Text("OK") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingScriptTitleForId = null }) { Text("CANCEL") }
            },
        )
    }

    val actionDeliverable = deliverables.firstOrNull { it.id == selectedId }
    if (action != null && actionDeliverable != null) {
        Alpha5PublicationActionDialog(
            action = action!!,
            deliverable = actionDeliverable,
            onDismiss = { action = null },
            onConfirm = { atMillis, url, note ->
                val now = System.currentTimeMillis()
                actionError = when {
                    atMillis <= 0L || atMillis > now + 60_000L -> "Choose a valid publication/update time."
                    !CreatorPublishWorkflow.validPublicationUrl(url) -> "Use an HTTPS publication link."
                    else -> null
                }
                if (actionError == null) {
                    val result = runCatching {
                        when (action) {
                            Alpha5PublicationAction.PUBLISH -> CreatorPublishWorkflow.publish(actionDeliverable, atMillis, url, note)
                            Alpha5PublicationAction.UPDATE -> CreatorPublishWorkflow.recordPublishedUpdate(actionDeliverable, atMillis, url, note)
                            Alpha5PublicationAction.REOPEN -> CreatorPublishWorkflow.reopen(actionDeliverable, atMillis, note)
                            null -> actionDeliverable
                        }
                    }
                    result.onSuccess {
                        replace(it)
                        action = null
                    }.onFailure {
                        actionError = it.message ?: "Could not update this publication."
                    }
                }
            },
        )
    }
}

@Composable
private fun Alpha5PublishSummary(deliverables: List<CreatorDeliverable>) {
    val published = deliverables.count { it.status == CreatorDeliverableStatus.PUBLISHED }
    val ready = deliverables.count { it.status == CreatorDeliverableStatus.READY }
    val planned = deliverables.size - published - ready
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
            Alpha5Metric("OUTPUTS", deliverables.size.toString())
            Alpha5Metric("PLANNED", planned.toString())
            Alpha5Metric("READY", ready.toString())
            Alpha5Metric("LIVE", published.toString())
        }
    }
}

@Composable
private fun Alpha5Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Alpha5DeliverableEditor(
    deliverable: CreatorDeliverable,
    parentTitle: String?,
    onChange: (CreatorDeliverable) -> Unit,
    scriptTitle: String,
    onUseScriptTitle: () -> Unit,
    onPublish: () -> Unit,
    onUpdate: () -> Unit,
    onReopen: () -> Unit,
    onYouTubeShort: () -> Unit,
    onInstagramReel: () -> Unit,
) {
    val gateResolved = CreatorPublishWorkflow.resolvedGateCount(deliverable)
    val gateTotal = CreatorPublishWorkflow.ensureGate(deliverable).publishGate.size
    val ready = CreatorPublishWorkflow.isReadyToPublish(deliverable)
    val unrecorded = CreatorPublishWorkflow.hasUnrecordedPublishedChanges(deliverable)
    var showVariants by rememberSaveable(deliverable.id) { mutableStateOf(false) }
    var showHistory by rememberSaveable(deliverable.id) { mutableStateOf(false) }

    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${deliverable.platform.uppercase()} · ${deliverable.format.uppercase()}", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(
                        deliverable.status.name.replace('_', ' '),
                        color = when (deliverable.status) {
                            CreatorDeliverableStatus.PLANNED -> MutedText
                            CreatorDeliverableStatus.READY -> MutedGold
                            CreatorDeliverableStatus.PUBLISHED -> SuccessGreen
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text("$gateResolved/$gateTotal checks", color = MutedText, fontSize = 12.sp)
            }
            parentTitle?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(5.dp)); Text("Derived from: $it", color = MutedText, fontSize = 12.sp)
            }

            Spacer(Modifier.height(18.dp))
            Alpha5Title("DETAILS", "")
            if (CreatorScriptPublishBridge.canUseTitle(deliverable, scriptTitle)) {
                OutlinedButton(
                    onClick = onUseScriptTitle,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Icon(Icons.Outlined.Title, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("USE SCRIPT TITLE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(9.dp))
            }
            Alpha5Field("FINAL TITLE", deliverable.title, { onChange(deliverable.copy(title = it)) }, "Final platform title")
            Alpha5Field("DESCRIPTION / CAPTION", deliverable.description, { onChange(deliverable.copy(description = it)) }, "Platform description or caption", 4)
            Alpha5Field("TAGS / HASHTAGS", deliverable.tags, { onChange(deliverable.copy(tags = it)) }, "Tags, keywords or hashtags", 2)
            Alpha5Field("FINAL THUMBNAIL / COVER", deliverable.thumbnailConcept, { onChange(deliverable.copy(thumbnailConcept = it)) }, "Chosen thumbnail or cover concept", 3)
            Alpha5Field("LIVE URL", deliverable.publishedUrl, { onChange(deliverable.copy(publishedUrl = it)) }, "https://…")

            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = { showVariants = !showVariants },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text("TITLE & COVER OPTIONS", color = ProjectorIvory, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(if (showVariants) "HIDE" else "SHOW", color = MutedGold, fontSize = 12.sp)
                Icon(if (showVariants) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null, tint = MutedGold)
            }
            if (showVariants) {
            Alpha5VariantSection(
                title = "TITLE OPTIONS",
                variants = deliverable.titleVariants,
                onVariants = { onChange(deliverable.copy(titleVariants = it)) },
                onUse = { onChange(deliverable.copy(title = it)) },
            )
            Spacer(Modifier.height(14.dp))
            Alpha5VariantSection(
                title = "COVER OPTIONS",
                variants = deliverable.thumbnailVariants,
                onVariants = { onChange(deliverable.copy(thumbnailVariants = it)) },
                onUse = { onChange(deliverable.copy(thumbnailConcept = it)) },
            )
            }

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Alpha5Title("FINAL CHECKS", "", Modifier.weight(1f))
                TextButton(onClick = {
                    onChange(deliverable.copy(
                        publishGate = deliverable.publishGate + CreatorPublishGateItem(
                            id = UUID.randomUUID().toString(),
                            title = "New check",
                        )
                    ))
                }) { Text("+ CHECK", fontSize = 8.sp) }
            }
            CreatorPublishWorkflow.ensureGate(deliverable).publishGate.forEach { gate ->
                Alpha5GateCard(
                    gate = gate,
                    onChange = { changed ->
                        onChange(deliverable.copy(
                            publishGate = CreatorPublishWorkflow.ensureGate(deliverable).publishGate.map {
                                if (it.id == changed.id) changed else it
                            }
                        ))
                    },
                    onRemove = {
                        onChange(deliverable.copy(
                            publishGate = CreatorPublishWorkflow.ensureGate(deliverable).publishGate.filterNot { it.id == gate.id }
                        ))
                    },
                )
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(18.dp))
            Alpha5Title("PUBLISH", "")
            when (deliverable.status) {
                CreatorDeliverableStatus.PUBLISHED -> {
                    if (unrecorded) {
                        Text("This published post has changes. Record an update to save them.", color = RecRed, fontSize = 8.5.sp)
                        Spacer(Modifier.height(7.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onUpdate, modifier = Modifier.weight(1f)) { Text("RECORD UPDATE", fontSize = 8.sp) }
                        OutlinedButton(onClick = onReopen, enabled = !unrecorded, modifier = Modifier.weight(1f)) { Text("REOPEN", fontSize = 8.sp) }
                    }
                    if (deliverable.publishedAtMillis > 0L) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "Live since ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(deliverable.publishedAtMillis))}",
                            color = SuccessGreen,
                            fontSize = 8.5.sp,
                        )
                    }
                }
                CreatorDeliverableStatus.PLANNED, CreatorDeliverableStatus.READY -> {
                    Button(onClick = onPublish, enabled = ready, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Publish, null); Spacer(Modifier.width(5.dp)); Text("CONFIRM PUBLICATION")
                    }
                    if (!ready) {
                        Spacer(Modifier.height(5.dp))
                        Text("Add a final title and complete the required checks.", color = MutedText, fontSize = 8.2.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Alpha5Title("REPURPOSE", "")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onYouTubeShort, modifier = Modifier.weight(1f)) { Text("→ YT SHORT", fontSize = 8.sp) }
                OutlinedButton(onClick = onInstagramReel, modifier = Modifier.weight(1f)) { Text("→ IG REEL", fontSize = 8.sp) }
            }

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Alpha5Title("HISTORY", "", Modifier.weight(1f))
                TextButton(onClick = { showHistory = !showHistory },
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = deliverable.publicationHistory.isNotEmpty()) {
                    Text(if (showHistory) "HIDE" else "VIEW " + deliverable.publicationHistory.size,
                        color = MutedGold, fontSize = 12.sp)
                    Icon(if (showHistory) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null, tint = MutedGold)
                }
            }
            if (showHistory) {
                deliverable.publicationHistory.asReversed().forEach { event ->
                    Alpha5HistoryCard(event)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun Alpha5VariantSection(
    title: String,
    variants: List<CreatorVariantIdea>,
    onVariants: (List<CreatorVariantIdea>) -> Unit,
    onUse: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        TextButton(onClick = { onVariants(variants + CreatorVariantIdea(id = UUID.randomUUID().toString())) }) {
            Text("+ IDEA", fontSize = 8.sp)
        }
    }
    variants.forEach { variant ->
        Surface(Modifier.fillMaxWidth().padding(bottom = 6.dp), RoundedCornerShape(14.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
            Column(Modifier.padding(10.dp)) {
                OutlinedTextField(
                    value = variant.text,
                    onValueChange = { value -> onVariants(variants.map { if (it.id == variant.id) it.copy(text = value) else it }) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text("Alternative") },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onUse(variant.text) }, enabled = variant.text.isNotBlank()) { Text("USE") }
                    TextButton(onClick = { onVariants(variants.filterNot { it.id == variant.id }) }) { Text("REMOVE", color = MutedText) }
                }
            }
        }
    }
    if (variants.isEmpty()) Text("No alternatives saved yet.", color = MutedText, fontSize = 8.sp)
}

@Composable
private fun Alpha5GateCard(
    gate: CreatorPublishGateItem,
    onChange: (CreatorPublishGateItem) -> Unit,
    onRemove: () -> Unit,
) {
    val protectedCheck = CreatorPublishWorkflow.isProtectedCheck(gate)
    val accent = when (gate.status) {
        CreatorPublishGateStatus.TODO -> MutedText
        CreatorPublishGateStatus.DONE -> SuccessGreen
        CreatorPublishGateStatus.SKIPPED -> MutedGold
    }
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, accent.copy(alpha = .5f))) {
        Column(Modifier.padding(10.dp)) {
            OutlinedTextField(
                value = gate.title,
                onValueChange = { onChange(gate.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !protectedCheck,
                label = { Text("Check") },
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = gate.required, onCheckedChange = { onChange(gate.copy(required = it)) }, enabled = !protectedCheck)
                Text(if (gate.required) "Required" else "Optional", color = MutedText, fontSize = 8.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onChange(gate.copy(status = CreatorPublishGateStatus.DONE)) }) { Text("DONE", fontSize = 8.sp) }
                TextButton(onClick = { onChange(gate.copy(status = CreatorPublishGateStatus.SKIPPED)) }, enabled = !gate.required && !protectedCheck) { Text("SKIP", fontSize = 8.sp) }
                TextButton(onClick = { onChange(gate.copy(status = CreatorPublishGateStatus.TODO)) }) { Text("RESET", fontSize = 8.sp) }
                IconButton(onClick = onRemove, enabled = !protectedCheck) { Icon(Icons.Outlined.DeleteOutline, "Remove", tint = MutedText) }
            }
        }
    }
}

@Composable
private fun Alpha5HistoryCard(event: CreatorPublicationEvent) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(11.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.kind.name, color = when (event.kind) {
                    CreatorPublicationEventKind.PUBLISHED -> SuccessGreen
                    CreatorPublicationEventKind.UPDATED -> MutedGold
                    CreatorPublicationEventKind.REOPENED -> RecRed
                }, fontSize = 8.sp, fontWeight = FontWeight.Black)
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.atMillis)), color = MutedText, fontSize = 7.5.sp)
            }
            if (event.titleSnapshot.isNotBlank()) Text(event.titleSnapshot, color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            if (event.url.isNotBlank()) Text(event.url, color = MutedText, fontSize = 7.8.sp, maxLines = 1)
            if (event.note.isNotBlank()) Text(event.note, color = MutedGold, fontSize = 8.sp)
        }
    }
}

@Composable
private fun Alpha5PublicationActionDialog(
    action: Alpha5PublicationAction,
    deliverable: CreatorDeliverable,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String) -> Unit,
) {
    val context = LocalContext.current
    var at by remember(action, deliverable.id) { mutableLongStateOf(System.currentTimeMillis()) }
    var url by remember(action, deliverable.id) { mutableStateOf(deliverable.publishedUrl) }
    var note by remember(action, deliverable.id) { mutableStateOf("") }
    val title = when (action) {
        Alpha5PublicationAction.PUBLISH -> "Confirm publication"
        Alpha5PublicationAction.UPDATE -> "Record publication update"
        Alpha5PublicationAction.REOPEN -> "Reopen deliverable"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text(title, color = ProjectorIvory, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${deliverable.platform} · ${deliverable.format}", color = MutedGold, fontSize = 9.sp)
                if (action != Alpha5PublicationAction.REOPEN) {
                    OutlinedButton(onClick = {
                        val date = Calendar.getInstance().apply { timeInMillis = at }
                        DatePickerDialog(context, { _, year, month, day ->
                            date.set(year, month, day)
                            TimePickerDialog(context, { _, hour, minute ->
                                date.set(Calendar.HOUR_OF_DAY, hour)
                                date.set(Calendar.MINUTE, minute)
                                date.set(Calendar.SECOND, 0)
                                at = date.timeInMillis
                            }, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), false).show()
                        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(at)))
                    }
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Live URL (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (action == Alpha5PublicationAction.REOPEN) "Reason / note" else "History note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                if (action == Alpha5PublicationAction.REOPEN) {
                    Text("Reopening keeps the publication record and sets this post back to Ready.", color = MutedText, fontSize = 8.5.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    if (action == Alpha5PublicationAction.REOPEN) System.currentTimeMillis() else at,
                    if (action == Alpha5PublicationAction.REOPEN) deliverable.publishedUrl else url,
                    note,
                )
            }) { Text(if (action == Alpha5PublicationAction.REOPEN) "REOPEN" else "CONFIRM") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}

@Composable
private fun Alpha5Title(title: String, @Suppress("UNUSED_PARAMETER") subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(11.dp))
    }
}

@Composable
private fun Alpha5Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
) {
    Text(label, color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        placeholder = { Text(placeholder, color = MutedText) },
        shape = RoundedCornerShape(14.dp),
    )
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun Alpha5Empty(text: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Text(text, color = MutedText, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
    }
}
