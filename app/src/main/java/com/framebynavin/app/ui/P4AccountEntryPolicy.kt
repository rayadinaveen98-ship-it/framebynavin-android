package com.framebynavin.app.ui

import com.framebynavin.app.cloud.CloudCreatorAccountRoute

/**
 * P4 screen intent is distinct from backend identity state:
 * Google authentication might resolve an existing account OR an incomplete one.
 * The only way to create a Creator ID is an explicit user action.
 */
internal enum class P4AccountEntryStep {
    CHOICE,
    SIGN_IN,
    SIGN_UP,
    CREATOR_ID,
    ACCOUNT_REVIEW,
}

internal enum class P4AccountIntent { SIGN_IN, SIGN_UP }

internal object P4AccountEntryPolicy {
    fun destinationAfterGoogle(
        intent: P4AccountIntent,
        route: CloudCreatorAccountRoute,
    ): P4AccountEntryStep? = when (route) {
        CloudCreatorAccountRoute.RETURNING_CREATOR -> null // existing identity resumes
        CloudCreatorAccountRoute.CREATOR_ID_REQUIRED -> when (intent) {
            P4AccountIntent.SIGN_IN -> P4AccountEntryStep.ACCOUNT_REVIEW
            P4AccountIntent.SIGN_UP -> P4AccountEntryStep.CREATOR_ID
        }
        CloudCreatorAccountRoute.SIGN_IN_REQUIRED -> when (intent) {
            P4AccountIntent.SIGN_IN -> P4AccountEntryStep.SIGN_IN
            P4AccountIntent.SIGN_UP -> P4AccountEntryStep.SIGN_UP
        }
    }

    fun backFrom(step: P4AccountEntryStep): P4AccountEntryStep = when (step) {
        P4AccountEntryStep.CREATOR_ID -> P4AccountEntryStep.SIGN_UP
        else -> P4AccountEntryStep.CHOICE
    }

    /** Existing setup is already usable: never re-run account choice on upgrade. */
    fun shouldShowChoice(accountComplete: Boolean, setupComplete: Boolean,
        creatorProfileComplete: Boolean): Boolean =
        !accountComplete && !(setupComplete && creatorProfileComplete)

    fun usernameAllowed(username: String): Boolean =
        username.matches(Regex("[a-z0-9_]{3,24}"))
}
