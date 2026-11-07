package com.framebynavin.app.phase1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Debug-only native Compose screenshot proposal for Backlot's common Reminder Hub.
 * Nothing is wired to app navigation, storage, cloud, alarm receivers or live data.
 *
 * This inherits the OWNER-CHOSEN Option A direction from Phase1VisualReviewActivity:
 * charcoal / ivory / restrained warm amber-coral, compact and readable.
 * Both workspaces use the same shell, while body information remains specialized.
 * Never ship these synthetic example items or turn them into default user reminders.
 */
private val HubInk = Phase1OptionAReviewTokens.Ink
private val HubPanel = Phase1OptionAReviewTokens.Panel
private val HubRaised = Phase1OptionAReviewTokens.Raised
private val HubLine = Phase1OptionAReviewTokens.Border
private val HubIvory = Phase1OptionAReviewTokens.Ivory
private val HubMuted = Phase1OptionAReviewTokens.Muted
private val HubGold = Phase1OptionAReviewTokens.Gold
private val HubWarm = Phase1OptionAReviewTokens.WarmAction
private val HubRadius = RoundedCornerShape(19.dp)

@Composable
internal fun Phase1HubVisualReview(scene: String) {
    val isPersonal = scene == "personal-hub"
    val workspace = if (isPersonal) "Personal" else "Creator"
    Column(
        Modifier.fillMaxSize().background(HubInk).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 15.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HubNavigation()
        Spacer(Modifier.height(3.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "BACKLOT  /  REMINDERS",
                color = HubGold, fontSize = 10.sp, letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Your reminders",
                color = HubIvory, fontSize = 31.sp, lineHeight = 35.sp,
                letterSpacing = (-0.8).sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Stay organized, without the clutter.",
                color = HubMuted, fontSize = 13.sp, lineHeight = 20.sp,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HubWorkspaceTab("Personal", selected = isPersonal, Modifier.weight(1f))
            HubWorkspaceTab("Creator", selected = !isPersonal, Modifier.weight(1f))
        }
        HubSummary(workspace, isPersonal)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            HubFilter("Today", true)
            HubFilter("Upcoming", false)
            HubFilter("Paused", false)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isPersonal) "TODAY'S REMINDERS" else "PROJECT REMINDERS",
                color = HubMuted, fontWeight = FontWeight.Bold,
                fontSize = 10.sp, letterSpacing = 1.1.sp,
            )
            Spacer(Modifier.weight(1f))
            Text("See all   ›", color = HubGold, fontSize = 12.sp)
        }
        if (isPersonal) {
            HubReminderItem(
                symbol = "◷",
                title = "Water the plants",
                subtitle = "Every morning  ·  7:30 AM",
                state = "Upcoming",
            )
            HubReminderItem(
                symbol = "♫",
                title = "Read a chapter",
                subtitle = "Daily  ·  8:00 PM",
                state = "Voice",
            )
            HubFootnote(
                heading = "Your personal space",
                description = "Keep routines, notes, photo journals and challenges together.",
                action = "View Spaces",
            )
        } else {
            HubReminderItem(
                symbol = "◉",
                title = "Review rough cut",
                subtitle = "Project: Film study  ·  3:00 PM",
                state = "Today",
            )
            HubReminderItem(
                symbol = "◷",
                title = "Publish project update",
                subtitle = "Project: Weekly series  ·  6:00 PM",
                state = "Upcoming",
            )
            HubFootnote(
                heading = "From your projects",
                description = "Creator reminders keep their project links and check-in actions.",
                action = "Open projects",
            )
        }
        HorizontalDivider(color = HubLine)
        Text(
            "SAMPLE DATA  ·  VISUAL PROTOTYPE ONLY",
            color = HubMuted.copy(alpha = .85f),
            fontSize = 10.sp, letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
        )
        Text(
            "No reminders are created, scheduled, completed or changed on this screen.",
            color = HubMuted, fontSize = 11.sp, lineHeight = 16.sp,
        )
    }
}

@Composable
private fun HubNavigation() {
    Row(
        Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back preview",
            tint = HubIvory, modifier = Modifier.size(23.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = HubPanel, shape = CircleShape,
                border = BorderStroke(1.dp, HubLine),
            ) {
                Box(Modifier.size(35.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.NotificationsActive,
                        contentDescription = "Activity Center preview",
                        tint = HubGold, modifier = Modifier.size(18.dp))
                }
            }
            Surface(
                color = HubRaised, shape = CircleShape,
                border = BorderStroke(1.dp, HubLine),
            ) {
                Box(Modifier.size(35.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Add,
                        contentDescription = "New reminder preview",
                        tint = HubIvory, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun HubWorkspaceTab(label: String, selected: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(13.dp)
    Surface(
        modifier = modifier.height(43.dp),
        color = if (selected) Phase1OptionAReviewTokens.ActiveChip else HubPanel,
        shape = shape,
        border = BorderStroke(1.dp, if (selected) HubGold else HubLine),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) HubIvory else HubMuted,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HubSummary(workspace: String, personal: Boolean) {
    Surface(
        shape = HubRadius,
        color = Color(0xFF201F22),
        border = BorderStroke(1.dp, Color(0xFF786851)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$workspace / TODAY",
                    color = HubGold, fontWeight = FontWeight.Bold,
                    fontSize = 10.sp, letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.weight(1f))
                Text("◌", color = HubGold, fontSize = 20.sp)
            }
            Text(
                if (personal) "A little progress, every day." else "Keep the next step moving.",
                color = HubIvory, fontSize = 21.sp, lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (personal) "2 example reminders  ·  1 coming up"
                    else "2 example project reminders  ·  1 due today",
                color = HubMuted, fontSize = 12.sp,
            )
            Box(
                Modifier.fillMaxWidth().height(4.dp)
                    .background(HubLine, RoundedCornerShape(5.dp)),
            ) {
                Box(Modifier.fillMaxWidth(.33f).height(4.dp)
                    .background(HubWarm, RoundedCornerShape(5.dp)))
            }
        }
    }
}

@Composable
private fun HubFilter(name: String, selected: Boolean) {
    val shape = RoundedCornerShape(25.dp)
    Surface(
        color = if (selected) Phase1OptionAReviewTokens.ActiveChip else HubPanel,
        shape = shape,
        border = BorderStroke(1.dp, if (selected) HubGold else HubLine),
    ) {
        Text(
            name,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) HubIvory else HubMuted,
        )
    }
}

@Composable
private fun HubReminderItem(
    symbol: String, title: String, subtitle: String, state: String,
) {
    Surface(
        color = HubPanel, shape = HubRadius,
        border = BorderStroke(1.dp, HubLine),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Surface(color = HubRaised, shape = RoundedCornerShape(12.dp)) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Text(symbol, fontSize = 19.sp, color = HubGold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = HubIvory, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, lineHeight = 18.sp)
                Text(subtitle, color = HubMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Text(state, color = if (state == "Today") HubGold else HubMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun HubFootnote(heading: String, description: String, action: String) {
    Surface(
        color = HubPanel, shape = HubRadius,
        border = BorderStroke(1.dp, HubLine),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(heading, color = HubIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(description, color = HubMuted, fontSize = 12.sp, lineHeight = 18.sp)
            Text("$action   ›", color = HubGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
