package com.framebynavin.app.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.reminders.SmartSessionStore
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private val heroQuotes = listOf(
    "Cinema begins where explanation ends.",
    "A frame can hold a lifetime before a word is spoken.",
    "Great stories make time disappear.",
    "Sometimes the silence between two cuts says everything.",
    "Series become memories when characters begin to feel real.",
    "The best images do not decorate a story. They become the story.",
    "A camera can reveal what a character is afraid to say.",
    "Every unforgettable scene has a rhythm of its own.",
    "We return to stories because they let us feel twice.",
    "One perfect frame can make an entire film stay with you.",
)

@Composable
internal fun V131CinematicWelcome() {
    val frame = remember { Animatable(0.82f) }
    val copy = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        frame.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(350)
        copy.animateTo(1f, tween(850))
    }
    LaunchedEffect(Unit) {
        delay(700)
        sweep.animateTo(1f, tween(1250, easing = FastOutSlowInEasing))
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(RecRed.copy(alpha = .13f), Color(0xFF0B0B0D), CinemaBlack),
                radius = 1150f,
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth(.78f).aspectRatio(1.55f).alpha(frame.value),
            contentAlignment = Alignment.Center,
        ) {
            // restrained cinematic frame corners
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.TopStart).width(46.dp).height(2.dp).background(ProjectorIvory))
                Box(Modifier.align(Alignment.TopStart).width(2.dp).height(32.dp).background(ProjectorIvory))
                Box(Modifier.align(Alignment.TopEnd).width(46.dp).height(2.dp).background(ProjectorIvory))
                Box(Modifier.align(Alignment.TopEnd).width(2.dp).height(32.dp).background(ProjectorIvory))
                Box(Modifier.align(Alignment.BottomStart).width(46.dp).height(2.dp).background(ProjectorIvory))
                Box(Modifier.align(Alignment.BottomStart).width(2.dp).height(32.dp).background(ProjectorIvory))
                Box(Modifier.align(Alignment.BottomEnd).width(46.dp).height(2.dp).background(ProjectorIvory))
                Box(Modifier.align(Alignment.BottomEnd).width(2.dp).height(32.dp).background(ProjectorIvory))

                Box(
                    Modifier.fillMaxWidth(.62f).height(1.dp).align(Alignment.Center)
                        .offset(x = ((sweep.value - .5f) * 80).dp)
                        .background(ProjectorIvory.copy(alpha = .08f + sweep.value * .12f))
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(copy.value)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(RecRed, CircleShape))
                    Spacer(Modifier.width(7.dp))
                    Text("REC", color = RecRed, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                }
                Spacer(Modifier.height(13.dp))
                Text("FRAME", color = ProjectorIvory, fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text("BY NAVIN", color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Spacer(Modifier.height(14.dp))
                Text("CREATOR OS", color = MutedText, fontSize = 8.sp, letterSpacing = 2.2.sp)
            }
        }

        Text(
            "PLAN  •  CREATE  •  PUBLISH  •  LEARN",
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 30.dp).alpha(copy.value),
            color = MutedText.copy(alpha = .75f),
            fontSize = 7.5.sp,
            letterSpacing = 1.4.sp,
        )
    }
}

