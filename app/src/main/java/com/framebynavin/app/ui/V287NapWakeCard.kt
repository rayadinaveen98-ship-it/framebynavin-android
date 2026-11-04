package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V287NapWakePolicy
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/** Nap start remains the V285 2 PM notification. Nap-end wakeup is a separate opt-in ALARM. */
@Composable
internal fun V287NapWakeCard(
    reminders: List<PersonalReminder>,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val nap = V287NapWakePolicy
    val existing = reminders.firstOrNull { it.id == nap.ID }
    var selected by remember {
        mutableStateOf(
            nap.Duration.entries.firstOrNull { nap.endTime(it) == existing?.time }
                ?: nap.Duration.ONE_HOUR
        )
    }
    var confirming by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val selectedEnd = nap.endTime(selected)
    val timeLabel = remember { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()) }
    val alreadySelected = existing != null && existing.time == selectedEnd && !existing.paused

    Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, CinemaLine),
        modifier = Modifier.fillMaxWidth().testTag("v287-nap-end-card"),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("AFTERNOON RESET", color = MutedGold,
                fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text("Wake gently from your nap", color = ProjectorIvory,
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Nap begins at 2:00 PM · choose when your wake-up alarm should sound",
                color = MutedText, fontSize = 11.sp)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                nap.Duration.entries.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { selected = option },
                        enabled = enabled && !busy,
                        label = { Text(option.display, fontSize = 11.sp) },
                        modifier = Modifier.testTag("v287-nap-length-" + option.minutes),
                    )
                }
            }
            Text("2:00 PM – " + selectedEnd.format(timeLabel),
                color = ProjectorIvory, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.testTag("v287-nap-window"))
            if (nap.overlapsEditing(selected)) {
                Text(
                    "This nap extends past your 3:15 PM production block. " +
                        "Shift editing later on days you choose a longer nap.",
                    color = MutedGold, fontSize = 11.sp,
                    modifier = Modifier.testTag("v287-nap-overlap-warning"),
                )
            }
            Text("One daily ALARM reminder only · 5-minute manual snooze · " +
                "no automatic retries. Full-screen wake-up depends on Android settings.",
                color = MutedText, fontSize = 10.sp)
            if (alreadySelected) {
                Text("Selected wake-up time already saved. You can edit, pause or delete " +
                    "its reminder below.", color = MutedGold, fontSize = 11.sp,
                    modifier = Modifier.testTag("v287-nap-already-saved"))
            } else {
                Button(
                    onClick = { confirming = true },
                    enabled = enabled && !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    modifier = Modifier.fillMaxWidth().testTag("v287-nap-save"),
                ) {
                    Text(if (existing == null) "REVIEW NAP WAKE-UP"
                    else "REVIEW NAP ALARM CHANGE")
                }
            }
            feedback?.let {
                Text(it, color = MutedGold, fontSize = 11.sp,
                    modifier = Modifier.testTag("v287-nap-feedback"))
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirming = false },
            title = { Text("Schedule nap wake-up?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Daily nap begins at 2:00 PM. Your chosen wake-up is " +
                        selectedEnd.format(timeLabel) + " (" + selected.minutes + " minutes).")
                    Text("This is an ALARM reminder. It can use audio and full-screen " +
                        "presentation only when Android allows it. Notification access and " +
                        "battery restrictions may affect delivery.")
                    if (existing != null) {
                        Text("The existing nap wake-up reminder will be updated rather " +
                            "than duplicated. Your separate completion history is retained.")
                    }
                    if (nap.overlapsEditing(selected)) {
                        Text("Longer rest overlaps your 3:15 PM production slot. " +
                            "Adjust that work block if you proceed.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = enabled && !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                val result = nap.save(context, selected)
                                feedback = if (result.androidRechecked) {
                                    "Nap wake-up saved for " + selectedEnd.format(timeLabel) +
                                        ". Check Android alarm permissions and verify on your phone."
                                } else {
                                    "Nap wake-up saved. Scheduling may need Android permission " +
                                        "or a manual retry. Your existing reminders were kept."
                                }
                                confirming = false
                            } catch (_: Exception) {
                                feedback = "Unable to finish nap wake-up setup. Check " +
                                    "the saved reminder below before retrying."
                                confirming = false
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("v287-nap-confirm"),
                ) { Text(if (busy) "SAVING" else "SAVE WAKE-UP") }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { confirming = false },
                    modifier = Modifier.testTag("v287-nap-cancel"),
                ) { Text("CANCEL") }
            },
            modifier = Modifier.testTag("v287-nap-confirm-dialog"),
        )
    }
}
