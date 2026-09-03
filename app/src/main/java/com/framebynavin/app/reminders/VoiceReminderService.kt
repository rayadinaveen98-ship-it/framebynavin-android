package com.framebynavin.app.reminders

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.data.CreatorTask
import java.util.Locale

class VoiceReminderService : Service() {
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val base = intent?.toTask() ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val task = base.copy(progress = intent.getIntExtra(ReminderConstants.EXTRA_PROGRESS, base.progress))

        ReminderNotifications.ensureChannel(this)
        startForeground(
            notificationId(task.id),
            NotificationCompat.Builder(this, ReminderConstants.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle(task.title)
                .setContentText("FrameByNavin voice reminder · ${task.dueLabel}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setOngoing(false)
                .build()
        )

        speak(task)
        handler.postDelayed({ stopSelf() }, 20_000L)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    private fun speak(task: CreatorTask) {
        tts?.shutdown()
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                stopSelf()
                return@TextToSpeech
            }
            tts?.language = Locale.getDefault()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { stopSelf() }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { stopSelf() }
            })
            val urgency = when (task.priority) {
                com.framebynavin.app.data.TaskPriority.NORMAL -> "reminder"
                com.framebynavin.app.data.TaskPriority.IMPORTANT -> "important creator reminder"
                com.framebynavin.app.data.TaskPriority.CRITICAL -> "critical creator deadline"
            }
            tts?.speak(
                "FrameByNavin. ${task.title}. This is your $urgency. Current stage: ${stageForVoice(task.progress)}.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "framebynavin-smart-${task.id}"
            )
        }
    }

    companion object {
        fun start(context: Context, task: CreatorTask) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, VoiceReminderService::class.java)
                    .putTask(task)
                    .putExtra(ReminderConstants.EXTRA_PROGRESS, task.progress)
            )
        }

        private fun notificationId(taskId: String): Int = taskId.hashCode() xor 0x5600
    }
}

private fun stageForVoice(progress: Int): String = when {
    progress >= 95 -> "upload and final check"
    progress >= 85 -> "thumbnail and metadata"
    progress >= 70 -> "editing"
    progress >= 55 -> "voice recording"
    progress >= 40 -> "script"
    progress >= 20 -> "research"
    else -> "planning"
}
