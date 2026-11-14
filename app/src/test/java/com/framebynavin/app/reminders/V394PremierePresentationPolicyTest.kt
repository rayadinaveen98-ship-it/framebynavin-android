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

    @Test fun visualMotionRespectsAccessibilityAndBatterySaver() {
        assertTrue(V394PremiereMotionPolicy.animate(true, false))
        assertFalse(V394PremiereMotionPolicy.animate(false, false))
        assertFalse(V394PremiereMotionPolicy.animate(true, true))
        assertFalse(V394PremiereMotionPolicy.animate(false, true))
    }

    @Test fun denserAtmosphereStillHasBoundedBatterySaverBudget() {
        assertTrue(V394PremiereMotionPolicy.particleCount(false) >= 80)
        assertTrue(V394PremiereMotionPolicy.particleCount(true) <= 24)
        assertTrue(V394PremiereMotionPolicy.particleCount(true) <
            V394PremiereMotionPolicy.particleCount(false))
    }

    @Test fun challengeCannotAutomaticallyOpenFullScreenFromVoiceNotification() {
        assertFalse(V394PremierePresentationPolicy.automaticChallengeScreen())
    }
}
