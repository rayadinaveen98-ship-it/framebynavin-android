package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Source-staged regression coverage only; test execution is deferred until
 * the founder explicitly approves the consolidated QA campaign.
 */
class P6aUnifiedFirstRunPolicyTest {
    private val policy = P6aUnifiedFirstRunPolicy

    @Test fun freshInstallMustStartWithAccountChoice() {
        assertEquals(P6aUnifiedFirstRunGate.ACCOUNT_CHOICE,
            policy.route(false, false, false, false))
    }

    @Test fun afterAccountChoiceSelectCompanionBeforeFullWizard() {
        assertEquals(P6aUnifiedFirstRunGate.COMPANION,
            policy.route(true, false, false, false))
    }

    @Test fun allNewUsersWithCompanionReachOriginalFivePageWizard() {
        assertEquals(P6aUnifiedFirstRunGate.FIVE_PAGE_SETUP,
            policy.route(true, false, false, true))
    }

    @Test fun incompleteSetupNeedsFivePageWizardRegardlessOfSavedLegacyFocus() {
        // The old Creator/Personal/Both first-run preference is not an input.
        assertEquals(P6aUnifiedFirstRunGate.FIVE_PAGE_SETUP,
            policy.route(true, true, false, true))
        assertEquals(P6aUnifiedFirstRunGate.FIVE_PAGE_SETUP,
            policy.route(true, false, true, true))
    }

    @Test fun fullySetupUsersNeverSeeAnotherGateEvenWithoutAccountOrCompanionFlag() {
        for (account in listOf(false, true)) {
            for (guide in listOf(false, true)) {
                assertEquals(P6aUnifiedFirstRunGate.READY,
                    policy.route(account, true, true, guide))
            }
        }
    }

    @Test fun personalAndHomeClicksCannotSelectOnboardingDestination() {
        // No feature, selected Home tab, Personal entry, or navigation input
        // exists in policy, so these taps cannot change first-run routing.
        repeat(20) {
            assertEquals(P6aUnifiedFirstRunGate.READY,
                policy.route(true, true, true, true))
        }
    }
}
