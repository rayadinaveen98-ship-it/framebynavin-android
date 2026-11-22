package com.framebynavin.app.reminders

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.framebynavin.app.data.VoicePersona
import java.util.Locale

/**
 * RX03: explicit local speaker identities for existing Creator voice profiles.
 *
 * No online synthesis, no microphone, no recording, no voice cloning, no new
 * VoicePersona enum values and no migration of existing saved project voices.
 * The installed Android engine may expose fewer than eight offline voices.
 */
internal data class RX03InstalledVoice(
    val name: String,
    val languageTag: String,
    val quality: Int,
    val latency: Int,
)

internal data class RX03VoiceSelection(
    val enginePackage: String,
    val name: String,
    val languageTag: String,
)

internal object RX03VoiceIdentityRegistry {
    private const val PREFS = "backlot_rx03_local_voice_identity_v1"
    private fun key(persona: VoicePersona, field: String) =
        "creator." + persona.name + "." + field

    fun installedOffline(tts: TextToSpeech, language: Locale = Locale.getDefault()):
        List<RX03InstalledVoice> {
        val voices = runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
        return voices.asSequence()
            .filter { !it.isNetworkConnectionRequired }
            .filter { it.locale.language == language.language }
            .distinctBy { it.name to it.locale.toLanguageTag() }
            .sortedWith(compareByDescending<Voice> { it.quality }
                .thenBy { it.latency }.thenBy { it.name })
            .map {
                RX03InstalledVoice(it.name, it.locale.toLanguageTag(), it.quality, it.latency)
            }.toList()
    }

    fun saved(context: Context, persona: VoicePersona): RX03VoiceSelection? {
        val settings = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val engine = settings.getString(key(persona, "engine"), null)
        val id = settings.getString(key(persona, "id"), null)
        val language = settings.getString(key(persona, "language"), null)
        if (engine.isNullOrBlank() || id.isNullOrBlank() || language.isNullOrBlank())
            return null
        return RX03VoiceSelection(engine, id, language)
    }

    /** Persist only an entry shown in the current installed, offline catalog. */
    fun save(context: Context, persona: VoicePersona,
             selection: RX03VoiceSelection): Boolean {
        if (selection.enginePackage.isBlank() || selection.name.isBlank() ||
            selection.languageTag.isBlank() || selection.name.length > 256 ||
            selection.enginePackage.length > 256 || selection.languageTag.length > 64)
            return false
        return context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(key(persona, "engine"), selection.enginePackage)
            .putString(key(persona, "id"), selection.name)
            .putString(key(persona, "language"), selection.languageTag)
            .commit()
    }

    fun clear(context: Context, persona: VoicePersona): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(key(persona, "engine"))
            .remove(key(persona, "id"))
            .remove(key(persona, "language"))
            .commit()

    /**
     * A remembered voice is honored only if the same Android TTS engine,
     * actual voice ID, locale and offline language are still installed.
     * Otherwise return null so Backlot uses the existing local fallback.
     */
    fun resolve(tts: TextToSpeech, selection: RX03VoiceSelection,
                language: Locale = Locale.getDefault()): Voice? {
        val engine = runCatching { tts.defaultEngine }.getOrNull().orEmpty()
        if (engine != selection.enginePackage) return null
        return runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
            .firstOrNull { voice ->
                !voice.isNetworkConnectionRequired &&
                    voice.name == selection.name &&
                    voice.locale.toLanguageTag() == selection.languageTag &&
                    voice.locale.language == language.language
            }
    }

    fun chooseVoice(tts: TextToSpeech, selection: RX03VoiceSelection,
                    language: Locale = Locale.getDefault()): Boolean {
        val voice = resolve(tts, selection, language) ?: return false
        return runCatching { tts.setVoice(voice) == TextToSpeech.SUCCESS }.getOrDefault(false)
    }
}
