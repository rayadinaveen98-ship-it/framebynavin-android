package com.framebynavin.app.voice

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.framebynavin.app.data.IdeaTranscriptionState
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface SavedVoiceIdeaTranscriberListener {
    fun onTranscriptionState(state: IdeaTranscriptionState, error: String = "")
    fun onTranscriptReady(text: String, confidence: Float?)
}

/**
 * Transcribes an already-saved Voice Idea without ever replacing its original M4A recording.
 *
 * The recorder stores AAC in an MPEG-4 container, while RecognizerIntent.EXTRA_AUDIO_SOURCE
 * expects raw audio. We therefore decode to an app-cache PCM16 file first. Android 13+ can then
 * pass that PCM file to the speech recognizer. The exact request is checked with
 * SpeechRecognizer.checkRecognitionSupport before startListening; if the service cannot verify
 * support we stop rather than risk EXTRA_AUDIO_SOURCE being ignored and the microphone opening.
 */
class SavedVoiceIdeaTranscriber(
    context: Context,
    private val listener: SavedVoiceIdeaTranscriberListener,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var activeJob: Job? = null
    private var recognizer: SpeechRecognizer? = null
    private var activeAudioSource: ParcelFileDescriptor? = null
    private var activePcmFile: File? = null
    private var session = 0L
    private var destroyed = false

    fun start(localRecordingPath: String, language: IdeaVoiceLanguage = IdeaVoiceLanguage.AUTO) {
        if (destroyed) return
        val token = ++session
        cancelActiveResources()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            listener.onTranscriptionState(
                IdeaTranscriptionState.UNAVAILABLE,
                "Saved recording transcription needs Android 13 or newer.",
            )
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            listener.onTranscriptionState(
                IdeaTranscriptionState.UNAVAILABLE,
                "Speech recognition is not available on this phone.",
            )
            return
        }

        val source = File(localRecordingPath)
        if (!source.exists() || !source.isFile || source.length() <= 0L) {
            listener.onTranscriptionState(
                IdeaTranscriptionState.FAILED,
                "The original Voice Idea recording could not be found.",
            )
            return
        }

        listener.onTranscriptionState(IdeaTranscriptionState.PENDING)
        activeJob = scope.launch {
            try {
                val decoded = withContext(Dispatchers.IO) { PcmVoiceDecoder.decode(appContext, source) }
                if (token != session || destroyed) {
                    decoded.file.delete()
                    return@launch
                }
                activePcmFile = decoded.file
                beginSupportCheck(token, decoded, language)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (token == session && !destroyed) {
                    finishFailure(
                        token,
                        IdeaTranscriptionState.FAILED,
                        error.message?.takeIf { it.isNotBlank() }
                            ?: "The saved recording could not be prepared for transcription.",
                    )
                }
            }
        }
    }

    fun cancel() {
        session++
        cancelActiveResources()
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        session++
        cancelActiveResources()
        scope.cancel()
    }

    private fun beginSupportCheck(token: Long, audio: DecodedPcmAudio, language: IdeaVoiceLanguage) {
        if (token != session || destroyed) return
        val current = runCatching { SpeechRecognizer.createSpeechRecognizer(appContext) }
            .getOrElse {
                finishFailure(token, IdeaTranscriptionState.UNAVAILABLE, "Speech recognition could not start on this phone.")
                return
            }
        recognizer = current

        val supportFd = runCatching { ParcelFileDescriptor.open(audio.file, ParcelFileDescriptor.MODE_READ_ONLY) }
            .getOrElse {
                finishFailure(token, IdeaTranscriptionState.FAILED, "The prepared recording could not be opened.")
                return
            }
        val supportIntent = buildRecognizerIntent(supportFd, audio, language)

        runCatching {
            current.checkRecognitionSupport(
                supportIntent,
                appContext.mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        runCatching { supportFd.close() }
                        if (token != session || destroyed) return

                        val ready = recognitionSupport.installedOnDeviceLanguages.isNotEmpty() ||
                            recognitionSupport.onlineLanguages.isNotEmpty()
                        if (!ready) {
                            finishFailure(
                                token,
                                IdeaTranscriptionState.UNAVAILABLE,
                                "A speech model for this saved recording is not ready on this phone.",
                            )
                            return
                        }
                        startRecognition(token, current, audio, language)
                    }

                    override fun onError(error: Int) {
                        runCatching { supportFd.close() }
                        if (token != session || destroyed) return
                        finishFailure(
                            token,
                            IdeaTranscriptionState.UNAVAILABLE,
                            "This phone cannot safely transcribe saved recordings with its current speech service.",
                        )
                    }
                },
            )
        }.onFailure {
            runCatching { supportFd.close() }
            finishFailure(
                token,
                IdeaTranscriptionState.UNAVAILABLE,
                "This phone cannot verify saved-recording speech support.",
            )
        }
    }

    private fun startRecognition(
        token: Long,
        current: SpeechRecognizer,
        audio: DecodedPcmAudio,
        language: IdeaVoiceLanguage,
    ) {
        if (token != session || destroyed) return
        val audioFd = runCatching { ParcelFileDescriptor.open(audio.file, ParcelFileDescriptor.MODE_READ_ONLY) }
            .getOrElse {
                finishFailure(token, IdeaTranscriptionState.FAILED, "The prepared recording could not be opened.")
                return
            }
        activeAudioSource = audioFd

        current.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onError(error: Int) {
                if (token != session || destroyed) return
                val unavailable = error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                finishFailure(
                    token,
                    if (unavailable) IdeaTranscriptionState.UNAVAILABLE else IdeaTranscriptionState.FAILED,
                    savedRecordingError(error),
                )
            }

            override fun onResults(results: Bundle?) {
                if (token != session || destroyed) return
                val text = bestText(results)
                if (text.isBlank()) {
                    finishFailure(token, IdeaTranscriptionState.FAILED, "No clear speech was found in this recording.")
                    return
                }
                val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    ?.firstOrNull()
                    ?.takeIf { it >= 0f }
                cleanupRecognitionResources()
                listener.onTranscriptReady(text, confidence)
                listener.onTranscriptionState(IdeaTranscriptionState.COMPLETED)
            }
        })

        val intent = buildRecognizerIntent(audioFd, audio, language)
        runCatching { current.startListening(intent) }
            .onFailure {
                finishFailure(token, IdeaTranscriptionState.FAILED, "Saved recording transcription could not start.")
            }
    }

    private fun buildRecognizerIntent(
        audioFd: ParcelFileDescriptor,
        audio: DecodedPcmAudio,
        language: IdeaVoiceLanguage,
    ): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLanguageTag(language))
        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audioFd)
        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, audio.channelCount)
        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, audio.sampleRate)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY)
            putExtra(RecognizerIntent.EXTRA_HIDE_PARTIAL_TRAILING_PUNCTUATION, true)
            putExtra(RecognizerIntent.EXTRA_MASK_OFFENSIVE_WORDS, false)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && language == IdeaVoiceLanguage.AUTO) {
            val allowed = arrayListOf("te-IN", "en-IN")
            putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
            putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, allowed)
            putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
            putStringArrayListExtra(RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES, allowed)
        }
    }

    private fun primaryLanguageTag(language: IdeaVoiceLanguage): String = when (language) {
        IdeaVoiceLanguage.TELUGU -> "te-IN"
        IdeaVoiceLanguage.ENGLISH -> "en-IN"
        IdeaVoiceLanguage.AUTO -> if (Locale.getDefault().language == "te") "te-IN" else "en-IN"
    }

    private fun bestText(results: Bundle?): String =
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()

    private fun finishFailure(token: Long, state: IdeaTranscriptionState, message: String) {
        if (token != session || destroyed) return
        cleanupRecognitionResources()
        listener.onTranscriptionState(state, message)
    }

    private fun cancelActiveResources() {
        activeJob?.cancel()
        activeJob = null
        cleanupRecognitionResources()
    }

    private fun cleanupRecognitionResources() {
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
        runCatching { activeAudioSource?.close() }
        activeAudioSource = null
        activePcmFile?.delete()
        activePcmFile = null
    }

    private fun savedRecordingError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No clear speech was found in this recording."
        SpeechRecognizer.ERROR_AUDIO -> "The saved recording could not be read by the speech service."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Saved recording transcription needs a connection on this phone right now."
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech transcription is busy. Try this recording again in a moment."
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech transcription is temporarily unavailable."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "That speech language is not available on this phone yet."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Speech recognition permission is unavailable for this recording."
        else -> "This recording could not be transcribed clearly."
    }
}

