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
        if (calendarId <= 0L || googleCalendars(context).none { it.id == calendarId }) return 0
        return syncDetailed(context, tasks, calendarId).changed
    }

    /** Kept for existing callers. A zero count does not mean initial sync failed. */
    fun sync(context: Context, tasks: List<CreatorTask>, calendarId: Long): Int =
        syncDetailed(context, tasks, calendarId).changed

    private enum class ManagedEvent { MISSING, OWNED, OTHER_CALENDAR, NOT_BACKLOT }

    private fun managedEvent(context: Context, eventId: Long, calendarId: Long): ManagedEvent {
        val projection = arrayOf(CalendarContract.Events.CALENDAR_ID, CalendarContract.Events.TITLE)
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            "${CalendarContract.Events._ID}=?",
            arrayOf(eventId.toString()),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return ManagedEvent.MISSING
            val existingCalendar = cursor.getLong(0)
            val existingTitle = cursor.getString(1)
            if (!CreatorCalendarSyncS1Policy.ownedTitle(existingTitle)) return ManagedEvent.NOT_BACKLOT
            return if (existingCalendar == calendarId) ManagedEvent.OWNED
                else ManagedEvent.OTHER_CALENDAR
        }
        // Don't assume a failed provider lookup means it is safe to overwrite an unknown event.
        error("Could not verify the existing calendar entry.")
    }

    /** S1: never edit/delete a stored event ID unless it still identifies a Backlot
     * event in the selected calendar. Calendar changes do not delete old-calendar events. */
    fun syncDetailed(context: Context, tasks: List<CreatorTask>, calendarId: Long): CreatorCalendarSyncReport {
        require(hasPermission(context)) { "Calendar permission is needed to sync." }
        require(calendarId > 0L && googleCalendars(context).any { it.id == calendarId }) {
            "Choose an available Google calendar before syncing."
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val active = CreatorCalendarSyncS1Policy.exportable(tasks)
        val activeKeys = active.map { EVENT_PREFIX + it.id }.toSet()
        var created = 0
        var updated = 0
        var removed = 0
        var blocked = 0

        active.forEach { task ->
            val key = EVENT_PREFIX + task.id
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, CreatorCalendarSyncS1Policy.title(task))
                put(CalendarContract.Events.DESCRIPTION, CreatorCalendarSyncS1Policy.description(task))
                put(CalendarContract.Events.DTSTART, task.dueAtMillis)
                put(CalendarContract.Events.DTEND, task.dueAtMillis + 30L * 60_000L)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 0)
            }
            val existing = prefs.getLong(key, -1L)
            val kind = if (existing > 0L) managedEvent(context, existing, calendarId)
                else ManagedEvent.MISSING
            when (kind) {
                ManagedEvent.OWNED -> {
                    val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existing)
                    if (context.contentResolver.update(uri, values, null, null) > 0) updated++ else blocked++
                }
                ManagedEvent.NOT_BACKLOT -> blocked++ // creator may have renamed it
                ManagedEvent.MISSING, ManagedEvent.OTHER_CALENDAR -> {
                    val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                        ?: error("Could not create a calendar entry.")
                    val id = ContentUris.parseId(uri)
                    require(id > 0L) { "Could not save the calendar entry." }
                    check(prefs.edit().putLong(key, id).commit()) { "Could not save calendar sync state." }
                    created++
                }
            }
        }

        // Cleanup only entries that Backlot created in the *currently selected* calendar.
        // Never delete a manually renamed entry or an entry in another calendar.
        prefs.all.filterKeys { it.startsWith(EVENT_PREFIX) && it !in activeKeys }
            .forEach { (key, raw) ->
                val id = raw as? Long ?: return@forEach
                when (managedEvent(context, id, calendarId)) {
                    ManagedEvent.OWNED -> {
                        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
                        if (context.contentResolver.delete(uri, null, null) > 0) {
                            check(prefs.edit().remove(key).commit()) { "Could not update calendar sync state." }
                            removed++
                        } else blocked++
                    }
                    ManagedEvent.MISSING -> prefs.edit().remove(key).apply()
                    ManagedEvent.OTHER_CALENDAR -> Unit // old calendar preserved
                    ManagedEvent.NOT_BACKLOT -> blocked++ // never touch user-edited entry
                }
            }
        return CreatorCalendarSyncReport(created, updated, removed, blocked)
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
                        Text("Show deadlines in Google Calendar", color=ProjectorIvory, fontSize=11.5.sp, fontWeight=FontWeight.Bold)
                        Text("Backlot reminders still work separately.", color=MutedText, fontSize=8.5.sp)
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
                    Text("No calendar is available. Add a Google Calendar on your phone, then try again.", color=MutedText, fontSize=8.8.sp, lineHeight=13.sp)
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
