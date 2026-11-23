package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prepared source only. Do not execute before consolidated QA approval. */
class P7PersonalHomePolicyTest {
    @Test fun healthyStateEnablesAllFourDestinations() {
        val a = P7PersonalHomePolicy.available(true, false, true, false, false)
        assertTrue(a.createReminder)
        assertTrue(a.openSpaces)
        assertTrue(a.openChallenges)
        assertTrue(a.openGroups)
    }

    @Test fun reminderReadErrorCannotOfferReminderWrites() {
        val a = P7PersonalHomePolicy.available(true, true, true, false, false)
        assertFalse(a.createReminder)
        assertFalse(a.openSpaces)
        assertFalse(a.openGroups)
        assertTrue(a.openChallenges)
    }

    @Test fun busyReminderWriteDisablesCreationOnly() {
        val a = P7PersonalHomePolicy.available(true, false, true, false, true)
        assertFalse(a.createReminder)
        assertTrue(a.openSpaces)
        assertTrue(a.openChallenges)
        assertTrue(a.openGroups)
    }

    @Test fun challengeFailureIsIndependentFromReminderStore() {
        val a = P7PersonalHomePolicy.available(true, false, true, true, false)
        assertFalse(a.openChallenges)
        assertTrue(a.createReminder)
        assertTrue(a.openSpaces)
        assertTrue(a.openGroups)
    }

    @Test fun loadingNeverPretendsActionsAreAvailable() {
        val a = P7PersonalHomePolicy.available(false, false, false, false, false)
        assertFalse(a.createReminder)
        assertFalse(a.openSpaces)
        assertFalse(a.openChallenges)
        assertFalse(a.openGroups)
    }
}
