package com.framebynavin.app.personal

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.MainActivity
import java.lang.ref.WeakReference
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** A stale OS broadcast cannot mark Done or show an altered challenge. */
internal object Phase1HabitOccurrencePolicy {
    fun current(challenge: Phase1HabitChallenge, scheduledAt: Long, now: Long): Boolean {
        if (scheduledAt <= 0L || now < scheduledAt ||
            now - scheduledAt > 86_400_000L || challenge.paused ||
            challenge.alertKind == null) return false
        val due = Instant.ofEpochMilli(scheduledAt)
        return challenge.nextAlertAfter(due.minusMillis(1))?.toInstant() == due
    }
}

internal object Phase1HabitNotification {
    const val ACTION = "com.framebynavin.app.personal.HABIT_FIRE"
    const val ACTION_SNOOZE = "com.framebynavin.app.personal.HABIT_SNOOZE"
    const val SNOOZE_MINUTES = "backlot.habit.snooze_minutes"
    const val ID = "backlot.habit.id"
    const val AT = "backlot.habit.at"
    const val TEXT = "backlot.habit.activity"
    private const val QUIET = "backlot_habit_quiet_v1"
    private const val ALARM = "backlot_habit_alarm_v1"
    private const val VOICE = "backlot_habit_voice_v1"
    fun channel(kind: Phase1HabitAlertKind): String = when(kind) {
        Phase1HabitAlertKind.QUIET -> QUIET
        Phase1HabitAlertKind.ALARM -> ALARM
        Phase1HabitAlertKind.VOICE -> VOICE
    }
    fun uri(id: String): Uri = Uri.parse("backlot-habit://fire/" + Uri.encode(id))
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(QUIET) == null) manager.createNotificationChannel(
            NotificationChannel(QUIET, "Quiet 21-day alerts", NotificationManager.IMPORTANCE_LOW)
                .apply { setSound(null, null) })
        if (manager.getNotificationChannel(ALARM) == null) manager.createNotificationChannel(
            NotificationChannel(ALARM, "21-day challenge alarms", NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    setSound(Settings.System.DEFAULT_ALARM_ALERT_URI,
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                    enableVibration(true)
                })
        if (manager.getNotificationChannel(VOICE) == null) manager.createNotificationChannel(
            NotificationChannel(VOICE, "21-day challenge voice", NotificationManager.IMPORTANCE_HIGH))
    }
    fun build(
        context: Context,
        challenge: Phase1HabitChallenge,
        scheduledAt: Long = 0L,
    ): Notification {
        val kind = requireNotNull(challenge.alertKind)
        val open = Intent(context, MainActivity::class.java)
            .setAction(PersonalDeliveryContract.ACTION_OPEN_PERSONAL)
            .setData(Uri.parse("backlot-habit://open/" + Uri.encode(challenge.id)))
        val pi = PendingIntent.getActivity(context, challenge.id.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Only the small white Spark silhouette may appear in the PUBLIC
        // lock-screen version; activity name, action and large pose stay private.
        val publicVersion = NotificationCompat.Builder(context, channel(kind))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Backlot reminder")
            .setContentText("Unlock to view today's activity")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        val builder = NotificationCompat.Builder(context, channel(kind))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Today's 21-day activity")
            .setContentText(challenge.activity)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (kind == Phase1HabitAlertKind.QUIET)
                NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setSilent(kind == Phase1HabitAlertKind.QUIET)
        // Only the exact, posted occurrence can be snoozed from the OS notice.
        // Completing a challenge is never inferred from an alert interaction.
        if (scheduledAt > 0L) {
            val snooze = PendingIntent.getBroadcast(
                context,
                challenge.id.hashCode() xor 0x2140,
                Intent(context, Phase1HabitSnoozeReceiver::class.java)
                    .setAction(ACTION_SNOOZE)
                    .setData(Uri.parse("backlot-habit://snooze/" +
                        Uri.encode(challenge.id) + "/" + scheduledAt))
                    .putExtra(ID, challenge.id)
                    .putExtra(AT, scheduledAt)
                    .putExtra(SNOOZE_MINUTES, 10),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, "SNOOZE 10m", snooze)
        }
        return builder.build()
    }

    /** Remove every posted notification for an archived/deleted/edited challenge. */
    fun cancelAllPosted(context: Context, challenge: Phase1HabitChallenge) {
        val manager = context.getSystemService(NotificationManager::class.java)
        (0L..20L).forEach { offset ->
            val day = challenge.startDate.plusDays(offset)
            manager.cancel("backlot:habit:" + challenge.id + ":" + day,
                challenge.id.hashCode())
        }
    }

    fun cancelPosted(context: Context, challenge: Phase1HabitChallenge, at: Long) {
        val day = LocalDate.ofInstant(Instant.ofEpochMilli(at), ZoneId.of(challenge.zoneId))
        context.getSystemService(NotificationManager::class.java).cancel(
            "backlot:habit:" + challenge.id + ":" + day,
            challenge.id.hashCode(),
        )
    }
}

