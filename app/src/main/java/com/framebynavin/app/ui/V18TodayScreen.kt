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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
internal fun PTodayScreen(
    creatorProfile: CreatorProfile,
    tasks: List<CreatorTask>,
    onAdd: () -> Unit,
    onStart: (String) -> Unit,
    onAdvance: (String) -> Unit,
    onViewAllReminders: () -> Unit,
    onFocus: (String) -> Unit,
    onOpenProject: (String) -> Unit = {},
) {
    val queue by remember { derivedStateOf { pActiveQueue(tasks).take(10) } }
    val queueIds by remember { derivedStateOf { queue.map { it.id } } }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(queueIds) { if (queue.none { it.id == selectedId }) selectedId = queue.firstOrNull()?.id }
    val index = queue.indexOfFirst { it.id == selectedId }.let { if (it < 0) 0 else it }
    val selected = queue.getOrNull(index)
    val personalization by remember(creatorProfile) {
        derivedStateOf { CreatorPersonalizationEngine.snapshot(creatorProfile, tasks) }
    }
    val haptics = LocalHapticFeedback.current
    var showWeeklyFocus by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(RecRed.copy(alpha = .055f), CinemaBlack), radius = 980f))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 124.dp)) {
            V18CinematicHomeHero(creatorProfile.safeDisplayName)
            Spacer(Modifier.height(20.dp))
            Text("TODAY", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(4.dp))
            Text(personalization.focusTitle, color = ProjectorIvory, fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black)
            if (personalization.focusBody.isNotBlank()) {
                Text(personalization.focusBody, color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.height(14.dp))
            V18CreatorFocusCard(creatorProfile, personalization, onClick = { showWeeklyFocus = true })
            Spacer(Modifier.height(18.dp))

            if (selected == null) {
                PEmptyState(
                    icon = Icons.Outlined.MovieCreation,
                    title = "Nothing needs your attention",
                    body = personalization.emptyProjectBody,
                    button = "CREATE PROJECT",
                    onClick = onAdd,
                )
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("UP NEXT", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("${index + 1} / ${queue.size}", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (queue.size > 1) Text("SWIPE", color = MutedText, fontSize = 8.sp, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(8.dp))

                var dragTotal by remember { mutableFloatStateOf(0f) }
                Box(Modifier.pointerInput(queue.size, index) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onHorizontalDrag = { _, amount -> dragTotal += amount },
                        onDragEnd = {
                            if (dragTotal < -55f && index < queue.lastIndex) selectedId = queue[index + 1].id
                            if (dragTotal > 55f && index > 0) selectedId = queue[index - 1].id
                            dragTotal = 0f
                        },
                    )
                }) {
                    AnimatedContent(targetState = selected.id, label = "todayProject") { targetId ->
                        queue.firstOrNull { it.id == targetId }?.let { targetTask ->
                            PTodayProjectCard(targetTask, onClick = { onOpenProject(targetTask.id) })
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                PQueueDots(index, queue.size)
                Spacer(Modifier.height(12.dp))
                PNextMoveCard(selected)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onStart(selected.id) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        border = BorderStroke(1.dp, CinemaLine),
                        shape = RoundedCornerShape(15.dp),
                    ) { Text(if (selected.status == TaskStatus.WORKING) "WORKING" else "START", color = ProjectorIvory, fontSize = 10.sp) }
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onFocus(selected.id)
                        },
                        modifier = Modifier.weight(1.35f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("FOCUS", fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAdvance(selected.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (CreatorWorkflowEngine.stageIndex(selected) == CreatorWorkflowEngine.templateFor(selected).stages.lastIndex) "MARK PUBLISHED" else "MARK STEP DONE",
                        color = MutedGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            PTodayReminders(tasks = tasks, onViewAll = onViewAllReminders)

        }
    }

    if (showWeeklyFocus) {
        V20WeeklyFocusDialog(
            profile = creatorProfile,
            tasks = tasks,
            personalization = personalization,
            onDismiss = { showWeeklyFocus = false },
            onOpenProject = { id ->
                showWeeklyFocus = false
                onOpenProject(id)
            },
        )
    }
}

@Composable
private fun V18CreatorFocusCard(
    profile: CreatorProfile,
    personalization: CreatorPersonalizationSnapshot,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = Color(0xFF171310),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CREATOR FOCUS", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(profile.primaryGoal, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${personalization.publishedThisWeek} / ${personalization.weeklyTarget}",
                    color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { personalization.weeklyProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (personalization.weeklyProgress >= 1f) SuccessGreen else MutedGold,
                trackColor = CinemaLine,
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${personalization.publishedThisWeek} / ${personalization.weeklyTarget} published this week",
                    color = MutedText,
                    fontSize = 8.8.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(personalization.platformSummary, color = MutedText, fontSize = 8.3.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val remaining = (personalization.weeklyTarget - personalization.publishedThisWeek).coerceAtLeast(0)
                val above = (personalization.publishedThisWeek - personalization.weeklyTarget).coerceAtLeast(0)
                Text(
                    when {
                        above > 0 -> "WEEKLY TARGET COMPLETE · +$above ABOVE"
                        remaining == 0 -> "WEEKLY TARGET COMPLETE"
                        else -> "$remaining PUBLISH${if (remaining == 1) "" else "ES"} REMAINING"
                    },
                    color = if (remaining == 0) SuccessGreen else ProjectorIvory.copy(alpha = .78f),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, "Open weekly focus", tint = MutedGold, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun V20WeeklyFocusDialog(
    profile: CreatorProfile,
    tasks: List<CreatorTask>,
    personalization: CreatorPersonalizationSnapshot,
    onDismiss: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    val start = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        .atStartOfDay(zone).toInstant().toEpochMilli()
    val published = tasks.filter {
        it.status == TaskStatus.DONE && it.completedAtMillis in start..now
    }.sortedByDescending { it.completedAtMillis }
    val active = pActiveQueue(tasks).take(6)
    val above = (personalization.publishedThisWeek - personalization.weeklyTarget).coerceAtLeast(0)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = CinemaSurfaceRaised,
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(19.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("WEEKLY FOCUS", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Text(profile.primaryGoal, color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close", tint = ProjectorIvory) }
                }
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF171310), border = BorderStroke(1.dp, MutedGold.copy(alpha = .25f))) {
                    Column(Modifier.padding(14.dp)) {
                        Text("WEEKLY OUTPUT", color = MutedText, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("${personalization.publishedThisWeek} / ${personalization.weeklyTarget}", color = if (personalization.publishedThisWeek >= personalization.weeklyTarget) SuccessGreen else MutedGold, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(
                            when {
                                above > 0 -> "Target complete · $above above plan"
                                personalization.publishedThisWeek >= personalization.weeklyTarget -> "Weekly target complete"
                                else -> "${personalization.weeklyTarget - personalization.publishedThisWeek} publish${if (personalization.weeklyTarget - personalization.publishedThisWeek == 1) "" else "es"} remaining"
                            },
                            color = MutedText,
                            fontSize = 9.5.sp,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("COMPLETED THIS WEEK", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (published.isEmpty()) {
                    Text("Nothing published yet this week.", color = MutedText, fontSize = 9.5.sp)
                } else published.forEach { task ->
                    Surface(
                        onClick = { if (task.archivedAtMillis <= 0L) onOpenProject(task.id) },
                        enabled = task.archivedAtMillis <= 0L,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(task.title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${task.platform} · ${task.contentType}${if (task.archivedAtMillis > 0L) " · Archived" else ""}", color = MutedText, fontSize = 8.5.sp)
                            }
                            if (task.archivedAtMillis <= 0L) Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(17.dp))
                        }
                    }
                }

                if (active.isNotEmpty()) {
                    Spacer(Modifier.height(11.dp))
                    Text("ACTIVE WORK", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    active.forEach { task ->
                        val pulse = ProjectPulseEngine.snapshot(task)
                        Surface(
                            onClick = { onOpenProject(task.id) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                            shape = RoundedCornerShape(15.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(task.title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${pulse.currentStage.label} · ${CreatorWorkflowEngine.progress(task)}%", color = MutedText, fontSize = 8.5.sp)
                                }
                                Text(pulse.stateLabel, color = if (pulse.state in setOf(ProjectPulseState.OVERDUE, ProjectPulseState.NEEDS_ATTENTION)) RecRed else MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PTodayProjectCard(task: CreatorTask, onClick: () -> Unit) {
    val stage = CreatorWorkflowEngine.currentStage(task)
    val progress = CreatorWorkflowEngine.progress(task)
    val overdue = task.dueAtMillis in 1 until System.currentTimeMillis()
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, if (overdue) RecRed.copy(alpha = .45f) else CinemaLine),
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(100.dp), color = if (overdue) RecRed.copy(alpha = .14f) else Color(0xFF171410)) {
                    Text(if (overdue) "OVERDUE" else task.dueLabel.uppercase(Locale.getDefault()), color = if (overdue) RecRed else MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Text(stage.label.uppercase(), color = ProjectorIvory, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(13.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 10.5.sp)
            Spacer(Modifier.height(16.dp))
            PStageRail(task)
            Spacer(Modifier.height(8.dp))
            Row {
                Text("$progress%", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("complete", color = MutedText, fontSize = 9.sp)
                Spacer(Modifier.weight(1f))
                Text(if (task.status == TaskStatus.WORKING) "IN PROGRESS" else "READY", color = if (task.status == TaskStatus.WORKING) MutedGold else MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PNextMoveCard(task: CreatorTask) {
    val recommendation = CreatorPriorityEngine.recommendation(task)
    val next = CreatorWorkflowEngine.nextStage(task)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(19.dp), CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NEXT MOVE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(Modifier.weight(1f))
                Text(recommendation.urgencyLabel, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
            }
            Spacer(Modifier.height(5.dp))
            Text(recommendation.action, color = ProjectorIvory, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(recommendation.reason, color = MutedText, fontSize = 9.3.sp, lineHeight = 13.sp)
            next?.let {
                Spacer(Modifier.height(5.dp))
                Text("After that · ${it.label}", color = MutedText, fontSize = 9.5.sp)
            }
        }
    }
}
