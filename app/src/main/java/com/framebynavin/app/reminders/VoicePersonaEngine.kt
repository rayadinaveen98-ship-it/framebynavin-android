package com.framebynavin.app.reminders

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
    fun apply(tts: TextToSpeech, persona: VoicePersona) {
        selectDeviceVoice(tts, persona)
        val (pitch, rate) = when (persona) {
            VoicePersona.WARM -> 0.99f to 0.90f
            VoicePersona.WOMAN -> 1.04f to 0.95f
            VoicePersona.MAN -> 0.92f to 0.88f
            VoicePersona.YOUNG -> 1.07f to 0.98f
            VoicePersona.FUNNY -> 1.13f to 1.03f
            VoicePersona.CARTOON -> 1.22f to 1.06f
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
        VoicePersona.YOUNG -> "Light, quick and energetic"
        VoicePersona.FUNNY -> "Playful pace with extra lift"
        VoicePersona.CARTOON -> "Bright, punchy character delivery"
        VoicePersona.ALIEN -> "Low, strange transmission tone"
        VoicePersona.ROBOT -> "Measured synthetic-style delivery"
    }

    fun previewText(persona: VoicePersona): String = when (persona) {
        VoicePersona.WARM -> "Backlot voice check. Calm, clear, and ready when you are."
        VoicePersona.WOMAN -> "Backlot voice check. Your next creative step is ready."
        VoicePersona.MAN -> "Backlot voice check. Focus on the next stage."
        VoicePersona.YOUNG -> "Backlot voice check. Ready to make something great?"
        VoicePersona.FUNNY -> "Hey creator. Backlot checking in. Your idea is not escaping today."
        VoicePersona.CARTOON -> "Backlot calling! Your next creative mission is ready."
        VoicePersona.ALIEN -> "Backlot transmission received. Creative signal detected."
        VoicePersona.ROBOT -> "Backlot system check. Creator task detected. Ready."
    }

    fun reminderText(
        persona: VoicePersona,
        title: String,
        urgency: String,
        stage: String,
        notes: String,
        includeNotes: Boolean,
    ): String {
        val core = when (persona) {
            VoicePersona.WARM -> "Backlot. $title. This is your $urgency. Current stage: $stage."
            VoicePersona.WOMAN -> "Backlot check-in. $title. Your $urgency is ready. You are at $stage."
            VoicePersona.MAN -> "Backlot. Focus check. $title. $urgency. Current stage: $stage."
            VoicePersona.YOUNG -> "Backlot reminder. $title. Time for your $urgency. You are on $stage."
            VoicePersona.FUNNY -> "Hey creator, Backlot here. $title. Your $urgency is waiting. Stage: $stage."
            VoicePersona.CARTOON -> "Backlot calling! $title. Time for your $urgency. Creative stage: $stage."
            VoicePersona.ALIEN -> "Backlot transmission. Subject: $title. Signal priority: $urgency. Current stage: $stage."
            VoicePersona.ROBOT -> "Backlot reminder. Task: $title. Priority: $urgency. Stage: $stage."
        }
        return if (includeNotes && notes.isNotBlank()) "$core $notes" else core
    }

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
            VoicePersona.FUNNY -> 3
            VoicePersona.CARTOON -> 3
            VoicePersona.ALIEN -> 2
            VoicePersona.ROBOT -> 2
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
            else -> local
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
