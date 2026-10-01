package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.CreatorChecklistStatus
import com.framebynavin.app.data.CreatorContentBlueprint
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.WorkflowStage
import com.framebynavin.app.data.productionProgressPercent
import com.framebynavin.app.ui.theme.*

/**
 * V150 project command center. Existing Blueprint, Production Board, Script Studio and Publish
 * Studio remain intact; this screen changes hierarchy so a creator can immediately see where the
 * project is, what comes next and which workspace should be opened.
 */
@Composable
internal fun V19ContentWorkspaceAlpha6Hub(
    task: CreatorTask,
    blueprint: CreatorContentBlueprint,
    onDismiss: () -> Unit,
    onOpenBlueprint: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenScript: () -> Unit,
    onOpenPublish: () -> Unit,
) {
    val workspace = task.workspace
    val template = CreatorWorkflowEngine.templateFor(task)
    val stageIndex = CreatorWorkflowEngine.stageIndex(task)
    val currentStage = CreatorWorkflowEngine.currentStage(task)
    val nextAction = CreatorWorkflowEngine.nextAction(task)
    val workflowProgress = CreatorWorkflowEngine.progress(task)
    val workspaceProgress = workspace.productionProgressPercent()
    val overallProgress = maxOf(workflowProgress, workspaceProgress).coerceIn(0, 100)

    val checklistDone = workspace.checklist.count { it.status == CreatorChecklistStatus.DONE }
    val checklistTotal = workspace.checklist.size
    val ready = workspace.deliverables.count { it.status == CreatorDeliverableStatus.READY }
    val published = workspace.deliverables.count { it.status == CreatorDeliverableStatus.PUBLISHED }
    val scriptBeats = workspace.scriptStudio?.beats?.size ?: 0
    val scriptMeta = when {
        scriptBeats > 0 -> "$scriptBeats script beat${if (scriptBeats == 1) "" else "s"} ready"
        workspace.script.isNotBlank() || workspace.hook.isNotBlank() -> "Draft in progress"
        else -> "Not started"
    }

    val primaryAction = workspaceActionForStage(
        stage = currentStage,
        onOpenProject = onOpenProject,
        onOpenScript = onOpenScript,
        onOpenPublish = onOpenPublish,
    )
    val primaryLabel = when {
        currentStage.id in SCRIPT_STAGE_IDS -> "CONTINUE SCRIPT"
        currentStage.id in PUBLISH_STAGE_IDS || CreatorWorkflowEngine.isPublicationStage(currentStage) -> "PREPARE PUBLISH"
        task.status.name == "DONE" -> "OPEN PROJECT"
        else -> "CONTINUE ${currentStage.label.uppercase()}"
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("CLOSE") }
                    Column(Modifier.weight(1f)) {
                        Text("PROJECT WORKSPACE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
                        Text(task.title, color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MutedGold.copy(alpha = .09f),
                        border = BorderStroke(1.dp, MutedGold.copy(alpha = .3f)),
                    ) {
                        Text("$overallProgress%", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }

                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkspacePill(blueprint.modeLabel)
                    WorkspacePill(blueprint.archetypeLabel)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WorkspacePill(task.platform)
                    WorkspacePill(task.contentType)
                }

                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { overallProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = RecRed,
                    trackColor = CinemaLine,
                )

                Spacer(Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = CinemaSurfaceRaised,
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .45f)),
                ) {
                    Column(Modifier.padding(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("CURRENT STAGE", color = RecRed, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(Modifier.height(3.dp))
                                Text(currentStage.label, color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
                            }
                            Text("${stageIndex + 1}/${template.stages.size}", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(9.dp))
                        Text("NEXT MOVE", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                        Text(nextAction, color = ProjectorIvory, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = primaryAction,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        ) {
                            Text(primaryLabel, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudDone, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("PROJECT SAVED", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text("Workspace changes stay connected to this project.", color = MutedText, fontSize = 8.5.sp)
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionEyebrow("PRODUCTION WORKFLOW")
                Text(template.label, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("See exactly where the project is without opening every tool.", color = MutedText, fontSize = 9.5.sp)
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        template.stages.forEachIndexed { index, stage ->
                            WorkflowTimelineRow(
                                stage = stage,
                                completed = task.status.name == "DONE" || index < stageIndex,
                                current = task.status.name != "DONE" && index == stageIndex,
                                isLast = index == template.stages.lastIndex,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                SectionEyebrow("WORKSPACES")
                Text("Everything for this project", color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("Strategy, production, writing and publishing remain separate—but connected.", color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)

                Spacer(Modifier.height(14.dp))
                WorkspaceToolCard(
                    icon = Icons.Outlined.AutoAwesome,
                    eyebrow = "BLUEPRINT",
                    title = blueprint.headline,
                    body = "Strategy, workflow guidance and project-specific prompts.",
                    meta = "${blueprint.modeLabel} · ${blueprint.archetypeLabel}",
                    status = "READY",
                    onClick = onOpenBlueprint,
                )
                Spacer(Modifier.height(9.dp))
                WorkspaceToolCard(
                    icon = Icons.Outlined.Dashboard,
                    eyebrow = "PRODUCTION BOARD",
                    title = "Plan & Produce",
                    body = "Brief, research, references, assets, checklist and outputs.",
                    meta = if (checklistTotal > 0) "$checklistDone/$checklistTotal checklist · ${workspace.assets.size} assets" else "${workspace.assets.size} assets · ${workspace.deliverables.size} outputs",
                    status = when {
                        checklistTotal > 0 && checklistDone == checklistTotal -> "READY"
                        workspace.isEmpty() -> "NOT STARTED"
                        else -> "IN PROGRESS"
                    },
                    onClick = onOpenProject,
                )
                Spacer(Modifier.height(9.dp))
                WorkspaceToolCard(
                    icon = Icons.Outlined.EditNote,
                    eyebrow = "SCRIPT STUDIO",
                    title = "Write the piece",
                    body = "Hook, narration, structured beats and visual notes.",
                    meta = scriptMeta,
                    status = when {
                        scriptBeats > 0 || workspace.script.isNotBlank() -> "IN PROGRESS"
                        else -> "NOT STARTED"
                    },
                    onClick = onOpenScript,
                )
                Spacer(Modifier.height(9.dp))
                WorkspaceToolCard(
                    icon = Icons.Outlined.Publish,
                    eyebrow = "PUBLISH STUDIO",
                    title = "Package & Publish",
                    body = "Titles, covers, descriptions, deliverables and final publish checks.",
                    meta = "$ready ready · $published published",
                    status = when {
                        published > 0 -> "LIVE"
                        ready > 0 -> "READY"
                        workspace.deliverables.isNotEmpty() -> "IN PROGRESS"
                        else -> "NOT STARTED"
                    },
                    onClick = onOpenPublish,
                )

                Spacer(Modifier.height(28.dp))
                Text(
                    "One project. One production history. Every workspace stays connected.",
                    color = MutedText,
                    fontSize = 8.5.sp,
                    modifier = Modifier.padding(bottom = 22.dp),
                )
            }
        }
    }
}

@Composable
private fun WorkspacePill(label: String) {
    if (label.isBlank()) return
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Text(label, color = MutedText, fontSize = 8.3.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), maxLines = 1)
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Text(text, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    Spacer(Modifier.height(3.dp))
}

@Composable
private fun WorkflowTimelineRow(
    stage: WorkflowStage,
    completed: Boolean,
    current: Boolean,
    isLast: Boolean,
) {
    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                when {
                    completed -> "✓"
                    current -> "●"
                    else -> "○"
                },
                color = when {
                    completed -> MutedGold
                    current -> RecRed
                    else -> MutedText
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
            )
            if (!isLast) {
                Box(Modifier.width(1.dp).height(28.dp), contentAlignment = Alignment.Center) {
                    Divider(color = if (completed) MutedGold.copy(alpha = .5f) else CinemaLine, modifier = Modifier.fillMaxHeight().width(1.dp))
                }
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f).padding(bottom = if (isLast) 3.dp else 8.dp)) {
            Text(stage.label, color = if (current) ProjectorIvory else if (completed) ProjectorIvory.copy(alpha = .82f) else MutedText, fontSize = 11.sp, fontWeight = if (current) FontWeight.Black else FontWeight.Bold)
            if (current) Text(stage.action, color = MutedText, fontSize = 8.7.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WorkspaceToolCard(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    body: String,
    meta: String,
    status: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MutedGold.copy(alpha = .08f),
                border = BorderStroke(1.dp, MutedGold.copy(alpha = .18f)),
            ) {
                Icon(icon, null, tint = MutedGold, modifier = Modifier.padding(10.dp).size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(eyebrow, color = RecRed, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                    Spacer(Modifier.weight(1f))
                    Text(status, color = statusColor(status), fontSize = 7.4.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(2.dp))
                Text(title, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(body, color = MutedText, fontSize = 8.9.sp, lineHeight = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(meta, color = MutedGold, fontSize = 8.1.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun workspaceActionForStage(
    stage: WorkflowStage,
    onOpenProject: () -> Unit,
    onOpenScript: () -> Unit,
    onOpenPublish: () -> Unit,
): () -> Unit = when {
    stage.id in SCRIPT_STAGE_IDS -> onOpenScript
    stage.id in PUBLISH_STAGE_IDS || stage.id == "published" || stage.id == "send" -> onOpenPublish
    else -> onOpenProject
}

private fun statusColor(status: String): Color = when (status) {
    "READY", "LIVE" -> MutedGold
    "IN PROGRESS" -> RecRed
    else -> MutedText
}

private val SCRIPT_STAGE_IDS = setOf("script", "narration", "outline", "draft", "commentary", "guide_script")
private val PUBLISH_STAGE_IDS = setOf("package", "packaging", "thumbnail", "metadata", "upload", "cover", "artwork", "recipe_metadata", "presentation")
