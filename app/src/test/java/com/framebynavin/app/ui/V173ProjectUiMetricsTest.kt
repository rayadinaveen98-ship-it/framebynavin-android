package com.framebynavin.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class V173ProjectUiMetricsTest {
    @Test fun projectChoiceAndSupportingCopyAreReadable() {
        assertTrue(V173ProjectUiMetrics.supportingText.value >= 12f)
        assertTrue(V173ProjectUiMetrics.choiceText.value >= 12f)
        assertTrue(V173ProjectUiMetrics.bodyText.value >= 13f)
        assertTrue(V173ProjectUiMetrics.reviewValueText.value >= 13f)
        assertTrue(V173ProjectUiMetrics.sectionHeadingText.value >= 18f)
    }

    @Test fun touchTargetsDoNotShrinkBelowAndroidRecommendedMinimum() {
        assertTrue(V173ProjectUiMetrics.touchTarget.value >= 48f)
        assertTrue(V173ProjectUiMetrics.actionMinHeight.value >= 48f)
        assertTrue(V173ProjectUiMetrics.reviewRowMinHeight.value >= 48f)
    }
}
