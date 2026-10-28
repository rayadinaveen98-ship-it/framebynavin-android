@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
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
    onPersonal: () -> Unit = {},
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
    // V219: presentation-only filter, never changes reminder storage or action tokens.
    var hubFilter by remember { mutableStateOf(V219ReminderHubFilter.ALL) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // Compute visible groups from real Center data; no synthetic reminders.
    val matchingGroups = V219ReminderSearchPolicy.filter(grouped, searchQuery)
    val visibleSelectionIds = V219ReminderSearchPolicy.visibleIds(matchingGroups, hubFilter)
    val responseTasks = if (searchQuery.isBlank()) tasks else {
        val words = V219ReminderSearchPolicy.terms(searchQuery)
        tasks.filter { V219ReminderSearchPolicy.matches(it, words) }
    }
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

    val reminderStatusPanels: @Composable () -> Unit = {
                    Spacer(Modifier.height(13.dp))
                    V181ReminderResponseSection(
                        tasks = responseTasks,
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

    }

    CreatorAdaptiveFullScreen(
        onDismissRequest = { if (selectionMode) selected = emptySet() else onDismiss() },
    ) {
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
                            selected = if (selected == visibleSelectionIds) emptySet() else visibleSelectionIds
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

                // V223.2: explicit workspace separation. This callback only
                // switches presentation; it never migrates CreatorTask data.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp)
                        .testTag("v2232-reminders-space-switcher"),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).height(43.dp),
                        color = RecRed.copy(alpha = .17f),
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.dp, RecRed.copy(alpha = .65f)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("CREATOR", color = ProjectorIvory,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Surface(
                        onClick = onPersonal,
                        modifier = Modifier.weight(1f).height(43.dp).testTag("v2232-open-personal"),
                        color = CinemaSurface,
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("PERSONAL", color = ProjectorIvory,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        V157InboxCount("NEEDS", attentionCount, RecRed, Modifier.weight(1f))
                        V157InboxCount("TODAY", todayCount, MutedGold, Modifier.weight(1f))
                        V157InboxCount("DELAYED", postponedCount, ProjectorIvory, Modifier.weight(1f))
                    }

                    // V219.2: on-device search, directly composed with V219 chips.
                    Spacer(Modifier.height(13.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { updated ->
                            searchQuery = updated.take(160)
                            // Never leave destructive bulk selections hidden by a search.
                            selected = emptySet()
                        },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = ""; selected = emptySet() },
                                    modifier = Modifier.testTag("v219-search-clear"),
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear reminder search")
                                }
                            }
                        },
                        label = { Text("Search reminders") },
                        placeholder = { Text("Title or notes", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth().testTag("v219-search-input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MutedGold,
                            unfocusedBorderColor = CinemaLine,
                            focusedTextColor = ProjectorIvory,
                            unfocusedTextColor = ProjectorIvory,
                            focusedLabelColor = MutedGold,
                            unfocusedLabelColor = MutedText,
                        ),
                    )
                    if (searchQuery.isNotBlank() && !historyOpen) {
                        Text(
                            "${V219ReminderHubFilter.ALL.count(matchingGroups)} matching reminders",
                            color = MutedText, fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp)
                                .testTag("v219-search-result-count"),
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .testTag("v219-hub-filters"),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        V219ReminderHubFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = !historyOpen && hubFilter == filter,
                                onClick = { hubFilter = filter; historyOpen = false; selected = emptySet() },
                                label = {
                                    Text("${filter.title} ${filter.count(matchingGroups)}", fontSize = 11.sp,
                                        maxLines = 1)
                                },
                                modifier = Modifier.testTag("v219-filter-${filter.name.lowercase()}"),
                            )
                        }
                        FilterChip(
                            selected = historyOpen,
                            onClick = {
                                historyOpen = !historyOpen
                                hubFilter = V219ReminderHubFilter.ALL
                                selected = emptySet()
                            },
                            label = { Text("History", fontSize = 11.sp, maxLines = 1) },
                            modifier = Modifier.testTag("v2193-history-tab"),
                        )
                    }
                    if (!historyOpen && searchQuery.isBlank()) reminderStatusPanels()
                    if (historyOpen) {
                        val historyWords = V219ReminderSearchPolicy.terms(searchQuery)
                        val historyTasks = if (historyWords.isEmpty()) tasks else
                            tasks.filter { V219ReminderSearchPolicy.matches(it, historyWords) }
                        V2193ReminderHistorySection(tasks = historyTasks)
                    } else if (activeIds.isEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        V157EmptyReminderCenter(onNew)
                    } else {
                        if (hubFilter.includes(ReminderCenterGroup.NEEDS_ATTENTION)) V157ReminderGroup(
                            label = "NEEDS ATTENTION",
                            subtitle = "Overdue, at-risk or waiting for a check-in",
                            tasks = matchingGroups[ReminderCenterGroup.NEEDS_ATTENTION].orEmpty(),
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
                        if (hubFilter.includes(ReminderCenterGroup.TODAY)) V157ReminderGroup(
                            label = "TODAY",
                            subtitle = "Coming up in the next 24 hours",
                            tasks = matchingGroups[ReminderCenterGroup.TODAY].orEmpty(),
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
                        if (hubFilter.includes(ReminderCenterGroup.SNOOZED_PAUSED)) V157ReminderGroup(
                            label = "SNOOZED / PAUSED",
                            subtitle = "Reminders you've delayed",
                            tasks = matchingGroups[ReminderCenterGroup.SNOOZED_PAUSED].orEmpty(),
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
                        if (hubFilter.includes(ReminderCenterGroup.UPCOMING)) V157ReminderGroup(
                            label = "UPCOMING",
                            subtitle = "Beyond the next 24 hours",
                            tasks = matchingGroups[ReminderCenterGroup.UPCOMING].orEmpty(),
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
                        if (hubFilter.count(matchingGroups) == 0) {
                            Text(
                                if (searchQuery.isNotBlank()) "No reminders match your search"
                                else "No reminders in this category",
                                color = MutedText,
                                modifier = Modifier.padding(vertical = 20.dp)
                                    .testTag("v219-empty-filter"))
                        }
                    }
                    // Show matches before nonmatching response/status panels during search.
                    if (!historyOpen && searchQuery.isNotBlank()) reminderStatusPanels()
                }
    }

    if (confirmDelete || turnOffTarget != null) {
        val targets = turnOffTarget?.let { setOf(it) } ?: selected.intersect(visibleSelectionIds)
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
