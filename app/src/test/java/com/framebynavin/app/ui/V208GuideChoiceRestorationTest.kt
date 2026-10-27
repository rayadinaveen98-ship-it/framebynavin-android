package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V208GuideChoiceRestorationTest {
    @Test fun allOriginalGuideChoicesAreSelectable() {
        assertEquals(
            listOf(
                V144GuideIdentity.FRAME,
                V144GuideIdentity.NAVI,
                V144GuideIdentity.FUNNY,
                V144GuideIdentity.CUTE,
            ),
            V145SelectableGuides,
        )
    }

    @Test fun savedSelectionsRemainTheSameAfterAnUpgrade() {
        V144GuideIdentity.entries.forEach { guide ->
            assertEquals(guide, v145NormalizeSavedGuide(guide.name))
        }
        assertNull(v145NormalizeSavedGuide("UNKNOWN"))
        assertNull(v145NormalizeSavedGuide(null))
    }
}
