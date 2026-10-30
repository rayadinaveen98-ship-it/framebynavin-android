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
    onSettings: () -> Unit,
    onPublishLate: (String) -> Unit,
    onReschedule: (String) -> Unit,
    onSkip: (String) -> Unit,
) {
    val overdue = tasks.filter {
        it.archivedAtMillis <= 0L &&
            (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING) &&
            it.dueAtMillis in 1 until System.currentTimeMillis()
    }.sortedBy { it.dueAtMillis }
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
        Text("Capture an idea or continue your work.", color = MutedText, fontSize = 13.sp)
        Spacer(Modifier.height(17.dp))

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

        if (overdue.isNotEmpty()) {
            Spacer(Modifier.height(19.dp))
            V161SectionHeading("NEEDS ATTENTION", "These projects are past their publish time.")
            overdue.take(4).forEach { task ->
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
                            AssistChip(onClick = { onPublishLate(task.id) }, label = { Text("+30 min", fontSize = 11.sp) })
                            AssistChip(onClick = { onReschedule(task.id) }, label = { Text("New time", fontSize = 11.sp) })
                            AssistChip(onClick = { onSkip(task.id) }, label = { Text("Skip", fontSize = 11.sp) })
                        }
                    }
                }
            }
        }

        V161SectionHeading("YOUR WORK", "Find what to make next.")
        V161ActionRow("Daily Brief", "What deserves your focus today", Icons.Outlined.Today,
            "quick-action-daily-brief", onDailyBrief)
        V161ActionRow("Idea Vault",
            if (readyIdeas > 0) "$readyIdeas ready-to-make idea${if (readyIdeas == 1) "" else "s"}"
            else "Find and develop your saved ideas",
            Icons.Outlined.Lightbulb, "quick-action-idea-vault", onIdeas)

        V161SectionHeading("PLAN & ORGANIZE", "See dates and recurring work.")
        V161ActionRow("Content Calendar", "Upcoming publishing and project dates",
            Icons.Outlined.CalendarMonth, "quick-action-content-calendar", onCalendar)
        V161ActionRow("Weekly Plan",
            if (weeklyAutoPlanEnabled) "Automatic project planning is on" else "Your recurring schedule",
            Icons.Outlined.DateRange, "quick-action-weekly-plan", onWeek)
        V161ActionRow("Automation", "Manage background planning and routines",
            Icons.Outlined.AutoAwesome, "quick-action-automation", onAutomation)

        V161SectionHeading("MORE TOOLS", "Existing shortcuts, unchanged.")
        V161ActionRow("Trending Moment", "Plan a quick response",
            Icons.Outlined.Bolt, "quick-action-trending-moment", onRelease)
        V161ActionRow("Reminders", "View and manage existing reminders",
            Icons.Outlined.Alarm, "quick-action-reminders", onReminders)

        V161SectionHeading("YOUR GROWTH", "A record of your creative work.")
        V161ActionRow("Creator Progress",
            "Level ${creatorProgress.level} · ${creatorProgress.totalXp} XP earned",
            Icons.Outlined.EmojiEvents, "quick-action-creator-progress", onCreatorProgress)

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = CinemaLine)
        Spacer(Modifier.height(9.dp))
        V161ActionRow("Settings", "Account, defaults and connections",
            Icons.Outlined.Settings, "quick-action-settings", onSettings)
    }
}

@Composable
private fun V161SectionHeading(label: String, subtitle: String) {
    Spacer(Modifier.height(20.dp))
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
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            .heightIn(min = 64.dp).testTag(tag),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
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
