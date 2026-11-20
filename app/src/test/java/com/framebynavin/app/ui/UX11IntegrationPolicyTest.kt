package com.framebynavin.app.ui

import com.framebynavin.app.ui.theme.V401WelcomeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX11 A–F integration contract tests for saved welcome choices,
 * premium account route semantics, face-crop bounds, and four real wizard
 * stages without skipping creation validation.
 */
class UX11IntegrationPolicyTest {
    @Test fun newWelcomeDefaultDoesNotOverrideExplicitOldSelections() {
        assertEquals(V401WelcomeStyle.CINEMATIC_EMBER,
            V401WelcomeStyle.fromStored(null))
        val older = listOf(
            V401WelcomeStyle.PARTICLE_SIGNATURE,
            V401WelcomeStyle.LIVING_FRAME,
            V401WelcomeStyle.PRISMATIC_BLOOM,
            V401WelcomeStyle.SPARK_FIRST_LIGHT,
        )
        for (saved in older) {
            assertEquals(saved, V401WelcomeStyle.fromStored(saved.name))
        }
        assertEquals(V401WelcomeStyle.PARTICLE_SIGNATURE,
            V401WelcomeStyle.fromStored("unknown-legacy-id"))
    }

    @Test fun onlyThreeNewWelcomeStylesUsePremiumRendererAndBoundedTiming() {
        val premium = listOf(
            V401WelcomeStyle.CINEMATIC_EMBER,
            V401WelcomeStyle.APERTURE_LIGHT_BLOOM,
            V401WelcomeStyle.VELVET_GLASS,
        )
        for (style in premium) assertTrue(UX11PremiumWelcomePolicy.supported(style))
        for (style in V401WelcomeStyle.entries.filter { it !in premium }) {
            assertFalse(UX11PremiumWelcomePolicy.supported(style))
        }
        assertEquals(2400L, UX11PremiumWelcomePolicy.DURATION_MS)
        assertEquals(0f, UX11PremiumWelcomePolicy.progress(0L, 100L, 1100L), 0.0001f)
        assertEquals(.5f, UX11PremiumWelcomePolicy.progress(600L, 100L, 1100L), 0.0001f)
        assertEquals(1f, UX11PremiumWelcomePolicy.progress(1600L, 100L, 1100L), 0.0001f)
    }

    @Test fun userFacingAuthChoiceNeverAssumesBackendIdentityOutcome() {
        assertTrue(UX11AuthPresentationPolicy.title(UX11AuthEntryMode.CHOOSE)
            .isNotBlank())
        assertTrue(UX11AuthPresentationPolicy.title(UX11AuthEntryMode.SIGN_UP)
            .contains("Backlot"))
        assertTrue(UX11AuthPresentationPolicy.title(UX11AuthEntryMode.LOG_IN)
            .contains("back"))
        assertEquals("CONNECTING TO GOOGLE…",
            UX11AuthPresentationPolicy.googleAction(
                UX11AuthEntryMode.SIGN_UP, busy = true))
        assertTrue(UX11AuthPresentationPolicy.googleAction(
            UX11AuthEntryMode.SIGN_UP, busy = false).contains("GOOGLE"))
        assertTrue(UX11AuthPresentationPolicy.googleAction(
            UX11AuthEntryMode.LOG_IN, busy = false).contains("GOOGLE"))
    }

    @Test fun personalComposerHasOnlyTextLabelsAndFourGatedSections() {
        assertEquals(listOf("Purpose","Schedule","Alert","Review"),
            UX11FComposerPresentationPolicy.titles)
        assertEquals(4, UX11FComposerPresentationPolicy.prompts.size)
        assertFalse(UX11FComposerPresentationPolicy.canReturn(0, 0, false))
        assertFalse(UX11FComposerPresentationPolicy.canReturn(1, 0, false))
        assertFalse(UX11FComposerPresentationPolicy.canReturn(2, 1, false))
        assertTrue(UX11FComposerPresentationPolicy.canReturn(0, 1, false))
        assertTrue(UX11FComposerPresentationPolicy.canReturn(1, 3, false))
        assertFalse(UX11FComposerPresentationPolicy.canReturn(0, 3, true))
        assertFalse(UX11FComposerPresentationPolicy.canReturn(-1, 2, false))
        assertFalse(UX11FComposerPresentationPolicy.canReturn(4, 3, false))
    }

    @Test fun originalCompanionFacesStayInsideEveryImageBoundary() {
        for (identity in V144GuideIdentity.entries) {
            for ((w,h) in listOf(192 to 192, 256 to 256, 160 to 256,
                    320 to 200, 64 to 64)) {
                val (at,size) = UX11CompanionFacePolicy.sourceWindow(identity,w,h)
                assertTrue(identity.name, at.x >= 0 && at.y >= 0)
                assertTrue(identity.name, size.width >= 1 && size.height >= 1)
                assertTrue(identity.name, at.x + size.width <= w)
                assertTrue(identity.name, at.y + size.height <= h)
                assertEquals(size.width, size.height)
            }
        }
    }
}
