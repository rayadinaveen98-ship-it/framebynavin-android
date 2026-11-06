package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.V298ReminderGroup
import com.framebynavin.app.personal.V310LinkTargets
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.ui.theme.*

/**
 * V310: one optional, collapsed Review affordance.
 * It changes an in-memory selection only; no Room/DataStore/alarm writes.
 */
@Composable
internal fun V310ReviewLinks(
    availableSpaces: List<V289SpaceEntity>,
    availableGroups: List<V298ReminderGroup>,
    spacesReady: Boolean,
    groupsReady: Boolean,
    spacesError: String?,
    groupsError: String?,
    value: V310LinkTargets,
    onChange: (V310LinkTargets) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedSpace = availableSpaces.firstOrNull { it.id == value.spaceId }
    val selectedGroup = availableGroups.firstOrNull { it.id == value.groupId }
    val linkedCount = listOfNotNull(selectedSpace, selectedGroup).size
    Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
        modifier = Modifier.fillMaxWidth().testTag("v310-review-links"),
    ) {
        Column(Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FolderOpen, null,
                    tint = MutedGold, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("ORGANIZE THIS REMINDER", color = ProjectorIvory,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (linkedCount == 0) "Optional · Add to a Space or Group"
                        else listOfNotNull(selectedSpace?.title, selectedGroup?.name)
                            .joinToString(" · "),
                        color = MutedText, fontSize = 11.sp,
                    )
                }
                TextButton(onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("v310-expand-links")) {
                    Text(if (expanded) "CLOSE" else "CHOOSE",
                        color = MutedGold, fontSize = 11.sp)
                    Icon(Icons.Outlined.ChevronRight, null,
                        tint = MutedGold, modifier = Modifier.size(17.dp))
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("MY SPACES", color = MutedGold, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold)
                    when {
                        spacesError != null -> Text(spacesError,
                            color = RecRed, fontSize = 11.sp,
                            modifier = Modifier.testTag("v310-space-load-error"))
                        !spacesReady -> Text("Loading your Spaces…",
                            color = MutedText, fontSize = 11.sp)
                        availableSpaces.isEmpty() -> Text(
                            "No active Spaces yet. You can link this reminder later.",
                            color = MutedText, fontSize = 11.sp)
                        else -> Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                .testTag("v310-space-choices"),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            availableSpaces.forEach { space ->
                                FilterChip(
                                    selected = value.spaceId == space.id,
                                    onClick = {
                                        onChange(value.copy(
                                            spaceId = if (value.spaceId == space.id) null else space.id
                                        ))
                                    },
                                    label = { Text(space.title, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("v310-space-${space.id}"),
                                )
                            }
                        }
                    }
                    Text("REMINDER GROUPS", color = MutedGold, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold)
                    when {
                        groupsError != null -> Text(groupsError,
                            color = RecRed, fontSize = 11.sp,
                            modifier = Modifier.testTag("v310-group-load-error"))
                        !groupsReady -> Text("Loading your Groups…",
                            color = MutedText, fontSize = 11.sp)
                        availableGroups.isEmpty() -> Text(
                            "No Groups yet. You can add one from Personal Settings.",
                            color = MutedText, fontSize = 11.sp)
                        else -> Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                .testTag("v310-group-choices"),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            availableGroups.forEach { group ->
                                FilterChip(
                                    selected = value.groupId == group.id,
                                    onClick = {
                                        onChange(value.copy(
                                            groupId = if (value.groupId == group.id) null else group.id
                                        ))
                                    },
                                    label = { Text(group.name, fontSize = 11.sp) },
                                    modifier = Modifier.testTag("v310-group-${group.id}"),
                                )
                            }
                        }
                    }
                    Text(
                        "Links are added only after the reminder is saved. " +
                            "Spaces and Groups do not change its schedule.",
                        color = MutedText, fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
