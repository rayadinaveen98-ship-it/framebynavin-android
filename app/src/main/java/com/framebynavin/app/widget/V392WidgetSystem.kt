package com.framebynavin.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.PersonalAgendaPolicy
import com.framebynavin.app.personal.PersonalDeliveryContract
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
 * V392: replacement launcher widgets. Only these three providers are registered
 * in the manifest. Legacy V224 and Creator widget providers remain UNREGISTERED.
 *
 * Widgets are read-only: they never mark reminders Done, create or edit tasks,
 * or disclose personal titles/notes on the Android launcher.
 */
private const val V392_REFRESH = "com.framebynavin.app.widget.V392_REFRESH"
private val v392Scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

internal enum class V392WidgetSource { CREATOR, PERSONAL, CHALLENGE }

internal data class V392NextReminder(
    val timeMillis: Long,
    val source: V392WidgetSource,
    val challengeId: String? = null,
)

internal data class V392ChallengePulse(
    val id: String,
    val dayNumber: Int?,
    val completedDays: Int,
    val paused: Boolean,
)

internal data class V392WidgetSnapshot(
    val creatorActive: Int?,
    val personalActive: Int?,
    val upcomingToday: Int?,
    val next: V392NextReminder?,
    val challenge: V392ChallengePulse?,
    val hasReadError: Boolean,
)

/** Pure aggregation of already-validated store snapshots. Never mutates stores. */
internal object V392WidgetSnapshotPolicy {
    fun build(
        tasks: List<CreatorTask>?,
        personal: List<PersonalReminder>?,
        challenges: List<Phase1HabitChallenge>?,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): V392WidgetSnapshot {
        val activeTasks = tasks?.filter {
            it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING
        }
        val nexts = mutableListOf<V392NextReminder>()
        activeTasks?.filter { it.reminderEnabled && it.reminderAtMillis > nowMillis }
            ?.forEach { nexts.add(V392NextReminder(it.reminderAtMillis, V392WidgetSource.CREATOR)) }
        val activePersonal = personal?.filter { !it.paused }
        activePersonal?.forEach { item ->
            val due = runCatching { PersonalAgendaPolicy.dueAtMillis(item, nowMillis) }.getOrNull()
            if (due != null && due > nowMillis)
                nexts.add(V392NextReminder(due, V392WidgetSource.PERSONAL))
        }
        challenges?.filter { !it.archived && !it.paused }?.forEach { challenge ->
            val due = runCatching {
                challenge.nextAlertAfter(Instant.ofEpochMilli(nowMillis))
                    ?.toInstant()?.toEpochMilli()
            }.getOrNull()
            if (due != null && due > nowMillis)
                nexts.add(V392NextReminder(due, V392WidgetSource.CHALLENGE, challenge.id))
        }
        val tomorrow = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val validCandidates = nexts.filter { it.timeMillis > nowMillis }
        val todayCount = if (tasks == null || personal == null || challenges == null) null
            else validCandidates.count { it.timeMillis < tomorrow }
        val activeChallenge = challenges?.firstOrNull {
            !it.archived && it.dayNumber(Instant.ofEpochMilli(nowMillis)) != null
        } ?: challenges?.firstOrNull { !it.archived }
        val pulse = activeChallenge?.let {
            V392ChallengePulse(it.id,
                it.dayNumber(Instant.ofEpochMilli(nowMillis)),
                it.confirmations.size.coerceIn(0, 21), it.paused)
        }
        return V392WidgetSnapshot(
            creatorActive = activeTasks?.size,
            personalActive = activePersonal?.size,
            upcomingToday = todayCount,
            next = validCandidates.minByOrNull { it.timeMillis },
            challenge = pulse,
            hasReadError = tasks == null || personal == null || challenges == null,
        )
    }
}

