package com.framebynavin.app.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2241WidgetSizingPolicyTest {
    @Test fun compactOnShortOrNarrowLauncherCells() {
        assertTrue(V2241WidgetSizingPolicy.compact(250, 120))
        assertTrue(V2241WidgetSizingPolicy.compact(200, 210))
        assertTrue(V2241WidgetSizingPolicy.compact(224, 175))
        assertTrue(V2241WidgetSizingPolicy.compact(225, 174))
    }

    @Test fun regularOnFullSizeAndUnknownPreviewDimensions() {
        assertFalse(V2241WidgetSizingPolicy.compact(250, 210))
        assertFalse(V2241WidgetSizingPolicy.compact(225, 175))
        assertFalse(V2241WidgetSizingPolicy.compact(0, 0))
    }
}