/** One real OS alarm per challenge, rearmed after reboot and user actions. */
internal class Phase1HabitAlarmScheduler(context: Context) {
    private val app = context.applicationContext
    private val alarms = app.getSystemService(AlarmManager::class.java)
    private fun pending(id: String, at: Long, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(app, id.hashCode() xor 0x210A,
            Intent(app, Phase1HabitAlarmReceiver::class.java)
                .setAction(Phase1HabitNotification.ACTION)
                .setData(Phase1HabitNotification.uri(id))
                .putExtra(Phase1HabitNotification.ID, id)
                .putExtra(Phase1HabitNotification.AT, at), flags)

    /** Explicit removal after a durable edit/archive/delete, including orphaned IDs. */
    fun cancelChallengePending(id: String) {
        require(id.matches(Regex("[A-Za-z0-9_-]{1,100}")))
        pending(id, 0L, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?.let { existing ->
                alarms.cancel(existing)
                existing.cancel()
            }
    }

    suspend fun reconcile(now: Instant = Instant.now()): Unit =
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
            Phase1HabitNotification.ensureChannels(app)
            val failures = mutableListOf<String>()
            // One challenge's failed alarm must not starve the other alarms.
            Phase1HabitStore(app).load().forEach { challenge ->
                val target = challenge.nextAlertAfter(now)?.toInstant()?.toEpochMilli()
                try {
                    if (target == null) {
                        pending(challenge.id, 0L,
                            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                            ?.let {
                                alarms.cancel(it)
                                it.cancel()
                            }
                    } else {
                        // Replacing the same PendingIntent identity avoids a cancel-before-
                        // schedule gap that could lose the upcoming occurrence on failure.
                        val pi = checkNotNull(pending(challenge.id, target,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                            alarms.canScheduleExactAlarms()
                        if (exact) {
                            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi)
                        } else {
                            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, pi)
                        }
                        Phase1HabitDeliveryLedger.recordSafely(app, challenge.id, target,
                            if (exact) Phase1HabitDeliveryStage.ARMED_EXACT
                            else Phase1HabitDeliveryStage.ARMED_INEXACT)
                    }
                } catch (error: Exception) {
                    android.util.Log.w("BacklotHabit",
                        "Challenge alarm could not be reconciled: ${challenge.id}", error)
                    if (target != null) Phase1HabitDeliveryLedger.recordSafely(
                        app, challenge.id, target, Phase1HabitDeliveryStage.RECONCILE_FAILED)
                    failures.add(challenge.id)
                }
            }
            check(failures.isEmpty()) {
                "Scheduling failed for ${failures.size} challenge(s); retry recovery"
            }
        }
}

internal class Phase1HabitAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Phase1HabitNotification.ACTION) return
        val id = intent.getStringExtra(Phase1HabitNotification.ID)
            ?.takeIf { it.isNotBlank() } ?: return
        val at = intent.getLongExtra(Phase1HabitNotification.AT, 0L)
        if (at <= 0L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val app = context.applicationContext
            try {
                withTimeout(9_000L) {
                    Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                        Phase1HabitDeliveryStage.OS_FIRED)
                    val record = StabilizationPersonalMutationGate.withStableSnapshot(app) {
                        Phase1HabitStore(app).load().firstOrNull { it.id == id }
                            ?.takeIf { Phase1HabitOccurrencePolicy.current(
                                it, at, System.currentTimeMillis()) }
                    }
                    when {
                        record == null -> Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                            Phase1HabitDeliveryStage.STALE_IGNORED)
                        !NotificationManagerCompat.from(app).areNotificationsEnabled() ->
                            Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                                Phase1HabitDeliveryStage.APP_NOTIFICATIONS_BLOCKED)
                        else -> {
                            Phase1HabitNotification.ensureChannels(app)
                            val manager = app.getSystemService(NotificationManager::class.java)
                            val channelBlocked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                manager.getNotificationChannel(
                                    Phase1HabitNotification.channel(requireNotNull(record.alertKind))
                                )?.importance == NotificationManager.IMPORTANCE_NONE
                            if (channelBlocked) {
                                Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                                    Phase1HabitDeliveryStage.CHANNEL_BLOCKED)
                            } else {
                                try {
                                    val day = LocalDate.ofInstant(Instant.ofEpochMilli(at),
                                        ZoneId.of(record.zoneId))
                                    manager.notify("backlot:habit:" + record.id + ":" + day,
                                        record.id.hashCode(),
                                        Phase1HabitNotification.build(app, record, at))
                                    Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                                        Phase1HabitDeliveryStage.NOTIFICATION_POSTED)
                                    if (record.alertKind == Phase1HabitAlertKind.VOICE) {
                                        Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                                            Phase1HabitDeliveryStage.VOICE_REQUESTED)
                                        runCatching {
                                            ContextCompat.startForegroundService(app,
                                                Intent(app, Phase1HabitVoiceService::class.java)
                                                    .putExtra(Phase1HabitNotification.TEXT, record.activity)
                                                    .putExtra(Phase1HabitNotification.ID, record.id)
                                                    .putExtra(Phase1HabitNotification.AT, at))
                                        }.onFailure {
                                            Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                                                Phase1HabitDeliveryStage.VOICE_SERVICE_RESTRICTED)
                                            android.util.Log.w("BacklotHabit",
                                                "Voice playback restricted", it)
                                        }
                                    }
                                } catch (error: Exception) {
                                    Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                                        Phase1HabitDeliveryStage.NOTIFICATION_FAILED)
                                    android.util.Log.w("BacklotHabit",
                                        "Challenge notification was not posted", error)
                                }
                            }
                        }
                    }
                    // Notification or voice delivery must never forge a Done receipt.
                    Phase1HabitAlarmScheduler(app).reconcile()
                }
            } catch (error: Exception) {
                Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                    Phase1HabitDeliveryStage.RECONCILE_FAILED)
                android.util.Log.w("BacklotHabit",
                    "Challenge delivery/recovery needs attention", error)
                // A timed-out goAsync() can lose the next-day rearm; repair it
                // later without extending Android's broadcast execution limit.
                runCatching { Phase1HabitRecoverySafetyNet.enqueue(app) }
                    .onFailure { android.util.Log.w("BacklotHabit",
                        "Challenge recovery work could not be queued", it) }
            } finally { pending.finish() }
        }
    }
}

