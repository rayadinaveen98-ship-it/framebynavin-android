package com.framebynavin.app.ui

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * A bounded, one-way fingerprint for unsaved project edits. The original
 * count-prefixed implementation accidentally stored entire scripts and notes
 * in the saved-instance-state Bundle, which can become extremely large.
 *
 * Hash the UTF-8 byte length and content of every field (including empties).
 * Only the 71-character prefixed digest goes into Compose saved state.
 * This does NOT persist unfinished data; the form's save/discard behavior is
 * deliberately unchanged.
 */
internal object V172ProjectDraftPolicy {
    fun signature(fields: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(fields.size).array())
        fields.forEach { field ->
            val utf8 = field.toByteArray(StandardCharsets.UTF_8)
            digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(utf8.size).array())
            digest.update(utf8)
        }
        return "sha256:" + digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun changed(original: String, current: String): Boolean = original != current

    /** Settings is a temporary detour; unlike Discard or Save it must not end an edit session. */
    fun resumeAfterSettings(returningToEditor: Boolean): Boolean = returningToEditor
}
