package com.framebynavin.app.ui

import com.framebynavin.app.ui.theme.BacklotUX10Tokens
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX10F source-authored tests for the handoff contract only.
 * NOT EXECUTED until the founder-approved integrated test gate after UX09
 * and the verified V337 P0 corrective lineage are reconciled.
 */
class UX10FContinuityPolicyTest {
    @Test
    fun `small phones stack the original analytics metrics`() {
        assertTrue(UX10FContinuityPolicy.stackMetricCards(320, 1f))
        assertTrue(UX10FContinuityPolicy.stackMetricCards(393, 1f))
        assertEquals(1, UX10FContinuityPolicy.metricColumns(393, 1f))
    }

    @Test
    fun `normal phones and tablets retain paired metrics`() {
        assertFalse(UX10FContinuityPolicy.stackMetricCards(400, 1f))
        assertEquals(2, UX10FContinuityPolicy.metricColumns(400, 1f))
        assertEquals(2, UX10FContinuityPolicy.metricColumns(800, 1f))
    }

    @Test
    fun `high font scales stack metrics even on wide tablets`() {
        assertTrue(UX10FContinuityPolicy.stackMetricCards(800, 1.35f))
        assertTrue(UX10FContinuityPolicy.stackMetricCards(800, 2f))
        assertEquals(1, UX10FContinuityPolicy.metricColumns(800, 1.35f))
    }

    @Test
    fun `invalid display scales fail toward readable single-column layout`() {
        assertTrue(UX10FContinuityPolicy.stackMetricCards(800, Float.NaN))
        assertTrue(UX10FContinuityPolicy.stackMetricCards(800, Float.POSITIVE_INFINITY))
        assertTrue(UX10FContinuityPolicy.stackMetricCards(800, 0f))
        assertEquals(1, UX10FContinuityPolicy.metricColumns(800, -1f))
    }

    @Test
    fun `Creator and YouTube metric columns use one common layout scale`() {
        assertEquals(1, UX10FContinuityPolicy.SINGLE_CARD_COLUMNS)
        assertEquals(2, UX10FContinuityPolicy.PAIRED_CARD_COLUMNS)
        assertTrue(UX10FContinuityPolicy.COMPACT_METRIC_WIDTH_DP >= 360)
        assertTrue(UX10FContinuityPolicy.LARGE_TEXT_SCALE > 1f)
    }

    @Test
    fun `Creator read failure must have priority over Activity Center empty`() {
        val failed = UX10EPresentationPolicy.activityEmptyCopy(
            allSourcesReady = false,
            readFailure = true,
            viewName = "NEEDS_ACTION",
        )
        assertTrue(failed.contains("unavailable"))
        assertFalse(failed.contains("Nothing currently needs a response."))
        assertFalse(failed.contains("Loading"))
    }

    @Test
    fun `original one-activity Challenge still contains exactly twenty one days`() {
        assertEquals(21, UX10DPersonalPresentationPolicy.CHALLENGE_DAY_COUNT)
        val days = (1..21).toList()
        val narrow = UX10DPersonalPresentationPolicy.challengeDayGridColumns(393, 2f)
        assertEquals(days, days.chunked(narrow).flatten())
    }

    @Test
    fun `approved design minimums remain common to every stage`() {
        assertTrue(BacklotUX10Tokens.minimumTouchTarget >= 48.dp)
        assertTrue(BacklotUX10Tokens.rowTarget >= 56.dp)
        assertTrue(BacklotUX10Tokens.cardRadius >=
            BacklotUX10Tokens.surfaceRadius)
        assertTrue(BacklotUX10Tokens.eyebrow.value >= 10f)
    }
}
