package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Source-staged contract tests. Run only during the owner-gated consolidated R2
 * stabilization test campaign; do not trigger a Gradle run for each change.
 */
class P10ReminderIconStylePolicyTest {
    @Test fun exactlyThreeDistinctFounderApprovedFamilies() {
        assertEquals(
            listOf(
                P10ReminderIconStyle.LUMINOUS_GLASS,
                P10ReminderIconStyle.CINEMATIC_EMBLEM,
                P10ReminderIconStyle.LIVING_ENERGY,
            ),
            P10ReminderIconStyle.entries,
        )
    }

    @Test fun freshOrUnknownChoiceAlwaysStartsInLivingEnergy() {
        assertEquals(
            P10ReminderIconStyle.LIVING_ENERGY,
            P10ReminderIconStylePolicy.DEFAULT,
        )
        assertEquals(
            P10ReminderIconStyle.LIVING_ENERGY,
            P10ReminderIconStylePolicy.fromSavedName(null),
        )
        assertEquals(
            P10ReminderIconStyle.LIVING_ENERGY,
            P10ReminderIconStylePolicy.fromSavedName("OLD_CINEMATIC_COLLECTION"),
        )
        assertEquals(
            P10ReminderIconStyle.LIVING_ENERGY,
            P10ReminderIconStylePolicy.fromSavedName(""),
        )
    }

    @Test fun allSelectedStylesRoundTripWithoutChangingIdentity() {
        P10ReminderIconStyle.entries.forEach { choice ->
            assertEquals(choice, P10ReminderIconStylePolicy.fromSavedName(choice.name))
        }
    }
}
