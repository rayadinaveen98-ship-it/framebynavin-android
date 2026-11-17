package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceProgressSummary
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryAccent
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import com.framebynavin.app.ui.theme.BacklotSurface

/**
 * UX07C presentation rules. These never infer progress from notification
 * delivery, note-writing or photo viewing. Room and Personal remain owners.
 */
internal object UX07CSpaceLabels {
    fun type(type: String): String =
        V289SpaceType.entries.firstOrNull { it.name == type }?.defaultTitle
            ?: "Personal Space"

    fun entry(kind: String): String = when (kind) {
        V289SpaceEntryKind.CHECK_IN.name -> "MANUAL CHECK-IN"
        V289SpaceEntryKind.METRIC.name -> "MANUAL MEASUREMENT"
        V289SpaceEntryKind.NOTE.name -> "PERSONAL NOTE"
        V289SpaceEntryKind.MILESTONE.name -> "RECORDED MILESTONE"
        else -> "RECORDED ACTIVITY"
    }
}

/** More useful and less box-heavy than the previous title/status-only row. */
@Composable
internal fun UX07CSpaceCard(
    space: V289SpaceEntity,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val archived = space.archivedAtMillis != null
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth().heightIn(min = 76.dp)
            .testTag("v289-space-" + space.id),
        shape = RoundedCornerShape(17.dp),
        color = BacklotSurface,
        border = BorderStroke(1.dp, BacklotBorder.copy(alpha = 0.45f)),
    ) {
        Row(
            Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                V289SpaceIcon(space.type),
                contentDescription = null,
                tint = if (archived) BacklotSecondaryText else BacklotSecondaryAccent,
                modifier = Modifier.size(25.dp),
            )
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    space.title,
                    color = BacklotPrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    UX07CSpaceLabels.type(space.type) +
                        if (archived) " · Archived" else " · Active",
                    color = BacklotSecondaryAccent,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (space.goalText.isNotBlank()) {
                    Text(
                        space.goalText,
                        color = BacklotSecondaryText,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("ux07c-space-goal"),
                    )
                }
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Open "+space.title,
                tint = BacklotSecondaryText,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/**
 * Summary of actual persisted manual logs. Linked-reminder counts are kept
 * separate and are not claimed as Space check-ins or personal achievements.
 */
@Composable
internal fun UX07CSpaceProgressSummary(
    summary: V289SpaceProgressSummary,
    linkedCount: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("v289-progress-summary"),
        color = BacklotSurface,
        shape = RoundedCornerShape(17.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "YOUR ACTIVITY",
                color = BacklotSecondaryAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UX07CMetric(
                    summary.totalRecordedEntries.toString(),
                    "Recorded logs",
                    Modifier.weight(1f),
                )
                UX07CMetric(
                    summary.daysWithActivityLastSeven.toString(),
                    "Active days · last 7",
                    Modifier.weight(1f),
                )
                UX07CMetric(
                    linkedCount.toString(),
                    "Linked reminders",
                    Modifier.weight(1f),
                )
            }
            Text(
                "Notes and photos do not count as activity. Linked reminder Done history is separate.",
                color = BacklotSecondaryText,
                fontSize = 11.sp,
                modifier = Modifier.testTag("ux07c-progress-source-note"),
            )
        }
    }
}

@Composable
private fun UX07CMetric(value: String, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            value,
            color = BacklotPrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            caption,
            color = BacklotSecondaryText,
            fontSize = 10.sp,
            lineHeight = 13.sp,
        )
    }
}
