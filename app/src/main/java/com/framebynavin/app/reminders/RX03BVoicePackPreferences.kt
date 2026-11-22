package com.framebynavin.app.reminders

import android.content.Context
import com.framebynavin.app.data.VoicePersona

/**
 * RX03B: opt-in default packs for Personal and 21-Day Challenge Voice.
 *
 * These preferences are separate from Creator's per-task saved persona and
 * separate from delivery, occurrence, music, repeat, completion and Snooze.
 * Null means existing Android TTS and exact legacy wording are preserved.
 *
 * Do not insert these into cloud or change legacy codec/migrations. A later
 * separately approved per-reminder selector must preserve user data integrity.
 */
internal enum class RX03BVoiceDomain { PERSONAL, CHALLENGE_21 }

internal object RX03BVoicePackPreferences {
    private const val FILE = "backlot_rx03b_reminder_voice_packs_v1"

    private fun key(domain: RX03BVoiceDomain): String = when (domain) {
        RX03BVoiceDomain.PERSONAL -> "personal_default_voice_pack"
        RX03BVoiceDomain.CHALLENGE_21 -> "challenge_default_voice_pack"
    }

    fun selected(context: Context, domain: RX03BVoiceDomain): VoicePersona? {
        // Corrupted/wrong-type local preferences must never crash a live
        // foreground voice service or turn a reminder into a delivery failure.
        val raw = runCatching {
            context.applicationContext
                .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getString(key(domain), null)
        }.getOrNull() ?: return null
        return runCatching { VoicePersona.valueOf(raw) }.getOrNull()
    }

    /** A null pack explicitly restores the original system TTS copy. */
    fun set(context: Context, domain: RX03BVoiceDomain, pack: VoicePersona?): Boolean {
        val editor = context.applicationContext
            .getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
        if (pack == null) editor.remove(key(domain))
        else editor.putString(key(domain), pack.name)
        return editor.commit()
    }
}
