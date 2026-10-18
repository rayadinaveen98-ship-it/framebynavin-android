package com.framebynavin.app.ui

/**
 * A snapshot of draft field values for an unsaved-change prompt.
 * Count-prefixed segments make the comparison unambiguous even when users type separators.
 * Only fingerprints live in Compose saved instance state; no background data is written.
 */
internal object V172ProjectDraftPolicy {
    fun signature(fields: List<String>): String =
        fields.joinToString(separator = "") { field -> "${field.length}:$field" }

    fun changed(original: String, current: String): Boolean = original != current

    /** Settings is a temporary detour; unlike Discard or Save it must not end an edit session. */
    fun resumeAfterSettings(returningToEditor: Boolean): Boolean = returningToEditor
}
