package com.framebynavin.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** UX06E: pure policy scenarios; NOT RUN until consolidated post-UX10 QA. */
class V407ReminderAccessPolicyTest {
    @Test fun creatorVoiceAndAlarmMayRequestFullScreenWhenAndroidAllows() {
        assertTrue(V407ReminderAccessInspector.fullScreenRelevant(
            V407ReminderOwner.CREATOR, ReminderCheckInKind.VOICE,
        ))
        assertTrue(V407ReminderAccessInspector.fullScreenRelevant(
            V407ReminderOwner.CREATOR, ReminderCheckInKind.ALARM,
        ))
    }

    @Test fun personalVoiceUsesNotificationNotAutomaticFullScreen() {
        assertFalse(V407ReminderAccessInspector.fullScreenRelevant(
            V407ReminderOwner.PERSONAL, ReminderCheckInKind.VOICE,
        ))
        assertTrue(V407ReminderAccessInspector.fullScreenRelevant(
            V407ReminderOwner.PERSONAL, ReminderCheckInKind.ALARM,
        ))
    }

    @Test fun permittedSnapshotNeverImpliesSeenOrHeard() {
        val allowed = V407ReminderAccessSnapshot(
            notifications = V407Access.ALLOWED,
            channel = V407Access.ALLOWED,
            preciseTiming = V407Access.ALLOWED,
            fullScreen = V407Access.NOT_APPLICABLE,
        )
        assertFalse(allowed.needsAttention)
        assertFalse(allowed.unknown)
        // The snapshot intentionally contains only Android capabilities.
        // No delivered/seen/heard/Done state exists in the contract.
    }

    @Test fun unknownChannelIsNotMisrepresentedAsAllowed() {
        val uncreated = V407ReminderAccessSnapshot(
            notifications = V407Access.ALLOWED,
            channel = V407Access.UNKNOWN,
            preciseTiming = V407Access.ALLOWED,
            fullScreen = V407Access.DEGRADED,
        )
        assertTrue(uncreated.unknown)
        assertTrue(uncreated.needsAttention)
    }

    @Test fun blockedNotificationsRemainVisibleEvenWhenOtherPermissionsGranted() {
        val snapshot = V407ReminderAccessSnapshot(
            notifications = V407Access.BLOCKED,
            channel = V407Access.ALLOWED,
            preciseTiming = V407Access.ALLOWED,
            fullScreen = V407Access.ALLOWED,
        )
        assertTrue(snapshot.needsAttention)
    }
}
