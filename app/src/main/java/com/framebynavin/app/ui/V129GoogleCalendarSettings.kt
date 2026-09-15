package com.framebynavin.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.*
import java.util.TimeZone

data class V129CalendarOption(val id: Long, val name: String, val account: String)

internal object CreatorCalendarSync {
    private const val PREFS = "framebynavin_google_calendar"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_CALENDAR_ID = "calendar_id"
    private const val EVENT_PREFIX = "event_"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun isEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
    fun setEnabled(context: Context, enabled: Boolean) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    fun selectedCalendarId(context: Context): Long = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_CALENDAR_ID, -1L)
    fun selectCalendar(context: Context, id: Long) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_CALENDAR_ID, id).apply()

    fun googleCalendars(context: Context): List<V129CalendarOption> {
        if (!hasPermission(context)) return emptyList()
        val result = mutableListOf<V129CalendarOption>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
        )
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.ACCOUNT_TYPE}=? AND ${CalendarContract.Calendars.VISIBLE}=1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=?",
            arrayOf("com.google", CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountI = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                result += V129CalendarOption(cursor.getLong(idI), cursor.getString(nameI).orEmpty(), cursor.getString(accountI).orEmpty())
            }
        }
        return result
    }

    fun syncIfEnabled(context: Context, tasks: List<CreatorTask>): Int {
        if (!isEnabled(context) || !hasPermission(context)) return 0
        val calendarId = selectedCalendarId(context)
        if (calendarId <= 0L) return 0
        return sync(context, tasks, calendarId)
    }

    fun sync(context: Context, tasks: List<CreatorTask>, calendarId: Long): Int {
        if (!hasPermission(context) || calendarId <= 0L) return 0
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var count = 0
        tasks.filter { it.dueAtMillis > 0L && it.status != TaskStatus.SKIPPED }.forEach { task ->
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "Creator project · ${task.title}")
                put(CalendarContract.Events.DESCRIPTION, listOf(task.platform, task.contentType, task.notes).filter { it.isNotBlank() }.joinToString(" · "))
                put(CalendarContract.Events.DTSTART, task.dueAtMillis)
                put(CalendarContract.Events.DTEND, task.dueAtMillis + 30L * 60_000L)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 0)
            }
            val key = EVENT_PREFIX + task.id
            val existing = prefs.getLong(key, -1L)
            if (existing > 0L) {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existing)
                if (context.contentResolver.update(uri, values, null, null) > 0) count++
            } else {
                val created = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                val id = created?.let(ContentUris::parseId) ?: -1L
                if (id > 0L) {
                    prefs.edit().putLong(key, id).apply()
                    count++
                }
            }
        }
        return count
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V129GoogleCalendarSettings(tasks: List<CreatorTask>) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var enabled by remember { mutableStateOf(CreatorCalendarSync.isEnabled(context)) }
    var selectedId by remember { mutableLongStateOf(CreatorCalendarSync.selectedCalendarId(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    val granted = remember(revision) { CreatorCalendarSync.hasPermission(context) }
    val calendars = remember(revision, granted) { CreatorCalendarSync.googleCalendars(context) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        revision++
        message = if (result.values.all { it }) "Calendar access ready." else "Calendar access was not granted."
    }

    LaunchedEffect(calendars, selectedId) {
        if (granted && calendars.isNotEmpty() && calendars.none { it.id == selectedId }) {
            selectedId = calendars.first().id
            CreatorCalendarSync.selectCalendar(context, selectedId)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("GOOGLE CALENDAR", color = MutedGold, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(5.dp))
        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(38.dp), RoundedCornerShape(12.dp), RecRed.copy(alpha=.12f)) {
                        Box(contentAlignment=Alignment.Center) { Icon(Icons.Outlined.CalendarMonth, null, tint=RecRed, modifier=Modifier.size(20.dp)) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Project deadlines in Google Calendar", color=ProjectorIvory, fontSize=11.5.sp, fontWeight=FontWeight.Bold)
                        Text("Your in-app reminders stay separate.", color=MutedText, fontSize=8.5.sp)
                    }
                    if (granted && calendars.isNotEmpty()) {
                        Switch(
                            checked=enabled,
                            onCheckedChange={ value -> enabled=value; CreatorCalendarSync.setEnabled(context,value); if (value) message="Sync is on." },
                            colors=SwitchDefaults.colors(checkedTrackColor=RecRed),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                if (!granted) {
                    Button(
                        onClick={ permissions.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) },
                        modifier=Modifier.fillMaxWidth(),
                        colors=ButtonDefaults.buttonColors(containerColor=RecRed),
                        shape=RoundedCornerShape(13.dp),
                    ) { Text("CONNECT GOOGLE CALENDAR", fontSize=9.sp, fontWeight=FontWeight.Black) }
                } else if (calendars.isEmpty()) {
                    Text("No writable Google Calendar is synced on this phone. Add or enable one in Android Calendar, then return here.", color=MutedText, fontSize=8.8.sp, lineHeight=13.sp)
                } else {
                    Text("Calendar", color=ProjectorIvory, fontSize=9.5.sp, fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        calendars.forEach { option ->
                            FilterChip(
                                selected=selectedId==option.id,
                                onClick={ selectedId=option.id; CreatorCalendarSync.selectCalendar(context, option.id); message=null },
                                label={ Text(option.name.ifBlank { option.account }, fontSize=8.sp, maxLines=1) },
                            )
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    OutlinedButton(
                        onClick={
                            CreatorCalendarSync.selectCalendar(context, selectedId)
                            val count=CreatorCalendarSync.sync(context, tasks, selectedId)
                            message = if (count > 0) "$count project deadline${if (count==1) "" else "s"} synced." else "Nothing new to sync."
                        },
                        modifier=Modifier.fillMaxWidth(),
                        shape=RoundedCornerShape(13.dp),
                        border=BorderStroke(1.dp, CinemaLine),
                    ) { Text("SYNC NOW", color=ProjectorIvory, fontSize=9.sp, fontWeight=FontWeight.Black) }
                }
                message?.let { Spacer(Modifier.height(7.dp)); Text(it, color=if (it.contains("not granted")) RecRed else MutedGold, fontSize=8.4.sp) }
            }
        }
    }
}
