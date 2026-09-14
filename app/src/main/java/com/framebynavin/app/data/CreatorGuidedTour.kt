package com.framebynavin.app.data

/** Final RC2 contextual first-run journey. It teaches the real product instead of a slideshow. */
enum class CreatorGuidedTourStep {
    TODAY,
    IDEAS,
    PROJECT,
    WORKSPACE,
    INSIGHTS,
    CONTROL,
}

object CreatorGuidedTourPolicy {
    const val CURRENT_VERSION = 1

    /**
     * Existing creators upgrading into v124 are considered complete so they are never forced
     * through onboarding again. Brand-new installs initialize at 0 and become eligible only after
     * account/creator setup is complete.
     */
    fun initialCompletedVersion(existingCreatorSetupComplete: Boolean): Int =
        if (existingCreatorSetupComplete) CURRENT_VERSION else 0

    fun shouldStart(
        accountOnboardingComplete: Boolean,
        creatorSetupComplete: Boolean,
        profileComplete: Boolean,
        completedVersion: Int,
        externalLaunch: Boolean,
    ): Boolean =
        !externalLaunch &&
            accountOnboardingComplete &&
            creatorSetupComplete &&
            profileComplete &&
            completedVersion < CURRENT_VERSION

    fun next(step: CreatorGuidedTourStep): CreatorGuidedTourStep? = when (step) {
        CreatorGuidedTourStep.TODAY -> CreatorGuidedTourStep.IDEAS
        CreatorGuidedTourStep.IDEAS -> CreatorGuidedTourStep.PROJECT
        CreatorGuidedTourStep.PROJECT -> CreatorGuidedTourStep.WORKSPACE
        CreatorGuidedTourStep.WORKSPACE -> CreatorGuidedTourStep.INSIGHTS
        CreatorGuidedTourStep.INSIGHTS -> CreatorGuidedTourStep.CONTROL
        CreatorGuidedTourStep.CONTROL -> null
    }
}
