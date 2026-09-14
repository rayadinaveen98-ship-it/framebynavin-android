package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorGuidedTourPolicyTest {
    @Test
    fun `existing creators are not forced through the new tour`() {
        assertEquals(
            CreatorGuidedTourPolicy.CURRENT_VERSION,
            CreatorGuidedTourPolicy.initialCompletedVersion(existingCreatorSetupComplete = true),
        )
    }

    @Test
    fun `brand new installs remain eligible after setup`() {
        assertEquals(0, CreatorGuidedTourPolicy.initialCompletedVersion(existingCreatorSetupComplete = false))
        assertTrue(
            CreatorGuidedTourPolicy.shouldStart(
                accountOnboardingComplete = true,
                creatorSetupComplete = true,
                profileComplete = true,
                completedVersion = 0,
                externalLaunch = false,
            ),
        )
    }

    @Test
    fun `deep link or widget launch never gets blocked by tour`() {
        assertFalse(
            CreatorGuidedTourPolicy.shouldStart(
                accountOnboardingComplete = true,
                creatorSetupComplete = true,
                profileComplete = true,
                completedVersion = 0,
                externalLaunch = true,
            ),
        )
    }

    @Test
    fun `tour waits until creator setup is actually complete`() {
        assertFalse(
            CreatorGuidedTourPolicy.shouldStart(
                accountOnboardingComplete = true,
                creatorSetupComplete = false,
                profileComplete = false,
                completedVersion = 0,
                externalLaunch = false,
            ),
        )
    }

    @Test
    fun `completed tour does not reopen automatically`() {
        assertFalse(
            CreatorGuidedTourPolicy.shouldStart(
                accountOnboardingComplete = true,
                creatorSetupComplete = true,
                profileComplete = true,
                completedVersion = CreatorGuidedTourPolicy.CURRENT_VERSION,
                externalLaunch = false,
            ),
        )
    }

    @Test
    fun `tour order matches the real creator journey`() {
        val visited = buildList {
            var step: CreatorGuidedTourStep? = CreatorGuidedTourStep.TODAY
            while (step != null) {
                add(step)
                step = CreatorGuidedTourPolicy.next(step)
            }
        }
        assertEquals(
            listOf(
                CreatorGuidedTourStep.TODAY,
                CreatorGuidedTourStep.IDEAS,
                CreatorGuidedTourStep.PROJECT,
                CreatorGuidedTourStep.WORKSPACE,
                CreatorGuidedTourStep.INSIGHTS,
                CreatorGuidedTourStep.CONTROL,
            ),
            visited,
        )
    }
}
