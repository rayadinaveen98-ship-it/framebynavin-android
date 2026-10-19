package com.framebynavin.app.reminders

/**
 * Minimal, responsive reminder copy. These helpers never mutate timing,
 * snooze policy, dismiss semantics, or reminder data.
 */
internal object V200ReminderLayoutPolicy {
    fun isCompact(screenHeightDp: Int): Boolean = screenHeightDp < 720
    fun useHorizontalActions(screenWidthDp: Int): Boolean = screenWidthDp >= 390

    /** Show useful short direction only when it adds information beyond the title. */
    fun supportingAction(suggestedAction: String, title: String): String? {
        val value = suggestedAction.trim()
        if (value.isBlank() || value.equals(title.trim(), ignoreCase = true)) return null
        return value
    }
}
