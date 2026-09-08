package com.framebynavin.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.lang.ref.WeakReference
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskPriority
import java.util.Locale

class VoiceReminderService : Service() {
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentTask: CreatorTask? = null
    private var currentToken: String = ""
    private var currentStartId: Int = 0
    private var speechGeneration = 0

    override fun onCreate() {
        super.onCreate()
        activeInstance = WeakReference(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val task = intent?.toTask() ?: run {
            if (currentTask == null) stopSelfResult(startId)
            return START_NOT_STICKY
        }
        val token = intent.getStringExtra(ReminderConstants.EXTRA_OCCURRENCE_ID).orEmpty()
        if (!ReminderOccurrenceStore(applicationContext).matches(task, token)) {
            if (currentTask == null) stopSelfResult(startId)
            return START_NOT_STICKY
        }
        val previous = currentTask
        val previousToken = currentToken
        if (previous != null && (previous.id != task.id || previousToken != token) &&
            ReminderOccurrenceStore(applicationContext).matches(previous, previousToken)) {
            // A single Android service can ring only one task at a time. Preserve the displaced
            // task as an actionable notification rather than silently dropping its reminder.
            runCatching { ReminderNotifications.show(applicationContext, previous,
                stageLabel = "Another reminder is ringing", occurrenceId = previousToken) }
        }
        currentToken = token
        currentStartId = startId
        currentTask = task
        handler.removeCallbacksAndMessages(null)
        ensureChannel()
        if (previous != null && previousToken != token) {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            wakeLock = null
        }
        acquireWakeLock(task)
        startForeground(notificationId(task.id), buildNotification(task))
        if (previous != null && previous.id != task.id)
            getSystemService(NotificationManager::class.java).cancel(notificationId(previous.id))
        beginSpeechCycle(task)
        return START_NOT_STICKY
    }

    private fun beginSpeechCycle(task: CreatorTask) {
        tts?.stop()
        tts?.shutdown()
        val token = currentToken
        val generation = ++speechGeneration
        tts = TextToSpeech(this) { status ->
            if (!isCurrent(task.id, token) || generation != speechGeneration) return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                stopVoiceService(task.id, token)
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            engine.language = Locale.getDefault()
            VoicePersonaEngine.apply(engine, task.voicePersona)

            val count = task.voiceRepeatCount.coerceIn(1, 3)
            val interval = task.voiceRepeatIntervalSeconds.coerceIn(5, 60) * 1000L
            repeat(count) { index ->
                handler.postDelayed({
                    if (isCurrent(task.id, token)) speakOnce(task, index)
                }, index * interval)
            }
            val totalWindow = ((count - 1) * interval + 18_000L).coerceAtMost(150_000L)
            handler.postDelayed({ stopVoiceService(task.id, token) }, totalWindow)
        }
    }

    private fun speakOnce(task: CreatorTask, index: Int) {
        val urgency = when (task.priority) {
            TaskPriority.NORMAL -> "reminder"
            TaskPriority.IMPORTANT -> "important creator reminder"
            TaskPriority.CRITICAL -> "critical creator deadline"
        }
        val stage = CreatorWorkflowEngine.currentStage(task).label
        val text = buildString {
            append("FrameByNavin. ${task.title}. This is your $urgency.")
            append(" Current stage: $stage.")
            if (task.notes.isNotBlank() && index == 0) append(" ${task.notes}")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "framebynavin-voice-${task.id}-$index")
    }

    private fun buildNotification(task: CreatorTask): android.app.Notification {
        val token = currentToken
        val fullScreen = PendingIntent.getActivity(
            this,
            task.id.hashCode() xor 0x5601,
            Intent(this, VoiceReminderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                data = android.net.Uri.parse("framebynavin://voice/${android.net.Uri.encode(task.id)}/${android.net.Uri.encode(token)}")
                putTask(task)
                putExtra(ReminderConstants.EXTRA_OCCURRENCE_ID, token)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, ReminderConstants.VOICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(task.title)
            .setContentText("Voice reminder · ${task.dueLabel}")
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
                ReminderConstants.VOICE_CHANNEL_ID,
                "Voice reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Spoken FrameByNavin reminders with a dedicated voice screen"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock(task: CreatorTask) {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(PowerManager::class.java)
        val maxWindow = ((task.voiceRepeatCount.coerceIn(1, 3) - 1) * task.voiceRepeatIntervalSeconds.coerceIn(5, 60) * 1000L + 30_000L)
            .coerceAtMost(180_000L)
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FrameByNavin:VoiceReminder").apply { acquire(maxWindow) }
    }

    private fun isCurrent(taskId: String, token: String): Boolean =
        currentTask?.id == taskId && currentToken == token && token.isNotBlank()

    private fun stopVoiceService(taskId: String? = null, token: String? = null) {
        if (taskId != null && (currentTask?.id != taskId || (token != null && currentToken != token))) return
        currentTask = null
        currentToken = ""
        speechGeneration++
        runCatching { tts?.stop() }
        tts?.shutdown()
        tts = null
        handler.removeCallbacksAndMessages(null)
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelfResult(currentStartId)
    }

    override fun onDestroy() {
        currentTask = null
        currentToken = ""
        speechGeneration++
        handler.removeCallbacksAndMessages(null)
        runCatching { tts?.stop() }
        tts?.shutdown()
        tts = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        if (activeInstance?.get() === this) activeInstance = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var activeInstance: WeakReference<VoiceReminderService>? = null
        fun start(context: Context, task: CreatorTask, occurrenceId: String? = null) {
            val token = occurrenceId ?: ReminderOccurrenceStore(context).issue(task)
            ContextCompat.startForegroundService(context, Intent(context, VoiceReminderService::class.java)
                .putTask(task).putExtra(ReminderConstants.EXTRA_OCCURRENCE_ID, token))
        }

        /** Unconditional stop is reserved for explicit global teardown, such as restore. */
        fun stop(context: Context) { context.stopService(Intent(context, VoiceReminderService::class.java)) }
        fun stop(context: Context, taskId: String, occurrenceId: String? = null) {
            Handler(Looper.getMainLooper()).post { activeInstance?.get()?.stopVoiceService(taskId, occurrenceId) }
        }
        fun notificationId(taskId: String): Int = taskId.hashCode() xor 0x5600

        fun totalWindowMillis(task: CreatorTask): Long {
            val count = task.voiceRepeatCount.coerceIn(1, 3)
            val interval = task.voiceRepeatIntervalSeconds.coerceIn(5, 60) * 1000L
            return ((count - 1) * interval + 18_000L).coerceAtMost(150_000L)
        }
    }
}
