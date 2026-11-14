package com.framebynavin.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** V394 rollout guards: the full screen is opt-in, never a delivery override. */
class V394PremierePresentationPolicyTest {
    @Test fun creatorRequiresExplicitOptIn() {
        assertFalse(V394PremierePresentationPolicy.creator(false))
        assertTrue(V394PremierePresentationPolicy.creator(true))
    }

    @Test fun personalNeverExposesLockedOrMissingReminder() {
        assertFalse(V394PremierePresentationPolicy.personal(false, false, true))
        assertFalse(V394PremierePresentationPolicy.personal(true, true, true))
        assertFalse(V394PremierePresentationPolicy.personal(true, false, false))
        assertTrue(V394PremierePresentationPolicy.personal(true, false, true))
    }

    @Test fun challengeCannotAutomaticallyOpenFullScreenFromVoiceNotification() {
        assertFalse(V394PremierePresentationPolicy.automaticChallengeScreen())
    }
}
