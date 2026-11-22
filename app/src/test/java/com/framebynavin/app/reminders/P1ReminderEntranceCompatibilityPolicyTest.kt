package com.framebynavin.app.reminders

import com.framebynavin.app.personal.Phase1HabitAlertKind
import com.framebynavin.app.personal.Phase1HabitChallenge
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Only source-staged; targeted QA runs after explicit owner gate. */
class P1ReminderEntranceCompatibilityPolicyTest {
    @Test fun entranceRequiresMediaCurrentAlertAndUnlockedScreen() {
        assertTrue(P1GalleryGatePolicy.shouldOffer(true, true, true, false))
        assertFalse(P1GalleryGatePolicy.shouldOffer(false, true, true, false))
        assertFalse(P1GalleryGatePolicy.shouldOffer(true, false, true, false))
        assertFalse(P1GalleryGatePolicy.shouldOffer(true, true, false, false))
        assertFalse(P1GalleryGatePolicy.shouldOffer(true, true, true, true))
    }

    private val start = LocalDate.of(2026, 11, 1)
    private val candidate = Phase1HabitChallenge(
        id = "P1_qa_synthetic",
        activity = "Morning walk",
        startDate = start,
        zoneId = "Asia/Kolkata",
        alertTime = LocalTime.of(7, 30),
        alertKind = Phase1HabitAlertKind.ALARM,
    )

    @Test fun challengeEntranceNeedsLiveVoiceOrAlarmInDayRange() {
        assertTrue(P1GalleryGatePolicy.challengeEligible(candidate, start))
        assertTrue(P1GalleryGatePolicy.challengeEligible(candidate, start.plusDays(20)))
        assertFalse(P1GalleryGatePolicy.challengeEligible(candidate, start.minusDays(1)))
        assertFalse(P1GalleryGatePolicy.challengeEligible(candidate, start.plusDays(21)))
        assertFalse(P1GalleryGatePolicy.challengeEligible(candidate.copy(paused = true), start))
        assertFalse(P1GalleryGatePolicy.challengeEligible(candidate.copy(archived = true), start))
        assertFalse(P1GalleryGatePolicy.challengeEligible(candidate.copy(
            alertKind = Phase1HabitAlertKind.QUIET), start))
        assertTrue(P1GalleryGatePolicy.challengeEligible(candidate.copy(
            alertKind = Phase1HabitAlertKind.VOICE), start))
    }
}
