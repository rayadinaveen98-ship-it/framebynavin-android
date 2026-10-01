from pathlib import Path
import re


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected one exact match in {path}, found {count}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1))


def regex_once(path: str, pattern: str, replacement: str) -> None:
    p = Path(path)
    text = p.read_text()
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"Expected one regex match in {path}, found {count}: {pattern[:100]!r}")
    p.write_text(updated)


# 1. Revenue: core earnings must survive optional YouTube report failures.
revenue = "app/src/main/java/com/framebynavin/app/youtube/YouTubeRevenue.kt"
new_sync = r'''    fun sync(
        accessToken: String,
        period: YouTubeRevenuePeriod,
        today: LocalDate = LocalDate.now(),
        currencyCode: String = deviceCurrencyCode(),
    ): YouTubeRevenueSnapshot {
        val range = rangeFor(period, today)
        val common = mapOf(
            "ids" to "channel==MINE",
            "startDate" to range.first.toString(),
            "endDate" to range.second.toString(),
            "currency" to currencyCode,
        )

        // Revenue totals are the critical dataset. Optional ad-rate, trend and content reports
        // may be temporarily unavailable without blanking the complete earnings card.
        val summary = queryReport(
            accessToken,
            common + ("metrics" to "views,estimatedRevenue,estimatedAdRevenue,monetizedPlaybacks"),
        ).firstOrNull().orEmpty()

        val adSummary: Map<String, Any?> = runCatching {
            queryReport(
                accessToken,
                common + ("metrics" to "playbackBasedCpm,cpm,adImpressions"),
            ).firstOrNull().orEmpty()
        }.getOrDefault(emptyMap())

        val trend: List<YouTubeRevenuePoint> = runCatching {
            queryReport(
                accessToken,
                common + mapOf(
                    "dimensions" to "day",
                    "metrics" to "estimatedRevenue",
                    "sort" to "day",
                ),
            ).mapNotNull { row ->
                val day = row["day"]?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                YouTubeRevenuePoint(day, row.double("estimatedRevenue"))
            }
        }.getOrDefault(emptyList())

        val content: List<YouTubeRevenueContentRow> = runCatching {
            queryReport(
                accessToken,
                common + mapOf(
                    "dimensions" to "video,creatorContentType",
                    "metrics" to "views,estimatedRevenue,estimatedAdRevenue,monetizedPlaybacks",
                    "sort" to "-estimatedRevenue",
                    "maxResults" to "50",
                ),
            ).mapNotNull { row ->
                val videoId = row["video"]?.toString()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                YouTubeRevenueContentRow(
                    videoId = videoId,
                    creatorContentType = row["creatorContentType"]?.toString().orEmpty(),
                    views = row.long("views"),
                    estimatedRevenue = row.double("estimatedRevenue"),
                    estimatedAdRevenue = row.double("estimatedAdRevenue"),
                    monetizedPlaybacks = row.long("monetizedPlaybacks"),
                )
            }
        }.getOrDefault(emptyList())

        return YouTubeRevenueSnapshot(
            period = period,
            startDate = range.first.toString(),
            endDate = range.second.toString(),
            currencyCode = currencyCode,
            views = summary.long("views"),
            estimatedRevenue = summary.double("estimatedRevenue"),
            estimatedAdRevenue = summary.double("estimatedAdRevenue"),
            playbackBasedCpm = adSummary.double("playbackBasedCpm"),
            impressionCpm = adSummary.double("cpm"),
            monetizedPlaybacks = summary.long("monetizedPlaybacks"),
            adImpressions = adSummary.long("adImpressions"),
            trend = trend,
            content = content,
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }

    internal fun rangeFor'''
regex_once(revenue, r'    fun sync\(.*?\n    internal fun rangeFor', new_sync)

integration = "app/src/main/java/com/framebynavin/app/ui/V144YouTubeRevenueIntegration.kt"
replace_once(
    integration,
    '        else ->\n            "YouTube revenue couldn\'t refresh right now. Your normal Insights are unaffected."',
    '        else ->\n            if (raw.isBlank()) {\n                "YouTube revenue couldn\'t refresh right now. Your normal Insights are unaffected."\n            } else {\n                "YouTube revenue couldn\'t refresh: ${raw.take(140)}"\n            }',
)

