package com.framebynavin.app.ui

import com.framebynavin.app.ui.theme.V401WelcomeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-only time/choice tests; visual fidelity still requires real device QA. */
class UX12D1SignatureTimelineTest {
    @Test
    fun approvedBrandMarkRevealsBeforeWordmarkAndTagline() {
        assertTrue(
            UX12D1SignatureTimeline.MARK_START_MS <
                UX12D1SignatureTimeline.WORDMARK_START_MS,
        )
        assertTrue(
            UX12D1SignatureTimeline.WORDMARK_START_MS <
                UX12D1SignatureTimeline.TAGLINE_START_MS,
        )
        assertTrue(UX12D1SignatureTimeline.mark(900L, false) > 0f)
        assertEquals(0f, UX12D1SignatureTimeline.wordmark(800L, false), 0f)
        assertEquals(0f, UX12D1SignatureTimeline.tagline(1_000L, false), 0f)
    }

    @Test
    fun markAndWordmarkAreFullyVisibleBeforeExitStarts() {
        val at = UX12D1SignatureTimeline.EXIT_START_MS
        assertEquals(1f, UX12D1SignatureTimeline.mark(at, false), 0f)
        assertEquals(1f, UX12D1SignatureTimeline.wordmark(at, false), 0f)
        assertEquals(1f, UX12D1SignatureTimeline.tagline(at, false), 0f)
        assertEquals(1f, UX12D1SignatureTimeline.exit(at, false), 0f)
    }

    @Test
    fun normalSignatureFadesAtEndOfExistingDuration() {
        assertEquals(0f, UX12D1SignatureTimeline.exit(UX11PremiumWelcomePolicy.DURATION_MS, false), 0f)
        assertTrue(UX12D1SignatureTimeline.exit(2_200L, false) < 1f)
    }

    @Test
    fun reducedMotionUsesStaticVisibleIdentity() {
        val at = 0L
        assertEquals(1f, UX12D1SignatureTimeline.mark(at, true), 0f)
        assertEquals(1f, UX12D1SignatureTimeline.wordmark(at, true), 0f)
        assertEquals(1f, UX12D1SignatureTimeline.tagline(at, true), 0f)
        assertEquals(1f, UX12D1SignatureTimeline.exit(at, true), 0f)
    }

    @Test
    fun premiumScopeLeavesClassicRoutesUntouched() {
        assertTrue(UX11PremiumWelcomePolicy.supported(V401WelcomeStyle.CINEMATIC_EMBER))
        assertTrue(UX11PremiumWelcomePolicy.supported(V401WelcomeStyle.APERTURE_LIGHT_BLOOM))
        assertTrue(UX11PremiumWelcomePolicy.supported(V401WelcomeStyle.VELVET_GLASS))
        assertFalse(UX11PremiumWelcomePolicy.supported(V401WelcomeStyle.PARTICLE_SIGNATURE))
    }
}
