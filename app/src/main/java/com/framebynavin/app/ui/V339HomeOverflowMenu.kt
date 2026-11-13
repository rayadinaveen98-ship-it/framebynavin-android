package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Compact, top-right Home rail. A single vertical-dots trigger discloses
 * Notification Center / Spark / Gallery. Clicking it again, pressing Back,
 * or tapping outside closes the Material popup.
 *
 * These callbacks open the ORIGINAL surfaces and editor. The menu never reads
 * notifications, acknowledges reminders, or saves images itself.
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
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
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
                    contentDescription = "Home options: Notification Center, Spark and Gallery"
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
                Icon(
                    imageVector = if (expanded) Icons.Outlined.Close
                        else Icons.Outlined.MoreVert,
                    contentDescription = null,
                    tint = ProjectorIvory,
                    modifier = Modifier.size(21.dp),
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
                                V339HomeOverflowAction.SPARK -> if (sparkEnabled) "Spark" else "Spark · off"
                                V339HomeOverflowAction.GALLERY -> "Gallery"
                            },
                            color = if (enabled) ProjectorIvory else MutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
        }
    }
}
