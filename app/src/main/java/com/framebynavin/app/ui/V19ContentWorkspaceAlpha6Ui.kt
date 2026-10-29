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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*

/**
 * V196.2 Content Workspace: one truthful project step, four equal tools, optional stage detail.
 * The editor callbacks and source-of-truth engines remain unchanged.
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
    // This remains workflow progress; a completed optional checklist is not project completion.
    val overallProgress = CreatorWorkflowEngine.progress(task).coerceIn(0, 100)
    val completed = task.status == TaskStatus.DONE
    val checklistDone = workspace.checklist.count { it.status == CreatorChecklistStatus.DONE }
    val checklistTotal = workspace.checklist.size
    val published = workspace.deliverables.count { it.status == CreatorDeliverableStatus.PUBLISHED }
    val scriptBeats = workspace.scriptStudio?.beats?.size ?: 0
    val hasScript = scriptBeats > 0 || workspace.script.isNotBlank() || workspace.hook.isNotBlank()
    var showStages by rememberSaveable(task.id) { mutableStateOf(false) }
    // Read-only prompts derived from canonical saved project content.
    val focusHints = V2285WorkspaceFocusPolicy.hints(task)

    val primaryAction = if (completed) onOpenProject else workspaceActionForStage(
        stage = currentStage,
        onOpenProject = onOpenProject,
        onOpenScript = onOpenScript,
        onOpenPublish = onOpenPublish,
    )
    val primaryLabel = when {
        completed -> "OPEN PLAN"
        currentStage.id in SCRIPT_STAGE_IDS -> "CONTINUE SCRIPT"
        currentStage.id in PUBLISH_STAGE_IDS || CreatorWorkflowEngine.isPublicationStage(currentStage) -> "PREPARE TO PUBLISH"
        else -> "CONTINUE " + currentStage.label.uppercase()
    }

    WorkspaceAdaptiveFrame {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Close project", tint = ProjectorIvory)
                    }
                    Spacer(Modifier.width(7.dp))
                    Column(Modifier.weight(1f)) {
                        Text("PROJECT", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            task.title,
                            color = ProjectorIvory,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    listOf(task.platform, task.contentType).filter { it.isNotBlank() }.joinToString(" · "),
                    color = MutedText, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = CinemaSurfaceRaised,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (completed) "FINISHED" else "CURRENT STEP", color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text(
                                (if (completed) template.stages.size else stageIndex + 1).toString() + "/" + template.stages.size,
                                color = MutedText, fontSize = 13.sp,
                            )
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(
                            if (completed) "Project complete" else currentStage.label,
                            color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Bold,
                        )
                        if (!completed && nextAction.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(nextAction, color = MutedText, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("PROGRESS", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.weight(1f))
                            Text(overallProgress.toString() + "%", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(5.dp),
                            color = RecRed, trackColor = CinemaLine,
                        )
                        Spacer(Modifier.height(19.dp))
                        Button(
                            onClick = primaryAction,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        ) {
                            Text(primaryLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                Text("WORKSPACE", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MinimalWorkspaceToolCard(
                        title = "Plan", detail = if (checklistTotal == 0) "Start planning" else checklistDone.toString() + "/" + checklistTotal + " tasks",
                        icon = Icons.Outlined.Dashboard, modifier = Modifier.weight(1f), onClick = onOpenProject,
                    )
                    MinimalWorkspaceToolCard(
                        title = "Script", detail = when {
                            scriptBeats > 0 -> scriptBeats.toString() + " beats"
                            hasScript -> "Draft started"
                            else -> "Not started"
                        },
                        icon = Icons.Outlined.EditNote, modifier = Modifier.weight(1f), onClick = onOpenScript,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MinimalWorkspaceToolCard(
                        title = "Publish", detail = if (published > 0) published.toString() + " published"
                            else if (workspace.deliverables.isNotEmpty()) workspace.deliverables.size.toString() + " planned"
                            else "Not started",
                        icon = Icons.Outlined.Publish, modifier = Modifier.weight(1f), onClick = onOpenPublish,
                    )
                    MinimalWorkspaceToolCard(
                        title = "Guide", detail = blueprint.archetypeLabel.ifBlank { "Creative guide" },
                        icon = Icons.Outlined.AutoAwesome, modifier = Modifier.weight(1f), onClick = onOpenBlueprint,
                    )
                }

                if (!completed && task.status != TaskStatus.SKIPPED) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "NEXT USEFUL MOVES", color = ProjectorIvory, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("v2285-workspace-focus"),
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Based on saved materials · suggestions don't advance your project",
                        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                    )
                    Spacer(Modifier.height(11.dp))
                    if (focusHints.isEmpty()) {
                        Text(
                            "Core materials are recorded. Continue your current project step.",
                            color = MutedText, fontSize = 12.sp,
                            modifier = Modifier.testTag("v2285-focus-clear"),
                        )
                    } else {
                        focusHints.forEach { hint ->
                            val openEditor = when (hint.destination) {
                                V2285WorkspaceDestination.PLAN -> onOpenProject
                                V2285WorkspaceDestination.SCRIPT -> onOpenScript
                                V2285WorkspaceDestination.PUBLISH -> onOpenPublish
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(bottom = 9.dp)
                                    .testTag("v2285-focus-${hint.key}"),
                                shape = RoundedCornerShape(16.dp),
                                color = CinemaSurface,
                                border = BorderStroke(1.dp, CinemaLine),
                            ) {
                                Column(Modifier.padding(13.dp)) {
                                    Text(
                                        hint.title, color = ProjectorIvory,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        hint.description, color = MutedText,
                                        fontSize = 12.sp, lineHeight = 17.sp,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    TextButton(
                                        onClick = openEditor,
                                        modifier = Modifier.testTag("v2285-open-${hint.destination.name.lowercase()}"),
                                    ) {
                                        Text("OPEN ${hint.destination.name}", color = MutedGold,
                                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(5.dp))
                                        Icon(Icons.Outlined.ArrowForward, null,
                                            tint = MutedGold, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                HorizontalDivider(color = CinemaLine)
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showStages = !showStages },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("PROJECT STEPS", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(if (showStages) "HIDE" else "VIEW ALL", color = MutedGold, fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(if (showStages) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null, tint = MutedGold)
                }
                if (showStages) {
                    Surface(shape = RoundedCornerShape(16.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            template.stages.forEachIndexed { index, stage ->
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 40.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val done = completed || index < stageIndex
                                    Text(
                                        when { done -> "✓"; index == stageIndex -> "●"; else -> "○" },
                                        color = when { done -> MutedGold; index == stageIndex -> RecRed; else -> MutedText },
                                        fontSize = 17.sp, modifier = Modifier.width(30.dp),
                                    )
                                    Text(
                                        stage.label, color = if (index == stageIndex || done) ProjectorIvory else MutedText,
                                        fontSize = 13.sp, fontWeight = if (index == stageIndex) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MinimalWorkspaceToolCard(
    title: String,
    detail: String,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 124.dp),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Icon(icon, contentDescription = null, tint = MutedGold, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(11.dp))
            Text(title, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(detail, color = MutedText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

private val SCRIPT_STAGE_IDS = setOf("script", "narration", "outline", "draft", "commentary", "guide_script")
private val PUBLISH_STAGE_IDS = setOf("package", "packaging", "thumbnail", "metadata", "upload", "cover", "artwork", "recipe_metadata", "presentation")
