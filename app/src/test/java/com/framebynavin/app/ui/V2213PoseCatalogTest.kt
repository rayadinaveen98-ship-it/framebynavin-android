package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V2213PoseCatalogTest {
    @Test fun thereAreExactlyNineMeaningfulExpressionNames() {
        assertEquals(9, V2213Pose.entries.size)
        assertEquals(9, V2213Pose.entries.map { it.label }.distinct().size)
    }

    @Test fun allHistoricStatesMapToAnExplicitVisualExpression() {
        val expectation = mapOf(
            CinePulseState.IDLE to V2213Pose.REST,
            CinePulseState.REST to V2213Pose.REST,
            CinePulseState.WAVE to V2213Pose.WELCOME,
            CinePulseState.LISTEN to V2213Pose.LISTEN,
            CinePulseState.LOOK to V2213Pose.THINK,
            CinePulseState.THINK to V2213Pose.THINK,
            CinePulseState.POINT to V2213Pose.POINT,
            CinePulseState.NOD to V2213Pose.ENCOURAGE,
            CinePulseState.PRESENT to V2213Pose.PRESENT,
            CinePulseState.SUCCESS to V2213Pose.CELEBRATE,
            CinePulseState.CELEBRATE to V2213Pose.CELEBRATE,
            CinePulseState.WALK to V2213Pose.MOVE,
        )
        assertEquals(CinePulseState.entries.size, expectation.size)
        expectation.forEach { (state, pose) ->
            assertEquals("Incorrect pose for $state", pose, V2213PoseCatalog.pose(state))
        }
    }

    @Test fun eachOfThreeGuidesHasNineUniqueIndependentDrawableResourceIds() {
        val all = V2213PoseCatalog.reviewIdentities.flatMap { identity ->
            V2213Pose.entries.map { pose ->
                val drawable = V2213PoseCatalog.conceptDrawable(identity, pose)
                assertTrue("$identity / $pose missing", drawable != 0)
                drawable
            }
        }
        assertEquals(27, all.size)
        assertEquals("Art must not reuse one pose texture", 27, all.distinct().size)
    }

    @Test fun sparkRemainsOutsideExperimentalArtCatalog() {
        assertFalse(V144GuideIdentity.FUNNY in V2213PoseCatalog.reviewIdentities)
        assertThrows(IllegalStateException::class.java) {
            V2213PoseCatalog.conceptDrawable(
                V144GuideIdentity.FUNNY, V2213Pose.CELEBRATE)
        }
    }
}
