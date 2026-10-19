@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.framebynavin.app.reminders.V196ReminderLaunchReadiness
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.ReminderCenterGroup
import com.framebynavin.app.data.ReminderCenterPolicy
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.reminders.ReminderActionSafety
import com.framebynavin.app.reminders.ReminderNotifications
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.SmartEscalationConfigStore
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * R3: focused reminder inbox. No direct database writes from a Composable:
 * host callbacks pass an occurrence fingerprint to the ViewModel's serialized
 * transaction, and existing project completion/publication logic is untouched.
 */
@Composable
internal fun V157ReminderCenterUi(
    tasks: List<CreatorTask>,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleteReminders: (Set<String>) -> Unit,
    onOpenReminderSettings: () -> Unit,
    onSnooze: ((CreatorTask, Int) -> Unit)?,
    onPause: ((CreatorTask) -> Unit)?,
    onReschedule: ((CreatorTask) -> Unit)?,
    snoozeMinutes: Int,
    onAcknowledge: ((CreatorTask, String, String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRefresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    val grouped = ReminderCenterPolicy.grouped(tasks, now)
    val activeIds = grouped.values.flatten().map { it.id }.toSet()
    var selected by remember { mutableStateOf(setOf<String>()) }
    var turnOffTarget by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selectionMode = selected.isNotEmpty()
    // Recheck Android grants when the user returns from permission Settings.
    val notificationsReady = remember(permissionRefresh, context) {
        ReminderNotifications.canPost(context)
    }
    val exactTimingReady = remember(permissionRefresh, context) {
        ReminderScheduler(context).canScheduleExact()
    }
    val activeReminders = grouped.values.flatten().filter { it.reminderEnabled }
    val hasTimeCritical = activeReminders.any { it.reminderMode in
        setOf(ReminderMode.VOICE, ReminderMode.ALARM, ReminderMode.SMART) }
    val needsAlarm = activeReminders.any { it.reminderMode in
        setOf(ReminderMode.ALARM, ReminderMode.SMART) }
    val needsVoice = activeReminders.any { it.reminderMode in
        setOf(ReminderMode.VOICE, ReminderMode.SMART) }
    val fullScreenReady = remember(permissionRefresh, context) {
        V196ReminderLaunchReadiness.canUseFullScreen(context)
    }
    val alertChannelsReady = remember(permissionRefresh, context, needsAlarm, needsVoice) {
        V196ReminderLaunchReadiness.alertChannelsHighPriority(context, needsAlarm, needsVoice)
    }
    val launchBlocker = V196ReminderLaunchReadiness.blocker(
        hasTimeCritical, notificationsReady, exactTimingReady, fullScreenReady, alertChannelsReady,
    )

    fun select(id: String) {
        selected = if (id in selected) selected - id else selected + id
    }

    Dialog(
        onDismissRequest = { if (selectionMode) selected = emptySet() else onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { if (selectionMode) selected = emptySet() else onDismiss() }) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory)
                    }
                    Spacer(Modifier.width(3.dp))
                    Column(Modifier.weight(1f)) {
                        Text("REMINDERS", color = RecRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(
                            if (selectionMode) "${selected.size} SELECTED" else "Stay on track",
                            color = ProjectorIvory,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (selectionMode) {
                        TextButton(onClick = {
                            selected = if (selected.size == activeIds.size) emptySet() else activeIds
                        }) { Text("SELECT ALL", fontSize = 11.sp) }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.DeleteOutline, "Delete selected", tint = RecRed)
                        }
                    } else {
                        IconButton(onClick = onNew) {
                            Icon(Icons.Outlined.Add, "Add", tint = ProjectorIvory)
                        }
                    }
                }

                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 45.dp),
                ) {
                    Text(
                        "Manage the next check-in without changing your project.",
                        color = MutedText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    val attentionCount = grouped[ReminderCenterGroup.NEEDS_ATTENTION].orEmpty().size
                    val todayCount = grouped[ReminderCenterGroup.TODAY].orEmpty().size
                    val postponedCount = grouped[ReminderCenterGroup.SNOOZED_PAUSED].orEmpty().size
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        V157InboxCount("NEEDS ACTION", attentionCount, RecRed, Modifier.weight(1f))
                        V157InboxCount("TODAY", todayCount, MutedGold, Modifier.weight(1f))
                        V157InboxCount("POSTPONED", postponedCount, ProjectorIvory, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(13.dp))
                    V181ReminderResponseSection(
                        tasks = tasks,
                        nowMillis = now,
                        snoozeMinutes = snoozeMinutes,
                        onOpenProject = onEdit,
                        onAcknowledge = onAcknowledge,
                    )
                    Spacer(Modifier.height(13.dp))
                    Surface(
                        onClick = {
                            when (launchBlocker) {
                                V196ReminderLaunchReadiness.Blocker.FULL_SCREEN ->
                                    V196ReminderLaunchReadiness.openFullScreenSettings(context)
                                V196ReminderLaunchReadiness.Blocker.NOTIFICATIONS,
                                V196ReminderLaunchReadiness.Blocker.ALERT_CHANNEL ->
                                    V196ReminderLaunchReadiness.openNotificationSettings(context)
                                else -> onOpenReminderSettings()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = CinemaSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (launchBlocker != V196ReminderLaunchReadiness.Blocker.NONE)
                                RecRed.copy(alpha = .5f) else CinemaLine,
                        ),
                    ) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (launchBlocker == V196ReminderLaunchReadiness.Blocker.NONE)
                                    Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                                null,
                                tint = if (launchBlocker != V196ReminderLaunchReadiness.Blocker.NONE)
                                    RecRed else MutedGold,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                when (launchBlocker) {
                                    V196ReminderLaunchReadiness.Blocker.NOTIFICATIONS ->
                                        "Notifications off · tap to enable"
                                    V196ReminderLaunchReadiness.Blocker.EXACT_TIMING ->
                                        "Precise reminders off · tap to fix"
                                    V196ReminderLaunchReadiness.Blocker.FULL_SCREEN ->
                                        "Full-screen alerts off · tap to enable"
                                    V196ReminderLaunchReadiness.Blocker.ALERT_CHANNEL ->
                                        "Alarm/voice alerts muted · tap to fix"
                                    V196ReminderLaunchReadiness.Blocker.NONE ->
                                        "Reminder alerts ready"
                                },
                                color = ProjectorIvory,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.Outlined.ChevronRight, "Reminder settings", tint = MutedGold)
                        }
                    }

                    if (activeIds.isEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        V157EmptyReminderCenter(onNew)
                    } else {
                        V157ReminderGroup(
                            label = "NEEDS ATTENTION",
                            subtitle = "Overdue, at-risk or waiting for a check-in",
                            tasks = grouped[ReminderCenterGroup.NEEDS_ATTENTION].orEmpty(),
                            accent = RecRed,
                            now = now,
                            selectionMode = selectionMode,
                            selected = selected,
                            snoozeMinutes = snoozeMinutes,
                            onSelect = ::select,
                            onHold = { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selected = selected + id
                            },
                            onEdit = onEdit,
                            onSnooze = onSnooze,
                            onPause = onPause,
                            onReschedule = onReschedule,
                            onTurnOff = { turnOffTarget = it },
                        )
                        V157ReminderGroup(
                            label = "TODAY",
                            subtitle = "Coming up in the next 24 hours",
                            tasks = grouped[ReminderCenterGroup.TODAY].orEmpty(),
                            accent = MutedGold,
                            now = now,
                            selectionMode = selectionMode,
                            selected = selected,
                            snoozeMinutes = snoozeMinutes,
                            onSelect = ::select,
                            onHold = { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selected = selected + id
                            },
                            onEdit = onEdit,
                            onSnooze = onSnooze,
                            onPause = onPause,
                            onReschedule = onReschedule,
                            onTurnOff = { turnOffTarget = it },
                        )
                        V157ReminderGroup(
                            label = "SNOOZED / PAUSED",
                            subtitle = "Reminders you've delayed",
                            tasks = grouped[ReminderCenterGroup.SNOOZED_PAUSED].orEmpty(),
                            accent = MutedText,
                            now = now,
                            selectionMode = selectionMode,
                            selected = selected,
                            snoozeMinutes = snoozeMinutes,
                            onSelect = ::select,
                            onHold = { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selected = selected + id
                            },
                            onEdit = onEdit,
                            onSnooze = onSnooze,
                            onPause = onPause,
                            onReschedule = onReschedule,
                            onTurnOff = { turnOffTarget = it },
                        )
                        V157ReminderGroup(
                            label = "UPCOMING",
                            subtitle = "Beyond the next 24 hours",
                            tasks = grouped[ReminderCenterGroup.UPCOMING].orEmpty(),
                            accent = MutedText,
                            now = now,
                            selectionMode = selectionMode,
                            selected = selected,
                            snoozeMinutes = snoozeMinutes,
                            onSelect = ::select,
                            onHold = { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selected = selected + id
                            },
                            onEdit = onEdit,
                            onSnooze = onSnooze,
                            onPause = onPause,
                            onReschedule = onReschedule,
                            onTurnOff = { turnOffTarget = it },
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete || turnOffTarget != null) {
        val targets = turnOffTarget?.let { setOf(it) } ?: selected
        AlertDialog(
            onDismissRequest = { confirmDelete = false; turnOffTarget = null },
            title = {
                Text("Turn off attention for ${targets.size} project${if (targets.size == 1) "" else "s"}?")
            },
            text = {
                Text("The projects remain. Only automatic check-ins and scheduled alerts are removed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    if (targets.isNotEmpty()) onDeleteReminders(targets)
                    selected = emptySet()
                    confirmDelete = false
                    turnOffTarget = null
                }) { Text("TURN OFF", color = RecRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false; turnOffTarget = null }) { Text("CANCEL") }
            },
        )
    }
}

