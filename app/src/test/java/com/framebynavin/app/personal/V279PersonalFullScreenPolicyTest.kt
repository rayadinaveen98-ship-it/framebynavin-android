package com.framebynavin.app.personal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V279PersonalFullScreenPolicyTest {
    @Test fun onlyOptedInPersonalAlarmMayRequestFullScreen() {
        assertTrue(PersonalFullScreenPolicy.eligible(PersonalAlert.ALARM, false, true))
        assertFalse(PersonalFullScreenPolicy.eligible(PersonalAlert.VOICE, false, true))
        assertFalse(PersonalFullScreenPolicy.eligible(PersonalAlert.NOTIFICATION, false, true))
    }

    @Test fun deniedSpecialAccessNeverRequestsFullScreen() {
        for (alert in PersonalAlert.entries) {
            assertFalse(PersonalFullScreenPolicy.eligible(alert, false, false))
        }
    }

    @Test fun existingUnansweredFollowUpNeverInterruptsAgain() {
        for (alert in PersonalAlert.entries) {
            assertFalse(PersonalFullScreenPolicy.eligible(alert, true, true))
        }
    }
}
