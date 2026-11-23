package com.framebynavin.app.ui

import com.framebynavin.app.cloud.CloudCreatorAccountRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Founder P4 approval: prepare these cases; DO NOT run any test before the
 * consolidated QA gate. Pure policies, no Google authentication or network.
 */
class P4AccountEntryPolicyTest {
    @Test fun signInDoesNotSilentlyCreateCreatorProfile() {
        assertEquals(
            P4AccountEntryStep.ACCOUNT_REVIEW,
            P4AccountEntryPolicy.destinationAfterGoogle(
                P4AccountIntent.SIGN_IN,
                CloudCreatorAccountRoute.CREATOR_ID_REQUIRED,
            ),
        )
        assertEquals(
            P4AccountEntryStep.SIGN_IN,
            P4AccountEntryPolicy.destinationAfterGoogle(
                P4AccountIntent.SIGN_IN,
                CloudCreatorAccountRoute.SIGN_IN_REQUIRED,
            ),
        )
    }

    @Test fun signUpRequiresAnExplicitCreatorIdForIncompleteGoogleIdentity() {
        assertEquals(
            P4AccountEntryStep.CREATOR_ID,
            P4AccountEntryPolicy.destinationAfterGoogle(
                P4AccountIntent.SIGN_UP,
                CloudCreatorAccountRoute.CREATOR_ID_REQUIRED,
            ),
        )
        assertEquals(
            P4AccountEntryStep.SIGN_UP,
            P4AccountEntryPolicy.destinationAfterGoogle(
                P4AccountIntent.SIGN_UP,
                CloudCreatorAccountRoute.SIGN_IN_REQUIRED,
            ),
        )
    }

    @Test fun alreadyCompletedGoogleCreatorReturnsWithoutSignupRepetition() {
        for (intent in P4AccountIntent.entries) {
            assertNull(
                P4AccountEntryPolicy.destinationAfterGoogle(
                    intent, CloudCreatorAccountRoute.RETURNING_CREATOR))
        }
    }

    @Test fun firstRunChoiceIsNeverForcedUponExistingCompletedLocalSetup() {
        assertTrue(P4AccountEntryPolicy.shouldShowChoice(false, false, false))
        assertTrue(P4AccountEntryPolicy.shouldShowChoice(false, true, false))
        assertFalse(P4AccountEntryPolicy.shouldShowChoice(true, false, false))
        assertFalse(P4AccountEntryPolicy.shouldShowChoice(false, true, true))
        assertFalse(P4AccountEntryPolicy.shouldShowChoice(true, true, true))
    }

    @Test fun backNavigationCannotSilentlyCompleteAnAccount() {
        assertEquals(P4AccountEntryStep.CHOICE,
            P4AccountEntryPolicy.backFrom(P4AccountEntryStep.SIGN_IN))
        assertEquals(P4AccountEntryStep.CHOICE,
            P4AccountEntryPolicy.backFrom(P4AccountEntryStep.SIGN_UP))
        assertEquals(P4AccountEntryStep.CHOICE,
            P4AccountEntryPolicy.backFrom(P4AccountEntryStep.ACCOUNT_REVIEW))
        assertEquals(P4AccountEntryStep.SIGN_UP,
            P4AccountEntryPolicy.backFrom(P4AccountEntryStep.CREATOR_ID))
    }

    @Test fun creatorUsernameMatchesExistingBackendNormalizationRules() {
        assertTrue(P4AccountEntryPolicy.usernameAllowed("abc"))
        assertTrue(P4AccountEntryPolicy.usernameAllowed("a_3"))
        assertTrue(P4AccountEntryPolicy.usernameAllowed("a".repeat(24)))
        assertFalse(P4AccountEntryPolicy.usernameAllowed("ab"))
        assertFalse(P4AccountEntryPolicy.usernameAllowed("a".repeat(25)))
        assertFalse(P4AccountEntryPolicy.usernameAllowed("Creator"))
        assertFalse(P4AccountEntryPolicy.usernameAllowed("has space"))
        assertFalse(P4AccountEntryPolicy.usernameAllowed("name.with.dot"))
    }
}
