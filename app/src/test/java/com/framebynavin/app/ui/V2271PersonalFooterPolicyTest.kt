package com.framebynavin.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2271PersonalFooterPolicyTest {
    @Test fun narrowPhonesStackThePrimaryAndSecondaryActions() {
        assertTrue(V2271PersonalFooterPolicy.vertical(320f, 1f))
        assertTrue(V2271PersonalFooterPolicy.vertical(379f, 1f))
        assertFalse(V2271PersonalFooterPolicy.vertical(380f, 1f))
    }

    @Test fun largeAccessibilityFontsStackEvenOnLargeDevices() {
        assertTrue(V2271PersonalFooterPolicy.vertical(480f, 1.3f))
        assertTrue(V2271PersonalFooterPolicy.vertical(480f, 1.75f))
        assertFalse(V2271PersonalFooterPolicy.vertical(480f, 1.2f))
    }
}
