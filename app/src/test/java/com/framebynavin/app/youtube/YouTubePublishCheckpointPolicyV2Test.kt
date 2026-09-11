package com.framebynavin.app.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubePublishCheckpointPolicyV2Test {
    @Test
    fun checkpointTargetsMatchLockedPublishingIntelligencePlan() {
        assertEquals(
            listOf(30, 120, 360, 1_440, 4_320, 10_080, 40_320),
            YouTubePublishCheckpointPolicy.targetMinutes,
        )
        assertEquals("30M", YouTubePublishCheckpointPolicy.label(30))
        assertEquals("24H", YouTubePublishCheckpointPolicy.label(1_440))
        assertEquals("28D", YouTubePublishCheckpointPolicy.label(40_320))
    }

    @Test
    fun dueTargetsNeverRewriteCapturedObservations() {
        val published = 1_000_000L
        val now = published + 7L * 60L * 60L * 1000L
        val due = YouTubePublishCheckpointPolicy.dueTargets(
            publishedAtMillis = published,
            nowMillis = now,
            captured = setOf(30, 120),
        )

        assertEquals(listOf(360), due)
    }

    @Test
    fun lateCaptureIsExplicitRatherThanPretendingExactCheckpoint() {
        assertEquals(
            YouTubeCheckpointQuality.NEAR_TARGET,
            YouTubePublishCheckpointPolicy.quality(targetMinutes = 30, actualAgeMinutes = 45),
        )
        assertEquals(
            YouTubeCheckpointQuality.LATE_CAPTURE,
            YouTubePublishCheckpointPolicy.quality(targetMinutes = 30, actualAgeMinutes = 600),
        )
        assertTrue(YouTubePublishCheckpointPolicy.quality(1_440, 3_000) == YouTubeCheckpointQuality.LATE_CAPTURE)
    }
}