internal object V392WidgetUpdater {
    suspend fun update(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val today = ids(app, manager, V392TodayHubWidgetProvider::class.java)
        val next = ids(app, manager, V392NextReminderWidgetProvider::class.java)
        val habit = ids(app, manager, V392ChallengePulseWidgetProvider::class.java)
        if (today.isEmpty() && next.isEmpty() && habit.isEmpty()) return

        // Preserve "unavailable" independently for unreadable stores. A corrupt
        // Personal record must not be rendered as "zero reminders".
        val tasks = runCatching { TaskStore(app).load() }.getOrNull()
        val personal = runCatching { PersonalReminderStore(app).load() }.getOrNull()
        val challenges = runCatching { Phase1HabitStore(app).load() }.getOrNull()
        val snapshot = V392WidgetSnapshotPolicy.build(tasks, personal, challenges,
            System.currentTimeMillis())

        today.forEach { id ->
            manager.updateAppWidget(id, todayViews(app, id, snapshot, compact(manager, id)))
        }
        next.forEach { id ->
            manager.updateAppWidget(id, nextViews(app, id, snapshot, compact(manager, id)))
        }
        habit.forEach { id ->
            manager.updateAppWidget(id, challengeViews(app, id, snapshot, compact(manager, id)))
        }
    }

    private fun ids(
        app: Context, manager: AppWidgetManager, cls: Class<out AppWidgetProvider>,
    ): IntArray = manager.getAppWidgetIds(ComponentName(app, cls))

    private fun compact(manager: AppWidgetManager, id: Int): Boolean {
        val opts = manager.getAppWidgetOptions(id)
        val h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        return h in 1..92
    }

    private fun todayViews(
        context: Context, id: Int, data: V392WidgetSnapshot, compact: Boolean,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_v392_today).apply {
        setTextViewText(R.id.widget_v392_today_creator,
            data.creatorActive?.let { "$it creator ${if (it == 1) "project" else "projects"}" } ?: "Creator data unavailable")
        setTextViewText(R.id.widget_v392_today_personal,
            data.personalActive?.let { "$it personal ${if (it == 1) "reminder" else "reminders"}" } ?: "Personal data unavailable")
        setTextViewText(R.id.widget_v392_today_upcoming,
            data.upcomingToday?.let { "$it reminders remaining today" } ?: "Check reminders inside Backlot")
        setViewVisibility(R.id.widget_v392_today_upcoming,
            if (compact) View.GONE else View.VISIBLE)
        setOnClickPendingIntent(R.id.widget_v392_today_root,
            launch(context, id, CreatorWidgetContract.ACTION_OPEN_TODAY, "today"))
        setOnClickPendingIntent(R.id.widget_v392_today_refresh,
            refreshIntent(context, id, V392TodayHubWidgetProvider::class.java))
    }

    private fun nextViews(
        context: Context, id: Int, data: V392WidgetSnapshot, compact: Boolean,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_v392_next).apply {
        val next = data.next
        val text = when {
            next != null -> SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(Date(next.timeMillis))
            data.hasReadError -> "Unavailable"
            else -> "All clear"
        }
        val source = when (next?.source) {
            V392WidgetSource.CREATOR -> "CREATOR REMINDER"
            V392WidgetSource.PERSONAL -> "PERSONAL REMINDER"
            V392WidgetSource.CHALLENGE -> "21-DAY CHALLENGE"
            null -> if (data.hasReadError) "Open Backlot to refresh" else "Nothing scheduled"
        }
        setTextViewText(R.id.widget_v392_next_time, text)
        setTextViewText(R.id.widget_v392_next_source, source)
        setViewVisibility(R.id.widget_v392_next_hint,
            if (compact) View.GONE else View.VISIBLE)
        val action = if (next?.source == V392WidgetSource.CREATOR)
            CreatorWidgetContract.ACTION_OPEN_REMINDERS
            else PersonalDeliveryContract.ACTION_OPEN_PERSONAL
        setOnClickPendingIntent(R.id.widget_v392_next_root,
            launch(context, id, action, "next", next?.challengeId))
        setOnClickPendingIntent(R.id.widget_v392_next_refresh,
            refreshIntent(context, id, V392NextReminderWidgetProvider::class.java))
    }

