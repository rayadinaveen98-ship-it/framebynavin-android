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
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.cloud.CloudLocalStore
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskPriority
import java.lang.ref.WeakReference
import java.util.Locale

class VoiceReminderService : Service() {
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentTask: CreatorTask? = null
    private var currentToken: String = ""
    private var currentCreatorName: String = "Creator"
    private var currentStartId: Int = 0
    private var speechGeneration = 0
    private var frozenSpeechText = ""
    private var currentUtteranceId: String? = null
    private var repeatCount = 1
    private var repeatIntervalMillis = 5_000L

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
        val sameOccurrence = previous?.id == task.id && previousToken == token
        val forceRestart = intent.getBooleanExtra(EXTRA_FORCE_RESTART, false)

        // Duplicate delivery of the same occurrence must never restart a sentence that is already
        // speaking. Explicit Replay is the only same-occurrence path allowed to restart speech.
        if (sameOccurrence && !forceRestart && tts != null) return START_NOT_STICKY

        if (previous != null && !sameOccurrence &&
            ReminderOccurrenceStore(applicationContext).matches(previous, previousToken)) {
            // A single Android service can ring only one task at a time. Preserve the displaced
            // task as an actionable notification rather than silently dropping its reminder.
            runCatching {
                ReminderNotifications.show(
                    applicationContext,
                    previous,
                    stageLabel = "Another reminder is ringing",
                    occurrenceId = previousToken,
                )
            }
        }