@Composable
internal fun V131HomeHeroSlideshow() {
    val context = LocalContext.current
    val resourceIds = remember {
        (1..10).mapNotNull { index ->
            val name = "hero_frame_${index.toString().padStart(2, '0')}"
            context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 }
        }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(resourceIds.size) {
        if (resourceIds.size > 1) {
            while (true) {
                delay(4_500L)
                index = (index + 1) % resourceIds.size
            }
        }
    }
    val quoteIndex = if (resourceIds.isEmpty()) 0 else index % heroQuotes.size

    Surface(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        shape = RoundedCornerShape(24.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine.copy(alpha = .7f)),
        shadowElevation = 8.dp,
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))) {
            if (resourceIds.isNotEmpty()) {
                AnimatedContent(
                    targetState = index.coerceIn(0, resourceIds.lastIndex),
                    transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(900)) },
                    label = "cinemaHero",
                ) { visibleIndex ->
                    Image(
                        painter = painterResource(resourceIds[visibleIndex]),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, CinemaBlack.copy(alpha = .12f), CinemaBlack.copy(alpha = .92f))
                        )
                    )
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(Color(0xFF16161A), Color(0xFF0B0B0D), RecRedDeep.copy(alpha = .35f))
                        )
                    )
                )
                Column(Modifier.align(Alignment.Center).padding(horizontal = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Collections, null, tint = MutedGold, modifier = Modifier.size(27.dp))
                    Spacer(Modifier.height(9.dp))
                    Text("YOUR CINEMA WALL", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
                    Text("Ready for your 10 favourite HD frames", color = MutedText, fontSize = 8.5.sp)
                }
            }

            Column(Modifier.align(Alignment.BottomStart).padding(17.dp)) {
                Text("FRAME NOTES", color = RecRed, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "“${heroQuotes[quoteIndex]}”",
                    color = ProjectorIvory,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.fillMaxWidth(.88f),
                )
            }
            if (resourceIds.size > 1) {
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(15.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(resourceIds.size) { dot ->
                        Box(
                            Modifier.width(if (dot == index) 14.dp else 5.dp).height(3.dp)
                                .background(if (dot == index) RecRed else ProjectorIvory.copy(alpha = .35f), RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun V131PlanScreen(
    tasks: List<CreatorTask>,
    onAdd: () -> Unit,
    onStart: (String) -> Unit,
    onDone: (String) -> Unit,
    onDeleteSelected: (Set<String>) -> Unit,
) {
    val now = System.currentTimeMillis()
    val visibleTasks = remember(tasks.toList()) { tasks.filter { it.status != TaskStatus.SKIPPED && it.archivedAtMillis <= 0L } }
    val active = visibleTasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
        .sortedBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE }
    val overdue = active.filter { it.dueAtMillis in 1 until now }
    val today = active.filter { it !in overdue && v131Date(it.dueAtMillis) == LocalDate.now() }
    val upcoming = active.filter { it !in overdue && it !in today }
    val completed = visibleTasks.filter { it.status == TaskStatus.DONE }.sortedByDescending { it.dueAtMillis }
    val selectable = (active + completed).map { it.id }.toSet()

    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selectionMode = selected.isNotEmpty()
    val haptics = LocalHapticFeedback.current

    Column(
        Modifier.fillMaxSize().background(CinemaBlack).verticalScroll(androidx.compose.foundation.rememberScrollState())
            .statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 124.dp)
    ) {
        V131SelectionTopBar(
            title = "PLAN",
            selectionMode = selectionMode,
            selectedCount = selected.size,
            onExitSelection = { selected = emptySet() },
            onSelectAll = { selected = if (selected.size == selectable.size) emptySet() else selectable },
            onDelete = { if (selected.isNotEmpty()) confirmDelete = true },
            onAdd = onAdd,
        )
        Spacer(Modifier.height(18.dp))
        Text("See the week clearly.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Text("Hold a project for 2 seconds to select and manage it.", color = MutedText, fontSize = 9.4.sp)
        Spacer(Modifier.height(18.dp))

        if (active.isEmpty() && completed.isEmpty()) {
            V131Empty("Your plan is clear", "Create a project when you know what you're making next.", onAdd)
        } else {
            V131PlanSection("OVERDUE", overdue, RecRed, selectionMode, selected, { id -> selected = v131Toggle(selected, id) }, {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress); selected = selected + it
            }, onStart, onDone)
            V131PlanSection("TODAY", today, MutedGold, selectionMode, selected, { id -> selected = v131Toggle(selected, id) }, {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress); selected = selected + it
            }, onStart, onDone)
            V131PlanSection("UPCOMING", upcoming, ProjectorIvory, selectionMode, selected, { id -> selected = v131Toggle(selected, id) }, {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress); selected = selected + it
            }, onStart, onDone)
            if (completed.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                V131PlanSection("COMPLETED", completed, SuccessGreen, selectionMode, selected, { id -> selected = v131Toggle(selected, id) }, {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress); selected = selected + it
                }, onStart, onDone)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${selected.size} project${if (selected.size == 1) "" else "s"}?") },
            text = { Text("This removes the selected project data from Plan and Studio. Weekly generated occurrences are suppressed so they do not immediately come back.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSelected(selected)
                    selected = emptySet()
                    confirmDelete = false
                }) { Text("DELETE", color = RecRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("CANCEL") } },
        )
    }
}

@Composable
private fun V131PlanSection(
    label: String,
    tasks: List<CreatorTask>,
    accent: Color,
    selectionMode: Boolean,
    selected: Set<String>,
    onToggleSelection: (String) -> Unit,
    onHold: (String) -> Unit,
    onStart: (String) -> Unit,
    onDone: (String) -> Unit,
) {
    if (tasks.isEmpty()) return
    Text("$label · ${tasks.size}", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
    Spacer(Modifier.height(8.dp))
    tasks.forEach { task ->
        val chosen = task.id in selected
        val progress = CreatorWorkflowEngine.progress(task)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).v131Hold2s(
                onHold = { onHold(task.id) },
                onTap = { if (selectionMode) onToggleSelection(task.id) },
            ),
            shape = RoundedCornerShape(18.dp),
            color = if (chosen) RecRed.copy(alpha = .10f) else CinemaSurface,
            border = BorderStroke(1.dp, if (chosen) RecRed else if (label == "OVERDUE") RecRed.copy(alpha = .35f) else CinemaLine),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectionMode) {
                        Icon(if (chosen) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (chosen) RecRed else MutedText, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(9.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${task.dueLabel} · ${task.platform}", color = if (label == "OVERDUE") RecRed else MutedText, fontSize = 9.sp)
                    }
                    Text(if (task.status == TaskStatus.DONE) "DONE" else CreatorWorkflowEngine.currentStage(task).label.uppercase(Locale.getDefault()), color = accent, fontSize = 8.4.sp, fontWeight = FontWeight.Bold)
                }
                if (!selectionMode) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(3.dp), color = accent, trackColor = Color(0xFF292929))
                    if (task.status != TaskStatus.DONE) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("$progress%", color = MutedText, fontSize = 8.5.sp)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onStart(task.id) }) { Text(if (task.status == TaskStatus.WORKING) "CONTINUE" else "START", color = ProjectorIvory, fontSize = 8.5.sp) }
                            TextButton(onClick = { onDone(task.id) }) { Text("DONE", color = SuccessGreen, fontSize = 8.5.sp) }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun V131ReminderCenter(
    tasks: List<CreatorTask>,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleteReminders: (Set<String>) -> Unit,
) {
    val context = LocalContext.current
    val sessions = remember { SmartSessionStore(context.applicationContext) }
    val now = System.currentTimeMillis()
    val active = tasks.filter {
        (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING) &&
            it.reminderEnabled && it.reminderMode != ReminderMode.NONE && it.archivedAtMillis <= 0L
    }.sortedBy { it.reminderAtMillis.takeIf { time -> time > 0L } ?: Long.MAX_VALUE }

    val remindingNow = active.filter { sessions.current(it.id)?.snoozedStage == null && sessions.current(it.id) != null }
    val snoozed = active.filter { task ->
        val session = sessions.current(task.id)
        val smart = session?.snoozedStage != null && session.snoozedUntilMillis > now
        val regular = task.reminderMode != ReminderMode.SMART && task.snoozeCount > 0 && task.reminderAtMillis > now
        (smart || regular) && task !in remindingNow
    }
    val upcoming = active.filter { it !in remindingNow && it !in snoozed && it.reminderAtMillis >= now && it.reminderAtMillis <= now + 24 * 60 * 60_000L }
    val later = active.filter { it !in remindingNow && it !in snoozed && it !in upcoming && it.reminderAtMillis > now }
    val attention = active.filter { it !in remindingNow && it.reminderAtMillis in 1 until now }

    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selectionMode = selected.isNotEmpty()
    val allIds = active.map { it.id }.toSet()
    val haptics = LocalHapticFeedback.current

    Dialog(onDismissRequest = { if (selectionMode) selected = emptySet() else onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                V131SelectionTopBar(
                    title = "REMINDERS",
                    selectionMode = selectionMode,
                    selectedCount = selected.size,
                    onExitSelection = { selected = emptySet() },
                    onSelectAll = { selected = if (selected.size == allIds.size) emptySet() else allIds },
                    onDelete = { if (selected.isNotEmpty()) confirmDelete = true },
                    onAdd = onNew,
                    onBack = onDismiss,
                )
                Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp)) {
                    Text("Stay on track", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Hold a reminder for 2 seconds to select it. Deleting here removes only the reminder, never the project or Plan item.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    if (active.isEmpty()) {
                        V131Empty("No active reminders", "Projects can still live in Today, Plan and Studio without alerts.", onNew)
                    } else {
                        V131ReminderGroup("REMINDING NOW", remindingNow, RecRed, selectionMode, selected, { id -> selected = v131Toggle(selected, id) }, {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress); selected = selected + it
                        }, onEdit)
                        V131ReminderGroup("NEEDS ATTENTION", attention, RecRed, selectionMode, selected, { id -> selected = v131Toggle(selected, id) }, {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress); selected = selected + it
                        }, onEdit)
                        V131ReminderGroup("SNOOZED", snoozed, MutedGold, selectionMode, selected, { id -> selected = v131Toggle(selected, id) }, {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress); selected = selected + it
                        }, onEdit)
                        V131ReminderGroup("UPCOMING", upcoming, MutedGold, selectionMode, selected, { id -> selected = v131Toggle(selected, id) }, {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress); selected = selected + it
                        }, onEdit)
                        V131ReminderGroup("LATER", later, MutedText, selectionMode, selected, { id -> selected = v131Toggle(selected, id) }, {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress); selected = selected + it
                        }, onEdit)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${selected.size} reminder${if (selected.size == 1) "" else "s"}?") },
            text = { Text("Only the reminder settings and scheduled alerts will be removed. Your projects remain in Plan and Studio.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteReminders(selected)
                    selected = emptySet()
                    confirmDelete = false
                }) { Text("DELETE REMINDERS", color = RecRed) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("CANCEL") } },
        )
    }
}

