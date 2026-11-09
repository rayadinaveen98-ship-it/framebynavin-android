package com.framebynavin.app.personal

/**
 * Full-screen presentation is restricted to an explicitly configured Personal
 * ALARM on its original occurrence. Voice and ordinary notifications remain
 * high-priority tappable notifications; a follow-up never interrupts again.
 *
 * Android still decides whether to launch the FSI. On an unlocked phone it may
 * show only a heads-up notification; denied access falls back to tap-to-open.
 */
internal object PersonalFullScreenPolicy {
    const val EXTRA_ALARM_VISUAL = "backlot.personal.alarm_visual"

    /**
     * Only the FIRST presentation of an explicit ALARM may interrupt.
     * Automatically repeated audio and optional follow-ups stay actionable
     * notifications, never a second involuntary over-keyguard Activity.
     *
     * The default keeps older, three-argument call sites source-compatible.
     */
    fun eligible(
        alert: PersonalAlert,
        followUp: Boolean,
        fullScreenPermissionAllowed: Boolean,
        automaticRetry: Boolean = false,
    ): Boolean = alert == PersonalAlert.ALARM &&
        !followUp && !automaticRetry && fullScreenPermissionAllowed

    /**
     * An FSI PendingIntent is distinct from the ordinary tap PendingIntent.
     * Its receiver opens over keyguard only after matching the active lease.
     * Text/voice/quiet follow-ups and automatic retries cannot request it.
     */
    const val FSI_OPEN_ACTION = "com.framebynavin.app.personal.ALARM_FULLSCREEN"

}
