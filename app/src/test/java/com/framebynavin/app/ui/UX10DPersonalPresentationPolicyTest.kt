package com.framebynavin.app.ui

import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.BacklotUX10Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX10D authored source tests ONLY. Per founder order these are not run until
 * the approved integrated candidate and physical reminder/Challenge checks.
 */
class UX10DPersonalPresentationPolicyTest {
    @Test
    fun `narrow phone stacks real Space metrics instead of shrinking captions`() {
        assertTrue(UX10DPersonalPresentationPolicy.stackSpaceMetrics(320, 1f))
        assertTrue(UX10DPersonalPresentationPolicy.stackSpaceMetrics(393, 1f))
        assertFalse(UX10DPersonalPresentationPolicy.stackSpaceMetrics(420, 1f))
        assertEquals(3, UX10DPersonalPresentationPolicy.SPACE_METRIC_COUNT)
    }

    @Test
    fun `large accessibility font stacks Space metrics even on a tablet`() {
        assertTrue(UX10DPersonalPresentationPolicy.stackSpaceMetrics(800, 1.35f))
        assertTrue(UX10DPersonalPresentationPolicy.stackSpaceMetrics(800, 2f))
        assertFalse(UX10DPersonalPresentationPolicy.stackSpaceMetrics(800, 1f))
    }

    @Test
    fun `Personal Done Snooze and group controls stack at high font and narrow width`() {
        assertTrue(UX10DPersonalPresentationPolicy.stackReminderResponses(320, 1f))
        assertTrue(UX10DPersonalPresentationPolicy.stackReminderResponses(430, 1.35f))
        assertFalse(UX10DPersonalPresentationPolicy.stackReminderResponses(430, 1f))
    }

    @Test
    fun `legacy Challenge day navigation respects both original endpoints`() {
        assertTrue(UX10DPersonalPresentationPolicy.stackChallengeDayNavigation(320, 1f))
        assertTrue(UX10DPersonalPresentationPolicy.stackChallengeDayNavigation(800, 2f))
        assertFalse(UX10DPersonalPresentationPolicy.stackChallengeDayNavigation(430, 1f))
    }

    @Test
    fun `one activity challenge remains twenty one distinct chronological dates`() {
        assertEquals(21, UX10DPersonalPresentationPolicy.CHALLENGE_DAY_COUNT)
        val narrowColumns = UX10DPersonalPresentationPolicy.challengeDayGridColumns(393, 1f)
        assertEquals(4, narrowColumns)
        val days = (1..UX10DPersonalPresentationPolicy.CHALLENGE_DAY_COUNT).toList()
        assertEquals(days, days.chunked(narrowColumns).flatten())
        assertEquals(6, days.chunked(narrowColumns).size)
    }

    @Test
    fun `wide Challenge grid uses seven columns unless accessibility requires four`() {
        assertEquals(7, UX10DPersonalPresentationPolicy.challengeDayGridColumns(800, 1f))
        assertEquals(7, UX10DPersonalPresentationPolicy.challengeDayGridColumns(480, 1f))
        assertEquals(4, UX10DPersonalPresentationPolicy.challengeDayGridColumns(480, 1.35f))
        assertEquals(4, UX10DPersonalPresentationPolicy.challengeDayGridColumns(320, 2f))
    }

    @Test
    fun `long Personal purpose labels receive more room without data defaults`() {
        assertEquals(148, UX10DPersonalPresentationPolicy.purposeCardWidthDp(1f))
        assertEquals(220, UX10DPersonalPresentationPolicy.purposeCardWidthDp(1.35f))
        assertEquals(220, UX10DPersonalPresentationPolicy.purposeCardWidthDp(2f))
    }

    @Test
    fun `invalid display scales favor readable fallback and never choose seven tiny cells`() {
        assertTrue(UX10DPersonalPresentationPolicy.stackSpaceMetrics(800, Float.NaN))
        assertTrue(UX10DPersonalPresentationPolicy.stackReminderResponses(
            800, Float.POSITIVE_INFINITY
        ))
        assertEquals(4, UX10DPersonalPresentationPolicy.challengeDayGridColumns(800, 0f))
        assertEquals(220, UX10DPersonalPresentationPolicy.purposeCardWidthDp(Float.NaN))
    }

    @Test
    fun `Personal UI actions and selected borders retain usable common minimums`() {
        assertTrue(BacklotUX10Tokens.minimumTouchTarget >= 48.dp)
        assertTrue(BacklotUX10Tokens.rowTarget >= 56.dp)
        assertEquals(1.dp, BacklotUX10Tokens.activeBorder().width)
        assertTrue(BacklotUX10Tokens.quietBorder().width <
            BacklotUX10Tokens.activeBorder().width)
    }

    @Test
    fun `existing Personal save cancel responsive thresholds are preserved`() {
        assertTrue(V2271PersonalFooterPolicy.vertical(320f, 1f))
        assertTrue(V2271PersonalFooterPolicy.vertical(430f, 1.3f))
        assertFalse(V2271PersonalFooterPolicy.vertical(430f, 1f))
    }
}