/** User-selected voice mode: foreground TextToSpeech, up to three announcements. */
internal class Phase1HabitVoiceService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0
    private var currentChallengeId = ""
    private var currentScheduledAt = 0L

    override fun onCreate() {
        super.onCreate()
        currentService = WeakReference(this)
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val request = intent ?: return START_NOT_STICKY
        val activity = request.getStringExtra(Phase1HabitNotification.TEXT)
            ?.takeIf { it.isNotBlank() } ?: return START_NOT_STICKY
        val id = request.getStringExtra(Phase1HabitNotification.ID)
            ?.takeIf { it.isNotBlank() } ?: return START_NOT_STICKY
        val at = request.getLongExtra(Phase1HabitNotification.AT, 0L)
        if (at <= 0L) return START_NOT_STICKY
        currentChallengeId = id
        currentScheduledAt = at
        val current = ++generation
        Phase1HabitNotification.ensureChannels(this)
        try {
            startForeground(0x210A, NotificationCompat.Builder(this,
                Phase1HabitNotification.channel(Phase1HabitAlertKind.VOICE))
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("21-day challenge voice").setContentText(activity)
                .setOngoing(true).build())
        } catch (error: Exception) {
            Phase1HabitDeliveryLedger.recordAsync(this, id, at,
                Phase1HabitDeliveryStage.VOICE_SERVICE_RESTRICTED)
            android.util.Log.w("BacklotHabit", "Foreground speech unavailable", error)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        Phase1HabitDeliveryLedger.recordAsync(this, id, at,
            Phase1HabitDeliveryStage.VOICE_FOREGROUND_STARTED)
        engine?.shutdown()
        engine = TextToSpeech(this) { status ->
            val tts = engine ?: return@TextToSpeech
            if (current != generation || status != TextToSpeech.SUCCESS ||
                tts.setLanguage(Locale.getDefault()) < TextToSpeech.LANG_AVAILABLE) {
                Phase1HabitDeliveryLedger.recordAsync(this, id, at,
                    Phase1HabitDeliveryStage.VOICE_TTS_UNAVAILABLE)
                stopSelf(startId)
                return@TextToSpeech
            }
            // Each completed utterance is acknowledged once. A duplicate or stale
            // TTS callback must never add an extra announcement.
            var lastCompletedIndex = -1
            fun speak(index: Int) {
                if (current != generation) return
                if (index !in 0 until Phase1HabitVoicePlaybackPolicy.REPEAT_COUNT) return
                if (tts.speak("It's time for " + activity, TextToSpeech.QUEUE_FLUSH,
                    null, current.toString() + "-" + index) != TextToSpeech.SUCCESS) {
                    Phase1HabitDeliveryLedger.recordAsync(this, id, at,
                        Phase1HabitDeliveryStage.VOICE_SPEAK_FAILED)
                    stopSelf(startId)
                } else if (index == 0) {
                    Phase1HabitDeliveryLedger.recordAsync(this, id, at,
                        Phase1HabitDeliveryStage.VOICE_SPEAK_STARTED)
                }
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    // Require this exact service generation. A stale callback
                    // from the previous challenge cannot advance current speech.
                    val prefix = "$current-"
                    if (utteranceId?.startsWith(prefix) != true) return
                    val completed = utteranceId.removePrefix(prefix).toIntOrNull() ?: return
                    handler.post {
                        if (current != generation || completed != lastCompletedIndex + 1) return@post
                        lastCompletedIndex = completed
                        val next = Phase1HabitVoicePlaybackPolicy.nextIndex(completed)
                        if (next == null) {
                            // The separately posted challenge notification remains
                            // actionable after the foreground audio stops.
                            stopSelf(startId)
                        } else {
                            handler.postDelayed({
                                if (current == generation && lastCompletedIndex == completed) speak(next)
                            }, Phase1HabitVoicePlaybackPolicy.REPEAT_GAP_MILLIS)
                        }
                    }
                }
                @Deprecated("Android TextToSpeech")
                override fun onError(utteranceId: String?) {
                    Phase1HabitDeliveryLedger.recordAsync(
                        this@Phase1HabitVoiceService, id, at,
                        Phase1HabitDeliveryStage.VOICE_SPEAK_FAILED)
                    handler.post { if (current == generation) stopSelf(startId) }
                }
            })
            speak(0)
        }
        // Watchdog is a safety bound, NOT the desired audible lifetime.
        // Allow full sentences, TTS initialization, and inter-repeat gaps.
        handler.postDelayed({ if (current == generation) stopSelf(startId) },
            Phase1HabitVoicePlaybackPolicy.WATCHDOG_MILLIS)
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        generation++
        handler.removeCallbacksAndMessages(null)
        engine?.stop()
        engine?.shutdown()
        engine = null
        currentChallengeId = ""
        currentScheduledAt = 0L
        if (currentService?.get() === this) currentService = null
        super.onDestroy()
    }

    companion object {
        private var currentService: WeakReference<Phase1HabitVoiceService>? = null

        /** Editing or removing a challenge must stop only that challenge's voice. */
        fun stopChallenge(challengeId: String) {
            currentService?.get()?.let { service ->
                service.handler.post {
                    if (service.currentChallengeId == challengeId) {
                        service.generation++
                        service.engine?.stop()
                        service.stopSelf()
                    }
                }
            }
        }

        /** A Snooze action stops ONLY the exact playing 21-day occurrence. */
        fun stopMatching(challengeId: String, scheduledAt: Long) {
            currentService?.get()?.let { service ->
                service.handler.post {
                    if (service.currentChallengeId == challengeId &&
                        service.currentScheduledAt == scheduledAt
                    ) {
                        service.generation++
                        service.engine?.stop()
                        service.stopSelf()
                    }
                }
            }
        }
    }
}
