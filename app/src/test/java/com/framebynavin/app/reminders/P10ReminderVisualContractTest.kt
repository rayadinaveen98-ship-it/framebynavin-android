package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P10 SOURCE-STAGED TEST CASES. These are NOT executed until the founder's
 * consolidated compile/QA gate after all visual stages are staged.
 */
internal class P10ReminderVisualContractTest {
    @Test fun onlyThreeApprovedBuiltInFamiliesAreVisible() {
        assertEquals(
            listOf("Living Bloom", "Pulse Studio", "Rise Core"),
            P10ReminderFamily.entries.map { it.label },
        )
    }

    @Test fun eachReminderDomainHasIndependentSensibleInitialSuggestion() {
        assertEquals(P10ReminderFamily.PULSE_STUDIO,
            P10ReminderVisualPolicy.suggestedFamily(P1ReminderScope.CREATOR))
        assertEquals(P10ReminderFamily.LIVING_BLOOM,
            P10ReminderVisualPolicy.suggestedFamily(P1ReminderScope.PERSONAL))
        assertEquals(P10ReminderFamily.RISE_CORE,
            P10ReminderVisualPolicy.suggestedFamily(P1ReminderScope.CHALLENGE_21))
        assertEquals(P10ReminderFamily.LIVING_BLOOM,
            P10ReminderVisualPolicy.resolve("LIVING_BLOOM", P1ReminderScope.CREATOR))
    }

    @Test fun retiredLegacyIdentifiersDoNotBecomeFamilySelections() {
        for (oldId in listOf("DIRECTOR", "PREMIERE", "APERTURE", "PERSONAL_SUNRISE", "PROJECTOR_LIGHT")) {
            assertEquals(P10ReminderFamily.PULSE_STUDIO,
                P10ReminderVisualPolicy.resolve(oldId, P1ReminderScope.CREATOR))
        }
    }

    @Test fun fullAndLowPowerMotionRemainVisibleButBounded() {
        val full = P10ReminderVisualPolicy.tuning(true, false, false, P10ReminderAct.ENTRANCE)
        val main = P10ReminderVisualPolicy.tuning(true, false, false, P10ReminderAct.MAIN)
        val saver = P10ReminderVisualPolicy.tuning(true, true, false, P10ReminderAct.ENTRANCE)
        assertTrue(full.animated)
        assertTrue(full.amplitude > main.amplitude)
        assertTrue(main.amplitude > 0f)
        assertTrue(saver.animated)
        assertTrue(saver.frameIntervalNanos > full.frameIntervalNanos)
        assertTrue(saver.activeMotes < full.activeMotes)
    }

    @Test fun systemAnimationOffNeverFallsBackToOldSceneOrBlocksControls() {
        val still = P10ReminderVisualPolicy.tuning(false, false, false, P10ReminderAct.MAIN)
        assertFalse(still.animated)
        assertEquals(0f, still.amplitude, 0.0001f)
        assertEquals(0, still.activeMotes)
    }

    @Test fun returningGalleryOwnersKeepTheirSelectedMediaOnUpgrade() {
        assertEquals(P10ReminderBackgroundSource.GALLERY,
            P10ReminderBackgroundPolicy.resolved(null, galleryAvailable = true))
        assertEquals(P10ReminderBackgroundSource.BUILT_IN,
            P10ReminderBackgroundPolicy.resolved(null, galleryAvailable = false))
    }

    @Test fun choosingBuiltInNeverImplicitlyErasesGalleryAndBrokenMediaFallsBack() {
        assertEquals(P10ReminderBackgroundSource.BUILT_IN,
            P10ReminderBackgroundPolicy.resolved("BUILT_IN", galleryAvailable = true))
        assertEquals(P10ReminderBackgroundSource.BUILT_IN,
            P10ReminderBackgroundPolicy.resolved("GALLERY", galleryAvailable = false))
        assertEquals(P10ReminderBackgroundSource.GALLERY,
            P10ReminderBackgroundPolicy.resolved("GALLERY", galleryAvailable = true))
    }

    @Test fun galleryDisplayRequiresActiveSelectionMediaAndUnlock() {
        assertFalse(P10ReminderBackgroundPolicy.showGallery(
            P10ReminderBackgroundSource.BUILT_IN, galleryAvailable = true, locked = false))
        assertFalse(P10ReminderBackgroundPolicy.showGallery(
            P10ReminderBackgroundSource.GALLERY, galleryAvailable = false, locked = false))
        assertFalse(P10ReminderBackgroundPolicy.showGallery(
            P10ReminderBackgroundSource.GALLERY, galleryAvailable = true, locked = true))
        assertTrue(P10ReminderBackgroundPolicy.showGallery(
            P10ReminderBackgroundSource.GALLERY, galleryAvailable = true, locked = false))
    }

    @Test fun entrancePreservesPrivacyAndRequiresDeliberateOpen() {
        assertFalse(P10ReminderVisualPolicy.visibleMedia(locked = true, hasSelectedMedia = true))
        assertTrue(P10ReminderVisualPolicy.visibleMedia(locked = false, hasSelectedMedia = true))
        assertFalse(P10ReminderVisualPolicy.shouldReveal(
            doubleTapped = true, accessibleOpen = false, unlocked = false))
        assertFalse(P10ReminderVisualPolicy.shouldReveal(
            doubleTapped = false, accessibleOpen = false, unlocked = true))
        assertTrue(P10ReminderVisualPolicy.shouldReveal(
            doubleTapped = true, accessibleOpen = false, unlocked = true))
        assertTrue(P10ReminderVisualPolicy.shouldReveal(
            doubleTapped = false, accessibleOpen = true, unlocked = true))
    }
}
