package com.framebynavin.app.personal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.framebynavin.app.MainActivity
import java.util.UUID

/** Distinct receivers + URI namespaces ensure no collision with creator PendingIntents. */
internal object PersonalDeliveryContract {
    const val EXTRA_PERSONAL_ID = "backlot.personal.id"
    const val EXTRA_OCCURRENCE = "backlot.personal.occurrence"
    const val EXTRA_FIRE_AT = "backlot.personal.fire_at"
    const val EXTRA_TOKEN = "backlot.personal.token"
    const val EXTRA_ACTION = "backlot.personal.action"
    const val ACTION_FIRE = "com.framebynavin.app.personal.FIRE"
    const val ACTION_DONE = "com.framebynavin.app.personal.DONE"
    const val ACTION_SNOOZE = "com.framebynavin.app.personal.SNOOZE"
    const val ACTION_DISMISS = "com.framebynavin.app.personal.DISMISS"
    const val ACTION_RESCHEDULE = "com.framebynavin.app.personal.RESCHEDULE"
    const val EXTRA_RESCHEDULE_AT = "backlot.personal.reschedule_at"
    const val SNOOZE_MINUTES = 10L
    fun notificationTag(id: String): String = "backlot:personal:$id"
    fun uri(id: String, action: String) =
        Uri.parse("backlot-personal://reminders/${Uri.encode(id)}/${Uri.encode(action)}")

    // Android PendingIntent identity excludes Intent extras. Action URIs MUST
    // carry each immutable lease token so old notifications cannot inherit a
    // new Snooze / Done token through FLAG_UPDATE_CURRENT.
    fun actionUri(id: String, action: String, token: String) =
        Uri.parse("backlot-personal://actions/${Uri.encode(id)}/${Uri.encode(action)}/${Uri.encode(token)}")
}

internal enum class PersonalApplyResult {
    ACCEPTED, STALE, NOT_PRESENTED, INVALID_TIME, NOT_FOUND,
}

/**
 * Namespaced RTC_WAKEUP scheduling with a durable occurrence lease. This layer
 * never receives a CreatorTask, invokes Project Pulse, or pretends VOICE/ALARM
 * will play sound before dedicated personal media services exist.
 */
internal class PersonalDeliveryCoordinator(context: Context) {
    private val app = context.applicationContext
    private val store = PersonalReminderStore(app)
    private val alarms = app.getSystemService(AlarmManager::class.java)

    suspend fun reconcile(nowMillis: Long = System.currentTimeMillis()) {
        store.load().forEach { original ->
            if (original.paused) {
                cancelPending(original.id)
                return@forEach
            }
            var record = original
            // A posted notification remains actionable after process restart.
            // Do NOT schedule it again just because its due time is in the past.
            if (record.pendingToken.isNotBlank() && record.presentedToken == record.pendingToken) {
                cancelPending(record.id)
                return@forEach
            }
            if (record.pendingToken.isBlank()) {
                val next = PersonalRecurrence.nextAfter(record, nowMillis) ?: run {
                    cancelPending(record.id)
                    return@forEach
                }
                val token = UUID.randomUUID().toString()
                record = store.update(record.id) { latest ->
                    // Latest is authoritative: never replace a concurrent new lease.
                    if (latest.pendingToken.isNotEmpty() || latest.paused) latest
                    else latest.copy(
                        pendingOccurrenceMillis = next,
                        pendingFireAtMillis = next,
                        pendingToken = token,
                        presentedToken = "",
                        snoozedUntilMillis = 0L,
                    )
                }.first { it.id == record.id }
            }
            if (record.pendingToken.isBlank() || record.paused) return@forEach
            if (record.pendingFireAtMillis <= nowMillis) {
                if (nowMillis - record.pendingFireAtMillis > 24L * 60 * 60 * 1000) {
                    // Long-overdue occurrence remains uncompleted, not falsely
                    // "Done". Reconciliation moves to future scheduling only
                    // after an explicit later policy revision for missed alerts.
                    cancelPending(record.id)
                    return@forEach
                }
                record = store.update(record.id) { current ->
                    if (current.pendingToken != record.pendingToken || current.presentedToken.isNotEmpty()) current
                    else current.copy(
                        pendingFireAtMillis = nowMillis + 2_000L,
                        pendingToken = UUID.randomUUID().toString(),
                        presentedToken = "",
                    )
                }.first { it.id == record.id }
            }
            if (record.presentedToken.isBlank()) arm(record)
        }
    }

