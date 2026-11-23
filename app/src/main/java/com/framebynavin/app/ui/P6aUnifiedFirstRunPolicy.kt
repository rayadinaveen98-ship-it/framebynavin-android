package com.framebynavin.app.ui

/**
 * A single Creator-first first-run path, independent of which feature the
 * user later opens. Personal OS remains a normal Backlot feature.
 *
 * Old V393 first-run focus preferences are intentionally ignored, not erased.
 */
internal enum class P6aUnifiedFirstRunGate {
    ACCOUNT_CHOICE, COMPANION, FIVE_PAGE_SETUP, READY
}

internal object P6aUnifiedFirstRunPolicy {
    fun route(
        accountComplete: Boolean,
        setupComplete: Boolean,
        profileComplete: Boolean,
        companionSelected: Boolean,
    ): P6aUnifiedFirstRunGate = when {
        // Both flags verified: do not show any onboarding screen again,
        // including for an existing local user whose account flag is unset.
        setupComplete && profileComplete -> P6aUnifiedFirstRunGate.READY
        !accountComplete -> P6aUnifiedFirstRunGate.ACCOUNT_CHOICE
        !companionSelected -> P6aUnifiedFirstRunGate.COMPANION
        else -> P6aUnifiedFirstRunGate.FIVE_PAGE_SETUP
    }
}
