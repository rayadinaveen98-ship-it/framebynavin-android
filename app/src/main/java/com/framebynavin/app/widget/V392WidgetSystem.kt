package com.framebynavin.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.PersonalDeliveryContract
import com.framebynavin.app.personal.PersonalRecurrence
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitStore
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * V392 is the REPLACEMENT family. No legacy provider is registered.
 * Read-only launcher surfaces: every action opens Backlot for user confirmation.
 * Personal titles, notes and challenge activity text never leave the app for a
 * launcher that may be visible on a shared or unlocked device.
 */
internal class V392TodayHubWidgetProvider : V392WidgetProvider()
internal class V392NextReminderWidgetProvider : V392WidgetProvider()
internal class V392ChallengePulseWidgetProvider : V392WidgetProvider()

private val v392ReceiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

internal abstract class V392WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        performRefresh(context)
    }

    override fun onEnabled(context: Context) {
        performRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle,
    ) {
        performRefresh(context)
    }

    private fun performRefresh(context: Context) {
        // Android can kill a receiver as soon as it returns. Bounded goAsync
        // lifetime protects the current render without a persistent worker.
        val pending = goAsync()
        v392ReceiverScope.launch {
            try {
                withTimeout(9_000L) {
                    val app = context.applicationContext
                    V392WidgetUpdater.updateAll(app, TaskStore(app).load())
                }
            } catch (failure: Exception) {
                android.util.Log.w("BacklotWidgets", "V392 refresh unavailable; next scheduled refresh can retry", failure)
            } finally {
                pending.finish()
            }
        }
    }
}

internal object V392WidgetUpdater {
    private data class Candidate(val at: Long, val source: String, val action: String,
                                 val challengeId: String? = null)

    private data class Summary(
        val projectsActive: Int,
        val creatorDueToday: Int,
        val personalDueToday: Int?,
        val next: Candidate?,
        val challenge: Phase1HabitChallenge?,
        val challengeAvailable: Boolean,
        val now: Long,
    )

    private fun personalUpcoming(
        records: List<PersonalReminder>, now: Long,
    ): List<Long> = records.filterNot { it.paused }.mapNotNull { reminder ->
        // A posted unanswered occurrence is not a new future appointment.
        if (reminder.pendingToken.isNotBlank()) {
            reminder.pendingFireAtMillis.takeIf {
                reminder.presentedToken.isBlank() && it > now
            }
        } else {
            PersonalRecurrence.nextAfter(reminder, now)
        }
    }

    /** Corrupt stores never appear as "zero". We show a private unavailable state. */
    private suspend fun summary(context: Context, tasks: List<CreatorTask>): Summary {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val active = tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
        val creatorDue = active.count { task ->
            task.dueAtMillis > 0L &&
                Instant.ofEpochMilli(task.dueAtMillis).atZone(zone).toLocalDate() == today
        }
        val creatorNext = active.filter {
            it.reminderEnabled && it.reminderMode != ReminderMode.NONE && it.reminderAtMillis > now
        }.minByOrNull { it.reminderAtMillis }

        val personal = runCatching { PersonalReminderStore(context).load() }.getOrNull()
        val personalTimes = personal?.let { runCatching { personalUpcoming(it, now) }.getOrNull() }
        val challengeRecords = runCatching { Phase1HabitStore(context).load() }.getOrNull()
        val currentChallenge = challengeRecords?.filterNot { it.archived }
            ?.sortedWith(compareByDescending<Phase1HabitChallenge> {
                val day = it.dayNumber(Instant.ofEpochMilli(now))
                day != null && !it.paused
            }.thenBy { it.startDate })?.firstOrNull()
        val challengeNext = challengeRecords?.asSequence()
            ?.filterNot { it.paused || it.archived || it.alertKind == null }
            ?.mapNotNull { record ->
                runCatching { record.nextAlertAfter(Instant.ofEpochMilli(now))?.toInstant()?.toEpochMilli() }
                    .getOrNull()?.let { Candidate(it, "21-day challenge",
                        PersonalDeliveryContract.ACTION_OPEN_PERSONAL, record.id) }
            }?.minByOrNull { it.at }

        val candidates = listOfNotNull(
            creatorNext?.let { Candidate(it.reminderAtMillis, "Creator reminder",
                CreatorWidgetContract.ACTION_OPEN_REMINDERS) },
            personalTimes?.minOrNull()?.let { Candidate(it, "Personal reminder",
                PersonalDeliveryContract.ACTION_OPEN_PERSONAL) },
            challengeNext,
        )
        return Summary(
            projectsActive = active.size,
            creatorDueToday = creatorDue,
            personalDueToday = personalTimes?.count { it < todayEnd },
            next = candidates.minByOrNull { it.at },
            challenge = currentChallenge,
            challengeAvailable = challengeRecords != null,
            now = now,
        )
    }

    suspend fun updateAll(context: Context, tasks: List<CreatorTask>) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val today = ids(manager, app, V392TodayHubWidgetProvider::class.java)
        val next = ids(manager, app, V392NextReminderWidgetProvider::class.java)
        val challenge = ids(manager, app, V392ChallengePulseWidgetProvider::class.java)
        if (today.isEmpty() && next.isEmpty() && challenge.isEmpty()) return

