package com.framebynavin.app.reminders

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.framebynavin.app.data.VoicePersona
import java.util.Locale

object VoicePersonaEngine {
    fun apply(tts: TextToSpeech, persona: VoicePersona) {
        selectDeviceVoice(tts, persona)
        val (pitch, rate) = when (persona) {
            VoicePersona.WARM -> 0.98f to 0.88f
            VoicePersona.YOUNG -> 1.04f to 0.98f
            VoicePersona.MAN -> 0.94f to 0.90f
            VoicePersona.WOMAN -> 1.02f to 0.94f
        }
        tts.setPitch(pitch)
        tts.setSpeechRate(rate)
    }

    fun label(persona: VoicePersona): String = when (persona) {
        VoicePersona.WARM -> "Nila"
        VoicePersona.YOUNG -> "Tara"
        VoicePersona.MAN -> "Arin"
        VoicePersona.WOMAN -> "Maya"
    }

    fun availabilityHint(tts: TextToSpeech): String {
        val count = candidateVoices(tts).size
        return when {
            count >= 4 -> "4 distinct device voices available"
            count > 1 -> "$count distinct device voices available"
            else -> "Your phone currently exposes one local voice"
        }
    }

    private fun selectDeviceVoice(tts: TextToSpeech, persona: VoicePersona) {
        val voices = candidateVoices(tts)
        if (voices.isEmpty()) return
        val index = when (persona) {
            VoicePersona.WARM -> 0
            VoicePersona.YOUNG -> 1
            VoicePersona.MAN -> 2
            VoicePersona.WOMAN -> 3
        }
        runCatching { tts.voice = voices[index % voices.size] }
    }

    private fun candidateVoices(tts: TextToSpeech): List<Voice> {
        val current = Locale.getDefault()
        return runCatching { tts.voices.orEmpty() }
            .getOrDefault(emptySet())
            .filter { !it.isNetworkConnectionRequired }
            .sortedWith(
                compareByDescending<Voice> { it.locale.language == current.language }
                    .thenByDescending { it.locale.country == current.country && current.country.isNotBlank() }
                    .thenByDescending { it.quality }
                    .thenBy { it.name }
            )
    }
}