@Composable
private fun V131ReminderGroup(
    label: String,
    tasks: List<CreatorTask>,
    accent: Color,
    selectionMode: Boolean,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onHold: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    if (tasks.isEmpty()) return
    Text("$label · ${tasks.size}", color = accent, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(8.dp))
    tasks.forEach { task ->
        val chosen = task.id in selected
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).v131Hold2s(
                onHold = { onHold(task.id) },
                onTap = { if (selectionMode) onToggle(task.id) else onEdit(task.id) },
            ),
            shape = RoundedCornerShape(18.dp),
            color = if (chosen) RecRed.copy(alpha = .10f) else CinemaSurface,
            border = BorderStroke(1.dp, if (chosen) RecRed else if (label == "REMINDING NOW" || label == "NEEDS ATTENTION") RecRed.copy(alpha = .4f) else CinemaLine),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Icon(if (chosen) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (chosen) RecRed else MutedText, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(v131ModeIcon(task.reminderMode), null, tint = accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(task.title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${v131ModeLabel(task.reminderMode)} · ${v131Format(task.reminderAtMillis)}", color = accent, fontSize = 9.sp)
                    Text("${task.platform} · ${CreatorWorkflowEngine.currentStage(task).label}", color = MutedText, fontSize = 8.6.sp)
                }
                if (!selectionMode) {
                    if (task.reminderMode == ReminderMode.SMART) Text(v131Priority(task.priority), color = MutedGold, fontSize = 7.8.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun V131StudioScreen(
    tasks: List<CreatorTask>,
    onAdd: () -> Unit,
    onAdvance: (String) -> Unit,
    onBack: (String) -> Unit,
    onFocus: (String) -> Unit,
    onArchive: (String) -> Unit,
    onUnarchive: (String) -> Unit,
    onDelete: (String) -> Unit,
    externalExpandId: String? = null,
    externalExpandNonce: Long = 0L,
) {
    val projects = tasks.filter { it.status != TaskStatus.SKIPPED && it.archivedAtMillis <= 0L }
        .sortedWith(compareBy<CreatorTask> { it.status == TaskStatus.DONE }.thenBy { it.dueAtMillis.takeIf { due -> due > 0L } ?: Long.MAX_VALUE })
    val archived = tasks.filter { it.archivedAtMillis > 0L }.sortedByDescending { it.archivedAtMillis }
    val listState = rememberLazyListState()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<CreatorTask?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(externalExpandNonce) {
        val id = externalExpandId ?: return@LaunchedEffect
        val position = projects.indexOfFirst { it.id == id }
        if (position >= 0) {
            expandedId = id
            listState.animateScrollToItem(position + 2)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(CinemaBlack).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 124.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("STUDIO", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Build the thing.", color = ProjectorIvory, fontSize = 29.sp, fontWeight = FontWeight.Black)
                }
                Surface(onClick = onAdd, shape = CircleShape, color = RecRed, modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, "Create project", tint = ProjectorIvory) }
                }
            }
            Text("Completed projects now have archive and delete controls.", color = MutedText, fontSize = 9.sp)
            Spacer(Modifier.height(18.dp))
        }

        if (projects.isEmpty()) {
            item { V131Empty("Studio is empty", "Create a project and its production stages will live here.", onAdd) }
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
                if (task.status == TaskStatus.DONE) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { onArchive(task.id) }) {
                            Icon(Icons.Outlined.Archive, null, tint = MutedGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("ARCHIVE", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { deleteTarget = task }) {
                            Icon(Icons.Outlined.DeleteOutline, null, tint = RecRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("DELETE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (archived.isNotEmpty()) {
            item {
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = { showArchived = !showArchived },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Inventory2, null, tint = MutedGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ARCHIVED · ${archived.size}", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Icon(if (showArchived) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = MutedText)
                    }
                }
            }
            if (showArchived) {
                items(archived, key = { "archived-${it.id}" }) { task ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = CinemaSurface.copy(alpha = .8f),
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(task.title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Archived · ${task.platform} ${task.contentType}", color = MutedText, fontSize = 8.5.sp)
                            }
                            TextButton(onClick = { onUnarchive(task.id) }) { Text("RESTORE", color = MutedGold, fontSize = 8.sp) }
                            IconButton(onClick = { deleteTarget = task }) { Icon(Icons.Outlined.DeleteOutline, "Delete", tint = RecRed, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${task.title}?") },
            text = { Text("This permanently removes the project from Creator OS. If it came from Weekly Plan, this occurrence will stay suppressed instead of being regenerated.") },
            confirmButton = { TextButton(onClick = { onDelete(task.id); deleteTarget = null }) { Text("DELETE", color = RecRed) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("CANCEL") } },
        )
    }
}

@Composable
private fun V131SelectionTopBar(
    title: String,
    selectionMode: Boolean,
    selectedCount: Int,
    onExitSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onAdd: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (selectionMode) {
            IconButton(onClick = onExitSelection) { Icon(Icons.Outlined.Close, "Exit selection", tint = ProjectorIvory) }
            Text("$selectedCount SELECTED", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSelectAll) { Text("SELECT ALL", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Delete selected", tint = RecRed) }
        } else {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
            Column(Modifier.weight(1f)) {
                Text(title, color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            }
            Surface(onClick = onAdd, shape = CircleShape, color = RecRed, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Add, "Add", tint = ProjectorIvory) }
            }
        }
    }
}

@Composable
private fun V131Empty(title: String, body: String, onAdd: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.MovieCreation, null, tint = MutedGold, modifier = Modifier.size(31.dp))
            Spacer(Modifier.height(9.dp))
            Text(title, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(body, color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(14.dp)) {
                Text("CREATE PROJECT", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun Modifier.v131Hold2s(onHold: () -> Unit, onTap: () -> Unit): Modifier = pointerInput(onHold, onTap) {
    detectTapGestures(
        onPress = {
            val started = SystemClock.elapsedRealtime()
            val released = tryAwaitRelease()
            if (released) {
                val held = SystemClock.elapsedRealtime() - started
                if (held >= 1_950L) onHold() else onTap()
            }
        }
    )
}

private fun v131Toggle(values: Set<String>, id: String): Set<String> = if (id in values) values - id else values + id
private fun v131Date(millis: Long): LocalDate? = millis.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
private fun v131Format(millis: Long): String = if (millis <= 0L) "No time" else SimpleDateFormat("EEE · h:mm a", Locale.getDefault()).format(Date(millis))
private fun v131Priority(priority: TaskPriority): String = priority.name.lowercase().replaceFirstChar { it.uppercase() }
private fun v131ModeLabel(mode: ReminderMode): String = when (mode) {
    ReminderMode.NONE -> "None"
    ReminderMode.SIMPLE -> "Notification"
    ReminderMode.VOICE -> "Voice"
    ReminderMode.ALARM -> "Alarm"
    ReminderMode.SMART -> "Smart"
}
private fun v131ModeIcon(mode: ReminderMode) = when (mode) {
    ReminderMode.NONE -> Icons.Outlined.NotificationsOff
    ReminderMode.SIMPLE -> Icons.Outlined.NotificationsNone
    ReminderMode.VOICE -> Icons.Outlined.RecordVoiceOver
    ReminderMode.ALARM -> Icons.Outlined.Alarm
    ReminderMode.SMART -> Icons.Outlined.AutoAwesome
}
