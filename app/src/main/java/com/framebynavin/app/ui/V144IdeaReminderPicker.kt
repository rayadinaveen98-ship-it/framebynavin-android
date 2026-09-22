package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

@Composable
internal fun V144IdeaReminderPicker(
    reminderAtMillis: Long,
    cadence: IdeaReminderCadence,
    enabled: Boolean,
    onChange: (Long, IdeaReminderCadence) -> Unit,
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val selectedLabel = remember(reminderAtMillis, cadence, now / 60_000L) {
        when {
            reminderAtMillis <= 0L -> "Off"
            cadence == IdeaReminderCadence.DAILY -> {
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(reminderAtMillis)
                "Daily · $time"
            }
            else -> SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(reminderAtMillis)
        }
    }

    fun customPicker() {
        val seed = Calendar.getInstance().apply {
            timeInMillis = reminderAtMillis.takeIf { it > now } ?: (now + 3 * 60 * 60_000L)
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val afterDate = Calendar.getInstance().apply {
                    timeInMillis = seed.timeInMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        afterDate.set(Calendar.HOUR_OF_DAY, hour)
                        afterDate.set(Calendar.MINUTE, minute)
                        afterDate.set(Calendar.SECOND, 0)
                        afterDate.set(Calendar.MILLISECOND, 0)
                        val chosen = afterDate.timeInMillis
                        if (chosen > System.currentTimeMillis()) onChange(chosen, IdeaReminderCadence.ONCE)
                    },
                    seed.get(Calendar.HOUR_OF_DAY),
                    seed.get(Calendar.MINUTE),
                    false,
                ).show()
            },
            seed.get(Calendar.YEAR),
            seed.get(Calendar.MONTH),
            seed.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    Column(Modifier.fillMaxWidth()) {
        Text("REMIND ME ABOUT THIS", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            if (enabled) "A light nudge keeps a promising idea from disappearing." else "Reminders stop automatically for converted or archived ideas.",
            color = MutedText,
            fontSize = 8.8.sp,
        )
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            FilterChip(
                selected = reminderAtMillis <= 0L,
                enabled = enabled,
                onClick = { onChange(0L, IdeaReminderCadence.ONCE) },
                label = { Text("Off", fontSize = 9.5.sp) },
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = false,
                enabled = enabled,
                onClick = { onChange(System.currentTimeMillis() + 3 * 60 * 60_000L, IdeaReminderCadence.ONCE) },
                label = { Text("3h", fontSize = 9.5.sp) },
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = false,
                enabled = enabled,
                onClick = { onChange(System.currentTimeMillis() + 6 * 60 * 60_000L, IdeaReminderCadence.ONCE) },
                label = { Text("6h", fontSize = 9.5.sp) },
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = false,
                enabled = enabled,
                onClick = {
                    val tomorrow = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onChange(tomorrow.timeInMillis, IdeaReminderCadence.ONCE)
                },
                label = { Text("Tomorrow", fontSize = 9.5.sp) },
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = cadence == IdeaReminderCadence.DAILY && reminderAtMillis > 0L,
                enabled = enabled,
                onClick = {
                    val nextDaily = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onChange(nextDaily.timeInMillis, IdeaReminderCadence.DAILY)
                },
                label = { Text("Daily", fontSize = 9.5.sp) },
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = reminderAtMillis > 0L && cadence == IdeaReminderCadence.ONCE,
                enabled = enabled,
                onClick = { customPicker() },
                label = { Text("Custom", fontSize = 9.5.sp) },
            )
        }
        if (reminderAtMillis > 0L) {
            Spacer(Modifier.height(7.dp))
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = CinemaSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
            ) {
                Text(
                    selectedLabel,
                    color = ProjectorIvory,
                    fontSize = 9.2.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
        }
    }
}
