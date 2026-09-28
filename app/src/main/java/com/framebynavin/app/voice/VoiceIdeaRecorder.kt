package com.framebynavin.app.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.util.UUID

/** Lifecycle state for a single Voice Idea recording session. */
enum class VoiceIdeaRecorderState {
    IDLE,
    RECORDING,
    PAUSED,
}

/** A successfully finalized local recording. The file is the source of truth for the Voice Idea. */
data class VoiceIdeaRecording(
    val localPath: String,
    val durationMillis: Long,
    val mimeType: String = MIME_TYPE,
) {
    companion object {
        const val MIME_TYPE = "audio/mp4"
    }
}

/**
 * App-private Voice Idea recorder.
 *
 * Audio is written into filesDir/voice_ideas. A session records to a temporary file and only
 * becomes a VoiceIdeaRecording after MediaRecorder.stop() succeeds. Transcription and cloud sync
 * are deliberately outside this class so they can fail/retry without touching the original audio.
 *
 * Callers should keep one instance per capture UI and call release() when that UI is disposed.
 */
class VoiceIdeaRecorder(context: Context) {
    private val appContext = context.applicationContext
    private val recordingDirectory = File(appContext.filesDir, DIRECTORY_NAME)

    private var recorder: MediaRecorder? = null
    private var workingFile: File? = null
    private var sessionId: String = ""
    private var startedAtElapsedMillis: Long = 0L
    private var pausedAtElapsedMillis: Long = 0L
    private var totalPausedMillis: Long = 0L

    var state: VoiceIdeaRecorderState = VoiceIdeaRecorderState.IDLE
        private set

    val currentWorkingPath: String?
        get() = workingFile?.absolutePath

