package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX10B source-only contracts. Tests authored but NOT executed until the
 * owner-approved consolidated Android/JVM/physical certification gate.
 */
class UX10BPresentationPolicyTest {
    @Test
    fun `small first-run layouts stack without changing selected values`() {
        assertTrue(UX10BPresentationPolicy.stackedFirstRunChoices(320, 1f))
        assertTrue(UX10BPresentationPolicy.stackedFirstRunChoices(359, 1f))
        assertFalse(UX10BPresentationPolicy.stackedFirstRunChoices(360, 1f))
        assertTrue(UX10BPresentationPolicy.stackedFirstRunChoices(430, 1.35f))
        assertFalse(UX10BPresentationPolicy.stackedFirstRunChoices(430, 1.0f))
    }

    @Test
    fun `Home actions stack for large text and narrow phones`() {
        assertTrue(UX10BPresentationPolicy.stackedHomeActions(320, 1.0f))
        assertTrue(UX10BPresentationPolicy.stackedHomeActions(360, 2.0f))
        assertFalse(UX10BPresentationPolicy.stackedHomeActions(393, 1.0f))
    }

    @Test
    fun `bottom navigation never chooses unreadable shrunken text`() {
        assertFalse(
            UX10BPresentationPolicy.showBottomNavigationText(55f, 1.0f, "Insights")
        )
        assertTrue(
            UX10BPresentationPolicy.showBottomNavigationText(60f, 1.0f, "Insights")
        )
        assertFalse(
            UX10BPresentationPolicy.showBottomNavigationText(65f, 2.0f, "Insights")
        )
        assertTrue(
            UX10BPresentationPolicy.showBottomNavigationText(120f, 2.0f, "Insights")
        )
    }

    @Test
    fun `narrow navigation keeps accessible icon-only controls`() {
        assertFalse(UX10BPresentationPolicy.showBottomNavigationText(45f, 1.0f, "Home"))
        assertFalse(UX10BPresentationPolicy.showBottomNavigationText(50f, 1.0f, "Home"))
        assertTrue(UX10BPresentationPolicy.showBottomNavigationText(60f, 1.0f, "Home"))
        assertFalse(UX10BPresentationPolicy.showBottomNavigationText(75f, 1.6f, "Home"))
    }

    @Test
    fun `invalid viewport measurements fail closed to readable icon only`() {
        assertFalse(UX10BPresentationPolicy.showBottomNavigationText(-1f, 1f, "Projects"))
        assertFalse(UX10BPresentationPolicy.showBottomNavigationText(
            Float.NaN, 1f, "Projects"
        ))
        assertFalse(UX10BPresentationPolicy.showBottomNavigationText(
            80f, Float.POSITIVE_INFINITY, "Projects"
        ))
        assertFalse(UX10BPresentationPolicy.showBottomNavigationText(
            80f, 0f, "Projects"
        ))
    }

    @Test
    fun `font scale threshold and viewport boundaries remain deterministic`() {
        assertTrue(UX10BPresentationPolicy.LARGE_TEXT_SCALE > 1f)
        assertTrue(UX10BPresentationPolicy.NARROW_PHONE_DP >= 320)
        assertTrue(
            UX10BPresentationPolicy.stackedHomeActions(
                UX10BPresentationPolicy.NARROW_PHONE_DP,
                UX10BPresentationPolicy.LARGE_TEXT_SCALE,
            )
        )
    }
}
