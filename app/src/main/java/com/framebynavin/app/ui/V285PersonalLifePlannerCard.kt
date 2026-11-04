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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V285PersonalLifePlan
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * An intentional schedule board, not a notification wall. 21 dated priorities
 * stay read-only; optional recurring reminders are installed only by consent.
 */
@Composable
internal fun V285PersonalLifePlannerCard(
    reminders: List<PersonalReminder>,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val plan = V285PersonalLifePlan
    val today = remember { LocalDate.now() }
    var selectedDay by remember { mutableStateOf(
        when {
            today.isBefore(plan.firstDay) -> plan.firstDay
            today.isAfter(plan.lastDay) -> plan.lastDay
            else -> today
        }
    ) }
    var selectedCategories by remember { mutableStateOf(V285PersonalLifePlan.Category.entries.toSet()) }
    var includingDailyFocus by remember { mutableStateOf(false) }
    var showingConfirmation by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val existingIds = remember(reminders) { reminders.mapTo(HashSet()) { it.id } }
    val routineTemplates = remember(selectedCategories) {
        plan.templates(plan.firstDay, java.time.ZoneId.systemDefault(), selectedCategories)
    }
    val availableFocus = plan.focusTemplates(System.currentTimeMillis(), java.time.ZoneId.systemDefault())
    val desired = routineTemplates + if (includingDailyFocus) availableFocus else emptyList()
    val missing = desired.count { it.id !in existingIds }
    val installed = plan.routines.count { it.id in existingIds }
    val day = plan.focusOn(selectedDay) ?: plan.days.first()
    val shortDate = remember { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()) }

    Surface(
        color = CinemaSurface,
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .36f)),
        shape = RoundedCornerShape(19.dp),
        modifier = Modifier.fillMaxWidth().testTag("v285-personal-life-planner"),
    ) {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("YOUR LIFE · 21-DAY RESET", color = MutedGold,
                fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text("One day. One clear priority.", color = ProjectorIvory,
                fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("5–25 NOV 2026 · 21 days · schedules can change without losing your history",
                color = MutedText, fontSize = 10.sp)
            Text("DAY " + (selectedDay.toEpochDay() - plan.firstDay.toEpochDay() + 1) + " / 21",
                color = MutedGold, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                modifier = Modifier.testTag("v285-selected-day"))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                plan.days.forEach { item ->
                    FilterChip(
                        selected = item.date == selectedDay,
                        onClick = { selectedDay = item.date },
                        label = { Text(item.date.format(shortDate), fontSize = 10.sp) },
                        modifier = Modifier.testTag("v285-day-" + item.date.toString()),
                    )
                }
            }
            Surface(
                color = MutedGold.copy(alpha = .09f),
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (day.publishing != null) {
                        Text(day.publishing, color = MutedGold,
                            fontWeight = FontWeight.Black, fontSize = 10.sp)
                    }
                    Text(day.primary, color = ProjectorIvory, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("v285-day-primary"))
                    Text(day.secondary, color = MutedText, fontSize = 12.sp)
                }
            }
            Text("DAILY RHYTHM", color = MutedGold, fontSize = 10.sp,
                fontWeight = FontWeight.Bold)
            Text(
                "7 AM wake + workout · 8 AM skincare · 8:15 breakfast · 9 AM script " +
                "· 11:15 edit · 1:30 lunch · 2 PM nap · 3:15 production · 5 PM walk " +
                "· 7:30 dinner · 9:30 skincare · 10 PM novel",
                color = ProjectorIvory, fontSize = 11.sp,
                modifier = Modifier.testTag("v285-daily-rhythm"),
            )
            Text("Your V284 weekday meals, workouts and personal alarms remain separate. " +
                "This board does not create Creator projects or post YouTube videos.",
                color = MutedText, fontSize = 10.sp)
            if (plan.hasReadingConflict(reminders)) {
                Text(
                    "SCHEDULE CONFLICT: Your existing 10:15 PM sleep voice reminder " +
                        "interrupts 10 PM reading. Edit that existing reminder to 10:40 PM " +
                        "if you want uninterrupted reading. We will not change it silently.",
                    color = RecRed, fontSize = 11.sp,
                    modifier = Modifier.testTag("v285-reading-conflict"),
                )
            }
            Text("CHOOSE REMINDER GROUPS", color = MutedGold,
                fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                V285PersonalLifePlan.Category.entries.forEach { category ->
                    FilterChip(
                        selected = category in selectedCategories,
                        enabled = enabled && !saving,
                        onClick = {
                            selectedCategories = if (category in selectedCategories) {
                                selectedCategories - category
                            } else {
                                selectedCategories + category
                            }
                        },
                        label = { Text(category.title, fontSize = 11.sp) },
                        modifier = Modifier.testTag("v285-category-" + category.name),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("One daily filming priority at 8:50 AM", color = ProjectorIvory,
                        fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Optional · future dates only · one quiet notification/day",
                        color = MutedText, fontSize = 10.sp)
                }
                Switch(
                    checked = includingDailyFocus,
                    enabled = enabled && !saving && availableFocus.isNotEmpty(),
                    onCheckedChange = { includingDailyFocus = it },
                    modifier = Modifier.testTag("v285-daily-focus-toggle"),
                )
            }
            Text("$installed of " + plan.routines.size +
                " routine reminders saved · selected notifications: " + desired.size,
                color = MutedText, fontSize = 11.sp,
                modifier = Modifier.testTag("v285-install-count"))
            if (missing > 0) {
                Button(
                    onClick = { showingConfirmation = true },
                    enabled = enabled && !saving &&
                        (selectedCategories.isNotEmpty() || includingDailyFocus),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    modifier = Modifier.fillMaxWidth().testTag("v285-install-routines"),
                ) { Text("REVIEW & ADD $missing REMINDERS") }
            } else {
                Text(
                    "Your selected routines are saved. Edit, pause or delete individual " +
                        "reminders in the agenda below.",
                    color = ProjectorIvory, fontSize = 11.sp,
                )
            }
            feedback?.let {
                Text(it, color = MutedGold, fontSize = 11.sp,
                    modifier = Modifier.testTag("v285-install-feedback"))
            }
        }
    }

    if (showingConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!saving) showingConfirmation = false },
            title = { Text("Add selected life routines?") },
            text = {
                Column(
                    Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text("Add $missing missing Personal Reminders. All selected routines recur " +
                        "and optional daily focus prompts happen only once on their planned dates. " +
                        "No current reminders or completion history will be replaced.")
                    V285PersonalLifePlan.Category.entries.filter { it in selectedCategories }.forEach { category ->
                        Text(category.title + ": " + plan.routines
                            .filter { it.category == category }
                            .joinToString { it.time.toString() + " " + it.title },
                            fontSize = 12.sp)
                    }
                    Text("All new reminders are gentle notifications with manual snooze; " +
                        "no surprise full-screen alerts or repeated voice loops. " +
                        "You can customize each later.")
                    Text("The 21-day YouTube goals are shown as a planning board. " +
                        "An optional 8:50 AM prompt is a private reminder, not a Creator " +
                        "project, publication event or automatic YouTube action.")
                    if (plan.hasReadingConflict(reminders)) {
                        Text("10:15 PM wind-down conflicts with 10 PM reading. " +
                            "Edit the existing wind-down time separately.")
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = enabled && !saving,
                    onClick = {
                        saving = true
                        scope.launch {
                            try {
                                val result = plan.install(context, selectedCategories, includingDailyFocus)
                                feedback = if (result.schedulingAccepted) {
                                    "${result.added} reminder records saved. Android delivery was " +
                                        "rechecked; verify notification and alarm permissions."
                                } else {
                                    "${result.added} reminder records saved, but Android delivery " +
                                        "needs attention. Check your notification permissions."
                                }
                                showingConfirmation = false
                            } catch (_: Exception) {
                                feedback = "Could not complete the save. Previously saved " +
                                    "reminders were not replaced. Check the saved count."
                                showingConfirmation = false
                            } finally {
                                saving = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("v285-confirm-install"),
                ) {
                    if (saving) CircularProgressIndicator()
                    else Text("ADD SELECTED")
                }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { showingConfirmation = false }) {
                    Text("CANCEL")
                }
            },
            modifier = Modifier.testTag("v285-install-dialog"),
        )
    }
}