    fun start(): Result<Unit> = runCatching {
        check(state == VoiceIdeaRecorderState.IDLE) { "A Voice Idea recording is already active." }
        ensureDirectory()

        val id = UUID.randomUUID().toString()
        val target = File(recordingDirectory, "$FILE_PREFIX$id$WORKING_SUFFIX")
        val mediaRecorder = newMediaRecorder()

        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(AUDIO_BIT_RATE)
                setAudioSamplingRate(AUDIO_SAMPLE_RATE)
                setOutputFile(target.absolutePath)
                prepare()
                start()
            }
        } catch (failure: Throwable) {
            runCatching { mediaRecorder.reset() }
            runCatching { mediaRecorder.release() }
            target.delete()
            throw failure
        }

        recorder = mediaRecorder
        workingFile = target
        sessionId = id
        startedAtElapsedMillis = SystemClock.elapsedRealtime()
        pausedAtElapsedMillis = 0L
        totalPausedMillis = 0L
        state = VoiceIdeaRecorderState.RECORDING
    }

    fun pause(): Result<Unit> = runCatching {
        check(state == VoiceIdeaRecorderState.RECORDING) { "Voice Idea recording is not active." }
        requireRecorder().pause()
        pausedAtElapsedMillis = SystemClock.elapsedRealtime()
        state = VoiceIdeaRecorderState.PAUSED
    }

    fun resume(): Result<Unit> = runCatching {
        check(state == VoiceIdeaRecorderState.PAUSED) { "Voice Idea recording is not paused." }
        requireRecorder().resume()
        val now = SystemClock.elapsedRealtime()
        if (pausedAtElapsedMillis > 0L) totalPausedMillis += (now - pausedAtElapsedMillis).coerceAtLeast(0L)
        pausedAtElapsedMillis = 0L
        state = VoiceIdeaRecorderState.RECORDING
    }

    /**
     * Finalizes the audio. If stop fails, the potentially corrupt working file is deleted rather
     * than being exposed as a saved Voice Idea.
     */
    fun stop(): Result<VoiceIdeaRecording> {
        if (state == VoiceIdeaRecorderState.IDLE) {
            return Result.failure(IllegalStateException("No Voice Idea recording is active."))
        }

        val activeRecorder = recorder
            ?: return Result.failure(IllegalStateException("Voice Idea recorder is unavailable."))
        val source = workingFile
            ?: return Result.failure(IllegalStateException("Voice Idea recording file is unavailable."))

        return runCatching {
            val stoppedAt = SystemClock.elapsedRealtime()
            val pausedTail = if (state == VoiceIdeaRecorderState.PAUSED && pausedAtElapsedMillis > 0L) {
                (stoppedAt - pausedAtElapsedMillis).coerceAtLeast(0L)
            } else 0L

            try {
                activeRecorder.stop()
            } catch (failure: RuntimeException) {
                source.delete()
                throw IllegalStateException("Voice Idea recording could not be finalized safely.", failure)
            } finally {
                runCatching { activeRecorder.reset() }
                runCatching { activeRecorder.release() }
                recorder = null
            }

            check(source.exists() && source.length() > 0L) {
                source.delete()
                "Voice Idea recording produced an empty audio file."
            }

            val finalFile = File(recordingDirectory, "$FILE_PREFIX$sessionId$FINAL_SUFFIX")
            val preservedFile = if (source.renameTo(finalFile)) finalFile else source
            val duration = (stoppedAt - startedAtElapsedMillis - totalPausedMillis - pausedTail)
                .coerceAtLeast(0L)

            resetSession(deleteWorkingFile = false)
            VoiceIdeaRecording(
                localPath = preservedFile.absolutePath,
                durationMillis = duration,
            )
        }.onFailure {
            runCatching { activeRecorder.reset() }
            runCatching { activeRecorder.release() }
            source.delete()
            resetSession(deleteWorkingFile = true)
        }
    }

    /** Discards the current session and its unfinished file. */
    fun cancel() {
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        resetSession(deleteWorkingFile = true)
    }

    /** Releases recorder resources. Active, unfinished capture is discarded intentionally. */
    fun release() = cancel()

    fun elapsedRecordingMillis(nowElapsedMillis: Long = SystemClock.elapsedRealtime()): Long {
        if (state == VoiceIdeaRecorderState.IDLE || startedAtElapsedMillis <= 0L) return 0L
        val pausedTail = if (state == VoiceIdeaRecorderState.PAUSED && pausedAtElapsedMillis > 0L) {
            (nowElapsedMillis - pausedAtElapsedMillis).coerceAtLeast(0L)
        } else 0L
        return (nowElapsedMillis - startedAtElapsedMillis - totalPausedMillis - pausedTail)
            .coerceAtLeast(0L)
    }

    private fun requireRecorder(): MediaRecorder =
        recorder ?: error("Voice Idea recorder is unavailable.")

    private fun ensureDirectory() {
        check(recordingDirectory.exists() || recordingDirectory.mkdirs()) {
            "Voice Idea storage could not be created."
        }
    }

    private fun resetSession(deleteWorkingFile: Boolean) {
        if (deleteWorkingFile) workingFile?.delete()
        workingFile = null
        sessionId = ""
        startedAtElapsedMillis = 0L
        pausedAtElapsedMillis = 0L
        totalPausedMillis = 0L
        state = VoiceIdeaRecorderState.IDLE
    }

    @Suppress("DEPRECATION")
    private fun newMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(appContext) else MediaRecorder()

    companion object {
        private const val DIRECTORY_NAME = "voice_ideas"
        private const val FILE_PREFIX = "voice_idea_"
        private const val WORKING_SUFFIX = ".recording.m4a"
        private const val FINAL_SUFFIX = ".m4a"
        private const val AUDIO_BIT_RATE = 128_000
        private const val AUDIO_SAMPLE_RATE = 44_100
        private const val ABANDONED_WORKING_FILE_AGE_MS = 24L * 60L * 60L * 1000L

        /**
         * Cleans only stale in-progress files left by an interrupted recorder session.
         * Finalized Voice Idea recordings are deliberately never touched here.
         */
        fun cleanupAbandonedWorkingFiles(
            context: Context,
            maxAgeMillis: Long = ABANDONED_WORKING_FILE_AGE_MS,
            nowMillis: Long = System.currentTimeMillis(),
        ): Int {
            val directory = File(context.applicationContext.filesDir, DIRECTORY_NAME)
            if (!directory.isDirectory) return 0

            var deleted = 0
            directory.listFiles().orEmpty().forEach { file ->
                val ageMillis = (nowMillis - file.lastModified()).coerceAtLeast(0L)
                val isAbandonedWorkingFile = file.isFile &&
                    file.name.startsWith(FILE_PREFIX) &&
                    file.name.endsWith(WORKING_SUFFIX, ignoreCase = true) &&
                    ageMillis >= maxAgeMillis
                if (isAbandonedWorkingFile && runCatching { file.delete() }.getOrDefault(false)) {
                    deleted++
                }
            }
            return deleted
        }
    }
}
