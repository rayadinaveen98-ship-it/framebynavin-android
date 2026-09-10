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
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorDeliverable
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorProjectReference
import com.framebynavin.app.data.CreatorTask
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
    var deliverables by remember(task.id, originalRevision) { mutableStateOf(task.workspace.deliverables) }

    fun addDeliverable(platform: String, format: String) {
        if (deliverables.any { it.platform.equals(platform, true) && it.format.equals(format, true) }) return
        deliverables = deliverables + CreatorDeliverable(
            id = UUID.randomUUID().toString(),
            platform = platform,
            format = format,
            title = task.title,
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
                        Text("CONTENT PROJECT 2.0", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
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
                    V19SectionTitle("DELIVERABLES", "One project can publish to multiple platforms independently")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { addDeliverable("YouTube", "Long video") }, modifier = Modifier.weight(1f)) {
                            Text("+ YOUTUBE", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { addDeliverable("Instagram", "Reel") }, modifier = Modifier.weight(1f)) {
                            Text("+ INSTAGRAM", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (deliverables.isEmpty()) {
                        V19EmptyCard("No deliverables yet", "Add YouTube and Instagram outputs without creating separate projects.")
                    } else {
                        deliverables.forEach { deliverable ->
                            V19DeliverableCard(
                                deliverable = deliverable,
                                onChange = { changed -> deliverables = deliverables.map { if (it.id == changed.id) changed else it } },
                                onRemove = { deliverables = deliverables.filterNot { it.id == deliverable.id } },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
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
                                    deliverables = deliverables,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    ) {
                        Text("SAVE WORKSPACE", fontSize = 10.sp, fontWeight = FontWeight.Black)
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
private fun V19DeliverableCard(
    deliverable: CreatorDeliverable,
    onChange: (CreatorDeliverable) -> Unit,
    onRemove: () -> Unit,
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
                }
                Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .11f)) {
                    Text(deliverable.status.name, color = accent, fontSize = 7.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(
                value = deliverable.title,
                onValueChange = { onChange(deliverable.copy(title = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Deliverable title") },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (deliverable.status == CreatorDeliverableStatus.PLANNED) {
                    OutlinedButton(
                        onClick = { onChange(deliverable.copy(status = CreatorDeliverableStatus.READY)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("MARK READY", fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                } else if (deliverable.status == CreatorDeliverableStatus.READY) {
                    Button(
                        onClick = {
                            onChange(deliverable.copy(status = CreatorDeliverableStatus.PUBLISHED, publishedAtMillis = System.currentTimeMillis()))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    ) { Text("MARK PUBLISHED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CinemaBlack) }
                } else {
                    OutlinedButton(
                        onClick = { onChange(deliverable.copy(status = CreatorDeliverableStatus.READY, publishedAtMillis = 0L, publishedUrl = "")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("REOPEN", fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                }
                TextButton(onClick = onRemove) {
                    Icon(Icons.Outlined.DeleteOutline, "Remove deliverable", tint = MutedText, modifier = Modifier.size(16.dp))
                }
            }
            if (deliverable.status == CreatorDeliverableStatus.PUBLISHED && deliverable.publishedAtMillis > 0L) {
                Text("Published independently · project remains open until its workflow is finished.", color = SuccessGreen, fontSize = 7.8.sp)
            }
        }
    }
}
