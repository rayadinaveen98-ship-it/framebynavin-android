from pathlib import Path
import re


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected one exact match in {path}, found {count}: {old[:120]!r}")
    p.write_text(text.replace(old, new, 1))


def regex_once(path: str, pattern: str, replacement: str) -> None:
    p = Path(path)
    text = p.read_text()
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"Expected one regex match in {path}, found {count}: {pattern[:120]!r}")
    p.write_text(updated)


path = "app/src/main/java/com/framebynavin/app/voice/SavedVoiceIdeaTranscriber.kt"

replace_once(
    path,
    "import android.speech.RecognitionListener\n",
    "import android.speech.RecognitionListener\nimport android.speech.RecognitionSupport\nimport android.speech.RecognitionSupportCallback\n",
)

replace_once(
    path,
    " * pass that PCM file to the speech recognizer. The prepared file remains alive for the\n * complete recognition session and is deleted only after success, failure or cancellation.\n",
    " * pass that PCM file to the speech recognizer. We verify the exact saved-audio request before\n * listening because Android may fall back to the microphone when EXTRA_AUDIO_SOURCE is unsupported.\n * One master descriptor stays alive across support-check and recognition; child descriptors are\n * duplicated from it instead of reopening/deleting the temporary file between asynchronous steps.\n",
)

replace_once(
    path,
    "    private var recognizer: SpeechRecognizer? = null\n    private var activeAudioSource: ParcelFileDescriptor? = null\n",
    "    private var recognizer: SpeechRecognizer? = null\n    private var activePreparedSource: ParcelFileDescriptor? = null\n    private var activeSupportSource: ParcelFileDescriptor? = null\n    private var activeAudioSource: ParcelFileDescriptor? = null\n",
)

replace_once(
    path,
    "                startRecognition(token, decoded, language)\n",
    "                beginSupportCheck(token, decoded, language)\n",
)

new_flow = r'''    private fun beginSupportCheck(
        token: Long,
        audio: DecodedPcmAudio,
        language: IdeaVoiceLanguage,
    ) {
        if (token != session || destroyed) return
        if (!audio.file.exists() || !audio.file.isFile || !audio.file.canRead() || audio.file.length() <= 0L) {
            finishFailure(token, IdeaTranscriptionState.FAILED, "The prepared recording is unavailable. Try transcribing again.")
            return
        }

        val current = runCatching { SpeechRecognizer.createSpeechRecognizer(appContext) }
            .getOrElse {
                finishFailure(token, IdeaTranscriptionState.UNAVAILABLE, "Speech recognition could not start on this phone.")
                return
            }
        recognizer = current

        // Open the prepared PCM once. Keeping this master descriptor alive prevents the real-device
        // reopen race that produced "The prepared recording could not be opened".
        val prepared = runCatching { ParcelFileDescriptor.open(audio.file, ParcelFileDescriptor.MODE_READ_ONLY) }
            .getOrElse { error ->
                finishFailure(
                    token,
                    IdeaTranscriptionState.FAILED,
                    "The prepared recording could not be opened (${error.javaClass.simpleName}).",
                )
                return
            }
        activePreparedSource = prepared

        val supportFd = runCatching { ParcelFileDescriptor.dup(prepared.fileDescriptor) }
            .getOrElse { error ->
                finishFailure(
                    token,
                    IdeaTranscriptionState.FAILED,
                    "The prepared recording could not be verified (${error.javaClass.simpleName}).",
                )
                return
            }
        activeSupportSource = supportFd
        val supportIntent = buildRecognizerIntent(supportFd, audio, language)

        runCatching {
            current.checkRecognitionSupport(
                supportIntent,
                appContext.mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        closeSupportSource(supportFd)
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
                        closeSupportSource(supportFd)
                        if (token != session || destroyed) return
                        val message = if (error == SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT) {
                            "This phone cannot verify saved-recording audio support, so Backlot will not open the microphone as a fallback."
                        } else {
                            "This phone cannot safely transcribe this saved recording right now (speech error $error)."
                        }
                        finishFailure(token, IdeaTranscriptionState.UNAVAILABLE, message)
                    }
                },
            )
        }.onFailure { error ->
            closeSupportSource(supportFd)
            finishFailure(
                token,
                IdeaTranscriptionState.UNAVAILABLE,
                "Saved-recording speech support could not be verified (${error.javaClass.simpleName}).",
            )
        }
    }

    private fun closeSupportSource(source: ParcelFileDescriptor) {
        if (activeSupportSource === source) activeSupportSource = null
        runCatching { source.close() }
    }

    private fun startRecognition(
        token: Long,
        current: SpeechRecognizer,
        audio: DecodedPcmAudio,
        language: IdeaVoiceLanguage,
    ) {
        if (token != session || destroyed) return
        val prepared = activePreparedSource
        if (prepared == null) {
            finishFailure(token, IdeaTranscriptionState.FAILED, "The prepared recording session was lost. Try again.")
            return
        }

        val audioFd = runCatching { ParcelFileDescriptor.dup(prepared.fileDescriptor) }
            .getOrElse { error ->
                finishFailure(
                    token,
                    IdeaTranscriptionState.FAILED,
                    "The prepared recording could not be handed to speech recognition (${error.javaClass.simpleName}).",
                )
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
                finishFailure(
                    token,
                    IdeaTranscriptionState.FAILED,
                    "Saved recording transcription could not start (${it.javaClass.simpleName}).",
                )
            }
    }

    private fun buildRecognizerIntent'''

regex_once(
    path,
    r"    private fun startRecognition\(.*?\n    private fun buildRecognizerIntent",
    new_flow,
)

replace_once(
    path,
    "        recognizer = null\n        runCatching { activeAudioSource?.close() }\n        activeAudioSource = null\n        activePcmFile?.delete()\n",
    "        recognizer = null\n        runCatching { activeSupportSource?.close() }\n        activeSupportSource = null\n        runCatching { activeAudioSource?.close() }\n        activeAudioSource = null\n        runCatching { activePreparedSource?.close() }\n        activePreparedSource = null\n        activePcmFile?.delete()\n",
)

build = "app/build.gradle.kts"
replace_once(build, 'versionName = "2.0.0-rc21-device-stability"', 'versionName = "2.0.0-rc22-device-stability"')

text = Path(path).read_text()
assert "checkRecognitionSupport" in text
assert "ParcelFileDescriptor.dup(prepared.fileDescriptor)" in text
assert "will not open the microphone as a fallback" in text
print("V147 saved-audio safety patch applied")
