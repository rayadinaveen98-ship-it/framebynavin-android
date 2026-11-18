package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.BacklotUX10Tokens

/**
 * Visual-only controls. The screen supplies original Personal delivery token
 * actions. Never call the scheduler or store from this Composable.
 */
@Composable
internal fun UX10DReminderQuickResponseActions(
    busy: Boolean,
    snoozeMinutes: Int,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
) {
    val stacked = UX10DPersonalPresentationPolicy.stackReminderResponses(
        LocalConfiguration.current.screenWidthDp,
        LocalDensity.current.fontScale,
    )
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UX10DResponseButton(
                label = "DONE",
                enabled = !busy,
                onClick = onDone,
                tag = "v288-row-done",
                modifier = Modifier.fillMaxWidth(),
            )
            UX10DResponseButton(
                label = "SNOOZE ${snoozeMinutes}m",
                enabled = !busy,
                onClick = onSnooze,
                tag = "v288-row-snooze",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UX10DResponseButton(
                label = "DONE",
                enabled = !busy,
                onClick = onDone,
                tag = "v288-row-done",
                modifier = Modifier.weight(1f),
            )
            UX10DResponseButton(
                label = "SNOOZE ${snoozeMinutes}m",
                enabled = !busy,
                onClick = onSnooze,
                tag = "v288-row-snooze",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun UX10DResponseButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = BacklotUX10Tokens.rowTarget)
            .testTag(tag),
    ) {
        Text(
            label,
            fontSize = BacklotUX10Tokens.supporting,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** Visual day-picker only. Original Challenge state and validation stay in host. */
@Composable
internal fun UX10DChallengeDayNavigation(
    dayIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val stacked = UX10DPersonalPresentationPolicy.stackChallengeDayNavigation(
        LocalConfiguration.current.screenWidthDp,
        LocalDensity.current.fontScale,
    )
    if (stacked) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UX10DChallengeNavButton(
                label = "PREVIOUS", previous = true,
                onClick = onPrevious, enabled = dayIndex > 0,
                tag = "v297-prev-day",
                modifier = Modifier.fillMaxWidth(),
            )
            UX10DChallengeNavButton(
                label = "NEXT", previous = false,
                onClick = onNext, enabled = dayIndex < 20,
                tag = "v297-next-day",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UX10DChallengeNavButton(
                label = "PREVIOUS", previous = true,
                onClick = onPrevious, enabled = dayIndex > 0,
                tag = "v297-prev-day",
                modifier = Modifier.weight(1f),
            )
            UX10DChallengeNavButton(
                label = "NEXT", previous = false,
                onClick = onNext, enabled = dayIndex < 20,
                tag = "v297-next-day",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun UX10DChallengeNavButton(
    label: String,
    previous: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    tag: String,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = BacklotUX10Tokens.rowTarget).testTag(tag),
    ) {
        if (previous) Icon(Icons.Outlined.ChevronLeft, null)
        Text(label, fontSize = BacklotUX10Tokens.supporting, maxLines = 2)
        if (!previous) Icon(Icons.Outlined.ChevronRight, null)
    }
}

/** Original group-edit and bulk Pause/Resume destinations, with adaptive layout. */
private data class UX10DGroupAction(
    val title: String,
    val tag: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)

@Composable
internal fun UX10DGroupActionControls(
    canEdit: Boolean,
    canDelete: Boolean,
    canPause: Boolean,
    canResume: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
) {
    val actions = listOf(
        UX10DGroupAction("EDIT GROUP", "v298-edit-group", canEdit, onEdit),
        UX10DGroupAction("DELETE GROUP", "v298-delete-open", canDelete, onDelete),
        UX10DGroupAction("PAUSE ALL", "fc13-pause-group", canPause, onPause),
        UX10DGroupAction("RESUME ALL", "fc13-resume-group", canResume, onResume),
    )
    val stacked = UX10DPersonalPresentationPolicy.stackReminderResponses(
        LocalConfiguration.current.screenWidthDp,
        LocalDensity.current.fontScale,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (stacked) {
            actions.forEach { action ->
                UX10DGroupButton(action, Modifier.fillMaxWidth())
            }
        } else {
            actions.chunked(2).forEach { pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    pair.forEach { action ->
                        UX10DGroupButton(action, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun UX10DGroupButton(action: UX10DGroupAction, modifier: Modifier) {
    OutlinedButton(
        onClick = action.onClick,
        enabled = action.enabled,
        modifier = modifier.heightIn(min = BacklotUX10Tokens.rowTarget)
            .testTag(action.tag),
    ) {
        Text(
            action.title,
            fontSize = BacklotUX10Tokens.supporting,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
