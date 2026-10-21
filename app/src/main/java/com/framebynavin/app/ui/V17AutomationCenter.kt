package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorAutomationPreferencesStore
import com.framebynavin.app.data.CreatorWeeklyPlanPreviewEngine
import com.framebynavin.app.data.CreatorAutomationStateStore
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorRoutine
import com.framebynavin.app.data.CreatorRoutinePolicy
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorTaskOrigin
import com.framebynavin.app.data.PostPublishCheckpoint
import com.framebynavin.app.data.PostPublishCheckpointStatus
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.WeeklyScheduleSlot
import com.framebynavin.app.reminders.CreatorAutoPlanWorker
import com.framebynavin.app.reminders.CreatorContextNudgeWorker
import com.framebynavin.app.reminders.CreatorRoutineWorker
import com.framebynavin.app.reminders.ReminderNotifications
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V17AutomationCenterScreen(
    tasks: List<CreatorTask>,
    postPublishCheckpoints: List<PostPublishCheckpoint> = emptyList(),
    weeklySlots: List<WeeklyScheduleSlot>,
    weeklyAutoPlanEnabled: Boolean,
    contextNudgesEnabled: Boolean,
    onClose: () -> Unit,
    onWeeklyAutoPlanChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val prefStore = remember { CreatorAutomationPreferencesStore(context.applicationContext) }
    val stateStore = remember { CreatorAutomationStateStore(context.applicationContext) }
    val settingsStore = remember { CreatorOsSettingsStore(context.applicationContext) }
    var prefs by remember { mutableStateOf(prefStore.snapshot()) }
    var runRequested by remember { mutableStateOf(false) }
    var requestedAfterMillis by remember { mutableLongStateOf(0L) }
    var statusRevision by remember { mutableIntStateOf(0) }
    var confirmAutoEnable by remember { mutableStateOf(false) }
    var confirmRefresh by remember { mutableStateOf(false) }
    var helpRunRequested by remember { mutableStateOf(false) }
    var nudgesEnabled by remember(contextNudgesEnabled) {
        mutableStateOf(settingsStore.snapshot().contextNudgesEnabled)
    }
    val notificationsReady = ReminderNotifications.canPost(context)

    val now = System.currentTimeMillis()
    val horizon = now + 14L * 24L * 60L * 60_000L
    val generated = tasks.count {
        it.origin == CreatorTaskOrigin.WEEKLY &&
            it.status != TaskStatus.SKIPPED &&
            it.dueAtMillis in (now + 1)..horizon
    }
    val enabledSlots = weeklySlots.count { it.enabled }
    val preview = CreatorWeeklyPlanPreviewEngine.build(weeklySlots, tasks)
    val postPublish = postPublishCheckpoints.count { it.status == PostPublishCheckpointStatus.PENDING }
    val lastPlannerAt = remember(statusRevision) { stateStore.lastPlannerAtMillis() }
    val lastCreated = remember(statusRevision) { stateStore.lastPlannerCreatedCount() }
    val plannerRequestCompleted = runRequested && lastPlannerAt > requestedAfterMillis

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 44.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("AUTOMATION", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Automation", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Plan ahead, automatically", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Backlot can plan and remind you. It won't post or delete your work.", color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)

            Spacer(Modifier.height(20.dp))
            Text("AUTO PLAN", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(MutedGold.copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = MutedGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Plan the next 14 days", color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (weeklyAutoPlanEnabled) "$enabledSlots active templates · $generated future projects saved"
                                else "Off · your templates and existing projects stay saved",
                                color = MutedText,
                                fontSize = 8.8.sp,
                            )
                        }
                        Switch(
                            checked = weeklyAutoPlanEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) confirmAutoEnable = true
                                else { onWeeklyAutoPlanChange(false); runRequested = false }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                        )
                    }
                    if (weeklyAutoPlanEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when {
                                    plannerRequestCompleted -> "Recorded run · ${v17Time(lastPlannerAt)} · $lastCreated created"
                                    runRequested -> "Requested; Android may delay it. No completion verified yet."
                                    lastPlannerAt > 0L -> "Last recorded run · ${v17Time(lastPlannerAt)} · $lastCreated created"
                                    else -> "No completed background run recorded yet"
                                },
                                color = MutedText,
                                fontSize = 8.5.sp,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                if (runRequested && !plannerRequestCompleted) statusRevision++
                                else { runRequested = false; confirmRefresh = true }
                            }) {
                                Text(if (runRequested && !plannerRequestCompleted) "CHECK STATUS" else "PREVIEW & UPDATE",
                                    color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("EXTRA REMINDERS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Get reminders when a project needs attention.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
            Spacer(Modifier.height(9.dp))
            V17StatusRow(
                icon = Icons.Outlined.Share,
                title = "Post-publish checkpoints",
                body = if (postPublish > 0) "$postPublish post-publish actions waiting" else "Automatic after publishing · keeps 24-hour and 7-day reviews attached to the project",
                enabled = true,
            )
            Spacer(Modifier.height(7.dp))
            V17AutomaticHelpRow(
                enabled = nudgesEnabled,
                notificationsReady = notificationsReady,
                checkRequested = helpRunRequested,
                onEnabledChange = { enabled ->
                    settingsStore.setContextNudgesEnabled(enabled)
                    nudgesEnabled = enabled
                    helpRunRequested = false
                    if (enabled) {
                        CreatorContextNudgeWorker.ensurePeriodic(context)
                        CreatorContextNudgeWorker.enqueueNow(context)
                        helpRunRequested = true
                    }
                },
                onCheckNow = {
                    CreatorContextNudgeWorker.ensurePeriodic(context)
                    CreatorContextNudgeWorker.enqueueNow(context)
                    helpRunRequested = true
                },
            )

            Spacer(Modifier.height(20.dp))
            Text("REGULAR REMINDERS", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("These are optional notification routines, checked periodically by Android. Times are approximate, not guaranteed alarms.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
            if (!notificationsReady) {
                Text("Notifications are currently unavailable on this device. Enabled routines may run without showing alerts.",
                    color = RecRed, fontSize = 10.sp)
            }
            Spacer(Modifier.height(9.dp))
            V17RoutineRow(
                title = "Daily Brief",
                schedule = CreatorRoutinePolicy.scheduleLabel(CreatorRoutine.DAILY_BRIEF),
                checked = prefs.dailyBriefRoutineEnabled,
                onChecked = { value ->
                    prefStore.setDailyBriefRoutineEnabled(value)
                    prefs = prefStore.snapshot()
                    CreatorRoutineWorker.ensurePeriodic(context)
                },
            )
            Spacer(Modifier.height(7.dp))
            V17RoutineRow(
                title = "Weekly Creator Review",
                schedule = CreatorRoutinePolicy.scheduleLabel(CreatorRoutine.WEEKLY_REVIEW),
                checked = prefs.weeklyReviewRoutineEnabled,
                onChecked = { value ->
                    prefStore.setWeeklyReviewRoutineEnabled(value)
                    prefs = prefStore.snapshot()
                    CreatorRoutineWorker.ensurePeriodic(context)
                },
            )
            Spacer(Modifier.height(7.dp))
            V17RoutineRow(
                title = "Idea Vault Review",
                schedule = CreatorRoutinePolicy.scheduleLabel(CreatorRoutine.IDEA_REVIEW),
                checked = prefs.ideaReviewRoutineEnabled,
                onChecked = { value ->
                    prefStore.setIdeaReviewRoutineEnabled(value)
                    prefs = prefStore.snapshot()
                    CreatorRoutineWorker.ensurePeriodic(context)
                },
            )

            Spacer(Modifier.height(20.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), RecRed.copy(alpha = .07f), border = BorderStroke(1.dp, RecRed.copy(alpha = .22f))) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Lock, null, tint = RecRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("You stay in control", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("Backlot won't post or delete anything for you.", color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
                    }
                }
            }
        }
    }

    if (confirmAutoEnable) {
        AlertDialog(
            onDismissRequest = { confirmAutoEnable = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Enable automatic planning?", color = ProjectorIvory) },
            text = { Text("This can add ${preview.missing.size} missing projects over the next 14 days. It will not modify existing projects or post content. Android chooses when the background worker executes.", color = MutedText) },
            confirmButton = {
                Button(onClick = {
                    confirmAutoEnable = false
                    onWeeklyAutoPlanChange(true)
                    requestedAfterMillis = stateStore.lastPlannerAtMillis()
                    CreatorAutoPlanWorker.enqueueNow(context)
                    statusRevision++
                    runRequested = true
                }, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("ENABLE AUTO PLAN") }
            },
            dismissButton = { TextButton(onClick = { confirmAutoEnable = false }) { Text("CANCEL") } },
        )
    }

    if (confirmRefresh) {
        AlertDialog(
            onDismissRequest = { confirmRefresh = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Request a plan update?", color = ProjectorIvory) },
            text = {
                Column {
                    Text("Up to ${preview.missing.size} missing projects may be added. Existing projects and creator edits stay unchanged.", color = MutedText)
                    preview.missing.take(4).forEach { occurrence ->
                        Text("• ${occurrence.slot.title}", color = ProjectorIvory, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    confirmRefresh = false
                    requestedAfterMillis = stateStore.lastPlannerAtMillis()
                    CreatorAutoPlanWorker.enqueueNow(context)
                    statusRevision++
                    runRequested = true
                }, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("REQUEST UPDATE") }
            },
            dismissButton = { TextButton(onClick = { confirmRefresh = false }) { Text("CANCEL") } },
        )
    }

}

@Composable
private fun V17StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    enabled: Boolean,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (enabled) MutedGold else MutedText, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Text(body, color = MutedText, fontSize = 8.6.sp, lineHeight = 12.sp)
            }
            Text(if (enabled) "ON" else "OFF", color = if (enabled) SuccessGreen else MutedText, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V17AutomaticHelpRow(
    enabled: Boolean,
    notificationsReady: Boolean,
    checkRequested: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, if (enabled) RecRed.copy(alpha = .40f) else CinemaLine)) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, null, tint = if (enabled) RecRed else MutedText, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Helpful reminders", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            !notificationsReady -> "Turn on notifications in Settings to get alerts."
                            enabled -> "On · Get alerts when active projects need attention"
                            else -> "Off · enable proactive project checks"
                        },
                        color = MutedText,
                        fontSize = 8.6.sp,
                        lineHeight = 12.sp,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                )
            }
            if (enabled) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (checkRequested) "Check requested · delivery unconfirmed" else "Android checks in the background when available",
                        color = if (checkRequested) SuccessGreen else MutedText,
                        fontSize = 8.3.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onCheckNow) {
                        Text("CHECK PROJECTS", color = RecRed, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun V17RoutineRow(
    title: String,
    schedule: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Repeat, null, tint = if (checked) RecRed else MutedText, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Text(schedule, color = MutedText, fontSize = 8.6.sp)
            }
            Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedTrackColor = RecRed))
        }
    }
}

private fun v17Time(millis: Long): String =
    SimpleDateFormat("EEE · h:mm a", Locale.getDefault()).format(Date(millis))
