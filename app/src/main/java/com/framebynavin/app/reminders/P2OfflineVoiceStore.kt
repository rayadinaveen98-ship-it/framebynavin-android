package com.framebynavin.app.reminders

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.framebynavin.app.data.VoicePersona
import java.util.Locale

internal data class P2VoiceIdentity(
    val enginePackage: String,
    val voiceName: String,
    val languageTag: String,
)

internal data class P2InstalledVoice(
    val identity: P2VoiceIdentity,
    val quality: Int,
    val latency: Int,
    val deviceVoice: Voice,
)

/** Voice IDs are not proof of perceptually distinct speakers. */
internal object P2VoiceIdentityPolicy {
    fun supportedLanguage(code: String): Boolean = code == "en" || code == "te"
    fun valid(identity: P2VoiceIdentity): Boolean =
        identity.enginePackage.length in 3..200 &&
        identity.voiceName.length in 1..250 &&
        identity.languageTag.length in 2..40 &&
        identity.enginePackage.none { it.isWhitespace() } &&
        identity.voiceName.none { it.code < 32 } &&
        supportedLanguage(Locale.forLanguageTag(identity.languageTag).language)

    fun same(a: P2VoiceIdentity, b: P2VoiceIdentity): Boolean =
        a.enginePackage == b.enginePackage &&
        a.voiceName == b.voiceName &&
        a.languageTag == b.languageTag

    fun auditionText(localeTag: String): String =
        if (Locale.forLanguageTag(localeTag).language == "te")
            "బ్యాక్‌లాట్ నుంచి ఒక రిమైండర్. మీ తదుపరి పని సిద్ధంగా ఉంది."
        else
            "Backlot reminder. Your next creative step is ready. Let's get started."
}

/**
 * User-selected offline TTS identities are strictly opt-in and local.
 * The eight persisted Creator voice personas and all reminder receipts stay unchanged.
 */
internal object P2OfflineVoiceStore {
    private const val PREFS = "backlot_p2_offline_voice_choices_v1"
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun key(persona: VoicePersona, field: String) =
        "creator." + persona.name + "." + field

    fun installed(tts: TextToSpeech, locale: Locale): List<P2InstalledVoice> {
        val engine = runCatching { tts.defaultEngine.orEmpty() }.getOrDefault("")
        if (engine.isBlank()) return emptyList()
        return runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
            .asSequence().filter {
                !it.isNetworkConnectionRequired && it.locale.language == locale.language
            }.filter {
                P2VoiceIdentityPolicy.valid(P2VoiceIdentity(
                    engine, it.name, it.locale.toLanguageTag()))
            }.distinctBy { it.name to it.locale.toLanguageTag() }
            .sortedWith(compareByDescending<Voice> { it.quality }
                .thenBy { it.latency }.thenBy { it.name })
            .map { voice ->
                P2InstalledVoice(
                    P2VoiceIdentity(engine, voice.name, voice.locale.toLanguageTag()),
                    voice.quality, voice.latency, voice)
            }.toList()
    }

    fun selected(context: Context, persona: VoicePersona): P2VoiceIdentity? {
        val p = prefs(context)
        val id = P2VoiceIdentity(
            enginePackage = p.getString(key(persona, "engine"), null) ?: return null,
            voiceName = p.getString(key(persona, "name"), null) ?: return null,
            languageTag = p.getString(key(persona, "locale"), null) ?: return null)
        return id.takeIf(P2VoiceIdentityPolicy::valid)
    }

    /** Save only actual offline IDs from the selected Android TTS engine. */
    @Synchronized
    fun save(context: Context, tts: TextToSpeech, persona: VoicePersona,
        identity: P2VoiceIdentity): Boolean {
        if (!P2VoiceIdentityPolicy.valid(identity)) return false
        val engine = runCatching { tts.defaultEngine.orEmpty() }.getOrDefault("")
        if (engine != identity.enginePackage) return false
        val exists = runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
            .any { voice -> !voice.isNetworkConnectionRequired &&
                P2VoiceIdentityPolicy.same(identity,
                    P2VoiceIdentity(engine, voice.name, voice.locale.toLanguageTag()))
            }
        if (!exists) return false
        return prefs(context).edit()
            .putString(key(persona, "engine"), identity.enginePackage)
            .putString(key(persona, "name"), identity.voiceName)
            .putString(key(persona, "locale"), identity.languageTag)
            .commit()
    }

    @Synchronized
    fun reset(context: Context, persona: VoicePersona): Boolean =
        prefs(context).edit()
            .remove(key(persona, "engine"))
            .remove(key(persona, "name"))
            .remove(key(persona, "locale"))
            .commit()

    /** Missing engine or voice always uses the original Backlot style fallback. */
    fun applyIfAvailable(context: Context, tts: TextToSpeech,
        persona: VoicePersona): Boolean {
        val id = selected(context, persona) ?: return false
        val engine = runCatching { tts.defaultEngine.orEmpty() }.getOrDefault("")
        if (engine != id.enginePackage) return false
        val match = runCatching { tts.voices.orEmpty() }.getOrDefault(emptySet())
            .firstOrNull { voice ->
                !voice.isNetworkConnectionRequired &&
                P2VoiceIdentityPolicy.same(id,
                    P2VoiceIdentity(engine, voice.name, voice.locale.toLanguageTag()))
            } ?: return false
        return runCatching { tts.setVoice(match) == TextToSpeech.SUCCESS }
            .getOrDefault(false)
    }
}