    fun cancelPending(id: String) {
        fireIntent(id, flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?.let { alarms.cancel(it) }
    }

    private fun arm(reminder: PersonalReminder) {
        val intent = fireIntent(
            reminder.id,
            reminder.pendingOccurrenceMillis,
            reminder.pendingFireAtMillis,
            reminder.pendingToken,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        val time = reminder.pendingFireAtMillis
        // When exact permission is missing Android may deliver this late.
        // Never claim precise or ringing Voice/Alarm presentation as a result.
        runCatching {
            if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
        }.onFailure {
            android.util.Log.w("BacklotPersonal", "Could not schedule personal occurrence", it)
        }
    }

    private fun fireIntent(
        id: String,
        original: Long = 0L,
        fireAt: Long = 0L,
        token: String = "",
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(app, PersonalReminderReceiver::class.java)
            .setAction(PersonalDeliveryContract.ACTION_FIRE)
            .setData(PersonalDeliveryContract.uri(id, "fire"))
            .putExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID, id)
            .putExtra(PersonalDeliveryContract.EXTRA_OCCURRENCE, original)
            .putExtra(PersonalDeliveryContract.EXTRA_FIRE_AT, fireAt)
            .putExtra(PersonalDeliveryContract.EXTRA_TOKEN, token)
        return PendingIntent.getBroadcast(app, id.hashCode(), intent, flags)
    }

    /**
     * Claim a real response under the *same* Personal DataStore mutation lock
     * as the occurrence journal. A stale token or repeated action is rejected.
     */
    suspend fun apply(
        id: String,
        originalOccurrence: Long,
        token: String,
        action: String,
        nowMillis: Long = System.currentTimeMillis(),
        rescheduleAtMillis: Long = 0L,
    ): PersonalApplyResult {
        if (token.isBlank() || originalOccurrence <= 0L) return PersonalApplyResult.STALE
        if (action == PersonalDeliveryContract.ACTION_RESCHEDULE &&
            rescheduleAtMillis <= nowMillis) return PersonalApplyResult.INVALID_TIME
        var result = PersonalApplyResult.NOT_FOUND
        store.mutate { current ->
            current.map { item ->
                if (item.id != id) return@map item
                result = when {
                    item.pendingToken != token || item.pendingOccurrenceMillis != originalOccurrence ->
                        PersonalApplyResult.STALE
                    item.presentedToken != token ->
                        PersonalApplyResult.NOT_PRESENTED
                    else -> PersonalApplyResult.ACCEPTED
                }
                if (result != PersonalApplyResult.ACCEPTED) return@map item
                val common = item.copy(
                    lastResponseAtMillis = nowMillis,
                    updatedAtMillis = nowMillis,
                )
                when (action) {
                    PersonalDeliveryContract.ACTION_DONE, PersonalDeliveryContract.ACTION_DISMISS ->
                        common.copy(
                            lastAcknowledgedOccurrenceMillis = maxOf(
                                item.lastAcknowledgedOccurrenceMillis, originalOccurrence,
                            ),
                            pendingOccurrenceMillis = 0L,
                            pendingFireAtMillis = 0L,
                            pendingToken = "",
                            presentedToken = "",
                            snoozedUntilMillis = 0L,
                            lastResponse = if (action == PersonalDeliveryContract.ACTION_DONE)
                                PersonalResponse.DONE else PersonalResponse.DISMISSED,
                        )
                    PersonalDeliveryContract.ACTION_SNOOZE, PersonalDeliveryContract.ACTION_RESCHEDULE -> {
                        val next = if (action == PersonalDeliveryContract.ACTION_SNOOZE)
                            nowMillis + PersonalDeliveryContract.SNOOZE_MINUTES * 60_000L
                        else rescheduleAtMillis
                        common.copy(
                            pendingFireAtMillis = next,
                            snoozedUntilMillis = next,
                            pendingToken = UUID.randomUUID().toString(),
                            presentedToken = "",
                            lastResponse = if (action == PersonalDeliveryContract.ACTION_SNOOZE)
                                PersonalResponse.SNOOZED else PersonalResponse.RESCHEDULED,
                        )
                    }
                    else -> {
                        result = PersonalApplyResult.STALE
                        item
                    }
                }
            }
        }
        if (result == PersonalApplyResult.ACCEPTED) {
            cancelPending(id)
            PersonalReminderNotifications.cancel(app, id)
            reconcile(nowMillis)
        }
        return result
    }

    /** Fire receiver calls this only after validating the stored lease. */
    suspend fun acknowledgePosted(id: String, occurrence: Long, fireAt: Long, token: String): Boolean {
        var claimed = false
        store.mutate { current ->
            current.map { item ->
                if (item.id == id && item.pendingToken == token &&
                    item.pendingOccurrenceMillis == occurrence && item.pendingFireAtMillis == fireAt &&
                    item.presentedToken.isBlank() && !item.paused) {
                    claimed = true
                    item.copy(presentedToken = token)
                } else item
            }
        }
        return claimed
    }

    suspend fun currentLease(id: String, occurrence: Long, fireAt: Long, token: String): PersonalReminder? =
        store.load().firstOrNull {
            it.id == id && !it.paused && it.pendingToken == token &&
                it.pendingOccurrenceMillis == occurrence && it.pendingFireAtMillis == fireAt &&
                it.presentedToken.isBlank()
        }
}
