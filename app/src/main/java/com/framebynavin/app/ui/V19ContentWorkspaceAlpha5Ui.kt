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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

private enum class Alpha5PublicationAction { PUBLISH, UPDATE, REOPEN }

@Composable
internal fun V19ContentWorkspaceAlpha5Hub(
    task: CreatorTask,
    onDismiss: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenScript: () -> Unit,
    onOpenPublish: () -> Unit,
) {
    val workspace = task.workspace
    val published = workspace.deliverables.count { it.status == CreatorDeliverableStatus.PUBLISHED }
    val ready = workspace.deliverables.count { it.status == CreatorDeliverableStatus.READY }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Close", tint = ProjectorIvory) }
                    Column(Modifier.weight(1f)) {
                        Text("CONTENT PROJECT 2.0 · ALPHA 5", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        Text(task.title, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Text("R${workspace.revision}", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(20.dp))
                Text("CREATOR WORKSPACE", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Plan the work, shape the script, then publish each output without closing the whole project.", color = MutedText, fontSize = 10.sp)

                Spacer(Modifier.height(18.dp))
                Alpha5HubCard(
                    Icons.Outlined.Dashboard,
                    "PROJECT COMMAND CENTER",
                    "Plan & Produce",
                    "Brief, research, assets, production steps, templates, deliverables and learnings.",
                    "${workspace.productionProgressPercent()}% production · ${workspace.deliverables.size} outputs",
                    onOpenProject,
                )
                Spacer(Modifier.height(10.dp))
                Alpha5HubCard(
                    Icons.Outlined.EditNote,
                    "SCRIPT STUDIO",
                    "Write & Prepare",
                    "Hooks, title ideas, structured beats, narration, visuals, B-roll and recording readiness.",
                    "Structured script · creator notes · readiness",
                    onOpenScript,
                )
                Spacer(Modifier.height(10.dp))
                Alpha5HubCard(
                    Icons.Outlined.RocketLaunch,
                    "ALPHA 5",
                    "Publish Studio",
                    "Final metadata, title/cover variants, pre-publish gates, publication history and repurposing.",
                    "$ready ready · $published published",
                    onOpenPublish,
                )

                Spacer(Modifier.weight(1f))
                Text(
                    "Publishing one deliverable never completes this project or its remaining outputs.",
                    color = MutedText,
                    fontSize = 8.5.sp,
                    modifier = Modifier.padding(bottom = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun Alpha5HubCard(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    body: String,
    meta: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MutedGold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(eyebrow, color = RecRed, fontSize = 7.8.sp, fontWeight = FontWeight.Black)
                Text(title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(body, color = MutedText, fontSize = 9.5.sp)
                Spacer(Modifier.height(5.dp))
                Text(meta, color = MutedGold, fontSize = 8.3.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MutedText)
        }
    }
}

@Composable
internal fun V19PublishStudioAlpha5Dialog(
    task: CreatorTask,
    onDismiss: () -> Unit,
    onSave: (String, Long, CreatorContentWorkspace) -> Unit,
) {
    val originalRevision = task.workspace.revision
    var deliverables by remember(task.id, originalRevision) {
        mutableStateOf(task.workspace.deliverables.map(CreatorPublishWorkflow::ensureGate))
    }
    var selectedId by remember(task.id, originalRevision) {
        mutableStateOf(deliverables.firstOrNull()?.id.orEmpty())
    }
    var action by remember { mutableStateOf<Alpha5PublicationAction?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                    Column(Modifier.weight(1f)) {
                        Text("PUBLISH STUDIO · ALPHA 5", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        Text(task.title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Text("R$originalRevision", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp).padding(bottom = 28.dp),
                ) {
                    Alpha5PublishSummary(deliverables)
                    Spacer(Modifier.height(16.dp))
                    Alpha5Title("OUTPUTS", "Choose the exact platform deliverable you are preparing.")
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        deliverables.forEach { item ->
                            FilterChip(
                                selected = selectedId == item.id,
                                onClick = { selectedId = item.id },
                                label = { Text("${item.platform} · ${item.format}", fontSize = 8.sp) },
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
                        Alpha5Empty("No deliverables yet. Add one here or create outputs from Plan & Produce.")
                    } else {
                        Alpha5DeliverableEditor(
                            deliverable = selected,
                            parentTitle = deliverables.firstOrNull { it.id == selected.parentDeliverableId }?.title,
                            onChange = { replace(it) },
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
                                "A published output has metadata changes that are not in its history yet. Record an update or reopen it before saving.",
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
                        ) { Text("SAVE PUBLISH WORKFLOW", fontWeight = FontWeight.Black) }
                    }
                }
            }
        }
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
        Text(value, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text(label, color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Alpha5DeliverableEditor(
    deliverable: CreatorDeliverable,
    parentTitle: String?,
    onChange: (CreatorDeliverable) -> Unit,
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

    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${deliverable.platform.uppercase()} · ${deliverable.format.uppercase()}", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
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
                Text("$gateResolved/$gateTotal checks", color = MutedText, fontSize = 8.sp)
            }
            parentTitle?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(5.dp)); Text("Derived from: $it", color = MutedText, fontSize = 8.sp)
            }

            Spacer(Modifier.height(18.dp))
            Alpha5Title("FINAL METADATA", "These fields are snapshotted into publication history.")
            Alpha5Field("FINAL TITLE", deliverable.title, { onChange(deliverable.copy(title = it)) }, "Final platform title")
            Alpha5Field("DESCRIPTION / CAPTION", deliverable.description, { onChange(deliverable.copy(description = it)) }, "Platform description or caption", 4)
            Alpha5Field("TAGS / HASHTAGS", deliverable.tags, { onChange(deliverable.copy(tags = it)) }, "Tags, keywords or hashtags", 2)
            Alpha5Field("FINAL THUMBNAIL / COVER", deliverable.thumbnailConcept, { onChange(deliverable.copy(thumbnailConcept = it)) }, "Chosen thumbnail or cover concept", 3)
            Alpha5Field("LIVE URL", deliverable.publishedUrl, { onChange(deliverable.copy(publishedUrl = it)) }, "https://…")

            Spacer(Modifier.height(12.dp))
            Alpha5VariantSection(
                title = "TITLE VARIANTS",
                variants = deliverable.titleVariants,
                onVariants = { onChange(deliverable.copy(titleVariants = it)) },
                onUse = { onChange(deliverable.copy(title = it)) },
            )
            Spacer(Modifier.height(14.dp))
            Alpha5VariantSection(
                title = "THUMBNAIL / COVER VARIANTS",
                variants = deliverable.thumbnailVariants,
                onVariants = { onChange(deliverable.copy(thumbnailVariants = it)) },
                onUse = { onChange(deliverable.copy(thumbnailConcept = it)) },
            )

            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Alpha5Title("PRE-PUBLISH GATE", "Required TODO items block publication.", Modifier.weight(1f))
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
            Alpha5Title("PUBLISH", "Confirmation records a snapshot; it does not finish the parent project.")
            when (deliverable.status) {
                CreatorDeliverableStatus.PUBLISHED -> {
                    if (unrecorded) {
                        Text("Metadata changed since the last publication record. Record an update before saving.", color = RecRed, fontSize = 8.5.sp)
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
                        Text("Choose a final title and resolve every required gate item first.", color = MutedText, fontSize = 8.2.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Alpha5Title("REPURPOSE", "Create linked outputs without duplicating the parent project.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onYouTubeShort, modifier = Modifier.weight(1f)) { Text("→ YT SHORT", fontSize = 8.sp) }
                OutlinedButton(onClick = onInstagramReel, modifier = Modifier.weight(1f)) { Text("→ IG REEL", fontSize = 8.sp) }
            }

            Spacer(Modifier.height(18.dp))
            Alpha5Title("PUBLICATION HISTORY", "Append-only snapshots of publish, correction and reopen actions.")
            if (deliverable.publicationHistory.isEmpty()) {
                Alpha5Empty("No publication events yet.")
            } else {
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
                label = { Text("Check") },
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = gate.required, onCheckedChange = { onChange(gate.copy(required = it)) })
                Text(if (gate.required) "Required" else "Optional", color = MutedText, fontSize = 8.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onChange(gate.copy(status = CreatorPublishGateStatus.DONE)) }) { Text("DONE", fontSize = 8.sp) }
                TextButton(onClick = { onChange(gate.copy(status = CreatorPublishGateStatus.SKIPPED)) }) { Text("SKIP", fontSize = 8.sp) }
                TextButton(onClick = { onChange(gate.copy(status = CreatorPublishGateStatus.TODO)) }) { Text("RESET", fontSize = 8.sp) }
                IconButton(onClick = onRemove) { Icon(Icons.Outlined.DeleteOutline, "Remove", tint = MutedText) }
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
                    Text("Reopening keeps the historical publication snapshot but returns this output to Ready. The parent project is unchanged.", color = MutedText, fontSize = 8.5.sp)
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
private fun Alpha5Title(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = MutedText, fontSize = 8.5.sp)
        Spacer(Modifier.height(8.dp))
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
    Text(label, color = MutedGold, fontSize = 7.7.sp, fontWeight = FontWeight.Black)
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
        Text(text, color = MutedText, fontSize = 8.5.sp, modifier = Modifier.padding(12.dp))
    }
}
