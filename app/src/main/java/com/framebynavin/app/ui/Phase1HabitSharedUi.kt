package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.Phase1HabitAlertKind
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitDayStatus
import com.framebynavin.app.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * SHARED Phase 1 screen visuals. Both emulator preview and future live screen
 * call these exact Compose implementations: no separate screenshot-only styling.
 * UI is pure; all persistence, alert, restore and owner actions are callbacks.
 * Not connected to released navigation until owner accepts native screenshots.
 */
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

@Composable
private fun ReviewPanel(featured: Boolean = false,
    content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (featured) BacklotSurfaceSelected else BacklotSurface,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, if (featured) BacklotAccent else BacklotBorder),
    ) {
        Column(Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun AccentLabel(label: String) {
    Text(label, color = BacklotAccent, fontWeight = FontWeight.Bold,
        fontSize = 10.sp, letterSpacing = 1.sp)
}

@Composable
private fun Hint(label: String) {
    Text(label, color = BacklotSecondaryText, fontSize = 12.sp, lineHeight = 18.sp)
}

@Composable
private fun Choice(label: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(20.dp),
        color = if (active) BacklotSurfaceSelected else BacklotSurfaceRaised,
        border = BorderStroke(1.dp, if (active) BacklotAccent else BacklotBorder),
    ) {
        Text(label, color = if (active) BacklotPrimaryText else BacklotSecondaryText,
            fontSize = 11.sp, modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp))
    }
}