    private fun challengeViews(
        context: Context, id: Int, data: V392WidgetSnapshot, compact: Boolean,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_v392_challenge).apply {
        val progress = data.challenge
        setTextViewText(R.id.widget_v392_challenge_day, when {
            progress?.dayNumber != null -> "Day ${progress.dayNumber} / 21"
            progress != null -> "21-day journey"
            data.hasReadError -> "Unavailable"
            else -> "Start a challenge"
        })
        setTextViewText(R.id.widget_v392_challenge_progress, when {
            progress != null -> "${progress.completedDays} / 21 completed" +
                if (progress.paused) " · Paused" else ""
            data.hasReadError -> "Open Backlot to refresh"
            else -> "No active challenge"
        })
        setViewVisibility(R.id.widget_v392_challenge_hint,
            if (compact) View.GONE else View.VISIBLE)
        setOnClickPendingIntent(R.id.widget_v392_challenge_root,
            launch(context, id, PersonalDeliveryContract.ACTION_OPEN_PERSONAL,
                "challenge", progress?.id))
        setOnClickPendingIntent(R.id.widget_v392_challenge_refresh,
            refreshIntent(context, id, V392ChallengePulseWidgetProvider::class.java))
    }

    private fun launch(
        context: Context, id: Int, action: String, kind: String,
        challengeId: String? = null,
    ): PendingIntent {
        val target = Intent(context, MainActivity::class.java).apply {
            this.action = action
            data = if (challengeId != null && action == PersonalDeliveryContract.ACTION_OPEN_PERSONAL)
                Uri.parse("backlot-habit://open/" + Uri.encode(challengeId))
            else Uri.parse("backlot-v392://open/$kind/$id")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(context, id xor kind.hashCode(), target,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun refreshIntent(
        context: Context, id: Int, cls: Class<out AppWidgetProvider>,
    ): PendingIntent {
        val refresh = Intent(context, cls).apply {
            action = V392_REFRESH
            data = Uri.parse("backlot-v392://refresh/$id")
        }
        return PendingIntent.getBroadcast(context, id xor 0x3923, refresh,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

internal object V392WidgetRefresh {
    /** Broadcast-only: safe to invoke after a data commit, never reads stores. */
    fun request(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val providers = listOf(
            V392TodayHubWidgetProvider::class.java,
            V392NextReminderWidgetProvider::class.java,
            V392ChallengePulseWidgetProvider::class.java,
        )
        if (providers.none { manager.getAppWidgetIds(ComponentName(app, it)).isNotEmpty() }) return
        runCatching {
            app.sendBroadcast(Intent(app, V392TodayHubWidgetProvider::class.java).apply {
                action = V392_REFRESH
            })
        }.onFailure { Log.w("BacklotV392Widget", "Refresh requested but unavailable", it) }
    }
}

/**
 * goAsync keeps Android's broadcast alive during the bounded DataStore reads.
 * No detached fire-and-forget widget updates after the receiver is killed.
 */
abstract class V392BaseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray,
    ) = render(context)

    override fun onEnabled(context: Context) = render(context)

    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager,
        appWidgetId: Int, newOptions: Bundle,
    ) = render(context)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == V392_REFRESH) render(context)
        else super.onReceive(context, intent)
    }

    private fun render(context: Context) {
        val pending = goAsync()
        v392Scope.launch {
            try {
                withTimeout(8_500L) { V392WidgetUpdater.update(context) }
            } catch (error: Exception) {
                Log.w("BacklotV392Widget", "Widget refresh unavailable; existing display retained", error)
            } finally {
                pending.finish()
            }
        }
    }
}

class V392TodayHubWidgetProvider : V392BaseWidgetProvider()
class V392NextReminderWidgetProvider : V392BaseWidgetProvider()
class V392ChallengePulseWidgetProvider : V392BaseWidgetProvider()
