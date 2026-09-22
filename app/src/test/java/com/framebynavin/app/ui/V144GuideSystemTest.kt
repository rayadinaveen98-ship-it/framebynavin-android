package com.framebynavin.app.ui

import com.framebynavin.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V144GuideSystemTest {

    @Test
    fun `v145 selectable roster contains only funny and cute`() {
        assertEquals(
            listOf(V144GuideIdentity.FUNNY, V144GuideIdentity.CUTE),
            V145SelectableGuides,
        )
        assertTrue(V145SelectableGuides.all { it.description.isNotBlank() })
    }

    @Test
    fun `legacy frame and navi preferences migrate to funny`() {
        assertEquals(V144GuideIdentity.FUNNY, v145NormalizeSavedGuide("FRAME"))
        assertEquals(V144GuideIdentity.FUNNY, v145NormalizeSavedGuide("NAVI"))
        assertEquals(V144GuideIdentity.FUNNY, v145NormalizeSavedGuide("FUNNY"))
        assertEquals(V144GuideIdentity.CUTE, v145NormalizeSavedGuide("CUTE"))
        assertNull(v145NormalizeSavedGuide(null))
        assertNull(v145NormalizeSavedGuide("UNKNOWN"))
    }

    @Test
    fun `funny resolves every semantic state to a real raster asset`() {
        CinePulseState.entries.forEach { state ->
            assertNotEquals(0, v144GuideDrawable(V144GuideIdentity.FUNNY, state))
        }
        assertEquals(R.drawable.guide_funny_present, v144GuideDrawable(V144GuideIdentity.FUNNY, CinePulseState.PRESENT))
        assertEquals(R.drawable.guide_funny_point, v144GuideDrawable(V144GuideIdentity.FUNNY, CinePulseState.POINT))
        assertEquals(R.drawable.guide_funny_thinking, v144GuideDrawable(V144GuideIdentity.FUNNY, CinePulseState.THINK))
        assertEquals(R.drawable.guide_funny_celebrate, v144GuideDrawable(V144GuideIdentity.FUNNY, CinePulseState.SUCCESS))
        assertEquals(R.drawable.guide_funny_celebrate, v144GuideDrawable(V144GuideIdentity.FUNNY, CinePulseState.CELEBRATE))
    }

    @Test
    fun `cute resolves every semantic state to a real raster asset`() {
        CinePulseState.entries.forEach { state ->
            assertNotEquals(0, v144GuideDrawable(V144GuideIdentity.CUTE, state))
        }
        assertEquals(R.drawable.guide_cute_welcome, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.WAVE))
        assertEquals(R.drawable.guide_cute_listening, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.LOOK))
        assertEquals(R.drawable.guide_cute_thinking, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.THINK))
        assertEquals(R.drawable.guide_cute_celebrate, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.SUCCESS))
        assertEquals(R.drawable.guide_cute_celebrate, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.CELEBRATE))
    }

    @Test
    fun `creator setup and guided tour semantic poses are covered for both active guides`() {
        val setupStates = listOf(
            CinePulseState.PRESENT,
            CinePulseState.LOOK,
            CinePulseState.POINT,
            CinePulseState.THINK,
            CinePulseState.SUCCESS,
        )
        val tourStates = listOf(
            CinePulseState.PRESENT,
            CinePulseState.POINT,
            CinePulseState.WALK,
            CinePulseState.WAVE,
            CinePulseState.THINK,
            CinePulseState.CELEBRATE,
        )

        V145SelectableGuides.forEach { guide ->
            (setupStates + tourStates).distinct().forEach { state ->
                assertNotEquals("$guide has no asset for $state", 0, v144GuideDrawable(guide, state))
            }
        }
    }
}
