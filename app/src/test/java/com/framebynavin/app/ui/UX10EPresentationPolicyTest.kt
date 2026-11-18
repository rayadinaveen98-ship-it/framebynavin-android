package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX10E authored source contract tests. NOT RUN under founder-approved
 * source-only staging; execute after UX10F + approved release integration.
 */
class UX10EPresentationPolicyTest {
    @Test
    fun `compact phone Insights tabs scroll rather than shrink four destinations`() {
        assertTrue(UX10EPresentationPolicy.scrollInsightsTabs(320, 1f))
        assertTrue(UX10EPresentationPolicy.scrollInsightsTabs(393, 1f))
        assertFalse(UX10EPresentationPolicy.scrollInsightsTabs(430, 1f))
        assertEquals(4, UX10EPresentationPolicy.INSIGHTS_TAB_COUNT)
    }

    @Test
    fun `large accessibility text forces readable tab strip on tablets`() {
        assertTrue(UX10EPresentationPolicy.scrollInsightsTabs(800, 1.35f))
        assertTrue(UX10EPresentationPolicy.scrollInsightsTabs(800, 2f))
        assertFalse(UX10EPresentationPolicy.scrollInsightsTabs(800, 1f))
        assertTrue(UX10EPresentationPolicy.INSIGHTS_MIN_TAB_WIDTH_DP >= 100)
    }

    @Test
    fun `invalid measurements use horizontal tab fallback rather than clipped labels`() {
        assertTrue(UX10EPresentationPolicy.scrollInsightsTabs(800, Float.NaN))
        assertTrue(UX10EPresentationPolicy.scrollInsightsTabs(
            800, Float.POSITIVE_INFINITY
        ))
        assertTrue(UX10EPresentationPolicy.scrollInsightsTabs(800, 0f))
    }

    @Test
    fun `genuine empty Activity Center preserves original per view meaning`() {
        assertEquals(
            "Nothing currently needs a response.",
            UX10EPresentationPolicy.activityEmptyCopy(true, false, "NEEDS_ACTION"),
        )
        assertEquals(
            "No confirmed activity history yet.",
            UX10EPresentationPolicy.activityEmptyCopy(true, false, "HISTORY"),
        )
        assertEquals(
            "No recorded reminder activity yet.",
            UX10EPresentationPolicy.activityEmptyCopy(true, false, "ALL"),
        )
    }

    @Test
    fun `unloaded local receipts never masquerade as empty saved activity`() {
        val copy = UX10EPresentationPolicy.activityEmptyCopy(
            allSourcesReady = false,
            readFailure = false,
            viewName = "HISTORY",
        )
        assertTrue(copy.contains("Loading"))
        assertFalse(copy.startsWith("No confirmed"))
    }

    @Test
    fun `failed private local read has priority over empty and loading`() {
        val copy = UX10EPresentationPolicy.activityEmptyCopy(
            allSourcesReady = false,
            readFailure = true,
            viewName = "ALL",
        )
        assertTrue(copy.contains("unavailable"))
        assertTrue(copy.contains("No saved records were changed"))
        assertFalse(copy.contains("No recorded reminder activity yet"))
    }

    @Test
    fun `widget visual preview preserves normal sized RemoteViews container`() {
        assertEquals(174, UX10EPresentationPolicy.widgetPreviewHeightDp(1f))
        assertEquals(174, UX10EPresentationPolicy.widgetPreviewHeightDp(1.34f))
        assertEquals(208, UX10EPresentationPolicy.widgetPreviewHeightDp(1.35f))
        assertEquals(208, UX10EPresentationPolicy.widgetPreviewHeightDp(2f))
    }

    @Test
    fun `unavailable font scale uses readable widget preview fallback`() {
        assertEquals(208, UX10EPresentationPolicy.widgetPreviewHeightDp(Float.NaN))
        assertEquals(208, UX10EPresentationPolicy.widgetPreviewHeightDp(0f))
    }
}
