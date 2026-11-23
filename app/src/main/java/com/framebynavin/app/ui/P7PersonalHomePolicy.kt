package com.framebynavin.app.ui

/** Point 7 presentation-only affordances. No Personal storage writes or alarms. */
internal data class P7PersonalHomeActions(
    val createReminder: Boolean,
    val openSpaces: Boolean,
    val openChallenges: Boolean,
    val openGroups: Boolean,
)

internal object P7PersonalHomePolicy {
    fun available(
        remindersLoaded: Boolean,
        reminderReadError: Boolean,
        challengesLoaded: Boolean,
        challengeReadError: Boolean,
        reminderWriteBusy: Boolean,
    ): P7PersonalHomeActions {
        val remindersReady = remindersLoaded && !reminderReadError
        return P7PersonalHomeActions(
            createReminder = remindersReady && !reminderWriteBusy,
            openSpaces = remindersReady,
            openChallenges = challengesLoaded && !challengeReadError,
            openGroups = remindersReady,
        )
    }
}
