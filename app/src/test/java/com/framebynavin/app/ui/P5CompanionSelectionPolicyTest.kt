package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source staged only. Owner QA approval is required before execution. */
class P5CompanionSelectionPolicyTest {
    @Test fun onlyApprovedCharactersCanBePreviewedAndCommitted() {
        assertEquals(V144GuideIdentity.FUNNY,
            P5CompanionSelectionPolicy.previewable("FUNNY"))
        assertEquals(V144GuideIdentity.MIRA,
            P5CompanionSelectionPolicy.previewable("MIRA"))
        assertEquals(V144GuideIdentity.FUNNY,
            P5CompanionSelectionPolicy.previewable("FRAME"))
        assertEquals(V144GuideIdentity.FUNNY,
            P5CompanionSelectionPolicy.previewable("BROKEN"))
        assertTrue(P5CompanionSelectionPolicy.canCommit(V144GuideIdentity.FUNNY))
        assertTrue(P5CompanionSelectionPolicy.canCommit(V144GuideIdentity.MIRA))
        assertFalse(P5CompanionSelectionPolicy.canCommit(V144GuideIdentity.FRAME))
        assertFalse(P5CompanionSelectionPolicy.canCommit(V144GuideIdentity.NAVI))
        assertFalse(P5CompanionSelectionPolicy.canCommit(V144GuideIdentity.CUTE))
    }

    @Test fun eachPoseUsesTheExistingRealApprovedSpriteFamily() {
        assertEquals(CinePulseState.WAVE,
            P5CompanionSelectionPolicy.characterState(P5CompanionPose.WELCOME))
        assertEquals(CinePulseState.THINK,
            P5CompanionSelectionPolicy.characterState(P5CompanionPose.THINKING))
        assertEquals(CinePulseState.CELEBRATE,
            P5CompanionSelectionPolicy.characterState(P5CompanionPose.CELEBRATE))
    }

    @Test fun legacySavedGuideNamesRemainReadableWithoutMigration() {
        for (id in V144GuideIdentity.entries) {
            assertEquals(id, v145NormalizeSavedGuide(id.name))
        }
        assertEquals(null, v145NormalizeSavedGuide("unknown"))
    }
}
