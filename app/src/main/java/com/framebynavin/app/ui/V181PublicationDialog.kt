package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.*
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
internal fun V181PublicationDialog(task: CreatorTask, onDismiss: () -> Unit, onSave: (Long, String) -> Unit) {
    val context = LocalContext.current
    var at by remember(task.id) { mutableLongStateOf(task.publishedAtMillis.takeIf { it > 0 } ?: System.currentTimeMillis()) }
    var url by remember(task.id) { mutableStateOf(task.publishedUrl) }
    var error by remember(task.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text("Publication details", color = ProjectorIvory, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Record when your content actually went live. Publishing is separate from finishing promotion or review.", color = MutedText, fontSize = 14.sp, lineHeight = 20.sp)
                Text(task.title, color = ProjectorIvory, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = {
                    val date = Calendar.getInstance().apply { timeInMillis = at }
                    DatePickerDialog(context, { _, year, month, day ->
                        date.set(year, month, day)
                        TimePickerDialog(context, { _, hour, minute ->
                            date.set(Calendar.HOUR_OF_DAY, hour)
                            date.set(Calendar.MINUTE, minute)
                            date.set(Calendar.SECOND, 0)
                            at = date.timeInMillis
                        }, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), false).show()
                    }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(at)))
                }
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Live link (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = RecRed, fontSize = 13.sp) }
                if (task.publishedAtMillis > 0L) {
                    TextButton(onClick = { onSave(0L, "") }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("REMOVE INCORRECT PUBLICATION", color = RecRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val clean = url.trim()
                error = when {
                    at <= 0L || at > System.currentTimeMillis() + 60_000L -> "Choose a valid past publication time."
                    clean.isNotEmpty() && !clean.startsWith("https://", ignoreCase = true) -> "Use an HTTPS link."
                    else -> null
                }
                if (error == null) onSave(at, clean)
            }) { Text("SAVE PUBLICATION") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } },
    )
}
