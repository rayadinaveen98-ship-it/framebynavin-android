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
import com.framebynavin.app.data.CreatorAutomationPreferences
import com.framebynavin.app.data.CreatorAutomationPreferencesStore
import com.framebynavin.app.data.CreatorAutomationStateStore
import com.framebynavin.app.data.CreatorRoutine
import com.framebynavin.app.data.CreatorRoutinePolicy
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorTaskOrigin
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.WeeklyScheduleSlot
import com.framebynavin.app.reminders.CreatorAutoPlanWorker
import com.framebynavin.app.reminders.CreatorRoutineWorker
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun V17AutomationCenterScreen(
    tasks: List<CreatorTask>,
    weeklySlots: List<WeeklyScheduleSlot>,
    weeklyAutoPlanEnabled: Boolean,
    contextNudgesEnabled: Boolean,
    onClose: () -> Unit,
    onWeeklyAutoPlanChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val prefStore = remember { CreatorAutomationPreferencesStore(context.applicationContext) }
    val stateStore = remember { CreatorAutomationStateStore(context.applicationContext) }
    var prefs by remember { mutableStateOf(prefStore.snapshot()) }
    var runRequested by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val horizon = now + 14L * 24L * 60L * 60_000L
    val generated = tasks.count {
        it.origin == CreatorTaskOrigin.WEEKLY &&
            it.status != TaskStatus.SKIPPED &&
            it.dueAtMillis in (now + 1)..horizon
    }
    val enabledSlots = weeklySlots.count { it.enabled }
    val postPublish = tasks.count { it.sourceRefId.startsWith("post-publish:") && it.status != TaskStatus.DONE && it.status != TaskStatus.SKIPPED }
    val lastPlannerAt = stateStore.lastPlannerAtMillis()
    val lastCreated = stateStore.lastPlannerCreatedCount()

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 44.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("AUTOMATION", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Creator Automation", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Let the repetitive parts run.", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("FrameByNavin can prepare and remind. Publishing and destructive actions still stay with you.", color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)

            Spacer(Modifier.height(20.dp))
            Text("BACKGROUND AUTO PLAN", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(MutedGold.copy(alpha = .12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.AutoAwesome, null, tint = MutedGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text("14-day creator planner", color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (weeklyAutoPlanEnabled) "$enabledSlots schedule slots · $generated upcoming projects prepared"
                                else "Off · your weekly schedule remains saved",
                                color = MutedText,
                                fontSize = 8.8.sp,
                            )
                        }
                        Switch(
                            checked = weeklyAutoPlanEnabled,
                            onCheckedChange = { enabled ->
                                onWeeklyAutoPlanChange(enabled)
                                if (enabled) {
                                    CreatorAutoPlanWorker.enqueueNow(context)
                                    runRequested = true
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                        )
                    }
                    if (weeklyAutoPlanEnabled) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when {
                                    runRequested -> "Planner refresh requested"
                                    lastPlannerAt > 0L -> "Last background check · ${v17Time(lastPlannerAt)} · $lastCreated added"
                                    else -> "Background planner is scheduled automatically",
                                },
                                color = MutedText,
                                fontSize = 8.5.sp,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                CreatorAutoPlanWorker.enqueueNow(context)
                                runRequested = true
                            }) {
                                Text("RUN NOW", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("ALWAYS-AWARE AUTOMATION", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            V17StatusRow(
                icon = Icons.Outlined.Share,
                title = "Post-publish follow-ups",
                body = if (postPublish > 0) "$postPublish follow-up actions currently active" else "Creates promotion + 24h + 7d follow-ups after YouTube publishing",
                enabled = true,
            )
            Spacer(Modifier.height(7.dp))
            V17StatusRow(
                icon = Icons.Outlined.NotificationsActive,
                title = "Context nudges",
                body = if (contextNudgesEnabled) "On · creator-risk checks remain separate from exact reminders" else "Off · enable from Settings if you want at-risk creator nudges",
                enabled = contextNudgesEnabled,
            )

            Spacer(Modifier.height(20.dp))
            Text("CREATOR ROUTINES", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Optional background notifications. They never create exact alarms.", color = MutedText, fontSize = 9.sp)
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
                        Text("You remain the final control", color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("Automation can prepare projects, follow-ups and routine notifications. It does not publish, delete creator work or post to social accounts.", color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
                    }
                }
            }
        }
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
            Text(if (enabled) "ON" else "OFF", color = if (enabled) SuccessGreen else MutedText, fontSize = 8.sp, fontWeight = FontWeight.Black)
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
