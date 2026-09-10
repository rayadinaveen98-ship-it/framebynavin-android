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
import androidx.compose.ui.text.font.FontWeight
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
    OVERVIEW("Overview"), BRIEF("Brief"), SCRIPT("Script"), RESEARCH("Research"), ASSETS("Assets"),
    PRODUCTION("Production"), DELIVERABLES("Deliverables"), LEARN("Learn")
}

@Composable
internal fun V19ContentWorkspaceAlpha3Dialog(
    task: CreatorTask,
    onDismiss: () -> Unit,
    onSave: (String, Long, CreatorContentWorkspace) -> Unit,
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
    var assetError by remember(task.id, originalRevision) { mutableStateOf<String?>(null) }

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

    LaunchedEffect(audience, viewerProblem, promise, angle, references, checklist, assets, deliverables, learnings) {
        delay(350)
        val snapshot = draft()
        withContext(Dispatchers.IO) {
            if (snapshot == task.workspace) draftStore.clearProject(task.id)
            else draftStore.saveProject(task.id, originalRevision, snapshot)
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
                val mime = context.contentResolver.getType(uri).orEmpty()
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Close", tint = ProjectorIvory) }
                    Column(Modifier.weight(1f)) {
                        Text("CONTENT PROJECT 2.0 · RC2", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(task.title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Text("R$originalRevision", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                if (recovered != null) {
                    Surface(Modifier.fillMaxWidth(), color = MutedGold.copy(alpha = .08f)) {
                        Text("Recovered unsaved Plan & Produce draft", color = MutedGold, fontSize = 8.5.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp))
                    }
                }

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Alpha3Section.entries.forEach { item ->
                        FilterChip(
                            selected = section == item,
                            onClick = { section = item },
                            label = { Text(item.label, fontSize = 8.5.sp) },
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
                            onJump = { section = it },
                        )
                        Alpha3Section.BRIEF -> {
                            A3Title("BRIEF", "Define the audience, tension, promise and angle.")
                            A3Field("AUDIENCE", audience, { audience = it }, "Who is this for?")
                            A3Field("VIEWER PROBLEM", viewerProblem, { viewerProblem = it }, "What question or need are they bringing?")
                            A3Field("PROMISE", promise, { promise = it }, "What will they get by staying?")
                            A3Field("ANGLE", angle, { angle = it }, "Why is your take different?")
                        }
                        Alpha3Section.SCRIPT -> {
                            A3Title("SCRIPT", "Script Studio is the single writing source of truth. Edit hooks, beats and narration there so structured and compiled script versions never diverge.")
                            A3ReadOnly("CURRENT HOOK", hook.ifBlank { "No hook saved yet." })
                            Spacer(Modifier.height(10.dp))
                            A3ReadOnly("COMPILED SCRIPT", script.ifBlank { "No structured script saved yet. Open Script Studio from the workspace hub." })
                        }
                        Alpha3Section.RESEARCH -> {
                            A3Title("RESEARCH", "Sources stay attached to the project.")
                            Button(onClick = { references = references + CreatorProjectReference(id = UUID.randomUUID().toString()) }) {
                                Icon(Icons.Outlined.AddLink, null); Spacer(Modifier.width(5.dp)); Text("ADD SOURCE")
                            }
                            Spacer(Modifier.height(10.dp))
                            references.forEach { ref ->
                                A3ReferenceCard(ref, { changed -> references = references.map { if (it.id == changed.id) changed else it } }) {
                                    references = references.filterNot { it.id == ref.id }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            if (references.isEmpty()) A3Empty("No research sources yet.")
                        }
                        Alpha3Section.ASSETS -> {
                            A3Title("ASSETS", "Pick real files from Android. Only durable content URI references are stored; media bytes are never copied into the project JSON.")
                            Button(onClick = { assetPicker.launch(arrayOf("image/*", "video/*", "audio/*", "application/pdf", "text/*")) }) {
                                Icon(Icons.Outlined.AttachFile, null); Spacer(Modifier.width(5.dp)); Text("PICK FROM DEVICE")
                            }
                            assetError?.let {
                                Spacer(Modifier.height(8.dp))
                                Text(it, color = RecRed, fontSize = 8.5.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            assets.forEach { asset ->
                                A3AssetCard(asset, { changed -> assets = assets.map { if (it.id == changed.id) changed else it } }) {
                                    assets = assets.filterNot { it.id == asset.id }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                            if (assets.isEmpty()) A3Empty("No assets linked yet.")
                        }
                        Alpha3Section.PRODUCTION -> {
                            A3Title("PRODUCTION CHECKLIST", "Flexible supporting steps for this content project. The main project workflow remains authoritative for stage progress, Project Pulse and reminders.")
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
                            if (checklist.isEmpty()) A3Empty("No supporting production checklist yet. Apply a template from Overview.")
                        }
                        Alpha3Section.DELIVERABLES -> {
                            A3Title("DELIVERABLES", "Plan outputs and derivatives here. Readiness, publication, live links and publication history are controlled only in Publish Studio.")
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
                                    onRemove = {
                                        val removed = CreatorContentStabilization.descendantIds(deliverables, setOf(d.id))
                                        deliverables = deliverables.filterNot { it.id in removed }
                                    },
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
                    Button(
                        onClick = { onSave(task.id, originalRevision, draft()) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp).height(50.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    ) { Text("SAVE CONTENT PROJECT", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
private fun Alpha3Overview(
    workspace: CreatorContentWorkspace,
    appliedTemplate: String,
    onApplyTemplate: (CreatorContentTemplate) -> Unit,
    onJump: (Alpha3Section) -> Unit,
) {
    val progress = workspace.productionProgressPercent()
    val next = workspace.nextProductionStep()?.title ?: "No checklist step queued"
    val published = workspace.deliverables.count { it.status == CreatorDeliverableStatus.PUBLISHED }
    val pending = workspace.deliverables.size - published
    A3Title("PROJECT COMMAND CENTER", "Checklist completion is supporting context; main project workflow remains the execution authority.")
    if (appliedTemplate.isNotBlank()) Text("Template merged: $appliedTemplate", color = SuccessGreen, fontSize = 9.sp)
    Spacer(Modifier.height(8.dp))
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Text("CHECKLIST $progress%", color = ProjectorIvory, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Spacer(Modifier.height(7.dp)); LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp)); Text("Next checklist item: $next", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                A3Metric("Sources", workspace.references.size.toString())
                A3Metric("Assets", workspace.assets.size.toString())
                A3Metric("Pending", pending.toString())
                A3Metric("Published", published.toString())
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("TEMPLATES", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        CreatorProjectTemplates.contentTemplates.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                row.forEach { template ->
                    OutlinedButton(onClick = { onApplyTemplate(template) }, modifier = Modifier.weight(1f)) {
                        Text(template.name, fontSize = 8.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("JUMP TO", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(Alpha3Section.SCRIPT, Alpha3Section.PRODUCTION, Alpha3Section.ASSETS, Alpha3Section.DELIVERABLES).forEach { target ->
            OutlinedButton(onClick = { onJump(target) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(target.label, fontSize = 7.4.sp)
            }
        }
    }
}

@Composable private fun A3Metric(label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = ProjectorIvory, fontWeight = FontWeight.Black, fontSize = 16.sp); Text(label, color = MutedText, fontSize = 7.5.sp) } }
@Composable private fun A3Title(title: String, subtitle: String) { Text(title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black); Text(subtitle, color = MutedText, fontSize = 8.7.sp); Spacer(Modifier.height(10.dp)) }
@Composable private fun A3Empty(text: String) { Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) { Text(text, color = MutedText, fontSize = 9.sp, modifier = Modifier.padding(14.dp)) } }

@Composable
private fun A3ReadOnly(label: String, value: String) {
    Text(label, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(4.dp))
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Text(value, color = ProjectorIvory, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(13.dp))
    }
}

@Composable
private fun A3Field(label: String, value: String, onChange: (String) -> Unit, placeholder: String, minLines: Int = 1) {
    Text(label, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), placeholder = { Text(placeholder) }, minLines = minLines, shape = RoundedCornerShape(14.dp))
    Spacer(Modifier.height(11.dp))
}

@Composable
private fun A3ReferenceCard(ref: CreatorProjectReference, onChange: (CreatorProjectReference) -> Unit, onRemove: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(ref.label, { onChange(ref.copy(label = it)) }, Modifier.fillMaxWidth(), label = { Text("Label") }, singleLine = true)
            Spacer(Modifier.height(6.dp)); OutlinedTextField(ref.url, { onChange(ref.copy(url = it)) }, Modifier.fillMaxWidth(), label = { Text("URL") }, singleLine = true)
            TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) { Text("REMOVE") }
        }
    }
}

@Composable
private fun A3AssetCard(asset: CreatorProjectAsset, onChange: (CreatorProjectAsset) -> Unit, onRemove: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.AttachFile, null, tint = MutedGold); Spacer(Modifier.width(6.dp)); Text(asset.kind.name, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(6.dp)); OutlinedTextField(asset.label, { onChange(asset.copy(label = it)) }, Modifier.fillMaxWidth(), label = { Text("Label") }, singleLine = true)
            Spacer(Modifier.height(6.dp)); Text(asset.location, color = MutedText, fontSize = 7.5.sp, maxLines = 2)
            Spacer(Modifier.height(6.dp)); OutlinedTextField(asset.notes, { onChange(asset.copy(notes = it)) }, Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 2)
            TextButton(onClick = onRemove, modifier = Modifier.align(Alignment.End)) { Text("REMOVE") }
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