# 2. Saved voice transcription: keep one prepared PCM file + descriptor alive end-to-end.
transcriber = "app/src/main/java/com/framebynavin/app/voice/SavedVoiceIdeaTranscriber.kt"
replace_once(transcriber, 'import android.speech.RecognitionSupport\n', '')
replace_once(transcriber, 'import android.speech.RecognitionSupportCallback\n', '')
replace_once(
    transcriber,
    ' * pass that PCM file to the speech recognizer. The exact request is checked with\n * SpeechRecognizer.checkRecognitionSupport before startListening; if the service cannot verify\n * support we stop rather than risk EXTRA_AUDIO_SOURCE being ignored and the microphone opening.\n',
    ' * pass that PCM file to the speech recognizer. The prepared file remains alive for the\n * complete recognition session and is deleted only after success, failure or cancellation.\n',
)
replace_once(transcriber, '                beginSupportCheck(token, decoded, language)\n', '                startRecognition(token, decoded, language)\n')

new_recognition = r'''    private fun startRecognition(
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

        val audioFd = runCatching { ParcelFileDescriptor.open(audio.file, ParcelFileDescriptor.MODE_READ_ONLY) }
            .getOrElse { error ->
                finishFailure(
                    token,
                    IdeaTranscriptionState.FAILED,
                    "The prepared recording could not be opened (${error.javaClass.simpleName}).",
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
regex_once(transcriber, r'    private fun beginSupportCheck\(.*?\n    private fun buildRecognizerIntent', new_recognition)

# 3. Cute guide: use the known-good master pose for every state until alternate exports are replaced.
raster = "app/src/main/java/com/framebynavin/app/ui/V144RasterGuideCharacter.kt"
regex_once(
    raster,
    r'    V144GuideIdentity\.CUTE -> when \(state\) \{.*?\n    \}\n\n    // Defensive fallback',
    '    V144GuideIdentity.CUTE -> R.drawable.guide_cute_welcome\n\n    // Defensive fallback',
)

# 4. Settings must reflect the active Funny/Cute roster only.
settings = "app/src/main/java/com/framebynavin/app/ui/V144SettingsHub.kt"
replace_once(
    settings,
    '                V144SettingsNote("Your guide choice is used across setup, guided tours and helper moments. Frame and Navi keep their vector renderer; Funny and Cute use the premium raster character system.")',
    '                V144SettingsNote("Your guide choice is used across setup, guided tours and helper moments. Funny and Cute are the active Backlot guides.")',
)
replace_once(
    settings,
    '        V144CategoryCard("Guide Character", "Frame, Navi, Funny or Cute", Icons.Outlined.Face, MutedGold) { onOpen(V144SettingsSection.GUIDE) }',
    '        V144CategoryCard("Guide Character", V145SelectableGuides.joinToString(" or ") { it.displayName }, Icons.Outlined.Face, MutedGold) { onOpen(V144SettingsSection.GUIDE) }',
)

# 5. Remember the selected Insights range while navigating tabs in the current app process.
insights = "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt"
replace_once(
    insights,
    'import java.util.Locale\n',
    'import java.util.Locale\n\nprivate object V147InsightsSessionState {\n    var windowDays: Int = 28\n}\n',
)
replace_once(
    insights,
    '    var windowDays by rememberSaveable { mutableIntStateOf(28) }',
    '    var windowDays by rememberSaveable { mutableIntStateOf(V147InsightsSessionState.windowDays) }',
)
replace_once(
    insights,
    '    LaunchedEffect(windowDays) {\n        refreshCacheView()',
    '    LaunchedEffect(windowDays) {\n        V147InsightsSessionState.windowDays = windowDays\n        refreshCacheView()',
)

# Release candidate stamp.
build_gradle = "app/build.gradle.kts"
replace_once(build_gradle, 'versionName = "2.0.0-rc20-idea-reminders"', 'versionName = "2.0.0-rc21-device-stability"')

# Safety assertions.
assert 'Frame, Navi, Funny or Cute' not in Path(settings).read_text()
assert 'checkRecognitionSupport' not in Path(transcriber).read_text()
assert 'guide_cute_thinking' not in Path(raster).read_text()
assert 'guide_cute_celebrate' not in Path(raster).read_text()
assert 'V147InsightsSessionState.windowDays' in Path(insights).read_text()
print("V147 device stability patch applied successfully")
