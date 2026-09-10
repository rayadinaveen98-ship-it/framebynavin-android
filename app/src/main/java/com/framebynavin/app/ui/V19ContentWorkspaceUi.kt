package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import java.util.UUID

@Composable
internal fun V19ContentWorkspaceDialog(
    task: CreatorTask,
    onDismiss: () -> Unit,
    onSave: (String, Long, CreatorContentWorkspace) -> Unit,
) {
    val originalRevision = task.workspace.revision
    var audience by remember(task.id, originalRevision) { mutableStateOf(task.workspace.audience) }
    var viewerProblem by remember(task.id, originalRevision) { mutableStateOf(task.workspace.viewerProblem) }
    var promise by remember(task.id, originalRevision) { mutableStateOf(task.workspace.promise) }
    var angle by remember(task.id, originalRevision) { mutableStateOf(task.workspace.angle) }
    var hook by remember(task.id, originalRevision) { mutableStateOf(task.workspace.hook) }
    var script by remember(task.id, originalRevision) { mutableStateOf(task.workspace.script) }
    var references by remember(task.id, originalRevision) { mutableStateOf(task.workspace.references) }
    var checklist by remember(task.id, originalRevision) { mutableStateOf(task.workspace.checklist) }
    var assets by remember(task.id, originalRevision) { mutableStateOf(task.workspace.assets) }
    var deliverables by remember(task.id, originalRevision) { mutableStateOf(task.workspace.deliverables) }
    var learnings by remember(task.id, originalRevision) { mutableStateOf(task.workspace.learnings) }

    fun addDeliverable(platform: String, format: String) {
        if (deliverables.any { it.platform.equals(platform, true) && it.format.equals(format, true) && it.parentDeliverableId.isBlank() }) return
        deliverables = deliverables + CreatorDeliverable(
            id = UUID.randomUUID().toString(),
            platform = platform,
            format = format,
            title = task.title,
        )
    }

    fun addStandardChecklist() {
        if (checklist.isNotEmpty()) return
        checklist = listOf("Research", "Script", "Record", "Edit", "Thumbnail", "Publish").map { title ->
            CreatorChecklistItem(id = UUID.randomUUID().toString(), title = title)
        }
    }

    fun addDerivative(parent: CreatorDeliverable) {
        val defaultPlatform = if (parent.platform.equals("Instagram", true)) "YouTube" else "Instagram"
        val defaultFormat = if (defaultPlatform == "YouTube") "Short" else "Reel"
        deliverables = deliverables + CreatorDeliverable(
            id = UUID.randomUUID().toString(),
            platform = defaultPlatform,
            format = defaultFormat,
            title = parent.title.ifBlank { task.title },
            parentDeliverableId = parent.id,
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Close workspace", tint = ProjectorIvory) }
                    Column(Modifier.weight(1f)) {
                        Text("CONTENT PROJECT 2.0 · ALPHA 2", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Text(task.title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Surface(shape = RoundedCornerShape(100.dp), color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
                        Text("R${originalRevision}", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
                ) {
                    V19SectionTitle("BRIEF", "What this piece is trying to do")
                    V19Field("AUDIENCE", audience, { audience = it }, "Who is this for?")
                    V19Field("VIEWER PROBLEM", viewerProblem, { viewerProblem = it }, "What question, need or tension are they bringing?")
                    V19Field("PROMISE", promise, { promise = it }, "What will they get by staying till the end?")
                    V19Field("ANGLE", angle, { angle = it }, "What makes your take different?")

                    Spacer(Modifier.height(24.dp))
                    V19SectionTitle("HOOK + SCRIPT", "Keep the opening and full narrative in the project")
                    V19Field("HOOK", hook, { hook = it }, "The first line, visual or question", minLines = 2)
                    V19Field("SCRIPT / OUTLINE", script, { script = it }, "Write the narration, outline, beats or shot notes here…", minLines = 8)

                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        V19SectionTitle("PRODUCTION CHECKLIST", "Track production without completing the whole project", Modifier.weight(1f))
                        TextButton(onClick = { checklist = checklist + CreatorChecklistItem(id = UUID.randomUUID().toString()) }) {
                            Icon(Icons.Outlined.Add, null, tint = MutedGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ADD", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (checklist.isEmpty()) {
                        V19EmptyCard("No production checklist", "Start with a simple creator workflow or add your own steps.")
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { addStandardChecklist() }, modifier = Modifier.fillMaxWidth()) {
                            Text("USE RESEARCH → SCRIPT → RECORD → EDIT → THUMBNAIL → PUBLISH", fontSize = 7.6.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        checklist.forEach { item ->
                            V19ChecklistCard(
                                item = item,
                                onChange = { changed -> checklist = checklist.map { if (it.id == changed.id) changed else it } },
                                onRemove = { checklist = checklist.filterNot { it.id == item.id } },
                            )
                            Spacer(Modifier.height(7.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        V19SectionTitle("RESEARCH", "Links stay attached to this project", Modifier.weight(1f))
                        TextButton(onClick = {
                            references = references + CreatorProjectReference(id = UUID.randomUUID().toString())
                        }) {
                            Icon(Icons.Outlined.AddLink, null, tint = MutedGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ADD", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (references.isEmpty()) {
                        V19EmptyCard("No research links yet", "Add articles, videos, interviews, documents or source pages.")
                    } else {
                        references.forEach { reference ->
                            Surface(
                                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                RoundedCornerShape(16.dp),
                                CinemaSurface,
                                border = BorderStroke(1.dp, CinemaLine),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    OutlinedTextField(
                                        value = reference.label,
                                        onValueChange = { value -> references = references.map { if (it.id == reference.id) it.copy(label = value) else it } },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Label") },
                                        singleLine = true,
                                    )
                                    Spacer(Modifier.height(7.dp))
                                    OutlinedTextField(
                                        value = reference.url,
                                        onValueChange = { value -> references = references.map { if (it.id == reference.id) it.copy(url = value) else it } },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("URL") },
                                        singleLine = true,
                                    )
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { references = references.filterNot { it.id == reference.id } }) {
                                            Text("REMOVE", color = MutedText, fontSize = 8.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        V19SectionTitle("ASSETS + REFERENCES", "Store locations only — large media stays outside the project database", Modifier.weight(1f))
                        TextButton(onClick = {
                            assets = assets + CreatorProjectAsset(id = UUID.randomUUID().toString())
                        }) {
                            Icon(Icons.Outlined.AttachFile, null, tint = MutedGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ADD", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (assets.isEmpty()) {
                        V19EmptyCard("No asset references", "Keep a content URI, file path or web link for footage, audio, documents and thumbnail material.")
                    } else {
                        assets.forEach { asset ->
                            V19AssetCard(
                                asset = asset,
                                onChange = { changed -> assets = assets.map { if (it.id == changed.id) changed else it } },
                                onRemove = { assets = assets.filterNot { it.id == asset.id } },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    V19SectionTitle("DELIVERABLES", "Each output has its own publishing state and metadata")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { addDeliverable("YouTube", "Long video") }, modifier = Modifier.weight(1f)) {
                            Text("+ YOUTUBE", fontSize = 8.2.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { addDeliverable("Instagram", "Reel") }, modifier = Modifier.weight(1f)) {
                            Text("+ INSTAGRAM", fontSize = 8.2.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { addDeliverable("Other", "Custom") }, modifier = Modifier.weight(1f)) {
                            Text("+ CUSTOM", fontSize = 8.2.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (deliverables.isEmpty()) {
                        V19EmptyCard("No deliverables yet", "Add platform outputs without creating separate projects.")
                    } else {
                        deliverables.forEach { deliverable ->
                            V19DeliverableCard(
                                deliverable = deliverable,
                                parentTitle = deliverable.parentDeliverableId.takeIf { it.isNotBlank() }?.let { parentId ->
                                    deliverables.firstOrNull { it.id == parentId }?.title?.ifBlank { it.format }
                                },
                                onChange = { changed -> deliverables = deliverables.map { if (it.id == changed.id) changed else it } },
                                onRemove = { deliverables = deliverables.filterNot { it.id == deliverable.id || it.parentDeliverableId == deliverable.id } },
                                onDerivative = { addDerivative(deliverable) },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    V19SectionTitle("LEARNINGS", "Keep what you want to carry into the next piece")
                    V19Field(
                        "PROJECT NOTES / LESSONS",
                        learnings,
                        { learnings = it },
                        "What worked, what felt difficult, what should you repeat or change next time?",
                        minLines = 5,
                    )
                }

                Surface(color = Color(0xF20B0B0C), tonalElevation = 8.dp) {
                    Button(
                        onClick = {
                            onSave(
                                task.id,
                                originalRevision,
                                CreatorContentWorkspace(
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
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    ) {
                        Text("SAVE CONTENT PROJECT", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun V19SectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = MutedText, fontSize = 8.7.sp)
        Spacer(Modifier.height(9.dp))
    }
}

@Composable
private fun V19Field(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, minLines: Int = 1) {
    Text(label, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
    Spacer(Modifier.height(5.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = MutedText) },
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun V19EmptyCard(title: String, body: String) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(body, color = MutedText, fontSize = 8.6.sp)
        }
    }
}

@Composable
private fun V19ChecklistCard(
    item: CreatorChecklistItem,
    onChange: (CreatorChecklistItem) -> Unit,
    onRemove: () -> Unit,
) {
    val accent = when (item.status) {
        CreatorChecklistStatus.TODO -> MutedText
        CreatorChecklistStatus.DONE -> SuccessGreen
        CreatorChecklistStatus.SKIPPED -> MutedGold
    }
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), CinemaSurface, border = BorderStroke(1.dp, accent.copy(alpha = .45f))) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                val next = when (item.status) {
                    CreatorChecklistStatus.TODO -> CreatorChecklistStatus.DONE
                    CreatorChecklistStatus.DONE -> CreatorChecklistStatus.TODO
                    CreatorChecklistStatus.SKIPPED -> CreatorChecklistStatus.TODO
                }
                onChange(item.copy(status = next))
            }) {
                Icon(
                    if (item.status == CreatorChecklistStatus.DONE) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    "Toggle checklist item",
                    tint = accent,
                )
            }
            OutlinedTextField(
                value = item.title,
                onValueChange = { onChange(item.copy(title = it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Production step") },
                singleLine = true,
            )
            IconButton(onClick = {
                onChange(item.copy(status = if (item.status == CreatorChecklistStatus.SKIPPED) CreatorChecklistStatus.TODO else CreatorChecklistStatus.SKIPPED))
            }) {
                Icon(Icons.Outlined.SkipNext, "Skip checklist item", tint = if (item.status == CreatorChecklistStatus.SKIPPED) MutedGold else MutedText)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.DeleteOutline, "Remove checklist item", tint = MutedText)
            }
        }
    }
}

@Composable
private fun V19AssetCard(
    asset: CreatorProjectAsset,
    onChange: (CreatorProjectAsset) -> Unit,
    onRemove: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AttachFile, null, tint = MutedGold, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("ASSET REFERENCE", color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val kinds = CreatorAssetKind.entries
                    onChange(asset.copy(kind = kinds[(asset.kind.ordinal + 1) % kinds.size]))
                }) {
                    Text(asset.kind.name, color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.DeleteOutline, "Remove asset reference", tint = MutedText, modifier = Modifier.size(17.dp))
                }
            }
            OutlinedTextField(
                value = asset.label,
                onValueChange = { onChange(asset.copy(label = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Label") },
                singleLine = true,
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = asset.location,
                onValueChange = { onChange(asset.copy(location = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Content URI / path / web link") },
                singleLine = true,
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = asset.notes,
                onValueChange = { onChange(asset.copy(notes = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                minLines = 2,
            )
            Text("Only the reference is saved here — not the media file itself.", color = MutedText, fontSize = 7.5.sp, modifier = Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
private fun V19DeliverableCard(
    deliverable: CreatorDeliverable,
    parentTitle: String?,
    onChange: (CreatorDeliverable) -> Unit,
    onRemove: () -> Unit,
    onDerivative: () -> Unit,
) {
    val accent = when (deliverable.status) {
        CreatorDeliverableStatus.PLANNED -> MutedText
        CreatorDeliverableStatus.READY -> MutedGold
        CreatorDeliverableStatus.PUBLISHED -> SuccessGreen
    }
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, accent.copy(alpha = .65f))) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (deliverable.platform.equals("YouTube", true)) Icons.Outlined.SmartDisplay else Icons.Outlined.SlowMotionVideo,
                    null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(deliverable.platform.uppercase(), color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(deliverable.format, color = MutedText, fontSize = 8.3.sp)
                    if (parentTitle != null) Text("Derivative of $parentTitle", color = MutedGold, fontSize = 7.5.sp)
                }
                Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .11f)) {
                    Text(deliverable.status.name, color = accent, fontSize = 7.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(
                    value = deliverable.platform,
                    onValueChange = { onChange(deliverable.copy(platform = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Platform") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = deliverable.format,
                    onValueChange = { onChange(deliverable.copy(format = it)) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Format") },
                    singleLine = true,
                )
            }
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = deliverable.title,
                onValueChange = { onChange(deliverable.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Deliverable title") },
                singleLine = true,
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = deliverable.deadlineLabel,
                onValueChange = { onChange(deliverable.copy(deadlineLabel = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Deadline / publishing target") },
                placeholder = { Text("Friday 8 PM, Sept 18, after main video…") },
                singleLine = true,
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = deliverable.description,
                onValueChange = { onChange(deliverable.copy(description = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description / caption") },
                minLines = 2,
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = deliverable.tags,
                onValueChange = { onChange(deliverable.copy(tags = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tags / hashtags") },
                minLines = 2,
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = deliverable.thumbnailConcept,
                onValueChange = { onChange(deliverable.copy(thumbnailConcept = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Thumbnail / cover concept") },
                minLines = 2,
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = deliverable.publishedUrl,
                onValueChange = { onChange(deliverable.copy(publishedUrl = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Published URL") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                when (deliverable.status) {
                    CreatorDeliverableStatus.PLANNED -> OutlinedButton(
                        onClick = { onChange(deliverable.copy(status = CreatorDeliverableStatus.READY)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("MARK READY", fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                    CreatorDeliverableStatus.READY -> Button(
                        onClick = {
                            onChange(deliverable.copy(status = CreatorDeliverableStatus.PUBLISHED, publishedAtMillis = System.currentTimeMillis()))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    ) { Text("MARK PUBLISHED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CinemaBlack) }
                    CreatorDeliverableStatus.PUBLISHED -> OutlinedButton(
                        onClick = { onChange(deliverable.copy(status = CreatorDeliverableStatus.READY, publishedAtMillis = 0L)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("REOPEN", fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                }
                OutlinedButton(onClick = onDerivative, modifier = Modifier.weight(1f)) {
                    Text("DERIVATIVE", fontSize = 7.7.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.DeleteOutline, "Remove deliverable", tint = MutedText, modifier = Modifier.size(16.dp))
                }
            }
            if (deliverable.status == CreatorDeliverableStatus.PUBLISHED && deliverable.publishedAtMillis > 0L) {
                Text("Published independently · project remains open until its workflow is finished.", color = SuccessGreen, fontSize = 7.8.sp)
            }
        }
    }
}