        currentToken = token
        currentStartId = startId
        currentTask = task
        currentCreatorName = resolveCreatorName()
        handler.removeCallbacksAndMessages(null)
        ensureChannel()
        if (previous != null && !sameOccurrence) {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            wakeLock = null
        }
        acquireWakeLock(task)
        startForeground(notificationId(task.id), buildNotification(task))
        if (previous != null && previous.id != task.id) {
            getSystemService(NotificationManager::class.java).cancel(notificationId(previous.id))
        }
        beginSpeechCycle(task)
        return START_NOT_STICKY
    }

    private fun resolveCreatorName(): String {
        val localCloud = CloudLocalStore(applicationContext)
        val session = runCatching { localCloud.loadSession() }.getOrNull()
        val cachedProfile = runCatching { localCloud.loadCreatorProfile() }.getOrNull()
            ?.takeIf { profile -> session != null && profile.userId == session.userId }
        val creatorProfileName = runCatching {
            CreatorOsSettingsStore(applicationContext).snapshot().creatorProfile.displayName
        }.getOrDefault("")
        return VoiceGreetingBuilder.preferredName(
            googleAccountName = session?.displayName.orEmpty(),
            cachedAccountName = cachedProfile?.displayName.orEmpty(),
            creatorProfileName = creatorProfileName,
        )
    }

    private fun beginSpeechCycle(task: CreatorTask) {
        runCatching { tts?.stop() }
        tts?.shutdown()
        tts = null
        currentUtteranceId = null

        val token = currentToken
        val generation = ++speechGeneration
        repeatCount = task.voiceRepeatCount.coerceIn(1, 3)
        repeatIntervalMillis = task.voiceRepeatIntervalSeconds.coerceIn(5, 60) * 1000L
        frozenSpeechText = buildSpeechText(task)

        tts = TextToSpeech(this) { status ->
            if (!isCurrent(task.id, token) || generation != speechGeneration) return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) {
                stopVoiceService(task.id, token)
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            engine.language = Locale.getDefault()
            VoicePersonaEngine.apply(engine, task.voicePersona)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    handler.post {
                        if (!isCurrent(task.id, token) || generation != speechGeneration) return@post
                        if (utteranceId.isNullOrBlank() || currentUtteranceId != utteranceId) return@post
                        currentUtteranceId = null
                        val completedIndex = utteranceIndex(utteranceId)
                        val nextIndex = completedIndex + 1
                        if (nextIndex < repeatCount) {
                            // Repeat timing starts only after the complete previous sentence has
                            // finished. This prevents QUEUE_FLUSH from cutting long reminders.
                            handler.postDelayed(
                                { speakFrozen(task, token, generation, nextIndex) },
                                repeatIntervalMillis,
                            )
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handler.post {
                        if (isCurrent(task.id, token) && generation == speechGeneration) {
                            stopVoiceService(task.id, token)
                        }
                    }
                }
            })

            speakFrozen(task, token, generation, 0)
            handler.postDelayed(
                { stopVoiceService(task.id, token) },
                totalWindowMillis(task),
            )
        }
    }

    private fun buildSpeechText(task: CreatorTask): String {
        val urgency = when (task.priority) {
            TaskPriority.NORMAL -> "reminder"
            TaskPriority.IMPORTANT -> "important creator reminder"
            TaskPriority.CRITICAL -> "critical creator deadline"
        }
        val stage = CreatorWorkflowEngine.currentStage(task).label
        val reminder = VoicePersonaEngine.reminderText(
            persona = task.voicePersona,
            title = task.title,
            urgency = urgency,
            stage = stage,
            notes = task.notes,
            includeNotes = true,
        )
        return "${VoiceGreetingBuilder.greeting(currentCreatorName)} $reminder"
    }

    private fun speakFrozen(task: CreatorTask, token: String, generation: Int, index: Int) {
        if (!isCurrent(task.id, token) || generation != speechGeneration) return
        if (currentUtteranceId != null) return
        val engine = tts ?: return
        val utteranceId = "backlot-voice-${task.id}-${token.hashCode()}-$generation-$index"
        currentUtteranceId = utteranceId
        val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val result = engine.speak(frozenSpeechText, queueMode, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            currentUtteranceId = null
            stopVoiceService(task.id, token)
        }
    }

    private fun utteranceIndex(utteranceId: String): Int =
        utteranceId.substringAfterLast('-').toIntOrNull() ?: Int.MAX_VALUE

    private fun buildNotification(task: CreatorTask): android.app.Notification {
        val token = currentToken
        val fullScreen = PendingIntent.getActivity(
            this,
            task.id.hashCode() xor 0x5601,
            Intent(this, VoiceReminderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                data = android.net.Uri.parse(
                    "framebynavin://voice/${android.net.Uri.encode(task.id)}/${android.net.Uri.encode(token)}"
                )
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
                description = "Spoken Backlot reminders with a dedicated voice screen"
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
        val maxWindow = (totalWindowMillis(task) + 10_000L).coerceAtMost(310_000L)
        wakeLock = power.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Backlot:VoiceReminder",
        ).apply { acquire(maxWindow) }
    }

    private fun isCurrent(taskId: String, token: String): Boolean =
        currentTask?.id == taskId && currentToken == token && token.isNotBlank()

    private fun stopVoiceService(taskId: String? = null, token: String? = null) {
        if (taskId != null &&
            (currentTask?.id != taskId || (token != null && currentToken != token))) return
        currentTask = null
        currentToken = ""
        currentCreatorName = "Creator"
        frozenSpeechText = ""
        currentUtteranceId = null
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
        currentCreatorName = "Creator"
        frozenSpeechText = ""
        currentUtteranceId = null
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
        private const val EXTRA_FORCE_RESTART = "voice_force_restart"
        private const val MAX_UTTERANCE_WINDOW_MILLIS = 45_000L
        private const val STOP_GRACE_MILLIS = 15_000L
        @Volatile private var activeInstance: WeakReference<VoiceReminderService>? = null

        fun start(
            context: Context,
            task: CreatorTask,
            occurrenceId: String? = null,
            forceRestart: Boolean = false,
        ) {
            val token = occurrenceId ?: ReminderOccurrenceStore(context).issue(task)
            ContextCompat.startForegroundService(
                context,
                Intent(context, VoiceReminderService::class.java)
                    .putTask(task)
                    .putExtra(ReminderConstants.EXTRA_OCCURRENCE_ID, token)
                    .putExtra(EXTRA_FORCE_RESTART, forceRestart),
            )
        }

        /** Unconditional stop is reserved for explicit global teardown, such as restore. */
        fun stop(context: Context) {
            context.stopService(Intent(context, VoiceReminderService::class.java))
        }

        fun stop(context: Context, taskId: String, occurrenceId: String? = null) {
            Handler(Looper.getMainLooper()).post {
                activeInstance?.get()?.stopVoiceService(taskId, occurrenceId)
            }
        }

        fun notificationId(taskId: String): Int = taskId.hashCode() xor 0x5600

        fun totalWindowMillis(task: CreatorTask): Long {
            val count = task.voiceRepeatCount.coerceIn(1, 3)
            val interval = task.voiceRepeatIntervalSeconds.coerceIn(5, 60) * 1000L
            return (
                count * MAX_UTTERANCE_WINDOW_MILLIS +
                    (count - 1) * interval +
                    STOP_GRACE_MILLIS
                ).coerceAtMost(300_000L)
        }
    }
}
