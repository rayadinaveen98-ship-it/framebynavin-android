package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V227IdeaFooterPolicyTest {
    @Test fun compactPhoneStacksSaveCancel() {
        assertTrue(V227IdeaFooterPolicy.stacked(320f, 1f))
        assertTrue(V227IdeaFooterPolicy.stacked(379.9f, 1f))
    }

    @Test fun accessibleLargeTextStacksEvenOnWideScreen() {
        assertTrue(V227IdeaFooterPolicy.stacked(490f, 1.3f))
        assertTrue(V227IdeaFooterPolicy.stacked(490f, 2f))
    }

    @Test fun normalFontAndWideScreenPreserveHorizontalLayout() {
        assertFalse(V227IdeaFooterPolicy.stacked(380f, 1.0f))
        assertFalse(V227IdeaFooterPolicy.stacked(440f, 1.2f))
    }
}
