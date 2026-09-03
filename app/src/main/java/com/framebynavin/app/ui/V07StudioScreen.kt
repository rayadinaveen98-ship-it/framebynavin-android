package com.framebynavin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.*

@Composable
internal fun V07StudioScreen(
    tasks: List<CreatorTask>,
    onQuickAdd: () -> Unit,
    onAdvance: (String) -> Unit,
    onBack: (String) -> Unit,
    onFocus: (String) -> Unit,
) {
    val active = tasks
        .filter { it.status != TaskStatus.SKIPPED }
        .sortedWith(compareBy<CreatorTask> { it.status == TaskStatus.DONE }.thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE })
    var selectedId by remember(active.map { it.id }) { mutableStateOf(active.firstOrNull { it.status != TaskStatus.DONE }?.id ?: active.firstOrNull()?.id) }
    val selected = active.firstOrNull { it.id == selectedId } ?: active.firstOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 122.dp)
    ) {
        V07SectionHeader("STUDIO", onQuickAdd)
        Spacer(Modifier.height(22.dp))
        Text("PRODUCTION WORKFLOW", color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Text("Each format follows the stages it actually needs.", color = MutedText, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))

        if (active.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CinemaSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
            ) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No creator projects yet", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                    Text("Create an item and its production pipeline will appear here.", color = MutedText, fontSize = 10.5.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onQuickAdd, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("CREATE PROJECT") }
                }
            }
            return@Column
        }

        Text("PROJECTS", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(7.dp))
        active.forEach { task ->
            val current = CreatorWorkflowEngine.currentStage(task)
            val progress = CreatorWorkflowEngine.progress(task)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.dp)
                    .clickable { selectedId = task.id },
                shape = RoundedCornerShape(16.dp),
                color = if (task.id == selected?.id) Color(0xFF17130F) else CinemaSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (task.id == selected?.id) MutedGold.copy(alpha = .55f) else CinemaLine),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(8.dp).background(
                            when {
                                task.status == TaskStatus.DONE -> SuccessGreen
                                task.id == selected?.id -> RecRed
                                else -> Color(0xFF4A4742)
                            },
                            CircleShape,
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        Text("${task.platform} · ${task.contentType} · ${if (task.status == TaskStatus.DONE) "Published" else current.label}", color = MutedText, fontSize = 9.5.sp)
                    }
                    Text("$progress%", color = if (task.status == TaskStatus.DONE) SuccessGreen else MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (selected != null) {
            Spacer(Modifier.height(13.dp))
            V07WorkflowProject(
                task = selected,
                onAdvance = { onAdvance(selected.id) },
                onBack = { onBack(selected.id) },
                onFocus = { onFocus(selected.id) },
            )
        }
    }
}

@Composable
private fun V07WorkflowProject(
    task: CreatorTask,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    onFocus: () -> Unit,
) {
    val template = CreatorWorkflowEngine.templateFor(task)
    val currentIndex = CreatorWorkflowEngine.stageIndex(task)
    val progress = CreatorWorkflowEngine.progress(task)
    val currentStage = CreatorWorkflowEngine.currentStage(task)
    val done = task.status == TaskStatus.DONE

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(template.label.uppercase(), color = RecRed, fontSize = 9.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(task.dueLabel, color = MutedGold, fontSize = 10.5.sp)
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (done) "COMPLETE" else "CURRENT · ${currentStage.label.uppercase()}", color = if (done) SuccessGreen else ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("$progress%", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = if (done) SuccessGreen else RecRed,
                trackColor = Color(0xFF303030),
            )
            Spacer(Modifier.height(18.dp))

            template.stages.forEachIndexed { index, stage ->
                val completed = done || index < currentIndex
                val current = !done && index == currentIndex
                val upcoming = !completed && !current
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(30.dp).background(
                            when {
                                completed -> SuccessGreen.copy(alpha = .16f)
                                current -> RecRed.copy(alpha = .18f)
                                else -> Color(0xFF141414)
                            },
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when {
                                completed -> Icons.Outlined.Check
                                current -> Icons.Outlined.RadioButtonChecked
                                else -> Icons.Outlined.Lock
                            },
                            contentDescription = null,
                            tint = when {
                                completed -> SuccessGreen
                                current -> RecRed
                                else -> Color(0xFF5C5852)
                            },
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stage.label,
                            color = if (upcoming) Color(0xFF77726C) else ProjectorIvory,
                            fontSize = 13.sp,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            when {
                                completed -> "Completed"
                                current -> stage.action
                                else -> "Upcoming"
                            },
                            color = when {
                                completed -> SuccessGreen.copy(alpha = .75f)
                                current -> MutedGold
                                else -> Color(0xFF5F5B56)
                            },
                            fontSize = 9.5.sp,
                        )
                    }
                }
                if (index < template.stages.lastIndex) {
                    Box(Modifier.padding(start = 14.dp).width(1.dp).height(16.dp).background(if (completed) SuccessGreen.copy(alpha = .25f) else CinemaLine))
                }
            }

            if (!done) {
                Spacer(Modifier.height(19.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFF10100F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text("NEXT ACTION", color = MutedText, fontSize = 8.5.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(currentStage.action, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onFocus,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null)
                    Spacer(Modifier.width(7.dp))
                    Text("FOCUS · ${currentStage.label.uppercase()}", fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                    ) {
                        Icon(Icons.Outlined.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("BACK", color = ProjectorIvory, fontSize = 9.5.sp)
                    }
                    Button(
                        onClick = onAdvance,
                        modifier = Modifier.weight(1.45f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272727)),
                    ) {
                        Text(
                            if (currentIndex == template.stages.lastIndex) "MARK PUBLISHED" else "COMPLETE STAGE",
                            color = ProjectorIvory,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