        val data = summary(app, tasks)
        for (id in today) manager.updateAppWidget(id, todayViews(app, manager, id, data))
        for (id in next) manager.updateAppWidget(id, nextViews(app, data))
        for (id in challenge) manager.updateAppWidget(id, challengeViews(app, data))
    }

    private fun ids(manager: AppWidgetManager, context: Context,
                    klass: Class<out AppWidgetProvider>): IntArray =
        manager.getAppWidgetIds(ComponentName(context, klass))

    /** Request only; never read DataStore inside a write transaction or BroadcastReceiver. */
    fun requestRefresh(context: Context) {
        val app = context.applicationContext
        runCatching {
            val manager = AppWidgetManager.getInstance(app)
            for (klass in listOf(V392TodayHubWidgetProvider::class.java,
                    V392NextReminderWidgetProvider::class.java, V392ChallengePulseWidgetProvider::class.java)) {
                val component = ComponentName(app, klass)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    app.sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .setComponent(component)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids))
                }
            }
        }.onFailure {
            android.util.Log.w("BacklotWidgets", "V392 refresh request unavailable", it)
        }
    }

    private fun compact(manager: AppWidgetManager, id: Int): Boolean =
        manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280) < 235

    private fun todayViews(context: Context, manager: AppWidgetManager, id: Int, s: Summary): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_v392_today_hub).apply {
            val day = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(s.now))
            setTextViewText(R.id.v392_today_date, day.uppercase(Locale.getDefault()))
            setTextViewText(R.id.v392_today_headline,
                if (s.projectsActive == 0 && s.personalDueToday == 0) "A fresh start" else "Your day, in focus")
            setTextViewText(R.id.v392_today_creator,
                "CREATOR\n" + s.projectsActive + " active · " + s.creatorDueToday + " due")
            setTextViewText(R.id.v392_today_personal,
                "PERSONAL\n" + (s.personalDueToday?.let { it.toString() + " reminders" } ?: "Open to check"))
            if (compact(manager, id)) {
                setViewVisibility(R.id.v392_today_headline, View.GONE)
                setTextViewTextSize(R.id.v392_today_creator, TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextViewTextSize(R.id.v392_today_personal, TypedValue.COMPLEX_UNIT_SP, 11f)
            }
            setOnClickPendingIntent(R.id.v392_today_open,
                launchIntent(context, CreatorWidgetContract.ACTION_OPEN_TODAY, "today"))
            setOnClickPendingIntent(R.id.v392_today_personal_open,
                launchIntent(context, PersonalDeliveryContract.ACTION_OPEN_PERSONAL, "personal"))
        }

    private fun nextViews(context: Context, s: Summary): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_v392_next_reminder).apply {
            setTextViewText(R.id.v392_next_time, s.next?.let { clock(it.at) } ?: "All clear")
            setTextViewText(R.id.v392_next_source, s.next?.source ?: "No upcoming reminders")
            setOnClickPendingIntent(R.id.v392_next_open,
                s.next?.let { candidateIntent(context, it) }
                    ?: launchIntent(context, CreatorWidgetContract.ACTION_OPEN_REMINDERS, "reminders"))
        }

    private fun challengeViews(context: Context, s: Summary): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_v392_challenge_pulse).apply {
            val challenge = s.challenge
            if (challenge == null) {
                setTextViewText(R.id.v392_challenge_day,
                    if (s.challengeAvailable) "Begin today" else "Open Backlot")
                setTextViewText(R.id.v392_challenge_progress,
                    if (s.challengeAvailable) "21 days · One activity" else "Open to check progress")
            } else {
                val day = challenge.dayNumber(Instant.ofEpochMilli(s.now))
                setTextViewText(R.id.v392_challenge_day, day?.let { "Day " + it + " / 21" }
                    ?: if (LocalDate.now(ZoneId.of(challenge.zoneId)).isBefore(challenge.startDate))
                        "Starts soon" else "21 days")
                setTextViewText(R.id.v392_challenge_progress,
                    challenge.confirmations.size.toString() + " of 21 confirmed" +
                        if (challenge.paused) " · Paused" else "")
            }
            setOnClickPendingIntent(R.id.v392_challenge_open,
                challenge?.let {
                    launchIntent(context, PersonalDeliveryContract.ACTION_OPEN_PERSONAL,
                        "challenge/" + it.id, "backlot-habit://open/" + Uri.encode(it.id))
                } ?: launchIntent(context, PersonalDeliveryContract.ACTION_OPEN_PERSONAL, "personal"))
        }

    private fun clock(millis: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

    private fun candidateIntent(context: Context, candidate: Candidate): PendingIntent =
        if (candidate.challengeId != null)
            launchIntent(context, PersonalDeliveryContract.ACTION_OPEN_PERSONAL,
                "challenge/" + candidate.challengeId,
                "backlot-habit://open/" + Uri.encode(candidate.challengeId))
        else launchIntent(context, candidate.action, candidate.source.lowercase(Locale.ROOT).replace(' ', '-'))

    private fun launchIntent(
        context: Context, action: String, key: String, data: String = "backlot-v392://open/" + key,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action).setData(Uri.parse(data))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(context, data.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
