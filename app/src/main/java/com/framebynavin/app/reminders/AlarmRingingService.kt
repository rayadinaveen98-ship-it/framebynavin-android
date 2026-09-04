package com.framebynavin.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderAlertType
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.VoicePersona
import java.util.Locale

class AlarmRingingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentTask: CreatorTask? = null
    private var currentSmartStage: SmartEscalationScheduler.Stage? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val task = intent?.toTask() ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val smartStage = runCatching {
            SmartEscalationScheduler.Stage.valueOf(intent.getStringExtra(ReminderConstants.EXTRA_ESCALATION_STAGE).orEmpty())
        }.getOrNull()
        currentTask = task
        currentSmartStage = smartStage

        handler.removeCallbacksAndMessages(null)
        stopAlertHardware()
        acquireWakeLock()
        startForeground(notificationId(task.id), buildNotification(task, smartStage))
        startAlarmSound(task)
        startVibration()
        if (task.voiceEnabled) startVoice(task)

        val timeoutMillis = task.alarmTimeoutSeconds.coerceIn(30, 300) * 1000L
        handler.postDelayed({
            val endingTask = currentTask
            val endingStage = currentSmartStage
            stopAlertHardware()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            if (endingTask != null && endingTask.reminderMode == ReminderMode.SMART) {
                val terminal = endingStage == SmartEscalationScheduler.Stage.CRITICAL ||
                    (endingStage == SmartEscalationScheduler.Stage.ALARM && endingTask.priority != TaskPriority.CRITICAL)
                if (terminal) SmartEscalationScheduler(applicationContext).finishSession(endingTask.id)
            }
        }, timeoutMillis)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopAlertHardware()
        currentTask = null
        currentSmartStage = null
        super.onDestroy()
    }

    private fun buildNotification(task: CreatorTask, smartStage: SmartEscalationScheduler.Stage?): android.app.Notification {
        val fullScreen = PendingIntent.getActivity(
            this,
            task.id.hashCode() xor 0x7150,
            Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putTask(task)
                smartStage?.let { putExtra(ReminderConstants.EXTRA_ESCALATION_STAGE, it.name) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, ReminderConstants.ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(task.title)
            .setContentText("FrameByNavin alarm · ${task.dueLabel}")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderConstants.ALARM_CHANNEL_ID,
                "Creator alarms",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Full-screen FrameByNavin creator alarms"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startAlarmSound(task: CreatorTask) {
        player?.release()
        val chosen = task.alarmSoundUri.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        player = createLoopingPlayer(chosen ?: defaultUri) ?: createLoopingPlayer(defaultUri)
        player?.start()
    }

    private fun createLoopingPlayer(uri: Uri?): MediaPlayer? {
        if (uri == null) return null
        return runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingingService, uri)
                isLooping = true
                prepare()
            }
        }.getOrNull()
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 350, 800, 350)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun startVoice(task: CreatorTask) {
        tts?.shutdown()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.let { VoicePersonaEngine.apply(it, task.voicePersona) }
                val urgency = when (task.priority) {
                    TaskPriority.NORMAL -> "reminder"
                    TaskPriority.IMPORTANT -> "important reminder"
                    TaskPriority.CRITICAL -> "critical deadline"
                }
                tts?.speak(
                    "FrameByNavin. ${task.title}. This is your $urgency.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "framebynavin-${task.id}",
                )
            }
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(PowerManager::class.java)
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FrameByNavin:NativeAlarm").apply {
            acquire(6 * 60 * 1000L)
        }
    }

    private fun stopAlertHardware() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    companion object {
        fun start(context: Context, task: CreatorTask, smartStage: SmartEscalationScheduler.Stage? = null) {
            val intent = Intent(context, AlarmRingingService::class.java).putTask(task)
            smartStage?.let { intent.putExtra(ReminderConstants.EXTRA_ESCALATION_STAGE, it.name) }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }

        fun notificationId(taskId: String): Int = taskId.hashCode() xor 0x7100
    }
}

