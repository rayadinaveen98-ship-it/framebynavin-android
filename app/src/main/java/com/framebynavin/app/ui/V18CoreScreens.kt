package com.framebynavin.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.cloud.CloudSyncActivity
import com.framebynavin.app.data.*
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.widget.CreatorWidgetContract
import com.framebynavin.app.widget.CreatorWidgetLaunch
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

@Composable
internal fun PPlanScreen(tasks: List<CreatorTask>, onAdd: () -> Unit, onStart: (String) -> Unit, onDone: (String) -> Unit) {
    val now = System.currentTimeMillis()
    val active = remember(tasks.toList()) { pActiveQueue(tasks) }
    val overdue = active.filter { it.dueAtMillis in 1 until now }
    val today = active.filter { it !in overdue && pDate(it.dueAtMillis) == LocalDate.now() }
    val upcoming = active.filter { it !in overdue && it !in today }
    val completed = tasks.filter { it.status == TaskStatus.DONE }.sortedByDescending { it.dueAtMillis }.take(8)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 124.dp)) {
        PTopBar("PLAN", onAdd)
        Spacer(Modifier.height(18.dp))
        Text("See the week clearly.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text("Deadlines grouped by what actually needs attention.", color = MutedText, fontSize = 10.5.sp)
        Spacer(Modifier.height(19.dp))

        if (active.isEmpty()) {
            PEmptyState(Icons.Outlined.EventAvailable, "Your plan is clear", "Add a project when you know what you're making next.", "CREATE PROJECT", onAdd)
        } else {
            if (overdue.isNotEmpty()) PPlanSection("OVERDUE", overdue, RecRed, onStart, onDone)
            if (today.isNotEmpty()) PPlanSection("TODAY", today, MutedGold, onStart, onDone)
            if (upcoming.isNotEmpty()) PPlanSection("UPCOMING", upcoming, ProjectorIvory, onStart, onDone)
        }

        if (completed.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("RECENTLY FINISHED", color = SuccessGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(8.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    completed.forEachIndexed { index, task ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(task.title, color = ProjectorIvory, fontSize = 10.5.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(task.platform, color = MutedText, fontSize = 8.5.sp)
                        }
                        if (index != completed.lastIndex) HorizontalDivider(color = CinemaLine.copy(alpha = .7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PPlanSection(label: String, tasks: List<CreatorTask>, accent: Color, onStart: (String) -> Unit, onDone: (String) -> Unit) {
    Text(label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
    Spacer(Modifier.height(8.dp))
    tasks.forEach { task ->
        val progress = CreatorWorkflowEngine.progress(task)
        Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, if (label == "OVERDUE") RecRed.copy(alpha = .35f) else CinemaLine)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text("${task.dueLabel} · ${task.platform}", color = if (label == "OVERDUE") RecRed else MutedText, fontSize = 9.sp)
                    }
                    Text(CreatorWorkflowEngine.currentStage(task).label, color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = if (label == "OVERDUE") RecRed else MutedGold, trackColor = Color(0xFF292929))
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$progress%", color = MutedText, fontSize = 8.5.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onStart(task.id) }) { Text(if (task.status == TaskStatus.WORKING) "CONTINUE" else "START", color = ProjectorIvory, fontSize = 10.sp) }
                    TextButton(onClick = { onDone(task.id) }) { Text("DONE", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
internal fun PStudioScreen(
    tasks: List<CreatorTask>,
    onAdd: () -> Unit,
    onAdvance: (String) -> Unit,
    onBack: (String) -> Unit,
    onFocus: (String) -> Unit,
    externalExpandId: String? = null,
    externalExpandNonce: Long = 0L,
) {
    val projects = tasks.filter { it.status != TaskStatus.SKIPPED }.sortedWith(compareBy<CreatorTask> { it.status == TaskStatus.DONE }.thenBy { it.dueAtMillis.takeIf { d -> d > 0 } ?: Long.MAX_VALUE })
    val listState = rememberLazyListState()
    val dismissInteraction = remember { MutableInteractionSource() }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(externalExpandNonce) {
        if (!externalExpandId.isNullOrBlank()) expandedId = externalExpandId
    }
    LaunchedEffect(projects.map { it.id }) { if (expandedId != null && projects.none { it.id == expandedId }) expandedId = null }
    LaunchedEffect(expandedId) {
        val id = expandedId ?: return@LaunchedEffect
        val i = projects.indexOfFirst { it.id == id }
        if (i >= 0) { delay(100); listState.animateScrollToItem(i + 3) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().statusBarsPadding().clickable(interactionSource = dismissInteraction, indication = null) { expandedId = null },
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 124.dp),
    ) {
        item { PTopBar("STUDIO", onAdd) }
        item {
            Column(Modifier.padding(top = 18.dp, bottom = 18.dp)) {
                Text("Your work, in motion.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("Open a project when you want to see all its steps.", color = MutedText, fontSize = 10.5.sp)
            }
        }
        if (projects.isEmpty()) {
            item { PEmptyState(Icons.Outlined.VideoCameraBack, "Studio is empty", "Create a project and its production steps will live here.", "CREATE PROJECT", onAdd) }
        } else {
            item { Text("PROJECTS · ${projects.size}", color = MutedText, fontSize = 8.5.sp, letterSpacing = 1.1.sp, modifier = Modifier.padding(bottom = 7.dp)) }
            itemsIndexed(projects, key = { _, task -> task.id }) { _, task ->
                val expanded = expandedId == task.id
                PStudioProject(
                    task = task,
                    expanded = expanded,
                    onToggle = { expandedId = if (expanded) null else task.id },
                    onAdvance = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAdvance(task.id)
                    },
                    onBack = { onBack(task.id) },
                    onFocus = { onFocus(task.id) },
                )
                Spacer(Modifier.height(9.dp))
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

@Composable
internal fun PStudioProject(
    task: CreatorTask,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    onFocus: () -> Unit,
    onEdit: () -> Unit = {},
    postPublishCheckpoints: List<PostPublishCheckpoint> = emptyList(),
    onEditPublication: () -> Unit = {},
    onCompletePostPublish: (String) -> Unit = {},
    onSkipPostPublish: (String) -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelect: () -> Unit = {},
    onLongPress: () -> Unit = {},
) {
    val current = CreatorWorkflowEngine.currentStage(task)
    val progress = CreatorWorkflowEngine.progress(task)
    val done = task.status == TaskStatus.DONE
    val pulse = ProjectPulseEngine.snapshot(task)
    val pulseAccent = when (pulse.state) {
        ProjectPulseState.COMPLETE -> SuccessGreen
        ProjectPulseState.OVERDUE, ProjectPulseState.NEEDS_ATTENTION -> RecRed
        ProjectPulseState.AT_RISK -> MutedGold
        ProjectPulseState.ON_TRACK, ProjectPulseState.CALM -> ProjectorIvory
    }

    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = { if (selectionMode) onSelect() else onToggle() },
                onLongClick = onLongPress,
            ),
            shape = RoundedCornerShape(if (expanded) 21.dp else 18.dp),
            color = if (selected) RecRed.copy(alpha = .10f) else if (expanded) Color(0xFF16130F) else CinemaSurface,
            border = BorderStroke(1.dp, if (selected) RecRed else if (expanded) MutedGold.copy(alpha = .5f) else CinemaLine),
        ) {
            Column(Modifier.padding(15.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        Text("${task.platform} · ${task.contentType} · ${task.dueLabel}", color = MutedText, fontSize = 8.8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (selectionMode) {
                        Icon(if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (selected) RecRed else MutedText)
                    } else {
                        Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = MutedText)
                    }
                }
                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(100.dp), color = pulseAccent.copy(alpha = .11f)) {
                        Text(pulse.stateLabel, color = pulseAccent, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("$progress%", color = if (done) SuccessGreen else MutedGold, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
                if (!done) {
                    Spacer(Modifier.height(9.dp))
                    Text("NEXT · ${pulse.nextAction}", color = ProjectorIvory, fontSize = 9.3.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (pulse.nextCheckpointAtMillis > 0L) {
                        Spacer(Modifier.height(3.dp))
                        Text("${pulse.checkpointLabel} · ${pFormatDateTime(pulse.nextCheckpointAtMillis)}", color = MutedText, fontSize = 8.3.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                PStageRail(task)
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column {
                Spacer(Modifier.height(5.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, null, tint = MutedGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("EDIT PROJECT", color = MutedGold, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                V071WorkflowInlineContent(
                    task = task,
                    onEditPublication = onEditPublication,
                    onAdvance = onAdvance,
                    onBack = onBack,
                    onFocus = onFocus,
                )
                if (postPublishCheckpoints.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    V21PostPublishSection(
                        checkpoints = postPublishCheckpoints,
                        onComplete = onCompletePostPublish,
                        onSkip = onSkipPostPublish,
                    )
                }
            }
        }
    }
}

@Composable
private fun V21PostPublishSection(
    checkpoints: List<PostPublishCheckpoint>,
    onComplete: (String) -> Unit,
    onSkip: (String) -> Unit,
) {
    val ordered = checkpoints.sortedBy { it.dueAtMillis }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("POST-PUBLISH", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(4.dp))
            Text("Publish is not the end.", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Review the response and carry the lesson into the next project.", color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
            Spacer(Modifier.height(12.dp))
            ordered.forEachIndexed { index, checkpoint ->
                val pending = checkpoint.status == PostPublishCheckpointStatus.PENDING
                val accent = when (checkpoint.status) {
                    PostPublishCheckpointStatus.PENDING -> MutedGold
                    PostPublishCheckpointStatus.DONE -> SuccessGreen
                    PostPublishCheckpointStatus.SKIPPED -> MutedText
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Icon(
                        when (checkpoint.status) {
                            PostPublishCheckpointStatus.PENDING -> Icons.Outlined.RadioButtonUnchecked
                            PostPublishCheckpointStatus.DONE -> Icons.Outlined.CheckCircle
                            PostPublishCheckpointStatus.SKIPPED -> Icons.Outlined.Close
                        },
                        null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(checkpoint.title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(checkpoint.description, color = MutedText, fontSize = 8.3.sp, lineHeight = 12.sp)
                        if (checkpoint.dueAtMillis > 0L) {
                            Text(pFormatDateTime(checkpoint.dueAtMillis), color = accent, fontSize = 8.2.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                    if (pending) {
                        Column(horizontalAlignment = Alignment.End) {
                            TextButton(onClick = { onComplete(checkpoint.id) }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)) {
                                Text("DONE", color = SuccessGreen, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { onSkip(checkpoint.id) }, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)) {
                                Text("SKIP", color = MutedText, fontSize = 8.5.sp)
                            }
                        }
                    }
                }
                if (index != ordered.lastIndex) {
                    HorizontalDivider(color = CinemaLine.copy(alpha = .7f), modifier = Modifier.padding(vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
internal fun PInsightsScreen(tasks: List<CreatorTask>, ideas: List<CreatorIdea>, onAdd: () -> Unit) {
    val active = tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    val done = tasks.count { it.status == TaskStatus.DONE }
    val skipped = tasks.count { it.status == TaskStatus.SKIPPED }
    val finished = done + skipped
    val completionRate = if (finished == 0) 0 else done * 100 / finished
    val avg = if (active.isEmpty()) 0 else active.sumOf { CreatorWorkflowEngine.progress(it) } / active.size
    val stageCounts = active.groupingBy { CreatorWorkflowEngine.currentStage(it).label }.eachCount().entries.sortedByDescending { it.value }
    val maxStage = stageCounts.maxOfOrNull { it.value } ?: 1
    val readyIdeas = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }
    val convertedIdeas = ideas.count { it.status == IdeaStatus.CONVERTED }
    val releaseDone = tasks.count { it.origin == CreatorTaskOrigin.RELEASE_DAY && it.status == TaskStatus.DONE }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 124.dp)) {
        PTopBar("INSIGHTS", onAdd)
        Spacer(Modifier.height(18.dp))
        Text("How you're creating.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text("A simple view of momentum, not a wall of numbers.", color = MutedText, fontSize = 10.5.sp)
        Spacer(Modifier.height(18.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PMetric("COMPLETED", done.toString(), SuccessGreen, Modifier.weight(1f))
            PMetric("FINISH RATE", "$completionRate%", MutedGold, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PMetric("ACTIVE", active.size.toString(), RecRed, Modifier.weight(1f))
            PMetric("AVG PROGRESS", "$avg%", ProjectorIvory, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Text("WHERE WORK IS SITTING", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("The longest bars are where your current projects are concentrated.", color = MutedText, fontSize = 9.3.sp)
        Spacer(Modifier.height(10.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
            Column(Modifier.padding(15.dp)) {
                if (stageCounts.isEmpty()) {
                    Text("No active projects right now.", color = MutedText, fontSize = 10.sp)
                } else stageCounts.take(6).forEach { entry ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.key, color = ProjectorIvory, fontSize = 10.sp, modifier = Modifier.width(82.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        LinearProgressIndicator(progress = { entry.value.toFloat() / maxStage.toFloat() }, modifier = Modifier.weight(1f).height(5.dp), color = MutedGold, trackColor = Color(0xFF292929))
                        Spacer(Modifier.width(8.dp)); Text(entry.value.toString(), color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PSmallStat("IDEAS READY", readyIdeas.toString(), Modifier.weight(1f))
            PSmallStat("IDEAS MADE", convertedIdeas.toString(), Modifier.weight(1f))
            PSmallStat("LIVE DONE", releaseDone.toString(), Modifier.weight(1f))
        }
        if (skipped > 0) {
            Spacer(Modifier.height(10.dp))
            Text("$skipped project${if (skipped == 1) " was" else "s were"} skipped. That's useful signal too — keep the plan realistic.", color = MutedText, fontSize = 9.3.sp, lineHeight = 14.sp)
        }
    }
}