internal data class DecodedPcmAudio(
    val file: File,
    val sampleRate: Int,
    val channelCount: Int,
)

/** AAC/M4A -> raw PCM16 cache decoder. The source recording is read-only. */
internal object PcmVoiceDecoder {
    private const val DEQUEUE_TIMEOUT_US = 10_000L
    private const val MAX_IDLE_POLLS = 1_000

    fun decode(context: Context, source: File): DecodedPcmAudio {
        val cacheDir = File(context.cacheDir, "voice_transcripts").apply { mkdirs() }
        val output = File.createTempFile("voice_idea_", ".pcm", cacheDir)
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(source.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track was found in this Voice Idea recording.")

            extractor.selectTrack(trackIndex)
            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: error("The Voice Idea audio format is unknown.")
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)

            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var outputEncoding = AudioFormat.ENCODING_PCM_16BIT

            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            var inputDone = false
            var outputDone = false
            var idlePolls = 0
            val info = MediaCodec.BufferInfo()

            FileOutputStream(output).use { stream ->
                while (!outputDone) {
                    var progressed = false
                    if (!inputDone) {
                        val inputIndex = codec.dequeueInputBuffer(DEQUEUE_TIMEOUT_US)
                        if (inputIndex >= 0) {
                            progressed = true
                            val inputBuffer = codec.getInputBuffer(inputIndex)
                                ?: error("The audio decoder did not provide an input buffer.")
                            val size = extractor.readSampleData(inputBuffer, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    when (val outputIndex = codec.dequeueOutputBuffer(info, DEQUEUE_TIMEOUT_US)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            progressed = true
                            val decodedFormat = codec.outputFormat
                            sampleRate = decodedFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channelCount = decodedFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            outputEncoding = if (decodedFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                                decodedFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                            } else {
                                AudioFormat.ENCODING_PCM_16BIT
                            }
                            check(outputEncoding == AudioFormat.ENCODING_PCM_16BIT) {
                                "The phone's audio decoder did not provide PCM16 output."
                            }
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER,
                        MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                        else -> if (outputIndex >= 0) {
                            progressed = true
                            val outputBuffer = codec.getOutputBuffer(outputIndex)
                            if (outputBuffer != null && info.size > 0 &&
                                info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0
                            ) {
                                val copy = outputBuffer.duplicate().apply {
                                    position(info.offset)
                                    limit(info.offset + info.size)
                                }
                                val bytes = ByteArray(copy.remaining())
                                copy.get(bytes)
                                stream.write(bytes)
                            }
                            outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }

                    idlePolls = if (progressed) 0 else idlePolls + 1
                    check(idlePolls < MAX_IDLE_POLLS) { "The audio decoder stopped responding." }
                }
            }

            check(outputEncoding == AudioFormat.ENCODING_PCM_16BIT) {
                "The phone's audio decoder did not provide PCM16 output."
            }
            check(output.length() > 0L) { "The Voice Idea recording decoded to empty audio." }
            return DecodedPcmAudio(output, sampleRate, channelCount)
        } catch (error: Throwable) {
            output.delete()
            throw error
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }
}
