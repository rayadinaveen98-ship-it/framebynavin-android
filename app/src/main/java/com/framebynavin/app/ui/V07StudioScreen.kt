package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.delay

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
        .sortedWith(
            compareBy<CreatorTask> { it.status == TaskStatus.DONE }
                .thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE }
        )

    val listState = rememberLazyListState()
    val dismissInteraction = remember { MutableInteractionSource() }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(active.map { it.id }) {
        if (expandedId != null && active.none { it.id == expandedId }) expandedId = null
    }

    LaunchedEffect(expandedId, active.map { it.id }) {
        val id = expandedId ?: return@LaunchedEffect
        val projectIndex = active.indexOfFirst { it.id == id }
        if (projectIndex >= 0) {
            // Let the inline expansion begin first, then keep the selected project anchored
            // near the top so project #8 or #15 never opens below the visible viewport.
            delay(120)
            listState.animateScrollToItem(projectIndex + 3)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .clickable(
                interactionSource = dismissInteraction,
                indication = null,
            ) { expandedId = null },
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 122.dp),
    ) {
        item {
            Box(Modifier.padding(top = 0.dp)) {
                V07SectionHeader("STUDIO", onQuickAdd)
            }
        }

        item {
            Column(Modifier.padding(top = 22.dp, bottom = 16.dp)) {
                Text("PRODUCTION WORKFLOW", color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("Tap a project to open its pipeline right where it lives.", color = MutedText, fontSize = 11.sp)
            }
        }

        if (active.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { },
                    shape = RoundedCornerShape(20.dp),
                    color = CinemaSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No creator projects yet", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                        Text("Create an item and its production pipeline will appear here.", color = MutedText, fontSize = 10.5.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onQuickAdd, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) {
                            Text("CREATE PROJECT")
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    "PROJECTS · ${active.size}",
                    color = MutedText,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }

            itemsIndexed(active, key = { _, task -> task.id }) { _, task ->
                val expanded = expandedId == task.id
                V071StudioAccordionItem(
                    task = task,
                    expanded = expanded,
                    onToggle = {
                        expandedId = if (expanded) null else task.id
                    },
                    onAdvance = { onAdvance(task.id) },
                    onBack = { onBack(task.id) },
                    onFocus = { onFocus(task.id) },
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                // A deliberate outside-tap zone: tapping below the project list closes
                // whichever project is open without requiring a second project tap.
                Spacer(Modifier.fillMaxWidth().height(72.dp))
            }
        }
    }
}

@Composable
private fun V071StudioAccordionItem(
    task: CreatorTask,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    onFocus: () -> Unit,
) {
    val template = CreatorWorkflowEngine.templateFor(task)
    val currentIndex = CreatorWorkflowEngine.stageIndex(task)
    val current = CreatorWorkflowEngine.currentStage(task)
    val progress = CreatorWorkflowEngine.progress(task)
    val done = task.status == TaskStatus.DONE

    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            shape = RoundedCornerShape(if (expanded) 18.dp else 16.dp),
            color = if (expanded) Color(0xFF17130F) else CinemaSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (expanded) MutedGold.copy(alpha = .55f) else CinemaLine,
            ),
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(8.dp).background(
                            when {
                                done -> SuccessGreen
                                expanded -> RecRed
                                else -> Color(0xFF4A4742)
                            },
                            CircleShape,
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 12.8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${task.platform} · ${task.contentType}",
                            color = MutedText,
                            fontSize = 9.5.sp,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (done) "100%" else "$progress%",
                            color = if (done) SuccessGreen else MutedGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Icon(
                            if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            if (expanded) "Collapse pipeline" else "Expand pipeline",
                            tint = if (expanded) ProjectorIvory else MutedText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(Modifier.height(9.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (done) "PUBLISHED" else "CURRENT · ${current.label.uppercase()}",
                        color = if (done) SuccessGreen else ProjectorIvory,
                        fontSize = 8.8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (expanded) "TAP AGAIN TO CLOSE" else "TAP TO OPEN",
                        color = Color(0xFF68635D),
                        fontSize = 7.8.sp,
                    )
                }
                Spacer(Modifier.height(7.dp))
                V071MiniStageRail(task)
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column {
                Spacer(Modifier.height(7.dp))
                V071WorkflowInlineContent(
                    task = task,
                    onAdvance = onAdvance,
                    onBack = onBack,
                    onFocus = onFocus,
                )
            }
        }
    }
}

@Composable
private fun V071MiniStageRail(task: CreatorTask) {
    val template = CreatorWorkflowEngine.templateFor(task)
    val currentIndex = CreatorWorkflowEngine.stageIndex(task)
    val done = task.status == TaskStatus.DONE

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        template.stages.forEachIndexed { index, _ ->
            val completed = done || index < currentIndex
            val current = !done && index == currentIndex
            Box(
                Modifier
                    .weight(1f)
                    .height(if (current) 4.dp else 3.dp)
                    .background(
                        when {
                            completed -> SuccessGreen.copy(alpha = .72f)
                            current -> RecRed
                            else -> Color(0xFF34312E)
                        },
                        RoundedCornerShape(100.dp),
                    )
            )
        }
    }
}

@Composable
private fun V071WorkflowInlineContent(
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
        modifier = Modifier
            .fillMaxWidth()
            // Consume taps inside the expanded project so the Studio outside-tap handler
            // does not collapse the panel while the user is reading/using its controls.
            .clickable { },
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(template.label.uppercase(), color = RecRed, fontSize = 8.7.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(task.dueLabel, color = MutedGold, fontSize = 10.3.sp)
                }
                Text(
                    if (done) "COMPLETE" else "STAGE ${currentIndex + 1}/${template.stages.size}",
                    color = if (done) SuccessGreen else MutedText,
                    fontSize = 8.7.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(11.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = if (done) SuccessGreen else RecRed,
                trackColor = Color(0xFF303030),
            )
            Spacer(Modifier.height(17.dp))

            template.stages.forEachIndexed { index, stage ->
                val completed = done || index < currentIndex
                val current = !done && index == currentIndex
                val upcoming = !completed && !current

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(29.dp).background(
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
                            fontSize = 12.8.sp,
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
                            fontSize = 9.3.sp,
                        )
                    }
                }

                if (index < template.stages.lastIndex) {
                    Box(
                        Modifier
                            .padding(start = 14.dp)
                            .width(1.dp)
                            .height(15.dp)
                            .background(if (completed) SuccessGreen.copy(alpha = .25f) else CinemaLine)
                    )
                }
            }

            if (!done) {
                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFF10100F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text("NEXT ACTION", color = MutedText, fontSize = 8.3.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(currentStage.action, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(11.dp))
                Button(
                    onClick = onFocus,
                    modifier = Modifier.fillMaxWidth().height(49.dp),
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
                        Text("BACK", color = ProjectorIvory, fontSize = 9.3.sp)
                    }
                    Button(
                        onClick = onAdvance,
                        modifier = Modifier.weight(1.45f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272727)),
                    ) {
                        Text(
                            if (currentIndex == template.stages.lastIndex) "MARK PUBLISHED" else "COMPLETE STAGE",
                            color = ProjectorIvory,
                            fontSize = 9.3.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(13.dp))
                Text(
                    "✓ Published workflow complete",
                    color = SuccessGreen,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
