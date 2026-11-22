package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklotV1RecoveryRoutePolicyTest {
    @Test fun freshInstallMustFinishAccountDecisionBeforeCreatorWizard() {
        assertTrue(BacklotV1RecoveryRoutePolicy.needsAccountEntry(false))
        assertFalse(BacklotV1RecoveryRoutePolicy.showCreatorSetup(
            accountOnboardingComplete = false, explicitlyRequested = true,
            creatorSetupComplete = false, profileComplete = false))
    }

    @Test fun signedInPersonalUserMayNavigateEveryTabWithoutCreatorWizard() {
        assertFalse(BacklotV1RecoveryRoutePolicy.needsAccountEntry(true))
        assertFalse(BacklotV1RecoveryRoutePolicy.showCreatorSetup(
            accountOnboardingComplete = true, explicitlyRequested = false,
            creatorSetupComplete = false, profileComplete = false))
    }

    @Test fun creatorWizardRequiresAnExplicitHomeOrProfileRequest() {
        assertTrue(BacklotV1RecoveryRoutePolicy.showCreatorSetup(
            accountOnboardingComplete = true, explicitlyRequested = true,
            creatorSetupComplete = false, profileComplete = false))
        assertTrue(BacklotV1RecoveryRoutePolicy.showCreatorSetup(
            accountOnboardingComplete = true, explicitlyRequested = true,
            creatorSetupComplete = true, profileComplete = false))
    }

    @Test fun completedProfileWithMissingSetupReceiptCanBeRepairedExplicitly() {
        assertTrue(BacklotV1RecoveryRoutePolicy.showCreatorSetup(
            accountOnboardingComplete = true, explicitlyRequested = true,
            creatorSetupComplete = false, profileComplete = true))
        assertFalse(BacklotV1RecoveryRoutePolicy.showCreatorSetup(
            accountOnboardingComplete = true, explicitlyRequested = false,
            creatorSetupComplete = false, profileComplete = true))
    }

    @Test fun completedCreatorIsNeverForcedBackIntoSetup() {
        for (requested in listOf(false, true)) {
            assertFalse(BacklotV1RecoveryRoutePolicy.showCreatorSetup(
                accountOnboardingComplete = true, explicitlyRequested = requested,
                creatorSetupComplete = true, profileComplete = true))
        }
    }

    @Test fun cancelOnlyClearsTransientRequestAndDoesNotChangeSavedProfile() {
        assertTrue(BacklotV1RecoveryRoutePolicy.showCreatorSetup(
            accountOnboardingComplete = true, explicitlyRequested = true,
            creatorSetupComplete = false, profileComplete = false))
        assertFalse(BacklotV1RecoveryRoutePolicy.showCreatorSetup(
            accountOnboardingComplete = true, explicitlyRequested = false,
            creatorSetupComplete = false, profileComplete = false))
    }
}
