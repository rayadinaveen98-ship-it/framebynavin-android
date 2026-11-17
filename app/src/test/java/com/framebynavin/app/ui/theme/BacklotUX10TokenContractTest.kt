package com.framebynavin.app.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Authored UX10A source contract tests. NOT EXECUTED until the founder-approved
 * consolidated certification gate on an integrated release candidate.
 */
class BacklotUX10TokenContractTest {
    @Test
    fun `interactive row and action touch targets are at least forty eight dp`() {
        assertTrue(BacklotUX10Tokens.minimumTouchTarget >= 48.dp)
        assertTrue(BacklotUX10Tokens.rowTarget >= 48.dp)
    }

    @Test
    fun `caption eyebrow and section typography are readable and ordered`() {
        assertTrue(BacklotUX10Tokens.eyebrow.value >= 10f)
        assertTrue(BacklotUX10Tokens.supporting.value >=
            BacklotUX10Tokens.eyebrow.value)
        assertTrue(BacklotUX10Tokens.body.value >=
            BacklotUX10Tokens.supporting.value)
        assertTrue(BacklotUX10Tokens.sectionTitle.value >=
            BacklotUX10Tokens.body.value)
    }

    @Test
    fun `density scale is predictable and keeps reasonable visual gutters`() {
        assertTrue(BacklotUX10Tokens.pageGutter >= 16.dp)
        assertTrue(BacklotUX10Tokens.compactPageGutter <=
            BacklotUX10Tokens.pageGutter)
        assertTrue(BacklotUX10Tokens.compactRadius <
            BacklotUX10Tokens.cardRadius)
        assertTrue(BacklotUX10Tokens.compactSectionGap >=
            BacklotUX10Tokens.rowGap)
    }

    @Test
    fun `quiet neutral outlines stay thinner than selected and error outlines`() {
        assertTrue(BacklotUX10Tokens.quietBorder().width < 1.dp)
        assertEquals(1.dp, BacklotUX10Tokens.activeBorder().width)
    }

    @Test
    fun `original approved visual identity and two selectable appearances remain`() {
        assertEquals(
            listOf(FrameTheme.DIRECTORS_CUT, FrameTheme.BACKLOT_LIGHT),
            BacklotSelectableAppearances,
        )
        assertTrue(BacklotSelectableAppearances.all {
            it.profile.visualLanguage == FrameVisualLanguage.CINEMATIC
        })
    }
}
