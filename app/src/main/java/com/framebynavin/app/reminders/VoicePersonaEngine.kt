package com.framebynavin.app.reminders

import android.speech.tts.TextToSpeech
import com.framebynavin.app.data.VoicePersona

object VoicePersonaEngine {
    fun apply(tts: TextToSpeech, persona: VoicePersona) {
        val (pitch, rate) = when (persona) {
            VoicePersona.WARM -> 0.96f to 0.86f
            VoicePersona.YOUNG -> 1.28f to 1.02f
            VoicePersona.MAN -> 0.82f to 0.91f
            VoicePersona.WOMAN -> 1.08f to 0.95f
        }
        tts.setPitch(pitch)
        tts.setSpeechRate(rate)
    }

    fun label(persona: VoicePersona): String = when (persona) {
        VoicePersona.WARM -> "Warm"
        VoicePersona.YOUNG -> "Young"
        VoicePersona.MAN -> "Adult Man"
        VoicePersona.WOMAN -> "Adult Woman"
    }
}
