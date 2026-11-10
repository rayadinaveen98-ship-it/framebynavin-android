package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitCompletionMode
import com.framebynavin.app.personal.Phase1HabitStore
import com.framebynavin.app.personal.V298ReminderGroup
import com.framebynavin.app.personal.V298ReminderGroupStore
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Small, independent Compose dialog bodies. Keeping these out of the
 * challenge's stateful host avoids deeply nested Compose/FIR data-flow trees.
 */
@Composable
internal fun FC13ChallengeLinksDialog(
    saving: Boolean,
    ready: Boolean,
    errorMessage: String?,
    spaces: List<V289SpaceEntity>,
    groups: List<V298ReminderGroup>,
    spaceId: String?,
    groupId: String?,
    onDismiss: () -> Unit,
    onSpaceChosen: (String?) -> Unit,
    onGroupChosen: (String?) -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Link 21-day challenge") },
        text = {
            Column(
                Modifier.heightIn(max = 410.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Optional links to your existing Spaces and Reminder Groups. " +
                        "Nothing is created, scheduled, or marked Done.",
                    color = BacklotSecondaryText,
                )
                if (!ready) {
                    CircularProgressIndicator(color = BacklotAccent)
                } else if (errorMessage != null) {
                    Text(errorMessage, color = BacklotError)
                } else {
                    Text("SPACE", fontWeight = FontWeight.Bold, color = BacklotPrimaryText)
                    FilterChip(
                        selected = spaceId == null,
                        onClick = { onSpaceChosen(null) },
                        label = { Text("None") },
                    )
                    spaces.forEach { space ->
                        FilterChip(
                            selected = spaceId == space.id,
                            onClick = { onSpaceChosen(space.id) },
                            label = { Text(space.title) },
                        )
                    }
                    if (spaces.isEmpty()) {
                        Text("No active Spaces saved yet.", color = BacklotSecondaryText)
                    }
                    Text("REMINDER GROUP", fontWeight = FontWeight.Bold, color = BacklotPrimaryText)
                    FilterChip(
                        selected = groupId == null,
                        onClick = { onGroupChosen(null) },
                        label = { Text("None") },
                    )
                    groups.forEach { group ->
                        FilterChip(
                            selected = groupId == group.id,
                            onClick = { onGroupChosen(group.id) },
                            label = { Text(group.name) },
                        )
                    }
                    if (groups.isEmpty()) {
                        Text("No Reminder Groups saved yet.", color = BacklotSecondaryText)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && ready && errorMessage == null,
                onClick = onSave,
                modifier = Modifier.testTag("fc13-save-challenge-links"),
            ) { Text("SAVE LINKS") }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("CANCEL") }
        },
    )
}

/**
 * Recheck the current linked records before committing owner-selected IDs.
 * No Space/Group is created and no reminder is acknowledged.
 */
internal suspend fun FC13SaveChallengeLinks(
    context: Context,
    store: Phase1HabitStore,
    groupStore: V298ReminderGroupStore,
    challengeId: String,
    chosenSpace: String?,
    chosenGroup: String?,
): List<Phase1HabitChallenge> {
    val currentGroups = groupStore.load().map { it.id }.toSet()
    val dbFile = context.getDatabasePath(V289SpacesDatabase.FILE_NAME)
    val currentSpaces = if (!dbFile.exists()) emptySet() else {
        require(dbFile.isFile)
        val db = V289SpacesDatabase.open(context)
        try {
            db.spaces().loadSpaces()
                .filter { it.archivedAtMillis == null }
                .map { it.id }.toSet()
        } finally {
            db.close()
        }
    }
    return store.updateLinks(
        challengeId, chosenSpace, chosenGroup,
        currentSpaces, currentGroups,
    )
}

@Composable
internal fun FC13ChallengeDayDialog(
    challenge: Phase1HabitChallenge,
    day: LocalDate,
    now: Instant,
    note: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onBackfill: () -> Unit,
    onSaveNote: () -> Unit,
) {
    val today = LocalDate.ofInstant(now, ZoneId.of(challenge.zoneId))
    val receipt = challenge.confirmations[day]
    val canBackfill = challenge.completionMode == Phase1HabitCompletionMode.FLEXIBLE &&
        day.isBefore(today) && receipt == null && !challenge.paused

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text("Day ${ChronoUnit.DAYS.between(challenge.startDate, day) + 1} · ${day}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (receipt != null)
                        "Confirmed at ${receipt} (actual recorded time)."
                    else "Not confirmed. Adding a note does not mark this day Done.",
                    color = BacklotPrimaryText,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChanged,
                    label = { Text("Day note · optional") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("fc13-challenge-day-note"),
                )
                if (canBackfill) {
                    Text(
                        "Flexible catch-up records today's confirmation time for " +
                            "this past date. No earlier timestamp is fabricated.",
                        color = BacklotSecondaryText,
                    )
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (canBackfill) {
                    TextButton(
                        enabled = !saving,
                        onClick = onBackfill,
                        modifier = Modifier.testTag("fc13-confirm-past-day"),
                    ) { Text("CONFIRM PAST DAY") }
                }
                TextButton(
                    enabled = !saving,
                    onClick = onSaveNote,
                    modifier = Modifier.testTag("fc13-save-day-note"),
                ) { Text("SAVE NOTE") }
            }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text("CLOSE") }
        },
    )
}
