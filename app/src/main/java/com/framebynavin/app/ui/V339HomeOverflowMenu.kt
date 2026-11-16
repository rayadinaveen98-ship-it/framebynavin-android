package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory

/**
 * Selected-companion avatar opens Backlot's compact Home menu. Original
 * Notification Center / companion / Gallery actions remain intact alongside
 * approved profile, workspace and reminder routes.
 *
 * These callbacks open the original surfaces and editor. The menu never
 * acknowledges reminders, edits user data or saves images itself.
 */
@Composable
internal fun V339HomeOverflowMenu(
    reminderActionNeeded: Boolean,
    sparkEnabled: Boolean,
    sparkSuggestionAvailable: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenSpark: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenSpaces: () -> Unit = {},
    onOpenWeeklyFocus: () -> Unit = {},
    onOpenCreatorReminders: () -> Unit = {},
    onOpenPersonalReminders: () -> Unit = {},
    activityPreview: V306ActivityItem? = null,
    showWorkspaceActions: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var remindersExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedGuide = V144GuidePrefs.currentGuide
    fun openExtra(action: () -> Unit) { expanded = false; action() }
    val collapsedIndicator = V339HomeOverflowPolicy.collapsedIndicator(
        reminderActionNeeded, sparkSuggestionAvailable, sparkEnabled,
    )

    fun select(action: V339HomeOverflowAction) {
        if (!V339HomeOverflowPolicy.enabled(action, sparkEnabled)) return
        expanded = false
        when (action) {
            V339HomeOverflowAction.NOTIFICATION_CENTER -> onOpenNotifications()
            V339HomeOverflowAction.SPARK -> onOpenSpark()
            V339HomeOverflowAction.GALLERY -> onOpenGallery()
        }
    }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = !expanded },
            modifier = Modifier.size(48.dp)
                .testTag("v339-home-overflow-trigger")
                .semantics {
                    contentDescription = "Open Backlot menu with " + selectedGuide.displayName
                    stateDescription = when {
                        expanded -> "Expanded"
                        collapsedIndicator == V339HomeOverflowAction.NOTIFICATION_CENTER ->
                            "Collapsed; a reminder needs action"
                        collapsedIndicator == V339HomeOverflowAction.SPARK ->
                            "Collapsed; a Spark suggestion is available"
                        else -> "Collapsed"
                    }
                },
            color = CinemaSurfaceRaised,
            shape = CircleShape,
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(v144GuideDrawable(selectedGuide, CinePulseState.WAVE)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp),
                )
                if (!expanded && collapsedIndicator != null) {
                    Box(
                        Modifier.align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(7.dp)
                            .background(
                                if (collapsedIndicator == V339HomeOverflowAction.SPARK)
                                    MutedGold else BacklotAccent,
                                CircleShape,
                            )
                            .testTag(
                                if (collapsedIndicator == V339HomeOverflowAction.SPARK)
                                    "v339-home-spark-indicator"
                                else "v339-home-action-indicator",
                            ),
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .defaultMinSize(minWidth = 188.dp)
                .testTag("v339-home-overflow-list"),
        ) {
            V339HomeOverflowPolicy.actions.forEach { action ->
                val enabled = V339HomeOverflowPolicy.enabled(action, sparkEnabled)
                val indicator = V339HomeOverflowPolicy.hasIndicator(
                    action, reminderActionNeeded, sparkSuggestionAvailable, sparkEnabled,
                )
                DropdownMenuItem(
                    onClick = { select(action) },
                    enabled = enabled,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        .testTag("v339-home-menu-" + action.name),
                    leadingIcon = {
                        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                when (action) {
                                    V339HomeOverflowAction.NOTIFICATION_CENTER ->
                                        if (reminderActionNeeded)
                                            Icons.Outlined.NotificationsActive
                                        else Icons.Outlined.NotificationsNone
                                    V339HomeOverflowAction.SPARK -> Icons.Outlined.AutoAwesome
                                    V339HomeOverflowAction.GALLERY -> Icons.Outlined.PhotoLibrary
                                },
                                contentDescription = null,
                                tint = if (indicator) {
                                    if (action == V339HomeOverflowAction.SPARK)
                                        MutedGold else BacklotAccent
                                } else if (enabled) ProjectorIvory else MutedText,
                                modifier = Modifier.size(20.dp),
                            )
                            if (indicator) Box(
                                Modifier.align(Alignment.TopEnd)
                                    .size(6.dp)
                                    .background(
                                        if (action == V339HomeOverflowAction.SPARK)
                                            MutedGold else BacklotAccent,
                                        CircleShape,
                                    ),
                            )
                        }
                    },
                    text = {
                        Text(
                            when (action) {
                                V339HomeOverflowAction.NOTIFICATION_CENTER -> "Notification Center"
                                V339HomeOverflowAction.SPARK -> if (sparkEnabled)
                                    selectedGuide.displayName else selectedGuide.displayName + " · off"
                                V339HomeOverflowAction.GALLERY -> "Gallery"
                            },
                            color = if (enabled) ProjectorIvory else MutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
            if (showWorkspaceActions) {
                HorizontalDivider(color = CinemaLine.copy(alpha = .55f))
                DropdownMenuItem(
                    onClick = { openExtra(onOpenProfile) },
                    text = { Text("My Profile & Account", color = ProjectorIvory) },
                    leadingIcon = { Icon(Icons.Outlined.AccountCircle, null, tint = MutedGold) },
                    modifier = Modifier.testTag("v402-menu-profile"),
                )
                activityPreview?.let { item ->
                    Text(
                        "NEEDS ATTENTION · " + item.title.take(46),
                        color = MutedText,
                        fontSize = 10.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                            .testTag("v402-menu-activity-preview"),
                    )
                    TextButton(
                        onClick = { openExtra(onOpenNotifications) },
                        modifier = Modifier.testTag("v402-menu-activity-see-more"),
                    ) { Text("See more in Activity Center", color = MutedGold) }
                }
                DropdownMenuItem(
                    onClick = { openExtra(onOpenSpaces) },
                    text = { Text("My Spaces", color = ProjectorIvory) },
                    leadingIcon = { Icon(Icons.Outlined.FolderOpen, null, tint = MutedGold) },
                    modifier = Modifier.testTag("v402-menu-spaces"),
                )
                DropdownMenuItem(
                    onClick = { openExtra(onOpenWeeklyFocus) },
                    text = { Text("Weekly Focus", color = ProjectorIvory) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarMonth, null, tint = MutedGold) },
                    modifier = Modifier.testTag("v402-menu-weekly-focus"),
                )
                HorizontalDivider(color = CinemaLine.copy(alpha = .55f))
                DropdownMenuItem(
                    onClick = { remindersExpanded = !remindersExpanded },
                    text = { Text("Reminders", color = ProjectorIvory) },
                    leadingIcon = { Icon(Icons.Outlined.Alarm, null, tint = MutedGold) },
                    trailingIcon = {
                        Icon(
                            if (remindersExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null, tint = MutedText,
                        )
                    },
                    modifier = Modifier.testTag("v402-menu-reminders-expand"),
                )
                if (remindersExpanded) {
                    DropdownMenuItem(
                        onClick = { openExtra(onOpenPersonalReminders) },
                        text = { Text("Personal Reminders", color = ProjectorIvory) },
                        modifier = Modifier.testTag("v402-menu-personal-reminders"),
                    )
                    DropdownMenuItem(
                        onClick = { openExtra(onOpenCreatorReminders) },
                        text = { Text("Creator Reminders", color = ProjectorIvory) },
                        modifier = Modifier.testTag("v402-menu-creator-reminders"),
                    )
                }
                HorizontalDivider(color = CinemaLine.copy(alpha = .55f))
                DropdownMenuItem(
                    onClick = { openExtra(onOpenSettings) },
                    text = { Text("Settings", color = ProjectorIvory) },
                    leadingIcon = { Icon(Icons.Outlined.Settings, null, tint = MutedGold) },
                    modifier = Modifier.testTag("v402-menu-settings"),
                )
            }
        }
    }
}
