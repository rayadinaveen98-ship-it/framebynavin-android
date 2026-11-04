package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure permission projections, with no Android system calls or persistent writes. */
class V286PersonalReadinessPolicyTest {
    private fun snapshot(
        notifications: Boolean? = true,
        exact: Boolean? = true,
        fullScreen: Boolean? = true,
        saver: Boolean? = false,
        exception: Boolean? = false,
        alarms: Int = 1,
    ) = V286PersonalReadinessSnapshot(
        notificationsAllowed = notifications,
        exactTimingAllowed = exact,
        fullScreenAlarmAllowed = fullScreen,
        batterySaverOn = saver,
        batteryOptimizationExempt = exception,
        activeAlarmCount = alarms,
    )

    @Test fun notificationDenialIsARealBlocker() {
        val flags = V286PersonalReadinessPolicy.signals(snapshot(notifications = false))
        assertTrue(flags.any { it.label == "Notifications are blocked" &&
            it.severity == V286ReadinessSeverity.REVIEW })
        assertEquals("Action needed · personal notifications are blocked",
            V286PersonalReadinessPolicy.summary(flags))
    }

    @Test fun exactDeniedStillDescribesInexactFallbackNotTotalFailure() {
        val flags = V286PersonalReadinessPolicy.signals(snapshot(exact = false))
        assertTrue(flags.any { it.label == "Precise timing unavailable" &&
            it.detail.contains("inexact fallback") })
        assertFalse(V286PersonalReadinessPolicy.summary(flags).startsWith("Action needed"))
    }

    @Test fun fullScreenWarningOnlyAppliesToAnActiveAlarm() {
        val without = V286PersonalReadinessPolicy.signals(
            snapshot(fullScreen = false, alarms = 0))
        assertTrue(without.none { it.label.contains("Full-screen") })
        val with = V286PersonalReadinessPolicy.signals(
            snapshot(fullScreen = false, alarms = 2))
        assertTrue(with.any { it.label == "Full-screen alarm access restricted" })
    }

    @Test fun batterySaverMayDeferWithoutMarkingAlertsMissed() {
        val flags = V286PersonalReadinessPolicy.signals(snapshot(saver = true))
        assertTrue(flags.any { it.label == "Battery saver is active" &&
            it.severity == V286ReadinessSeverity.REVIEW })
        assertTrue(flags.none { it.label.contains("Missed") || it.label.contains("Failed") })
    }

    @Test fun unknownAccessIsNotReportedAsUserDenied() {
        val flags = V286PersonalReadinessPolicy.signals(
            snapshot(notifications = null, exact = null, fullScreen = null))
        assertTrue(flags.any { it.label == "Notification access unknown" &&
            it.severity == V286ReadinessSeverity.NOTE })
        assertTrue(flags.none { it.label == "Notifications are blocked" })
    }

    @Test fun allPermissionsPresentStillDoesNotGuaranteeDelivery() {
        val flags = V286PersonalReadinessPolicy.signals(snapshot())
        val label = V286PersonalReadinessPolicy.summary(flags)
        assertTrue(label.contains("still needs real-device testing"))
        assertTrue(flags.any { it.label == "Standard battery management" })
    }
}
