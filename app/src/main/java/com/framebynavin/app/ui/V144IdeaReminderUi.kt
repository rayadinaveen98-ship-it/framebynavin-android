package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.IdeaReminderCadence
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private enum class V144IdeaReminderChoice(val label: String) {
    OFF("Off"),
    THREE_HOURS("3h"),
    SIX_HOURS("6h"),
    TOMORROW("Tomorrow"),
    DAILY("Daily"),
    CUSTOM("Custom"),
}

@Composable
internal fun V144IdeaReminderPicker(
    reminderAtMillis: Long,
    cadence: IdeaReminderCadence,
    enabled: Boolean,
    onChange: (Long, IdeaReminderCadence) -> Unit,
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val selected = v144ReminderChoice(reminderAtMillis, cadence, now)
    val activeLabel = when {
        !enabled -> "Unavailable for converted or archived ideas"
        reminderAtMillis <= now -> "Off"
        cadence == IdeaReminderCadence.DAILY -> "Daily · ${v144FormatReminder(reminderAtMillis)}"
        else -> "One nudge · ${v144FormatReminder(reminderAtMillis)}"
    }

    fun apply(choice: V144IdeaReminderChoice) {
        val current = System.currentTimeMillis()
        when (choice) {
            V144IdeaReminderChoice.OFF -> onChange(0L, IdeaReminderCadence.ONCE)
            V144IdeaReminderChoice.THREE_HOURS -> onChange(current + 3L * 60L * 60_000L, IdeaReminderCadence.ONCE)
            V144IdeaReminderChoice.SIX_HOURS -> onChange(current + 6L * 60L * 60_000L, IdeaReminderCadence.ONCE)
            V144IdeaReminderChoice.TOMORROW -> onChange(current + 24L * 60L * 60_000L, IdeaReminderCadence.ONCE)
            V144IdeaReminderChoice.DAILY -> {
                val first = reminderAtMillis.takeIf { cadence == IdeaReminderCadence.DAILY && it > current }
                    ?: current + 24L * 60L * 60_000L
                onChange(first, IdeaReminderCadence.DAILY)
            }
            V144IdeaReminderChoice.CUSTOM -> {
                val initial = Calendar.getInstance().apply {
                    timeInMillis = reminderAtMillis.takeIf { it > current } ?: (current + 24L * 60L * 60_000L)
                }
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val picked = Calendar.getInstance().apply {
                            timeInMillis = initial.timeInMillis
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                        }
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                picked.set(Calendar.HOUR_OF_DAY, hour)
                                picked.set(Calendar.MINUTE, minute)
                                picked.set(Calendar.SECOND, 0)
                                picked.set(Calendar.MILLISECOND, 0)
                                onChange(
                                    picked.timeInMillis.coerceAtLeast(System.currentTimeMillis() + 60_000L),
                                    IdeaReminderCadence.ONCE,
                                )
                            },
                            initial.get(Calendar.HOUR_OF_DAY),
                            initial.get(Calendar.MINUTE),
                            false,
                        ).show()
                    },
                    initial.get(Calendar.YEAR),
                    initial.get(Calendar.MONTH),
                    initial.get(Calendar.DAY_OF_MONTH),
                ).apply {
                    datePicker.minDate = System.currentTimeMillis() - 1_000L
                }.show()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Text(
                "IDEA NUDGE",
                color = MutedGold,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .8.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text("Remind me about this", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(activeLabel, color = MutedText, fontSize = 8.5.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                V144IdeaReminderChoice.entries.forEach { choice ->
                    FilterChip(
                        selected = selected == choice,
                        onClick = { if (enabled) apply(choice) },
                        enabled = enabled,
                        label = { Text(choice.label, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                if (cadence == IdeaReminderCadence.DAILY && reminderAtMillis > now)
                    "Daily repeats until you stop it, archive it, or turn it into a project."
                else
                    "3h, 6h, Tomorrow and Custom send one nudge only.",
                color = MutedText,
                fontSize = 7.8.sp,
                lineHeight = 11.sp,
            )
        }
    }
}

private fun v144ReminderChoice(
    reminderAtMillis: Long,
    cadence: IdeaReminderCadence,
    now: Long,
): V144IdeaReminderChoice {
    if (reminderAtMillis <= now) return V144IdeaReminderChoice.OFF
    if (cadence == IdeaReminderCadence.DAILY) return V144IdeaReminderChoice.DAILY
    val remaining = reminderAtMillis - now
    val tolerance = 5L * 60_000L
    return when {
        abs(remaining - 3L * 60L * 60_000L) <= tolerance -> V144IdeaReminderChoice.THREE_HOURS
        abs(remaining - 6L * 60L * 60_000L) <= tolerance -> V144IdeaReminderChoice.SIX_HOURS
        abs(remaining - 24L * 60L * 60_000L) <= tolerance -> V144IdeaReminderChoice.TOMORROW
        else -> V144IdeaReminderChoice.CUSTOM
    }
}

private fun v144FormatReminder(atMillis: Long): String =
    SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(atMillis)