internal fun Intent.putTask(task: CreatorTask): Intent =
    putExtra(ReminderConstants.EXTRA_TASK_ID, task.id)
        .putExtra(ReminderConstants.EXTRA_TITLE, task.title)
        .putExtra(ReminderConstants.EXTRA_PLATFORM, task.platform)
        .putExtra(ReminderConstants.EXTRA_CONTENT_TYPE, task.contentType)
        .putExtra(ReminderConstants.EXTRA_DUE_LABEL, task.dueLabel)
        .putExtra(ReminderConstants.EXTRA_DUE_AT, task.dueAtMillis)
        .putExtra(ReminderConstants.EXTRA_PRIORITY, task.priority.name)
        .putExtra(ReminderConstants.EXTRA_PROGRESS, task.progress)
        .putExtra(ReminderConstants.EXTRA_NOTES, task.notes)
        .putExtra(ReminderConstants.EXTRA_SCHEDULED_AT, task.reminderAtMillis)
        .putExtra(ReminderConstants.EXTRA_ALERT_TYPE, task.alertType.name)
        .putExtra(ReminderConstants.EXTRA_ALARM_SOUND_URI, task.alarmSoundUri)
        .putExtra(ReminderConstants.EXTRA_VOICE_ENABLED, task.voiceEnabled)
        .putExtra(ReminderConstants.EXTRA_REMINDER_MODE, task.reminderMode.name)
        .putExtra(ReminderConstants.EXTRA_VOICE_PERSONA, task.voicePersona.name)
        .putExtra(ReminderConstants.EXTRA_VOICE_REPEAT_COUNT, task.voiceRepeatCount)
        .putExtra(ReminderConstants.EXTRA_VOICE_REPEAT_INTERVAL, task.voiceRepeatIntervalSeconds)
        .putExtra(ReminderConstants.EXTRA_ALARM_TIMEOUT_SECONDS, task.alarmTimeoutSeconds)

internal fun Intent.toTask(): CreatorTask? {
    val taskId = getStringExtra(ReminderConstants.EXTRA_TASK_ID) ?: return null
    val priority = runCatching {
        TaskPriority.valueOf(getStringExtra(ReminderConstants.EXTRA_PRIORITY).orEmpty())
    }.getOrDefault(TaskPriority.IMPORTANT)
    val alertType = runCatching {
        ReminderAlertType.valueOf(getStringExtra(ReminderConstants.EXTRA_ALERT_TYPE).orEmpty())
    }.getOrDefault(ReminderAlertType.NOTIFICATION)
    val reminderMode = runCatching {
        ReminderMode.valueOf(getStringExtra(ReminderConstants.EXTRA_REMINDER_MODE).orEmpty())
    }.getOrDefault(if (alertType == ReminderAlertType.ALARM) ReminderMode.ALARM else ReminderMode.SIMPLE)
    val persona = runCatching {
        VoicePersona.valueOf(getStringExtra(ReminderConstants.EXTRA_VOICE_PERSONA).orEmpty())
    }.getOrDefault(VoicePersona.WARM)

    return CreatorTask(
        id = taskId,
        title = getStringExtra(ReminderConstants.EXTRA_TITLE).orEmpty().ifBlank { "FrameByNavin reminder" },
        platform = getStringExtra(ReminderConstants.EXTRA_PLATFORM).orEmpty(),
        contentType = getStringExtra(ReminderConstants.EXTRA_CONTENT_TYPE).orEmpty(),
        dueLabel = getStringExtra(ReminderConstants.EXTRA_DUE_LABEL).orEmpty(),
        dueAtMillis = getLongExtra(ReminderConstants.EXTRA_DUE_AT, 0L),
        progress = getIntExtra(ReminderConstants.EXTRA_PROGRESS, 0).coerceIn(0, 100),
        reminderEnabled = true,
        reminderAtMillis = getLongExtra(ReminderConstants.EXTRA_SCHEDULED_AT, 0L),
        priority = priority,
        notes = getStringExtra(ReminderConstants.EXTRA_NOTES).orEmpty(),
        alertType = alertType,
        alarmSoundUri = getStringExtra(ReminderConstants.EXTRA_ALARM_SOUND_URI).orEmpty(),
        voiceEnabled = getBooleanExtra(ReminderConstants.EXTRA_VOICE_ENABLED, reminderMode == ReminderMode.VOICE || reminderMode == ReminderMode.SMART),
        smartEscalationEnabled = reminderMode == ReminderMode.SMART,
        reminderMode = reminderMode,
        voicePersona = persona,
        voiceRepeatCount = getIntExtra(ReminderConstants.EXTRA_VOICE_REPEAT_COUNT, 3).coerceIn(1, 3),
        voiceRepeatIntervalSeconds = getIntExtra(ReminderConstants.EXTRA_VOICE_REPEAT_INTERVAL, 10).coerceIn(5, 60),
        alarmTimeoutSeconds = getIntExtra(ReminderConstants.EXTRA_ALARM_TIMEOUT_SECONDS, 120).coerceIn(30, 300),
    )
}
