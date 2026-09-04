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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val task = intent?.toTask() ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        currentTask = task
        handler.removeCallbacksAndMessages(null)
        ensureChannel()
        acquireWakeLock(task)
        startForeground(notificationId(task.id), buildNotification(task))
        beginSpeechCycle(task)
        return START_NOT_STICKY
    }

    private fun beginSpeechCycle(task: CreatorTask) {
        tts?.stop()
        tts?.shutdown()
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                stopVoiceService()
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            engine.language = Locale.getDefault()
            VoicePersonaEngine.apply(engine, task.voicePersona)

            val count = task.voiceRepeatCount.coerceIn(1, 3)
            val interval = task.voiceRepeatIntervalSeconds.coerceIn(5, 60) * 1000L
            repeat(count) { index ->
                handler.postDelayed({
                    if (currentTask?.id == task.id) speakOnce(task, index)
                }, index * interval)
            }
            val totalWindow = ((count - 1) * interval + 18_000L).coerceAtMost(150_000L)
            handler.postDelayed({ stopVoiceService() }, totalWindow)
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
        val fullScreen = PendingIntent.getActivity(
            this,
            task.id.hashCode() xor 0x5601,
            Intent(this, VoiceReminderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putTask(task)
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

    private fun stopVoiceService() {
        currentTask = null
        runCatching { tts?.stop() }
        tts?.shutdown()
        tts = null
        handler.removeCallbacksAndMessages(null)
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        currentTask = null
        handler.removeCallbacksAndMessages(null)
        runCatching { tts?.stop() }
        tts?.shutdown()
        tts = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        fun start(context: Context, task: CreatorTask) {
            ContextCompat.startForegroundService(context, Intent(context, VoiceReminderService::class.java).putTask(task))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VoiceReminderService::class.java))
        }

        fun notificationId(taskId: String): Int = taskId.hashCode() xor 0x5600

        fun totalWindowMillis(task: CreatorTask): Long {
            val count = task.voiceRepeatCount.coerceIn(1, 3)
            val interval = task.voiceRepeatIntervalSeconds.coerceIn(5, 60) * 1000L
            return ((count - 1) * interval + 18_000L).coerceAtMost(150_000L)
        }
    }
}
