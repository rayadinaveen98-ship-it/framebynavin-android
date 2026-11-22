package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Staged only: do not run without founder authorization. */
class P1GalleryImportPolicyTest {
    @Test fun boundedMediaBytes() {
        assertFalse(P1GalleryImportPolicy.validBytes(0))
        assertFalse(P1GalleryImportPolicy.validBytes(-1))
        assertTrue(P1GalleryImportPolicy.validBytes(1))
        assertTrue(P1GalleryImportPolicy.validBytes(40L * 1024L * 1024L))
        assertFalse(P1GalleryImportPolicy.validBytes(40L * 1024L * 1024L + 1))
    }

    @Test fun fifteenSecondVideoMaxIsInclusive() {
        assertFalse(P1GalleryImportPolicy.validVideoDuration(0))
        assertFalse(P1GalleryImportPolicy.validVideoDuration(-1))
        assertTrue(P1GalleryImportPolicy.validVideoDuration(15_000L))
        assertFalse(P1GalleryImportPolicy.validVideoDuration(15_001L))
    }

    @Test fun safeFramingClampsExtremeValues() {
        val bounded = P1GalleryImportPolicy.boundedFraming(
            P1MediaFraming(zoom = 99f, horizontal = -3f, vertical = 2.5f))
        assertEquals(2.5f, bounded.zoom, 0f)
        assertEquals(-1f, bounded.horizontal, 0f)
        assertEquals(1f, bounded.vertical, 0f)
        assertEquals(P1MediaFraming(), P1GalleryImportPolicy.boundedFraming(P1MediaFraming()))
    }

    @Test fun v337ActualHostSupportIsNotInvented() {
        assertEquals(3, P1ReminderScope.entries.size)
        assertTrue(P1V337HostCompatibility.liveEntranceAvailable(P1ReminderScope.CREATOR))
        assertTrue(P1V337HostCompatibility.liveEntranceAvailable(P1ReminderScope.PERSONAL))
        assertTrue(P1V337HostCompatibility.liveEntranceAvailable(P1ReminderScope.CHALLENGE_21))
    }
}
