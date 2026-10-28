package com.framebynavin.app.personal

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.MainActivity
import com.framebynavin.app.R

/** Private-by-default notifications: personal title hidden on lock screen. */
internal object PersonalReminderNotifications {
    private const val CHANNEL = "backlot_personal_reminders_v1"
    private const val CHANNEL_NAME = "Personal reminders"
    private const val NOTIFICATION_ID = 7301

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            .apply {
                description = "Backlot personal reminders (separate from creator check-ins)"
                enableVibration(true)
            }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun show(context: Context, reminder: PersonalReminder): Boolean {
        if (!canPost(context) || reminder.pendingToken.isBlank()) return false
        val tag = PersonalDeliveryContract.notificationTag(reminder.id)
        val generic = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.backlot_v198_notification_monochrome)
            .setContentTitle("Personal reminder")
            .setContentText("Open Backlot to view your reminder")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        val opening = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            Intent(context, if (reminder.alert == PersonalAlert.NOTIFICATION)
                MainActivity::class.java else PersonalAlertActivity::class.java).apply {
                action = PersonalDeliveryContract.ACTION_OPEN_PERSONAL
                // The alert activity is bound to the exact posted occurrence,
                // so old notifications cannot control newer personal reminders.
                data = PersonalDeliveryContract.actionUri(reminder.id, "open", reminder.pendingToken)
                putExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID, reminder.id)
                putExtra(PersonalDeliveryContract.EXTRA_TOKEN, reminder.pendingToken)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val detail = when (reminder.alert) {
            PersonalAlert.NOTIFICATION -> "Personal reminder"
            PersonalAlert.VOICE -> "Voice presentation not yet enabled · notification fallback"
            PersonalAlert.ALARM -> "Alarm presentation not yet enabled · notification fallback"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.backlot_v198_notification_monochrome)
            .setContentTitle(reminder.title)
            .setContentText(detail)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(generic)
            .setOnlyAlertOnce(true)
            .setContentIntent(opening)
            .setAutoCancel(false)
            .addAction(0, "DONE", action(context, reminder, PersonalDeliveryContract.ACTION_DONE, 11))
            .addAction(0, "SNOOZE 10m", action(context, reminder, PersonalDeliveryContract.ACTION_SNOOZE, 12))
            .addAction(0, "DISMISS", action(context, reminder, PersonalDeliveryContract.ACTION_DISMISS, 13))
            .build()
        return runCatching {
            context.getSystemService(NotificationManager::class.java)
                .notify(tag, NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    fun cancel(context: Context, id: String) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(PersonalDeliveryContract.notificationTag(id), NOTIFICATION_ID)
    }

    private fun action(
        context: Context,
        reminder: PersonalReminder,
        action: String,
        salt: Int,
    ): PendingIntent {
        val intent = Intent(context, PersonalReminderActionReceiver::class.java)
            .setAction(action)
            .setData(PersonalDeliveryContract.actionUri(reminder.id, action, reminder.pendingToken))
            .putExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID, reminder.id)
            .putExtra(PersonalDeliveryContract.EXTRA_OCCURRENCE, reminder.pendingOccurrenceMillis)
            .putExtra(PersonalDeliveryContract.EXTRA_TOKEN, reminder.pendingToken)
        return PendingIntent.getBroadcast(
            context, reminder.id.hashCode() xor salt, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
