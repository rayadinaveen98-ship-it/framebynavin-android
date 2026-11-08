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
    fun build(context: Context, challenge: Phase1HabitChallenge): Notification {
        val kind = requireNotNull(challenge.alertKind)
        val open = Intent(context, MainActivity::class.java)
            .setAction(PersonalDeliveryContract.ACTION_OPEN_PERSONAL)
            .setData(Uri.parse("backlot-habit://open/" + Uri.encode(challenge.id)))
        val pi = PendingIntent.getActivity(context, challenge.id.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, channel(kind))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Today's 21-day activity")
            .setContentText(challenge.activity)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (kind == Phase1HabitAlertKind.QUIET)
                NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setSilent(kind == Phase1HabitAlertKind.QUIET)
            .build()
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

    suspend fun reconcile(now: Instant = Instant.now()): Unit =
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
            Phase1HabitNotification.ensureChannels(app)
            Phase1HabitStore(app).load().forEach { challenge ->
                pending(challenge.id, 0L,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                    ?.let { alarms.cancel(it) }
                val at = challenge.nextAlertAfter(now)?.toInstant()?.toEpochMilli()
                    ?: return@forEach
                val intent = checkNotNull(pending(challenge.id, at,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    alarms.canScheduleExactAlarms()) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
                } else {
                    alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
                }
            }
        }
}

internal class Phase1HabitAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Phase1HabitNotification.ACTION) return
        val id = intent.getStringExtra(Phase1HabitNotification.ID)
            ?.takeIf { it.isNotBlank() } ?: return
        val at = intent.getLongExtra(Phase1HabitNotification.AT, 0L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(9_000L) {
                    val app = context.applicationContext
                    val record = StabilizationPersonalMutationGate.withStableSnapshot(app) {
                        Phase1HabitStore(app).load().firstOrNull { it.id == id }
                            ?.takeIf { Phase1HabitOccurrencePolicy.current(
                                it, at, System.currentTimeMillis()) }
                    }
                    if (record != null &&
                        NotificationManagerCompat.from(app).areNotificationsEnabled()) {
                        Phase1HabitNotification.ensureChannels(app)
                        val day = LocalDate.ofInstant(Instant.ofEpochMilli(at),
                            ZoneId.of(record.zoneId))
                        app.getSystemService(NotificationManager::class.java).notify(
                            "backlot:habit:" + record.id + ":" + day,
                            record.id.hashCode(), Phase1HabitNotification.build(app, record))
                        if (record.alertKind == Phase1HabitAlertKind.VOICE) {
                            runCatching {
                                ContextCompat.startForegroundService(app,
                                    Intent(app, Phase1HabitVoiceService::class.java)
                                        .putExtra(Phase1HabitNotification.TEXT, record.activity))
                            }.onFailure {
                                android.util.Log.w("BacklotHabit", "Voice playback restricted", it)
                            }
                        }
                    }
                    // No synthetic Done: only explicit owner confirmation writes it.
                    Phase1HabitAlarmScheduler(app).reconcile()
                }
            } catch (error: Exception) {
                android.util.Log.w("BacklotHabit", "Challenge alarm needs attention", error)
            } finally { pending.finish() }
        }
    }
}

/** User-selected voice mode: foreground TextToSpeech, up to three announcements. */
internal class Phase1HabitVoiceService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var generation = 0
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val activity = intent?.getStringExtra(Phase1HabitNotification.TEXT)
            ?.takeIf { it.isNotBlank() } ?: return START_NOT_STICKY
        val current = ++generation
        Phase1HabitNotification.ensureChannels(this)
        try {
            startForeground(0x210A, NotificationCompat.Builder(this,
                Phase1HabitNotification.channel(Phase1HabitAlertKind.VOICE))
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("21-day challenge voice").setContentText(activity)
                .setOngoing(true).build())
        } catch (error: Exception) {
            android.util.Log.w("BacklotHabit", "Foreground speech unavailable", error)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        engine?.shutdown()
        engine = TextToSpeech(this) { status ->
            val tts = engine ?: return@TextToSpeech
            if (current != generation || status != TextToSpeech.SUCCESS ||
                tts.setLanguage(Locale.getDefault()) < TextToSpeech.LANG_AVAILABLE) {
                stopSelf(startId)
                return@TextToSpeech
            }
            fun speak(index: Int) {
                if (current != generation) return
                if (index >= 3) { stopSelf(startId); return }
                if (tts.speak("It's time for " + activity, TextToSpeech.QUEUE_FLUSH,
                    null, current.toString() + "-" + index) != TextToSpeech.SUCCESS) {
                    stopSelf(startId)
                }
            }
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    val n = utteranceId?.substringAfterLast('-')?.toIntOrNull() ?: return
                    handler.post { speak(n + 1) }
                }
                @Deprecated("Android TextToSpeech")
                override fun onError(utteranceId: String?) {
                    handler.post { if (current == generation) stopSelf(startId) }
                }
            })
            speak(0)
        }
        handler.postDelayed({ if (current == generation) stopSelf(startId) }, 30_000L)
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        generation++
        engine?.stop()
        engine?.shutdown()
        engine = null
        super.onDestroy()
    }
}
