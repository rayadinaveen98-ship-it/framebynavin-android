package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.CreatorViewModel
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FrameByNavinV031App(vm: CreatorViewModel = viewModel()) {
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FrameByNavinV03App(vm)

        // V0.3.1 compatibility hit-target over the existing header + button.
        // The next UI refactor will merge this picker directly into the shared Quick Add component.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 10.dp, top = 2.dp)
                .size(66.dp)
                .clickable { showQuickAdd = true }
        )
    }

    if (showQuickAdd) {
        FunctionalQuickAddDialog(
            onDismiss = { showQuickAdd = false },
            onAdd = { title, platform, contentType, dueLabel, dueAtMillis, reminderEnabled ->
                vm.addTask(
                    title = title,
                    platform = platform,
                    contentType = contentType,
                    dueLabel = dueLabel,
                    reminderEnabled = reminderEnabled,
                    reminderAtMillis = dueAtMillis,
                    priority = TaskPriority.IMPORTANT,
                    notes = "",
                )
                showQuickAdd = false
            }
        )
    }
}

@Composable
private fun FunctionalQuickAddDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, Long, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var platform by rememberSaveable { mutableStateOf("Instagram") }
    var contentType by rememberSaveable { mutableStateOf("Reel") }
    var dueAtMillis by rememberSaveable { mutableLongStateOf(defaultQuickAddDueMillis()) }
    var reminderEnabled by rememberSaveable { mutableStateOf(true) }

    fun openDateTimePicker() {
        val initial = Calendar.getInstance().apply { timeInMillis = dueAtMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        dueAtMillis = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    },
                    initial.get(Calendar.HOUR_OF_DAY),
                    initial.get(Calendar.MINUTE),
                    false,
                ).show()
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    val dueLabel = quickAddDueLabel(dueAtMillis)
    val dueIsFuture = dueAtMillis > System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        titleContentColor = ProjectorIvory,
        textContentColor = MutedText,
        title = { Text("Quick Add", fontWeight = FontWeight.Black) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / content title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(14.dp))
                Text("PLATFORM", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Instagram", "YouTube", "X").forEach { option ->
                        FilterChip(
                            selected = platform == option,
                            onClick = { platform = option },
                            label = { Text(option) },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("FORMAT", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Reel", "Long-form", "Post").forEach { option ->
                        FilterChip(
                            selected = contentType == option,
                            onClick = { contentType = option },
                            label = { Text(option) },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("DUE / PUBLISH TIME", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { openDateTimePicker() },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (dueIsFuture) CinemaLine else RecRed),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                dueLabel,
                                color = ProjectorIvory,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                SimpleDateFormat("EEE, d MMM yyyy · h:mm a", Locale.getDefault()).format(Date(dueAtMillis)),
                                color = MutedText,
                                fontSize = 9.5.sp,
                            )
                        }
                        Text("CHANGE", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!dueIsFuture) {
                    Spacer(Modifier.height(6.dp))
                    Text("Choose a future publish time.", color = RecRed, fontSize = 10.sp)
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("REMIND AT PUBLISH TIME", color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        Text("Uses the native reminder engine.", color = MutedText, fontSize = 9.5.sp)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ProjectorIvory, checkedTrackColor = RecRed),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(title, platform, contentType, dueLabel, dueAtMillis, reminderEnabled)
                },
                enabled = title.isNotBlank() && dueIsFuture,
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
            ) { Text("ADD") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedText) }
        },
    )
}

private fun defaultQuickAddDueMillis(): Long = Calendar.getInstance().apply {
    add(Calendar.HOUR_OF_DAY, 1)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun quickAddDueLabel(millis: Long): String {
    val now = Calendar.getInstance()
    val selected = Calendar.getInstance().apply { timeInMillis = millis }
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

    return when {
        sameDay(now, selected) -> "Today · $time"
        sameDay(tomorrow, selected) -> "Tomorrow · $time"
        else -> SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
    }
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
