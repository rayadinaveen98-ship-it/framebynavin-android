package com.framebynavin.app.ui

import com.framebynavin.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V144GuideSystemTest {

    @Test
    fun `current selectable roster contains only production-ready funny and cute`() {
        assertEquals(
            listOf(V144GuideIdentity.FUNNY, V144GuideIdentity.CUTE),
            V144SelectableGuides,
        )
        assertTrue(V144SelectableGuides.all { it.description.isNotBlank() })
    }

    @Test
    fun `legacy frame and navi selections migrate to cute`() {
        assertEquals(V144GuideIdentity.CUTE, v144SelectableGuideOrDefault(V144GuideIdentity.FRAME))
        assertEquals(V144GuideIdentity.CUTE, v144SelectableGuideOrDefault(V144GuideIdentity.NAVI))
        assertEquals(V144GuideIdentity.CUTE, v144SelectableGuideOrDefault(null))
        assertEquals(V144GuideIdentity.CUTE, v144SelectableGuideOrDefault(V144GuideIdentity.CUTE))
        assertEquals(V144GuideIdentity.FUNNY, v144SelectableGuideOrDefault(V144GuideIdentity.FUNNY))
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
    fun `cute setup starts from stable welcome raster and covers all states`() {
        CinePulseState.entries.forEach { state ->
            assertNotEquals(0, v144GuideDrawable(V144GuideIdentity.CUTE, state))
        }
        assertEquals(R.drawable.guide_cute_welcome, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.IDLE))
        assertEquals(R.drawable.guide_cute_welcome, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.PRESENT))
        assertEquals(R.drawable.guide_cute_welcome, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.POINT))
        assertEquals(R.drawable.guide_cute_welcome, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.WAVE))
        assertEquals(R.drawable.guide_cute_listening, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.LOOK))
        assertEquals(R.drawable.guide_cute_thinking, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.THINK))
        assertEquals(R.drawable.guide_cute_celebrate, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.SUCCESS))
        assertEquals(R.drawable.guide_cute_celebrate, v144GuideDrawable(V144GuideIdentity.CUTE, CinePulseState.CELEBRATE))
    }

    @Test
    fun `creator setup and guided tour semantic poses are covered for every selectable guide`() {
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

        V144SelectableGuides.forEach { guide ->
            (setupStates + tourStates).distinct().forEach { state ->
                assertNotEquals("$guide has no asset for $state", 0, v144GuideDrawable(guide, state))
            }
        }
    }
}
