package com.framebynavin.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source staged only; execute during the founder-approved consolidated QA gate. */
class P10CreatorLockScreenMediaPolicyTest {
    @Test fun creatorGalleryIsPrivateUntilUserExplicitlyOptsIn() {
        assertFalse(P10CreatorLockScreenMediaPolicy.allowed(
            P1ReminderScope.CREATOR, locked = true, ownerOptedIn = false))
        assertTrue(P10CreatorLockScreenMediaPolicy.allowed(
            P1ReminderScope.CREATOR, locked = true, ownerOptedIn = true))
        assertTrue(P10CreatorLockScreenMediaPolicy.allowed(
            P1ReminderScope.CREATOR, locked = false, ownerOptedIn = false))
    }

    @Test fun personalAndChallengeGalleryNeverAppearOnKeyguard() {
        for (scope in listOf(P1ReminderScope.PERSONAL, P1ReminderScope.CHALLENGE_21)) {
            assertFalse(P10CreatorLockScreenMediaPolicy.allowed(
                scope, locked = true, ownerOptedIn = false))
            assertFalse(P10CreatorLockScreenMediaPolicy.allowed(
                scope, locked = true, ownerOptedIn = true))
            assertTrue(P10CreatorLockScreenMediaPolicy.allowed(
                scope, locked = false, ownerOptedIn = false))
        }
    }

    @Test fun galleryModeAndActualMediaAreStillRequired() {
        assertFalse(P10ReminderBackgroundPolicy.showGallery(
            P10ReminderBackgroundSource.BUILT_IN,
            galleryAvailable = true, locked = true,
            scope = P1ReminderScope.CREATOR, creatorLockScreenOptIn = true))
        assertFalse(P10ReminderBackgroundPolicy.showGallery(
            P10ReminderBackgroundSource.GALLERY,
            galleryAvailable = false, locked = true,
            scope = P1ReminderScope.CREATOR, creatorLockScreenOptIn = true))
        assertTrue(P10ReminderBackgroundPolicy.showGallery(
            P10ReminderBackgroundSource.GALLERY,
            galleryAvailable = true, locked = true,
            scope = P1ReminderScope.CREATOR, creatorLockScreenOptIn = true))
    }

    @Test fun legacyThreeArgumentCallsRemainPrivacySafe() {
        assertFalse(P10ReminderBackgroundPolicy.showGallery(
            P10ReminderBackgroundSource.GALLERY,
            galleryAvailable = true, locked = true))
        assertTrue(P10ReminderBackgroundPolicy.showGallery(
            P10ReminderBackgroundSource.GALLERY,
            galleryAvailable = true, locked = false))
    }
}
