package com.framebynavin.app.reminders

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * RX03C authorised Backlot greeting contract.
 *
 * Historical V148 labels and assertions are revised to verify the new
 * approved product behaviour. No service, Android build or user-data source
 * is altered merely to satisfy legacy greeting copy tests.
 */
class VoiceGreetingBuilderV148Test {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun usesCreatorFirstNameWhenAvailable() {
        assertEquals(
            "Hey Navin, good afternoon.",
            VoiceGreetingBuilder.greeting(" Navin Rayadi ", at(14, 15), zone),
        )
    }

    @Test
    fun prefersGoogleFirstNameForVoiceIdentity() {
        assertEquals(
            "Navin",
            VoiceGreetingBuilder.preferredName(
                googleAccountName = "Navin Rayadi",
                cachedAccountName = "Cached Name",
                creatorProfileName = "Creator Profile",
            ),
        )
    }

    @Test
    fun usesCachedAccountWhenGoogleNameIsMissing() {
        assertEquals(
            "Naveen",
            VoiceGreetingBuilder.preferredName(
                googleAccountName = "",
                cachedAccountName = "Naveen Kumar",
                creatorProfileName = "Creator Profile",
            ),
        )
    }

    @Test
    fun usesCreatorProfileWhenAccountNamesAreMissing() {
        assertEquals(
            "Navin",
            VoiceGreetingBuilder.preferredName(
                googleAccountName = "",
                cachedAccountName = "",
                creatorProfileName = "Navin Creator",
            ),
        )
    }

    @Test
    fun unknownIdentityNeverClaimsCreatorIsThePersonalFirstName() {
        assertEquals(
            "Hey there, good morning.",
            VoiceGreetingBuilder.greeting("   ", at(9, 0), zone),
        )
        // Keep previous external default API backward compatible, but use
        // the true known-name-only resolver when speaking personal greetings.
        assertEquals("Creator", VoiceGreetingBuilder.preferredName("", "", ""))
        assertEquals("", VoiceGreetingBuilder.preferredKnownName("", "", ""))
    }

    @Test
    fun sharedSpokenReminderPrependsExactlyOneTimeAwareGreeting() {
        assertEquals(
            "Hey Navin, good evening. Backlot. Rajamouli video. This is your important creator reminder.",
            VoiceGreetingBuilder.spokenReminder(
                creatorName = "Navin Rayadi",
                reminderBody = "Backlot. Rajamouli video. This is your important creator reminder.",
                nowMillis = at(20, 30),
                zoneId = zone,
            ),
        )
    }

    @Test
    fun sharedSpokenReminderDoesNotAddTrailingSpaceForEmptyBody() {
        assertEquals(
            "Hey there, good morning.",
            VoiceGreetingBuilder.spokenReminder(
                creatorName = "",
                reminderBody = "   ",
                nowMillis = at(8, 0),
                zoneId = zone,
            ),
        )
    }

    @Test
    fun morningBoundaryMatchesProductRule() {
        assertEquals("Hey Navin, good morning.",
            VoiceGreetingBuilder.greeting("Navin", at(5, 0), zone))
        assertEquals("Hey Navin, good morning.",
            VoiceGreetingBuilder.greeting("Navin", at(11, 59), zone))
    }

    @Test
    fun afternoonBoundaryMatchesProductRule() {
        assertEquals("Hey Navin, good afternoon.",
            VoiceGreetingBuilder.greeting("Navin", at(12, 0), zone))
        assertEquals("Hey Navin, good afternoon.",
            VoiceGreetingBuilder.greeting("Navin", at(16, 59), zone))
    }

    @Test
    fun eveningStartsAtFivePmAndEndsBeforeMidnight() {
        assertEquals("Hey Navin, good evening.",
            VoiceGreetingBuilder.greeting("Navin", at(17, 0), zone))
        assertEquals("Hey Navin, good evening.",
            VoiceGreetingBuilder.greeting("Navin", at(23, 59), zone))
    }

    @Test
    fun overnightGreetingNeverSaysGoodNightOrGoodEvening() {
        assertEquals("Hey Navin.",
            VoiceGreetingBuilder.greeting("Navin", at(0, 0), zone))
        assertEquals("Hey Navin.",
            VoiceGreetingBuilder.greeting("Navin", at(4, 59), zone))
    }

    @Test
    fun cachedEmailsAndUrlsAreNotSpokenAsFirstNames() {
        assertEquals("Hey there, good morning.",
            VoiceGreetingBuilder.greeting("navin@example.com", at(9, 0), zone))
        assertEquals("Hey there.",
            VoiceGreetingBuilder.greeting("https://example.com", at(2, 0), zone))
        assertEquals("Naveen",
            VoiceGreetingBuilder.preferredKnownName(
                "navin@example.com", "Naveen Kumar", "Creator Profile"))
    }

    @Test
    fun spokenBodyIsTrimmedWithoutChangingItsWords() {
        assertEquals(
            "Hey Navin, good afternoon. Drink water.",
            VoiceGreetingBuilder.spokenReminder(
                creatorName = "Navin",
                reminderBody = "  Drink water.  ",
                nowMillis = at(12, 1),
                zoneId = zone,
            ),
        )
    }

    private fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 9, 30, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
