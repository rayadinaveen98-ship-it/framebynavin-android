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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.FramePalette
import com.framebynavin.app.ui.theme.FrameTheme

/**
 * DEBUG-ONLY native Android design evidence for the Personal & Creator Hubs.
 *
 * Owner correction: Director's Cut is the primary Backlot language. Always read
 * the actual shipping theme (FrameTheme.DIRECTORS_CUT / BACKLOT_LIGHT) instead
 * of using the former warm-gold Option A mockup colors.
 *
 * The palette contains a secondary accent, but this screen deliberately does
 * NOT use it. REC red is the only brand accent here. Main text is foreground,
 * secondary text is muted. No gold gradients, amber states or custom typography.
 *
 * Fixtures are inert; user data, reminders, alarms, cloud, permissions and
 * app navigation are NEVER read or mutated by this debug-only preview.
 */
@Composable
internal fun Phase1HubVisualReview(scene: String) {
    val personal = scene.startsWith("personal-hub")
    val light = scene.endsWith("-light")
    val palette = if (light) FrameTheme.BACKLOT_LIGHT.palette
                  else FrameTheme.DIRECTORS_CUT.palette
    Column(
        Modifier.fillMaxSize().background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        DcToolbar(palette)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "BACKLOT  /  REMINDERS",
                color = palette.primary,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Your reminders",
                color = palette.foreground,
                fontSize = 29.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.7).sp,
            )
            Text(
                "A clear view of what matters today.",
                color = palette.muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
        DcWorkspaceTabs(personal, palette)
        DcTodayPanel(personal, palette)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DcFilter("Today", true, palette)
            DcFilter("Upcoming", false, palette)
            DcFilter("Paused", false, palette)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (personal) "PERSONAL REMINDERS" else "PROJECT REMINDERS",
                color = palette.muted,
                fontSize = 10.sp,
                letterSpacing = 1.25.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text("SEE ALL", color = palette.primary, fontSize = 11.sp,
                letterSpacing = .5.sp, fontWeight = FontWeight.SemiBold)
        }
        if (personal) {
            DcReminderRow(
                title = "Water the plants",
                detail = "Every morning  ·  7:30 AM",
                status = "Upcoming",
                urgent = false,
                palette = palette,
            )
            DcReminderRow(
                title = "Read a chapter",
                detail = "Daily  ·  8:00 PM",
                status = "Voice",
                urgent = false,
                palette = palette,
            )
            DcRelatedPanel(
                title = "My Spaces",
                description = "Routines, notes, photo journals and your 21-day challenge.",
                action = "OPEN SPACES",
                palette = palette,
            )
        } else {
            DcReminderRow(
                title = "Review rough cut",
                detail = "Film study  ·  3:00 PM",
                status = "Due today",
                urgent = true,
                palette = palette,
            )
            DcReminderRow(
                title = "Publish project update",
                detail = "Weekly series  ·  6:00 PM",
                status = "Upcoming",
                urgent = false,
                palette = palette,
            )
            DcRelatedPanel(
                title = "Your projects",
                description = "Keep each creator reminder linked to its project and next step.",
                action = "OPEN PROJECTS",
                palette = palette,
            )
        }
        HorizontalDivider(color = palette.line)
        Text(
            "DESIGN PREVIEW  ·  EXAMPLE DATA ONLY",
            color = palette.muted,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
        Text(
            "This preview does not create, edit, send, complete or schedule reminders.",
            color = palette.muted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun DcToolbar(p: FramePalette) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Back preview",
                tint = p.foreground,
                modifier = Modifier.size(23.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DcToolbarIcon(
                p,
                icon = { Icon(Icons.Outlined.NotificationsActive,
                    contentDescription = "Activity Center preview",
                    tint = p.muted, modifier = Modifier.size(19.dp)) },
            )
            DcToolbarIcon(
                p,
                icon = { Icon(Icons.Outlined.Add,
                    contentDescription = "New reminder preview",
                    tint = p.foreground, modifier = Modifier.size(20.dp)) },
            )
        }
    }
}

@Composable
private fun DcToolbarIcon(
    p: FramePalette,
    icon: @Composable () -> Unit,
) {
    Surface(
        color = p.surface,
        shape = CircleShape,
        border = BorderStroke(1.dp, p.line),
    ) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { icon() }
    }
}

@Composable
private fun DcWorkspaceTabs(personal: Boolean, p: FramePalette) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DcWorkspaceTab("Personal", personal, p, Modifier.weight(1f))
        DcWorkspaceTab("Creator", !personal, p, Modifier.weight(1f))
    }
}

@Composable
private fun DcWorkspaceTab(
    text: String,
    selected: Boolean,
    p: FramePalette,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(46.dp),
        color = if (selected) p.primaryDeep else p.surface,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (selected) p.primary else p.line),
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Box(
                    Modifier.size(6.dp).background(p.primary, CircleShape)
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text,
                color = if (selected) p.foreground else p.muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DcTodayPanel(personal: Boolean, p: FramePalette) {
    Surface(
        color = p.surfaceRaised,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, p.line),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(p.primary, CircleShape))
                Spacer(Modifier.size(7.dp))
                Text(
                    if (personal) "PERSONAL  /  TODAY" else "CREATOR  /  TODAY",
                    color = p.muted,
                    fontSize = 10.sp,
                    letterSpacing = 1.35.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text("07 NOV", color = p.muted, fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("02", color = p.foreground, fontSize = 42.sp,
                    fontWeight = FontWeight.Black, lineHeight = 43.sp)
                Spacer(Modifier.size(13.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (personal) "reminders for today" else "project reminders",
                        color = p.foreground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (personal) "One routine at a time" else "Keep the next step moving",
                        color = p.muted,
                        fontSize = 12.sp,
                    )
                }
            }
            HorizontalDivider(color = p.line)
            Text(
                if (personal) "Next reminder  ·  7:30 AM"
                else "Next check-in  ·  3:00 PM",
                color = p.muted, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun DcFilter(name: String, selected: Boolean, p: FramePalette) {
    Surface(
        color = if (selected) p.primaryDeep else p.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (selected) p.primary else p.line),
    ) {
        Text(
            name,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            color = if (selected) p.foreground else p.muted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun DcReminderRow(
    title: String,
    detail: String,
    status: String,
    urgent: Boolean,
    palette: FramePalette,
) {
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, palette.line),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = palette.primaryDeep,
                shape = RoundedCornerShape(10.dp),
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(12.dp).background(
                            palette.primary.copy(alpha = if (urgent) 1f else .72f),
                            CircleShape,
                        )
                    )
                }
            }
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    title,
                    color = palette.foreground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp,
                )
                Text(detail, color = palette.muted, fontSize = 11.sp,
                    lineHeight = 16.sp)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(status,
                    color = if (urgent) palette.primary else palette.muted,
                    fontSize = 10.sp,
                    fontWeight = if (urgent) FontWeight.Bold else FontWeight.Normal)
                Icon(
                    Icons.Outlined.ChevronRight, contentDescription = null,
                    tint = palette.muted, modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun DcRelatedPanel(
    title: String,
    description: String,
    action: String,
    palette: FramePalette,
) {
    Surface(
        color = palette.surfaceRaised,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, palette.line),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(title, color = palette.foreground, fontSize = 15.sp,
                    fontWeight = FontWeight.Bold)
                Text(description, color = palette.muted, fontSize = 12.sp,
                    lineHeight = 18.sp)
                Text(action, color = palette.primary, fontSize = 11.sp,
                    letterSpacing = .4.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(8.dp))
            Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                tint = palette.primary, modifier = Modifier.size(21.dp))
        }
    }
}