@Composable
private fun V157InboxCount(label: String, count: Int, accent: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
            Text(count.toString(), color = accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(label, color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun V157ReminderGroup(
    label: String,
    subtitle: String,
    tasks: List<CreatorTask>,
    accent: Color,
    now: Long,
    selectionMode: Boolean,
    selected: Set<String>,
    snoozeMinutes: Int,
    onSelect: (String) -> Unit,
    onHold: (String) -> Unit,
    onEdit: (String) -> Unit,
    onSnooze: ((CreatorTask, Int) -> Unit)?,
    onPause: ((CreatorTask) -> Unit)?,
    onReschedule: ((CreatorTask) -> Unit)?,
    onTurnOff: (String) -> Unit,
) {
    if (tasks.isEmpty()) return
    val context = LocalContext.current
    val smartConfig = remember(context) { SmartEscalationConfigStore(context.applicationContext) }
    Spacer(Modifier.height(22.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        Text("${tasks.size}", color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(3.dp))
    Text(subtitle, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)
    Spacer(Modifier.height(9.dp))

    tasks.forEach { task ->
        val chosen = task.id in selected
        val pulse = ProjectPulseEngine.snapshot(task, now)
        val actionable = ReminderActionSafety.isActionable(task)
        val snoozeAt = ReminderCenterPolicy.nextSnoozeAt(task, now, snoozeMinutes)
        val smartWindowReady = snoozeAt != null &&
            ReminderCenterPolicy.validTargetWindow(task, now, snoozeAt, smartConfig.get(task))
        val canSnooze = actionable && onSnooze != null && smartWindowReady
        val canReschedule = actionable && onReschedule != null
        val canPause = onPause != null && ReminderCenterPolicy.canPause(task, now)
        var optionsOpen by remember(task.id) { mutableStateOf(false) }

        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).combinedClickable(
                onClick = { if (selectionMode) onSelect(task.id) else onEdit(task.id) },
                onLongClick = { onHold(task.id) },
            ),
            shape = RoundedCornerShape(17.dp),
            color = if (chosen) RecRed.copy(alpha = .11f) else CinemaSurface,
            border = BorderStroke(1.dp, if (chosen) RecRed else CinemaLine),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    if (selectionMode) {
                        Icon(
                            if (chosen) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            null,
                            tint = if (chosen) RecRed else MutedText,
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                    } else {
                        Icon(
                            if (task.pulseManagedReminder) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsNone,
                            null,
                            tint = accent,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 14.sp,
                            lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${V156ReminderSetupCopy.title(task.attentionPlan)} · ${pulse.currentStage.label}",
                            color = MutedText, fontSize = 11.sp, lineHeight = 15.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        if (task.reminderAtMillis > 0L) {
                            Text(
                                "Next alert: ${pFormatDateTime(task.reminderAtMillis)}",
                                color = if (task.reminderAtMillis <= now) RecRed else MutedGold,
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 5.dp),
                            )
                        } else {
                            Text(
                                "Choose a time for your next check-in",
                                color = MutedGold, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp),
                            )
                        }
                    }
                    if (!selectionMode) {
                        Box {
                            IconButton(onClick = { optionsOpen = true }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Outlined.MoreVert, "Reminder options", tint = ProjectorIvory)
                            }
                            DropdownMenu(expanded = optionsOpen, onDismissRequest = { optionsOpen = false }) {
                                if (canPause) {
                                    DropdownMenuItem(
                                        text = { Text("Pause for 2 hours") },
                                        onClick = { optionsOpen = false; onPause?.invoke(task) },
                                        leadingIcon = { Icon(Icons.Outlined.PauseCircle, null) },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Edit project") },
                                    onClick = { optionsOpen = false; onEdit(task.id) },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                )
                                DropdownMenuItem(
                                    text = { Text("Turn off reminders") },
                                    onClick = { optionsOpen = false; onTurnOff(task.id) },
                                    leadingIcon = { Icon(Icons.Outlined.NotificationsOff, null) },
                                )
                            }
                        }
                    }
                }
                if (!selectionMode && (canSnooze || canReschedule)) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (canSnooze) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f).heightIn(min = 45.dp),
                                onClick = { onSnooze?.invoke(task, snoozeMinutes) },
                                border = BorderStroke(1.dp, CinemaLine),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Icon(Icons.Outlined.Snooze, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("SNOOZE ${snoozeMinutes}m", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                        if (canReschedule) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f).heightIn(min = 45.dp),
                                onClick = { onReschedule?.invoke(task) },
                                border = BorderStroke(1.dp, CinemaLine),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CHANGE TIME", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                    if (task.reminderMode == ReminderMode.SMART) {
                        Spacer(Modifier.height(5.dp))
                        Text(
                            if (!smartWindowReady) "Smart escalation needs more time. Choose a later time."
                            else "Snoozing moves the final Smart reminder time.",
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }
                if (!selectionMode && !actionable) {
                    Spacer(Modifier.height(7.dp))
                    TextButton(onClick = { onEdit(task.id) }) {
                        Text("SET UP NEXT REMINDER", color = MutedGold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun V157EmptyReminderCenter(onNew: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.NotificationsNone, null, tint = MutedGold, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(9.dp))
        Text("Nothing needs attention", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
            "Projects with reminders will show up here.",
            color = MutedText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 5.dp),
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onNew, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) {
            Text("NEW PROJECT")
        }
    }
}
