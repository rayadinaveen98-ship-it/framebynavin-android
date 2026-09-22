package com.framebynavin.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaReminderCadence
import com.framebynavin.app.data.IdeaReminderPolicy
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.widget.CreatorWidgetContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object IdeaReminderContract {
    const val EXTRA_IDEA_ID = "idea_reminder_id"
    const val EXTRA_EXPECTED_AT = "idea_reminder_expected_at"
    const val ACTION_FIRE = "com.framebynavin.app.reminders.IDEA_FIRE"
    const val ACTION_SNOOZE_3H = "com.framebynavin.app.reminders.IDEA_SNOOZE_3H"
    const val ACTION_STOP = "com.framebynavin.app.reminders.IDEA_STOP"
}

class IdeaReminderScheduler(context: Context) {
    private val app = context.applicationContext
    private val alarm = app.getSystemService(AlarmManager::class.java)
    private val prefs = app.getSharedPreferences("idea_reminders_v144", Context.MODE_PRIVATE)

    fun schedule(idea: CreatorIdea) {
        val normalized = IdeaReminderPolicy.normalize(idea)
        if (!IdeaReminderPolicy.isActionable(normalized) || normalized.reminderAtMillis <= System.currentTimeMillis()) {
            cancel(idea.id)
            return
        }
        val pending = fireIntent(normalized.id, normalized.reminderAtMillis)
        val exactDelivery = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
        if (exactDelivery) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, normalized.reminderAtMillis, pending)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, normalized.reminderAtMillis, pending)
        }
        remember(normalized.id)
    }

    fun cancel(ideaId: String) {
        alarm.cancel(fireIntent(ideaId, 0L))
        IdeaReminderNotifications.cancel(app, ideaId)
        forget(ideaId)
    }

    suspend fun reconcile() {
        val now = System.currentTimeMillis()
        val store = IdeaVaultStore(app)
        val normalized = store.mutate { current -> current.map { IdeaReminderPolicy.normalize(it, now) } }
        val active = normalized.filter(IdeaReminderPolicy::isActionable).associateBy { it.id }
        (knownIds() - active.keys).forEach(::cancel)
        active.values.forEach(::schedule)
    }

    private fun fireIntent(ideaId: String, expectedAt: Long): PendingIntent {
        val intent = Intent(app, IdeaReminderReceiver::class.java)
            .setAction(IdeaReminderContract.ACTION_FIRE)
            .setData(Uri.parse("framebynavin://idea-reminder/${Uri.encode(ideaId)}"))
            .putExtra(IdeaReminderContract.EXTRA_IDEA_ID, ideaId)
        if (expectedAt > 0L) intent.putExtra(IdeaReminderContract.EXTRA_EXPECTED_AT, expectedAt)
        return PendingIntent.getBroadcast(
            app,
            ideaId.hashCode() xor 0x1DEA,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun knownIds(): Set<String> = prefs.getStringSet("scheduled_ids", emptySet()).orEmpty().toSet()

    private fun remember(ideaId: String) {
        prefs.edit().putStringSet("scheduled_ids", knownIds() + ideaId).apply()
    }

    private fun forget(ideaId: String) {
        prefs.edit().putStringSet("scheduled_ids", knownIds() - ideaId).apply()
    }
}

class IdeaReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != IdeaReminderContract.ACTION_FIRE) return
        val ideaId = intent.getStringExtra(IdeaReminderContract.EXTRA_IDEA_ID).orEmpty()
        val expectedAt = intent.getLongExtra(IdeaReminderContract.EXTRA_EXPECTED_AT, 0L)
        if (ideaId.isBlank() || expectedAt <= 0L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                val store = IdeaVaultStore(app)
                val current = store.load().firstOrNull { it.id == ideaId }
                if (current == null || !IdeaReminderPolicy.isActionable(current) || current.reminderAtMillis != expectedAt) {
                    IdeaReminderScheduler(app).cancel(ideaId)
                    return@launch
                }
                val now = System.currentTimeMillis()
                val fired = IdeaReminderPolicy.afterFired(current, now)
                val saved = store.mutate { ideas -> ideas.map { if (it.id == ideaId) fired.copy(updatedAtMillis = now) else it } }
                    .firstOrNull { it.id == ideaId } ?: fired
                val scheduler = IdeaReminderScheduler(app)
                if (saved.reminderCadence == IdeaReminderCadence.DAILY && IdeaReminderPolicy.isActionable(saved)) {
                    scheduler.schedule(saved)
                } else {
                    scheduler.cancel(ideaId)
                }
                IdeaReminderNotifications.show(app, saved)
            } finally {
                pending.finish()
            }
        }
    }
}

class IdeaReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != IdeaReminderContract.ACTION_SNOOZE_3H && action != IdeaReminderContract.ACTION_STOP) return
        val ideaId = intent.getStringExtra(IdeaReminderContract.EXTRA_IDEA_ID).orEmpty()
        if (ideaId.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                val store = IdeaVaultStore(app)
                val now = System.currentTimeMillis()
                val updated = store.mutate { ideas ->
                    ideas.map { idea ->
                        if (idea.id != ideaId) idea
                        else when (action) {
                            IdeaReminderContract.ACTION_SNOOZE_3H -> idea.copy(
                                reminderAtMillis = now + 3 * 60 * 60_000L,
                                updatedAtMillis = now,
                            )
                            else -> idea.copy(
                                reminderAtMillis = 0L,
                                reminderCadence = IdeaReminderCadence.ONCE,
                                updatedAtMillis = now,
                            )
                        }
                    }
                }.firstOrNull { it.id == ideaId }
                val scheduler = IdeaReminderScheduler(app)
                if (updated != null && IdeaReminderPolicy.isActionable(updated)) scheduler.schedule(updated)
                else scheduler.cancel(ideaId)
                IdeaReminderNotifications.cancel(app, ideaId)
            } finally {
                pending.finish()
            }
        }
    }
}

object IdeaReminderNotifications {
    fun show(context: Context, idea: CreatorIdea): Boolean {
        ReminderNotifications.ensureChannel(context)
        if (!ReminderNotifications.canPost(context)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false

        val body = "Still worth exploring? Open this idea or turn it into a project."
        val notification = NotificationCompat.Builder(context, ReminderConstants.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("💡 ${idea.title}")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIdeaIntent(context, idea.id, CreatorWidgetContract.IDEA_MODE_OPEN, 1))
            .addAction(0, "OPEN", openIdeaIntent(context, idea.id, CreatorWidgetContract.IDEA_MODE_OPEN, 2))
            .addAction(0, "MAKE PROJECT", openIdeaIntent(context, idea.id, CreatorWidgetContract.IDEA_MODE_CONVERT, 3))
            .addAction(0, "SNOOZE 3H", actionIntent(context, idea.id, IdeaReminderContract.ACTION_SNOOZE_3H, 4))
            .addAction(0, "STOP", actionIntent(context, idea.id, IdeaReminderContract.ACTION_STOP, 5))
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(notificationId(idea.id), notification)
            true
        }.getOrDefault(false)
    }

    fun cancel(context: Context, ideaId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(ideaId))
    }

    private fun openIdeaIntent(context: Context, ideaId: String, mode: String, salt: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(CreatorWidgetContract.ACTION_IDEA_VAULT)
            .setData(Uri.parse("framebynavin://idea/${Uri.encode(ideaId)}/${Uri.encode(mode)}"))
            .putExtra(CreatorWidgetContract.EXTRA_IDEA_ID, ideaId)
            .putExtra(CreatorWidgetContract.EXTRA_IDEA_MODE, mode)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            ideaId.hashCode() xor (salt shl 16),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(context: Context, ideaId: String, action: String, salt: Int): PendingIntent {
        val intent = Intent(context, IdeaReminderActionReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("framebynavin://idea-reminder-action/${Uri.encode(ideaId)}/$salt"))
            .putExtra(IdeaReminderContract.EXTRA_IDEA_ID, ideaId)
        return PendingIntent.getBroadcast(
            context,
            ideaId.hashCode() xor (salt shl 16),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(ideaId: String): Int = "idea:$ideaId".hashCode()
}
