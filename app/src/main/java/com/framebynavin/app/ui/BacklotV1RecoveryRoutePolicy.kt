package com.framebynavin.app.ui

/**
 * Backlot 1.0 recovery: setup is not a side-effect of navigation.
 *
 * Settings, Personal, Ideas, Calendar, projects and Creator reminders must
 * remain navigable after account entry even if Creator profile setup has not
 * been completed. The original five-page wizard is available only after an
 * explicit user request from Home or Profile.
 */
internal object BacklotV1RecoveryRoutePolicy {
    fun needsAccountEntry(accountOnboardingComplete: Boolean): Boolean =
        !accountOnboardingComplete

    fun showCreatorSetup(
        accountOnboardingComplete: Boolean,
        explicitlyRequested: Boolean,
        creatorSetupComplete: Boolean,
        profileComplete: Boolean,
    ): Boolean =
        accountOnboardingComplete && explicitlyRequested &&
            (!creatorSetupComplete || !profileComplete)
}
