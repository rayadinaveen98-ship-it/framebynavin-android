package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorRewardProgressSnapshot
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.IdeaStatus
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.*

/**
 * Phase 2 replaces the flat Quick Actions directory with readable, grouped destinations.
 * Every legacy action remains wired to the exact same callback; no project/reminder semantics change.
 */
@Composable
internal fun V161QuickActionsHub(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    weeklyAutoPlanEnabled: Boolean,
    creatorProgress: CreatorRewardProgressSnapshot,
    onNewProject: () -> Unit,
    onQuickCapture: () -> Unit,
    onDailyBrief: () -> Unit,
    onCalendar: () -> Unit,
    onRelease: () -> Unit,
    onIdeas: () -> Unit,
    onWeek: () -> Unit,
    onReminders: () -> Unit,
    onAutomation: () -> Unit,
    onCreatorProgress: () -> Unit,
    // Preserve existing Quick Actions caller/API compatibility for older UI tests.
    // The main app passes a real navigation callback explicitly.
    onProgressJourney: () -> Unit = {},
    onSettings: () -> Unit,
    onPublishLate: (String) -> Unit,
    onReschedule: (String) -> Unit,
    onSkip: (String) -> Unit,
    projectsLoaded: Boolean = true,
) {
    val nowMillis = System.currentTimeMillis()
    val snapshot = UX08FQuickActionsPolicy.snapshot(tasks, nowMillis)
    val overdue = if (projectsLoaded) snapshot.overdueProjects else emptyList()
    // This is strictly a local pending confirmation, not persisted status.
    var pendingSkipId by remember { mutableStateOf<String?>(null) }
    val pendingSkipTask = pendingSkipId?.let { id ->
        tasks.singleOrNull { it.id == id }
    }
    val safeToConfirmSkip = projectsLoaded &&
        UX08FQuickActionsPolicy.isSafeSkipConfirmation(
            pendingSkipId, tasks, System.currentTimeMillis(),
        )
    val readyIdeas = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE }
    val compactActions = LocalConfiguration.current.screenWidthDp < 350 ||
        LocalDensity.current.fontScale >= 1.35f

    Column(
        Modifier.fillMaxWidth()
            .heightIn(max = 730.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
    ) {
        Text("BACKLOT", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text("Quick Actions", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text(
            "Make something, plan the next step or revisit genuine progress.",
            color = MutedText, fontSize = 12.sp, lineHeight = 17.sp,
        )
        Spacer(Modifier.height(15.dp))

        // On narrow phones or high font scale, keep both primary actions full-width.
        if (compactActions) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                V161MainAction("New Project", "Start creating", Icons.Outlined.Add,
                    "quick-action-new-project", onNewProject, Modifier.fillMaxWidth())
                V161MainAction("Quick Capture", "Save an idea", Icons.Outlined.Mic,
                    "quick-action-quick-capture", onQuickCapture, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V161MainAction("New Project", "Start creating", Icons.Outlined.Add,
                    "quick-action-new-project", onNewProject, Modifier.weight(1f))
                V161MainAction("Quick Capture", "Save an idea", Icons.Outlined.Mic,
                    "quick-action-quick-capture", onQuickCapture, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(15.dp))
        if (!projectsLoaded) {
            Text(
                "Creator projects are still loading. Quick Actions remain available; " +
                    "deadline counts will appear after the saved project list loads.",
                color = MutedText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.testTag("ux08f-projects-loading"),
            )
        } else {
            Row(
                Modifier.fillMaxWidth().testTag("ux08f-saved-project-summary"),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${snapshot.activeProjects} active projects",
                    color = MutedText, fontSize = 11.sp,
                )
                Text("·", color = MutedText, fontSize = 11.sp)
                Text(
                    "${snapshot.pastDeadlineProjects} past deadline",
                    color = if (snapshot.pastDeadlineProjects > 0) RecRed else MutedText,
                    fontSize = 11.sp,
                )
            }
        }

        if (overdue.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            V161SectionHeading(
                "NEEDS ATTENTION",
                "Actual saved projects past their deadlines. " +
                    "The controls below change original project records.",
            )
            overdue.forEach { task ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = BacklotSurfaceSelected,
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .26f)),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(task.dueLabel, color = RecRed, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistChip(
                                onClick = { onPublishLate(task.id) },
                                label = { Text("+30 min", fontSize = 11.sp) },
                                modifier = Modifier.testTag("ux08f-overdue-plus-30-" + task.id),
                            )
                            AssistChip(
                                onClick = { onReschedule(task.id) },
                                label = { Text("New time", fontSize = 11.sp) },
                                modifier = Modifier.testTag("ux08f-overdue-new-time-" + task.id),
                            )
                            AssistChip(
                                onClick = { pendingSkipId = task.id },
                                label = { Text("Skip", fontSize = 11.sp) },
                                modifier = Modifier.testTag("ux08f-overdue-skip-" + task.id),
                            )
                        }
                    }
                }
            }
            if (snapshot.pastDeadlineProjects > overdue.size) {
                TextButton(
                    onClick = onDailyBrief,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("ux08f-review-all-deadlines"),
                ) {
                    Text(
                        "Review all ${snapshot.pastDeadlineProjects} past-due projects in Daily Brief",
                        color = MutedGold, fontSize = 11.sp,
                    )
                }
            }
        }

        V161SectionHeading("YOUR DAY", "Choose where to start and what is approaching.")
        V161ActionRow(
            "Daily Brief", "Real priorities, due-today work and what needs review",
            Icons.Outlined.Today, "quick-action-daily-brief", onDailyBrief,
        )
        V161ActionRow(
            "Content Calendar", "Week, Month and Agenda · saved plans, not published claims",
            Icons.Outlined.CalendarMonth, "quick-action-content-calendar", onCalendar,
        )

        V161SectionHeading("CREATE & ORGANIZE", "Keep ideas and recurring production connected.")
        V161ActionRow(
            "Idea Vault",
            if (readyIdeas > 0) "$readyIdeas ready-to-make idea${if (readyIdeas == 1) "" else "s"}"
            else "Find and develop your saved ideas",
            Icons.Outlined.Lightbulb, "quick-action-idea-vault", onIdeas,
        )
        V161ActionRow(
            "Weekly Plan",
            if (weeklyAutoPlanEnabled) "Automatic project planning is on" else "Review recurring creator slots",
            Icons.Outlined.DateRange, "quick-action-weekly-plan", onWeek,
        )
        V161ActionRow(
            "Automation", "Original planning automations and routines",
            Icons.Outlined.AutoAwesome, "quick-action-automation", onAutomation,
        )

        V161SectionHeading("YOUR PROGRESS", "Review real records and credited Creator rewards.")
        V161ActionRow(
            "Progress Journey", "Browse recorded days and historical Creator actions",
            Icons.Outlined.CalendarMonth, "quick-action-progress-journey", onProgressJourney,
        )
        V161ActionRow(
            "Creator Progress",
            "Level ${creatorProgress.level} · ${creatorProgress.totalXp} credited XP · " +
                "${creatorProgress.unlockedAchievementCount} achievements",
            Icons.Outlined.EmojiEvents, "quick-action-creator-progress", onCreatorProgress,
        )

        V161SectionHeading("REMINDERS & RELEASE", "Original reminders and release planning.")
        V161ActionRow(
            "Reminders", "View and manage existing Creator and Personal alerts",
            Icons.Outlined.Alarm, "quick-action-reminders", onReminders,
        )
        V161ActionRow(
            "Trending Moment", "Plan a timely creative response",
            Icons.Outlined.Bolt, "quick-action-trending-moment", onRelease,
        )

        Spacer(Modifier.height(13.dp))
        HorizontalDivider(color = CinemaLine.copy(alpha = 0.55f))
        Spacer(Modifier.height(8.dp))
        V161ActionRow(
            "Settings", "Account, defaults and connections",
            Icons.Outlined.Settings, "quick-action-settings", onSettings,
        )
    }

    if (pendingSkipId != null) {
        AlertDialog(
            onDismissRequest = { pendingSkipId = null },
            title = { Text("Skip this project?", color = ProjectorIvory) },
            text = {
                Text(
                    if (safeToConfirmSkip)
                        "${pendingSkipTask?.title ?: "Selected project"} will be marked Skipped " +
                            "and its original Creator reminders will be disabled. " +
                            "The saved project is not deleted."
                    else "This project has changed or is no longer eligible. " +
                        "Close this confirmation and review its current status.",
                    color = MutedText,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = safeToConfirmSkip,
                    onClick = {
                        val confirmedId = pendingSkipId
                        if (confirmedId != null &&
                            UX08FQuickActionsPolicy.isSafeSkipConfirmation(
                                confirmedId, tasks, System.currentTimeMillis(),
                            )
                        ) {
                            pendingSkipId = null
                            onSkip(confirmedId)
                        }
                    },
                    modifier = Modifier.testTag("ux08f-confirm-skip"),
                ) { Text("SKIP PROJECT", color = RecRed) }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingSkipId = null },
                    modifier = Modifier.testTag("ux08f-cancel-skip"),
                ) { Text("KEEP PROJECT", color = ProjectorIvory) }
            },
            containerColor = CinemaSurfaceRaised,
        )
    }
}

@Composable
private fun V161SectionHeading(label: String, subtitle: String) {
    Spacer(Modifier.height(17.dp))
    Text(label, color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
    Text(subtitle, color = MutedText, fontSize = 11.sp)
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun V161MainAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 118.dp).testTag(tag),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurfaceRaised,
        border = BorderStroke(1.dp, RecRed.copy(alpha = .22f)),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(
                Modifier.size(38.dp).background(RecRed.copy(alpha = .12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, title, tint = RecRed, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.weight(1f))
            Text(title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MutedText, fontSize = 11.sp, maxLines = 2)
        }
    }
}

@Composable
private fun V161ActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)
            .heightIn(min = 64.dp).testTag(tag),
        shape = RoundedCornerShape(13.dp),
        color = CinemaSurface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, CinemaLine.copy(alpha = 0.34f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).background(CinemaSurfaceRaised, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = MutedGold, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MutedText, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(19.dp))
        }
    }
}
