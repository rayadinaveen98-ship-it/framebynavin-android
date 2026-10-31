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

    fun eligible(
        alert: PersonalAlert,
        followUp: Boolean,
        fullScreenPermissionAllowed: Boolean,
    ): Boolean = alert == PersonalAlert.ALARM &&
        !followUp && fullScreenPermissionAllowed
}
