package com.framebynavin.app.reminders

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.framebynavin.app.data.VoicePersona
import java.util.Locale

/**
 * Local-first Backlot voice styling.
 *
 * Human personas select the strongest local device voices available. Character personas remain
 * useful even on phones exposing only one local voice by combining stronger pitch/rate profiles
 * with persona-specific reminder phrasing. This deliberately does not pretend to be neural TTS.
 */
object VoicePersonaEngine {
    fun apply(
        tts: TextToSpeech,
        persona: VoicePersona,
        context: Context? = null,
        previewVoiceName: String? = null,
    ) {
        // Explicit user-selected offline speaker first. Do not override it
        // with persona-index fallback; old users keep their prior behavior.
        val engineName = runCatching { tts.defaultEngine }.getOrNull().orEmpty()
        val explicit = if (previewVoiceName != null) {
            RX03VoiceIdentityRegistry.installedOffline(tts)
                .firstOrNull { it.name == previewVoiceName }
                ?.let { RX03VoiceSelection(engineName, it.name, it.languageTag) }
        } else {
            context?.let { RX03VoiceIdentityRegistry.saved(it, persona) }
        }
        val selected = explicit != null &&
            RX03VoiceIdentityRegistry.chooseVoice(tts, explicit)
        if (!selected) selectDeviceVoice(tts, persona)
        val (pitch, rate) = when (persona) {
            VoicePersona.WARM -> 0.99f to 0.90f
            VoicePersona.WOMAN -> 1.04f to 0.95f
            VoicePersona.MAN -> 0.92f to 0.88f
            VoicePersona.YOUNG -> 1.07f to 0.98f
            VoicePersona.FUNNY -> 1.17f to 1.08f
            VoicePersona.CARTOON -> 1.25f to 1.10f
            VoicePersona.ALIEN -> 0.76f to 0.84f
            VoicePersona.ROBOT -> 0.86f to 0.80f
        }
        tts.setPitch(pitch)
        tts.setSpeechRate(rate)
    }

    fun label(persona: VoicePersona): String = when (persona) {
        VoicePersona.WARM -> "Warm Human"
        VoicePersona.WOMAN -> "Clear Human"
        VoicePersona.MAN -> "Deep Human"
        VoicePersona.YOUNG -> "Bright Human"
        VoicePersona.FUNNY -> "Funny"
        VoicePersona.CARTOON -> "Cartoon"
        VoicePersona.ALIEN -> "Alien"
        VoicePersona.ROBOT -> "Robot"
    }

    fun category(persona: VoicePersona): String = when (persona) {
        VoicePersona.WARM, VoicePersona.WOMAN, VoicePersona.MAN, VoicePersona.YOUNG -> "HUMAN"
        VoicePersona.FUNNY -> "FUN"
        VoicePersona.CARTOON -> "CHARACTER"
        VoicePersona.ALIEN, VoicePersona.ROBOT -> "SCI-FI"
    }

    fun description(persona: VoicePersona): String = when (persona) {
        VoicePersona.WARM -> "Calm, soft and steady"
        VoicePersona.WOMAN -> "Clean and conversational"
        VoicePersona.MAN -> "Lower, slower and grounded"
        VoicePersona.YOUNG -> "Upbeat, bright and encouraging"
        VoicePersona.FUNNY -> "Witty, bouncy, friendly delivery"
        VoicePersona.CARTOON -> "Big, theatrical character energy"
        VoicePersona.ALIEN -> "Low, strange transmission tone"
        VoicePersona.ROBOT -> "Measured synthetic-style delivery"
    }

    /** Exact same audio text for identity comparison, independent of wording/style. */
    fun comparisonPreviewText(): String =
        "Backlot reminder. Your next creative task is ready. Let's get started."

    /** Use the actual new Creator script in the persona picker preview. */
    fun previewText(persona: VoicePersona): String =
        RX03BReminderScripts.creatorPreview(persona)

    /** The runtime Creator reminder gets the same safe, domain-aware copy. */
    fun reminderText(
        persona: VoicePersona,
        title: String,
        urgency: String,
        stage: String,
        notes: String,
        includeNotes: Boolean,
    ): String = RX03BReminderScripts.creator(
        persona, title, urgency, stage, notes, includeNotes,
    )

    fun availabilityHint(tts: TextToSpeech): String {
        val count = candidateVoices(tts).size
        return when {
            count > 1 -> "$count strong local device voices · 8 Backlot delivery styles"
            count == 1 -> "1 strong local device voice · 8 Backlot delivery styles"
            else -> "Device TTS voice availability is limited · 8 Backlot delivery styles"
        }
    }

    private fun selectDeviceVoice(tts: TextToSpeech, persona: VoicePersona) {
        val voices = candidateVoices(tts)
        if (voices.isEmpty()) return
        val requestedIndex = when (persona) {
            VoicePersona.WARM -> 0
            VoicePersona.WOMAN -> 1
            VoicePersona.MAN -> 2
            VoicePersona.YOUNG -> 3
            VoicePersona.FUNNY -> 4
            VoicePersona.CARTOON -> 5
            VoicePersona.ALIEN -> 6
            VoicePersona.ROBOT -> 7
        }
        val voice = voices[requestedIndex.coerceAtMost(voices.lastIndex)]
        runCatching { tts.voice = voice }
    }

    /**
     * Keep persona variety inside the best local quality tier instead of walking into a visibly
     * lower-quality or wrong-language voice just because a persona requested a later list index.
     */
    private fun candidateVoices(tts: TextToSpeech): List<Voice> {
        val current = Locale.getDefault()
        val local = runCatching { tts.voices.orEmpty() }
            .getOrDefault(emptySet())
            .filter { !it.isNetworkConnectionRequired }
        if (local.isEmpty()) return emptyList()

        val exactLocale = local.filter { it.locale == current }
        val sameLanguage = local.filter { it.locale.language == current.language }
        val localePool = when {
            exactLocale.isNotEmpty() -> exactLocale
            sameLanguage.isNotEmpty() -> sameLanguage
            else -> return emptyList() // Never switch speech to an unrelated language.
        }

        val bestQuality = localePool.maxOfOrNull { it.quality } ?: Voice.QUALITY_NORMAL
        val qualityFloor = maxOf(Voice.QUALITY_NORMAL, bestQuality - 100)
        val qualityPool = localePool.filter { it.quality >= qualityFloor }.ifEmpty { localePool }

        return qualityPool.sortedWith(
            compareByDescending<Voice> { it.quality }
                .thenBy { it.latency }
                .thenByDescending { it.locale.country == current.country && current.country.isNotBlank() }
                .thenBy { it.name }
        )
    }
}
