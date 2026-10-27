package com.framebynavin.app.reminders

/** Presentation-only motion choices, never part of alarm and voice callbacks. */
internal object V201ReminderMotionPolicy {
    const val PRESS_MILLIS = 140
    const val OPTIONS_ENTER_MILLIS = 220
    const val OPTIONS_EXIT_MILLIS = 160
    const val FADE_MILLIS = 150

    /** Press feedback should be responsive without deforming text. */
    fun targetScale(pressed: Boolean, prominent: Boolean, motionEnabled: Boolean): Float =
        when {
            !motionEnabled || !pressed -> 1f
            prominent -> .976f
            else -> .985f
        }
}