@Composable
internal fun CreateHabitReview(
    title: String, onTitle: (String) -> Unit,
    date: LocalDate, onDate: (LocalDate) -> Unit,
    mode: Phase1HabitAlertKind?, onMode: (Phase1HabitAlertKind?) -> Unit,
    time: LocalTime, onTime: (LocalTime) -> Unit,
    error: Boolean, onStart: () -> Unit,
    previewOnly: Boolean = false,
    liveErrorMessage: String? = null,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AccentLabel("NEW 21-DAY CHALLENGE")
            Text("Make it stick.", color = BacklotPrimaryText,
                fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
            Hint("One small action. Every day. For 21 days.")
            ReviewPanel {
                Hint("YOUR ACTIVITY")
                OutlinedTextField(
                    value = title, onValueChange = onTitle, singleLine = true,
                    label = { Text("What will you repeat?") },
                    modifier = Modifier.fillMaxWidth().testTag("habit-activity"),
                )
            }
            ReviewPanel {
                Hint("START DATE")
                TextButton(
                    onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            onDate(LocalDate.of(year, month + 1, day))
                        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                    },
                    modifier = Modifier.testTag("habit-date"),
                ) {
                    Text(date.format(dateFormat) + "   ▾", color = BacklotPrimaryText,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            ReviewPanel {
                Hint("DAILY ALERT")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Choice("Alarm", mode == Phase1HabitAlertKind.ALARM) {
                        onMode(Phase1HabitAlertKind.ALARM)
                    }
                    Choice("Voice", mode == Phase1HabitAlertKind.VOICE) {
                        onMode(Phase1HabitAlertKind.VOICE)
                    }
                    Choice("Quiet", mode == Phase1HabitAlertKind.QUIET) {
                        onMode(Phase1HabitAlertKind.QUIET)
                    }
                    Choice("Off", mode == null) { onMode(null) }
                }
                if (mode != null) {
                    TextButton(onClick = {
                        TimePickerDialog(context, { _, hour, minute ->
                            onTime(LocalTime.of(hour, minute))
                        }, time.hour, time.minute, false).show()
                    }, modifier = Modifier.testTag("habit-time")) {
                        Text(time.format(timeFormat), color = BacklotPrimaryText,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else Hint("No alert. You can still mark each day done.")
            }
            ReviewPanel {
                Text("What happens next?", color = BacklotPrimaryText,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Hint("One activity for 21 dates. Only your explicit Done action counts.")
            }
            if (error || liveErrorMessage != null) Text(
                if (error) "Enter one activity to continue." else liveErrorMessage.orEmpty(),
                color = BacklotError, modifier = Modifier.testTag("habit-error"))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Button(onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("habit-start"),
                shape = RoundedCornerShape(13.dp)) {
                Text("Start my 21 days", fontWeight = FontWeight.Bold)
            }
            Hint(if (previewOnly) "Prototype only · Nothing scheduled or saved"
                 else "Your progress will be saved locally. Alerts are not yet active.")
        }
    }
}

@Composable
internal fun DashboardHabitReview(
    habit: Phase1HabitChallenge, now: Instant,
    onDone: () -> Unit, onUndo: () -> Unit, onPause: () -> Unit,
    previewOnly: Boolean = false,
) {
    val today = LocalDate.ofInstant(now, ZoneId.of(habit.zoneId))
    val dayNumber = habit.dayNumber(now)
    val doneToday = today in habit.confirmations
    val count = habit.confirmations.size
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AccentLabel("21-DAY CHALLENGE")
        Text(habit.activity, color = BacklotPrimaryText,
            fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold)
        Hint("One activity, once a day. Progress is always confirmed by you.")
        ReviewPanel(featured = true) {
            Hint("YOU'RE ON")
            Text(
                if (dayNumber != null) "Day $dayNumber / 21"
                else if (today.isBefore(habit.startDate)) "Not started"
                else "Finished",
                color = BacklotPrimaryText, fontWeight = FontWeight.Bold,
                fontSize = 27.sp, modifier = Modifier.testTag("habit-day-counter"),
            )
            Hint("$count days completed · Current streak " + habit.currentStreak(now))
            LinearProgressIndicator(
                progress = { count / 21f },
                modifier = Modifier.fillMaxWidth().height(7.dp),
                color = BacklotAccent, trackColor = BacklotBorder,
            )
        }
        ReviewPanel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your 21 days", color = BacklotPrimaryText, fontWeight = FontWeight.Bold)
                Text("$count OF 21 DONE", color = BacklotSecondaryText, fontSize = 11.sp,
                    modifier = Modifier.testTag("habit-done-count"))
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                (1..21).chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        week.forEach { index ->
                            val day = habit.startDate.plusDays(index.toLong() - 1L)
                            val state = habit.status(day, now)
                            val isDone = state == Phase1HabitDayStatus.DONE
                            val current = day == today
                            Box(
                                Modifier.weight(1f).aspectRatio(1f)
                                    .background(
                                        if (isDone) BacklotAccent else BacklotSurfaceRaised,
                                        RoundedCornerShape(11.dp))
                                    .testTag("habit-day-$index"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (isDone) "✓" else "$index",
                                    color = if (isDone) Color.White else if (current)
                                        BacklotAccent else BacklotSecondaryText,
                                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Hint("Red = Done · Red number = Today · Grey = not confirmed")
        }
        ReviewPanel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Today's activity", color = BacklotPrimaryText,
                    fontWeight = FontWeight.Bold)
                Text(if (doneToday) "DONE" else if (habit.paused) "PAUSED" else "TODAY",
                    color = BacklotAccent, fontSize = 11.sp)
            }
            Hint(habit.activity + (habit.alertTime?.let {
                " · " + it.format(timeFormat) + " preference"
            } ?: " · No alert"))
        }
        if (dayNumber != null) {
            if (doneToday) {
                Button(onClick = onUndo,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                        .testTag("habit-undo")) {
                    Text("Undo today's completion")
                }
            } else {
                Button(onClick = onDone, enabled = !habit.paused,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                        .testTag("habit-done")) {
                    Text(if (habit.paused) "Resume to mark today" else "Mark today done")
                }
            }
        }
        OutlinedButton(onClick = onPause, modifier = Modifier.fillMaxWidth()
            .testTag("habit-pause")) {
            Text(if (habit.paused) "Resume challenge" else "Pause challenge")
        }
        Text(if (previewOnly) "Review only · No data or alerts saved"
            else "Progress saved on this device · Android alert permissions apply",
            color = BacklotSecondaryText, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}
