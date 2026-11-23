package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Staged only. Do not run until founder approves consolidated QA. */
class P2VoiceIdentityPolicyTest {
    private val id = P2VoiceIdentity(
        enginePackage = "com.google.android.tts",
        voiceName = "en-in-local-speaker-example",
        languageTag = "en-IN",
    )

    @Test fun exactIdentityMustIncludeEngineSpeakerAndLocale() {
        assertTrue(P2VoiceIdentityPolicy.same(id, id.copy()))
        assertFalse(P2VoiceIdentityPolicy.same(id,
            id.copy(enginePackage = "org.example.other")))
        assertFalse(P2VoiceIdentityPolicy.same(id,
            id.copy(voiceName = "en-in-different")))
        assertFalse(P2VoiceIdentityPolicy.same(id,
            id.copy(languageTag = "en-US")))
    }

    @Test fun onlySupportedWellFormedOfflineSpeakerReferencesCanBeSaved() {
        assertTrue(P2VoiceIdentityPolicy.valid(id))
        assertTrue(P2VoiceIdentityPolicy.valid(id.copy(languageTag = "te-IN")))
        assertFalse(P2VoiceIdentityPolicy.valid(id.copy(enginePackage = "")))
        assertFalse(P2VoiceIdentityPolicy.valid(id.copy(voiceName = "")))
        assertFalse(P2VoiceIdentityPolicy.valid(id.copy(languageTag = "fr-FR")))
        assertFalse(P2VoiceIdentityPolicy.valid(id.copy(voiceName = "\u0000")))
        assertFalse(P2VoiceIdentityPolicy.valid(id.copy(voiceName = "x".repeat(251))))
    }

    @Test fun sampleScriptIsConstantForSameLanguage() {
        assertEquals(P2VoiceIdentityPolicy.auditionText("en-IN"),
            P2VoiceIdentityPolicy.auditionText("en-US"))
        assertTrue(P2VoiceIdentityPolicy.auditionText("en-IN").contains("Backlot"))
        assertTrue(P2VoiceIdentityPolicy.auditionText("te-IN").contains("బ్యాక్‌లాట్"))
    }
}
