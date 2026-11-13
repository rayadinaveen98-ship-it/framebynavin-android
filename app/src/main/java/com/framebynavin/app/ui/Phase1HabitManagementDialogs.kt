package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.framebynavin.app.personal.Phase1HabitAlertKind
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitCompletionMode
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotError
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Edits a live single-activity challenge, not the legacy V297 format. */
@Composable
internal fun Phase1HabitEditDialog(
    challenge: Phase1HabitChallenge,
    busy: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (
        activity: String,
        startDate: LocalDate,
        time: LocalTime?,
        kind: Phase1HabitAlertKind?,
        completionMode: Phase1HabitCompletionMode,
    ) -> Unit,
) {
    val context = LocalContext.current
    var activity by remember(challenge) { mutableStateOf(challenge.activity) }
    var startDate by remember(challenge) { mutableStateOf(challenge.startDate) }
    var kind by remember(challenge) { mutableStateOf(challenge.alertKind) }
    var time by remember(challenge) {
        mutableStateOf(challenge.alertTime ?: LocalTime.of(7, 30))
    }
    var completion by remember(challenge) {
        mutableStateOf(challenge.completionMode)
    }
    val canMoveStart = challenge.confirmations.isEmpty() && challenge.dayNotes.isEmpty()
    val dateLabel = DateTimeFormatter.ofPattern("d MMM yyyy")
    val timeLabel = DateTimeFormatter.ofPattern("h:mm a")
    AlertDialog(
        modifier = Modifier.testTag("habit-manage-edit-dialog"),
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Edit 21-day challenge") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 530.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = activity,
                    onValueChange = { activity = it.take(200) },
                    label = { Text("One activity") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("habit-manage-edit-activity"),
                )
                Text("Original Done receipts and daily notes stay unchanged.")
                TextButton(
                    onClick = {
                        DatePickerDialog(context,
                            { _, year, month, day ->
                                startDate = LocalDate.of(year, month + 1, day)
                            }, startDate.year, startDate.monthValue - 1,
                            startDate.dayOfMonth).show()
                    },
                    enabled = !busy && canMoveStart,
                ) { Text("START: " + startDate.format(dateLabel), color = BacklotAccent) }
                if (!canMoveStart) {
                    Text("Start date is locked because this challenge has saved " +
                        "Done days or notes. They will not be shifted or erased.")
                }
                Text("Alert type")
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    val modes: List<Pair<String, Phase1HabitAlertKind?>> = listOf(
                        "Off" to null, "Quiet" to Phase1HabitAlertKind.QUIET,
                        "Alarm" to Phase1HabitAlertKind.ALARM,
                        "Voice" to Phase1HabitAlertKind.VOICE,
                    )
                    modes.forEach { (label, selectedKind) ->
                        FilterChip(
                            selected = kind == selectedKind,
                            onClick = { kind = selectedKind },
                            enabled = !busy,
                            label = { Text(label) },
                        )
                    }
                }
                if (kind != null) {
                    TextButton(onClick = {
                        TimePickerDialog(context, { _, hour, minute ->
                            time = LocalTime.of(hour, minute)
                        }, time.hour, time.minute, false).show()
                    }, enabled = !busy) {
                        Text("ALERT TIME: " + time.format(timeLabel), color = BacklotAccent)
                    }
                    Text("Android permissions and background restrictions still apply.")
                }
                Text("Completion rule")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Phase1HabitCompletionMode.STRICT,
                        Phase1HabitCompletionMode.FLEXIBLE,
                    ).forEach { rule ->
                        FilterChip(
                            selected = completion == rule,
                            onClick = { completion = rule },
                            enabled = !busy,
                            label = { Text(if (rule == Phase1HabitCompletionMode.STRICT)
                                "Strict" else "Flexible") },
                        )
                    }
                }
                Text("Switching from Flexible to Strict may be blocked if older " +
                    "Done receipts would become invalid. Nothing is deleted automatically.")
                if (!saveError.isNullOrBlank()) {
                    Text(saveError, color = BacklotError)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && activity.trim().isNotEmpty(),
                onClick = {
                    onSave(activity.trim(), startDate,
                        if (kind == null) null else time, kind, completion)
                },
                modifier = Modifier.testTag("habit-manage-edit-save"),
            ) { Text(if (busy) "SAVING" else "SAVE CHANGES", color = BacklotAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("CANCEL") }
        },
    )
}

@Composable
internal fun Phase1HabitConfirmManagementDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag("habit-manage-confirm-dialog"),
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                enabled = !busy, onClick = onConfirm,
                modifier = Modifier.testTag("habit-manage-confirm"),
            ) { Text(if (busy) "SAVING" else confirmLabel,
                color = if (destructive) BacklotError else BacklotAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("CANCEL") }
        },
    )
}
