package com.framebynavin.app.personal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.framebynavin.app.R
import com.framebynavin.app.reminders.ReminderPianoLoop
import java.lang.ref.WeakReference
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Personal audio only: NEVER uses CreatorTask, creator receivers or Project Pulse.
 * Playback can run only after a Personal notification was visibly posted and
 * its exact token recorded. Failure to start foreground service leaves the
 * original notification and user-action buttons intact.
 *
 * No full-screen intent is requested for Personal (Android/Play eligibility).
 */
internal class PersonalPlaybackService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private val piano = ReminderPianoLoop()
    private var tts: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentId = ""
    private var currentToken = ""
    private var generation = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        currentService = WeakReference(this)
        val channel = NotificationChannel(
            CHANNEL, "Personal reminder playback", NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Short-lived sound or speech during a personal reminder"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        val id = intent.getStringExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID).orEmpty()
        val token = intent.getStringExtra(PersonalDeliveryContract.EXTRA_TOKEN).orEmpty()
        val occurrence = intent.getLongExtra(PersonalDeliveryContract.EXTRA_OCCURRENCE, 0L)
        val fireAt = intent.getLongExtra(PersonalDeliveryContract.EXTRA_FIRE_AT, 0L)
        if (id.isBlank() || token.isBlank() || occurrence <= 0 || fireAt <= 0) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        if (currentId == id && currentToken == token) return START_NOT_STICKY

        // Android requires an immediate foreground handoff. A generic PRIVATE
        // placeholder avoids revealing a title before the async lease check.
        try {
            startForeground(SERVICE_NOTIFICATION_ID, serviceNotification(id, token))
        } catch (failure: Exception) {
            android.util.Log.w("BacklotPersonal", "Playback foreground unavailable", failure)
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        stopHardware()
        currentId = id
        currentToken = token
        val admittedGeneration = ++generation
        scope.launch {
            val reminder = runCatching {
                withContext(Dispatchers.IO) {
                    PersonalReminderStore(applicationContext).load().firstOrNull { it.id == id }
                }
            }.getOrNull()
            if (admittedGeneration != generation ||
                !PersonalPlaybackPolicy.isEligible(
                    reminder, id, occurrence, fireAt, token, System.currentTimeMillis(),
                )) {
                if (admittedGeneration == generation) endPlayback()
                return@launch
            }
            val verified = reminder ?: return@launch
            val maxDuration = if (verified.alert == PersonalAlert.ALARM)
                PersonalPlaybackPolicy.ALARM_MAX_MILLIS else PersonalPlaybackPolicy.VOICE_MAX_MILLIS
            acquireShortWakeLock(maxDuration)
            handler.postDelayed({
                if (generation == admittedGeneration) endPlayback()
            }, maxDuration)
            when (verified.alert) {
                PersonalAlert.ALARM -> playAlarm(admittedGeneration)
                PersonalAlert.VOICE -> speakReminder(verified.title, admittedGeneration)
                PersonalAlert.NOTIFICATION -> endPlayback()
            }
        }
        return START_NOT_STICKY
    }

    private fun serviceNotification(id: String, token: String): Notification {
        val opening = PendingIntent.getActivity(
            this, id.hashCode(),
            Intent(this, PersonalAlertActivity::class.java).apply {
                action = PersonalDeliveryContract.ACTION_OPEN_PERSONAL
                data = PersonalDeliveryContract.actionUri(id, "playback-open", token)
                putExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID, id)
                putExtra(PersonalDeliveryContract.EXTRA_TOKEN, token)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.backlot_v198_notification_monochrome)
            .setContentTitle("Personal reminder")
            .setContentText("Reminder sound in progress")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(opening)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun acquireShortWakeLock(duration: Long) {
        val manager = getSystemService(PowerManager::class.java)
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "backlot:personalAudio")
            .also { it.acquire(duration + 5_000L) }
    }

    private fun playAlarm(expectedGeneration: Int) {
        val alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (alarm == null) { endPlayback(); return }
        val media = MediaPlayer()
        player = media
        try {
            media.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            media.setDataSource(this, alarm)
            media.isLooping = true
            media.setOnPreparedListener { prepared ->
                if (expectedGeneration == generation) {
                    runCatching { prepared.start() }.onFailure { endPlayback() }
                }
            }
            media.setOnErrorListener { _, _, _ ->
                if (expectedGeneration == generation) endPlayback()
                true
            }
            media.prepareAsync()
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                getSystemService(VibratorManager::class.java).defaultVibrator
            else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(
                longArrayOf(0L, 600L, 350L, 600L, 550L), 0,
            ))
        } catch (failure: Exception) {
            android.util.Log.w("BacklotPersonal", "Alarm playback unavailable; notification retained", failure)
            endPlayback()
        }
    }

    private fun speakReminder(title: String, expectedGeneration: Int) {
        // Spoken personal notes are deliberately excluded; the title is frozen
        // for all three repeats. User Done/Snooze or another occurrence ends
        // this generation and silences both TTS and piano immediately.
        val message = "Personal reminder. $title"
        piano.start() // Best-effort original offline music; speech always remains primary.
        try {
            tts = TextToSpeech(this) { status ->
                if (expectedGeneration != generation) return@TextToSpeech
                val engine = tts ?: return@TextToSpeech
                if (status != TextToSpeech.SUCCESS) { endPlayback(); return@TextToSpeech }
                val language = engine.setLanguage(Locale.getDefault())
                if (language == TextToSpeech.LANG_MISSING_DATA ||
                    language == TextToSpeech.LANG_NOT_SUPPORTED) {
                    endPlayback()
                    return@TextToSpeech
                }
                engine.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                val expectedBase = "personal:${currentToken}:$expectedGeneration"
                var activeId: String? = null

                fun play(index: Int) {
                    if (expectedGeneration != generation) return
                    val utteranceId = "$expectedBase:$index"
                    activeId = utteranceId
                    piano.setSpeaking(true)
                    val result = engine.speak(
                        message,
                        if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                        null,
                        utteranceId,
                    )
                    if (result != TextToSpeech.SUCCESS) endPlayback()
                }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        handler.post {
                            if (expectedGeneration != generation || activeId != utteranceId) return@post
                            activeId = null
                            piano.setSpeaking(false)
                            val completed = utteranceId?.substringAfterLast(':')?.toIntOrNull()
                                ?: run { endPlayback(); return@post }
                            val next = PersonalPlaybackPolicy.nextVoiceIndex(completed)
                            if (next == null) endPlayback()
                            else handler.postDelayed({
                                if (expectedGeneration == generation && activeId == null) play(next)
                            }, PersonalPlaybackPolicy.VOICE_REPEAT_GAP_MILLIS)
                        }
                    }
                    @Deprecated("Required by Android TextToSpeech callback")
                    override fun onError(utteranceId: String?) {
                        handler.post {
                            if (expectedGeneration == generation && activeId == utteranceId) endPlayback()
                        }
                    }
                })
                play(0)
            }
        } catch (failure: Exception) {
            android.util.Log.w("BacklotPersonal", "Personal voice unavailable; notification retained", failure)
            endPlayback()
        }
    }

    private fun requestStop(id: String, token: String) {
        // Response receivers run on IO; audio teardown must occur on Main.
        handler.post { if (id == currentId && token == currentToken) endPlayback() }
    }

    private fun endPlayback() {
        ++generation
        currentId = ""
        currentToken = ""
        stopHardware()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopHardware() {
        handler.removeCallbacksAndMessages(null)
        piano.stop()
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        wakeLock?.let { runCatching { if (it.isHeld) it.release() } }
        wakeLock = null
    }

    override fun onDestroy() {
        ++generation
        stopHardware()
        currentId = ""
        currentToken = ""
        if (currentService?.get() === this) currentService = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.framebynavin.app.personal.PLAYBACK_START"
        private const val CHANNEL = "backlot_personal_playback_v1"
        private const val SERVICE_NOTIFICATION_ID = 7323
        private var currentService: WeakReference<PersonalPlaybackService>? = null

        fun tryStart(context: Context, reminder: PersonalReminder): Boolean {
            if (reminder.alert == PersonalAlert.NOTIFICATION || reminder.presentedToken != reminder.pendingToken)
                return false
            return runCatching {
                ContextCompat.startForegroundService(
                    context.applicationContext,
                    Intent(context, PersonalPlaybackService::class.java)
                        .setAction(ACTION_START)
                        .putExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID, reminder.id)
                        .putExtra(PersonalDeliveryContract.EXTRA_OCCURRENCE, reminder.pendingOccurrenceMillis)
                        .putExtra(PersonalDeliveryContract.EXTRA_FIRE_AT, reminder.pendingFireAtMillis)
                        .putExtra(PersonalDeliveryContract.EXTRA_TOKEN, reminder.pendingToken),
                )
                true
            }.onFailure {
                android.util.Log.w("BacklotPersonal", "Background media playback restricted; notification retained", it)
            }.getOrDefault(false)
        }

        fun stopMatching(id: String, token: String) {
            currentService?.get()?.requestStop(id, token)
        }

        /** Editing/pausing/deleting an entire reminder also silences its audio. */
        fun stopForReminder(id: String) {
            currentService?.get()?.let { service ->
                service.handler.post {
                    if (service.currentId == id) service.endPlayback()
                }
            }
        }
    }
}
