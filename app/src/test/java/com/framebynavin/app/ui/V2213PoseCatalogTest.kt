package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for a REVIEW-ONLY art direction. The live renderer still uses the
 * approved V217 concepts and Spark's historical original WebPs.
 */
class V2213PoseCatalogTest {
    @Test fun threeExperimentalGuidesEachHaveNineUniqueResourceIds() {
        assertEquals(9, V2213Pose.entries.size)
        assertEquals(9, V2213PoseCatalog.REQUIRED_COUNT_PER_GUIDE)
        assertEquals(3, V2213PoseCatalog.studiedGuides.size)
        val all = mutableSetOf<Int>()
        for (guide in V2213PoseCatalog.studiedGuides) {
            val ids = V2213Pose.entries.map { pose ->
                requireNotNull(V2213PoseCatalog.drawable(guide, pose))
            }
            assertEquals("Duplicate drawable within ${guide.name}", 9, ids.toSet().size)
            all += ids
        }
        assertEquals("27 distinct resource refs required", 27, all.size)
    }

    @Test fun sparkIsStrictlyExcludedAndNeverGetsAReplacementResource() {
        for (pose in V2213Pose.entries) {
            assertNull(V2213PoseCatalog.drawable(V144GuideIdentity.FUNNY, pose))
        }
        assertFalse(V144GuideIdentity.FUNNY in V2213PoseCatalog.studiedGuides)
    }

    @Test fun allHistoricalGuideStatesMapToASupportedVisualIntent() {
        val mapped = CinePulseState.entries.map(V2213PoseCatalog::poseFor)
        assertEquals(CinePulseState.entries.size, mapped.size)
        assertEquals(V2213Pose.WELCOME, V2213PoseCatalog.poseFor(CinePulseState.WAVE))
        assertEquals(V2213Pose.LISTEN, V2213PoseCatalog.poseFor(CinePulseState.LISTEN))
        assertEquals(V2213Pose.THINK, V2213PoseCatalog.poseFor(CinePulseState.THINK))
        assertEquals(V2213Pose.POINT, V2213PoseCatalog.poseFor(CinePulseState.POINT))
        assertEquals(V2213Pose.PRESENT, V2213PoseCatalog.poseFor(CinePulseState.PRESENT))
        assertEquals(V2213Pose.CELEBRATE, V2213PoseCatalog.poseFor(CinePulseState.CELEBRATE))
        assertEquals(V2213Pose.REST, V2213PoseCatalog.poseFor(CinePulseState.REST))
        assertTrue(mapped.all { it in V2213Pose.entries })
    }
}
