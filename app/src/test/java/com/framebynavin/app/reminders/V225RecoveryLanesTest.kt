package com.framebynavin.app.reminders

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V225RecoveryLanesTest {
    @Test fun creatorFailureNeverStarvesPersonalOrIdeaRecovery() = runBlocking {
        val called = mutableListOf<String>()
        val outcome = V225RecoveryLanes.run(
            creator = { called.add("creator"); error("Creator storage unavailable") },
            personal = { called.add("personal") },
            ideas = { called.add("ideas") },
        )
        assertEquals(listOf("creator", "personal", "ideas"), called)
        assertFalse(outcome.creatorRecovered)
        assertTrue(outcome.personalRecovered)
        assertTrue(outcome.ideasRecovered)
        assertFalse(outcome.allSucceeded)
    }

    @Test fun personalFailureStillLetsIdeaRecoveryRunAndRequestsRetry() = runBlocking {
        val called = mutableListOf<String>()
        val outcome = V225RecoveryLanes.run(
            creator = { called.add("creator") },
            personal = { called.add("personal"); error("Personal store temporarily unavailable") },
            ideas = { called.add("ideas") },
        )
        assertEquals(listOf("creator", "personal", "ideas"), called)
        assertTrue(outcome.creatorRecovered)
        assertFalse(outcome.personalRecovered)
        assertTrue(outcome.ideasRecovered)
        assertFalse(outcome.allSucceeded)
    }

    @Test fun healthyRecoveryCompletesAllThreeLanes() = runBlocking {
        val outcome = V225RecoveryLanes.run(creator = {}, personal = {}, ideas = {})
        assertTrue(outcome.allSucceeded)
    }

    @Test fun cancellationMustNotBeSwallowedByRetryMechanism() = runBlocking {
        var personalReached = false
        try {
            V225RecoveryLanes.run(
                creator = { throw CancellationException("Android stopping worker") },
                personal = { personalReached = true },
                ideas = {},
            )
            throw AssertionError("Cancellation must propagate")
        } catch (expected: CancellationException) {
            assertFalse(personalReached)
        }
    }
}
