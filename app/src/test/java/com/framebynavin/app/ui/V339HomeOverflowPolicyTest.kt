package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The menu is presentation/navigation only. These tests intentionally do not
 * create notifications, acknowledge reminders, edit Spark, or touch photo data.
 */
class V339HomeOverflowPolicyTest {
    @Test fun threeHomeDestinationsAppearInTheRequestedOrder() {
        assertEquals(
            listOf(
                V339HomeOverflowAction.NOTIFICATION_CENTER,
                V339HomeOverflowAction.SPARK,
                V339HomeOverflowAction.GALLERY,
            ),
            V339HomeOverflowPolicy.actions,
        )
        assertEquals(3, V339HomeOverflowPolicy.actions.distinct().size)
    }

    @Test fun notificationAndGalleryRemainAvailableWhenSparkIsTurnedOff() {
        assertTrue(V339HomeOverflowPolicy.enabled(
            V339HomeOverflowAction.NOTIFICATION_CENTER, sparkEnabled = false))
        assertFalse(V339HomeOverflowPolicy.enabled(
            V339HomeOverflowAction.SPARK, sparkEnabled = false))
        assertTrue(V339HomeOverflowPolicy.enabled(
            V339HomeOverflowAction.GALLERY, sparkEnabled = false))
        assertTrue(V339HomeOverflowPolicy.enabled(
            V339HomeOverflowAction.SPARK, sparkEnabled = true))
    }

    @Test fun notificationDotMeansActionNeededNotUnread() {
        assertTrue(V339HomeOverflowPolicy.hasIndicator(
            V339HomeOverflowAction.NOTIFICATION_CENTER,
            needsReminderAction = true, hasSparkSuggestion = false, sparkEnabled = false))
        assertFalse(V339HomeOverflowPolicy.hasIndicator(
            V339HomeOverflowAction.NOTIFICATION_CENTER,
            needsReminderAction = false, hasSparkSuggestion = true, sparkEnabled = true))
    }

    @Test fun SparkSuggestionIsOnlyGoldWhenSparkIsEnabled() {
        assertFalse(V339HomeOverflowPolicy.hasIndicator(
            V339HomeOverflowAction.SPARK, false, true, sparkEnabled = false))
        assertFalse(V339HomeOverflowPolicy.hasIndicator(
            V339HomeOverflowAction.SPARK, false, false, sparkEnabled = true))
        assertTrue(V339HomeOverflowPolicy.hasIndicator(
            V339HomeOverflowAction.SPARK, false, true, sparkEnabled = true))
    }

    @Test fun collapsedButtonUsesRedActionBeforeGoldSparkAndNeverClaimsUnread() {
        assertEquals(V339HomeOverflowAction.NOTIFICATION_CENTER,
            V339HomeOverflowPolicy.collapsedIndicator(
                needsReminderAction = true, hasSparkSuggestion = true, sparkEnabled = true))
        assertEquals(V339HomeOverflowAction.SPARK,
            V339HomeOverflowPolicy.collapsedIndicator(
                needsReminderAction = false, hasSparkSuggestion = true, sparkEnabled = true))
        assertEquals(null, V339HomeOverflowPolicy.collapsedIndicator(
            needsReminderAction = false, hasSparkSuggestion = true, sparkEnabled = false))
        assertEquals(null, V339HomeOverflowPolicy.collapsedIndicator(
            needsReminderAction = false, hasSparkSuggestion = false, sparkEnabled = true))
    }

    @Test fun galleryNeverFakesAnAlertDot() {
        for (needed in listOf(false, true)) {
            for (suggestion in listOf(false, true)) {
                assertFalse(V339HomeOverflowPolicy.hasIndicator(
                    V339HomeOverflowAction.GALLERY, needed, suggestion, true))
            }
        }
    }
}
