package com.framebynavin.app.reminders

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceGreetingBuilderV148Test {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun usesCreatorFirstNameWhenAvailable() {
        assertEquals(
            "Hi, Good Afternoon, Navin.",
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
    fun fallsBackToCreatorWhenNameIsMissing() {
        assertEquals(
            "Hi, Good Morning, Creator.",
            VoiceGreetingBuilder.greeting("   ", at(9, 0), zone),
        )
        assertEquals(
            "Creator",
            VoiceGreetingBuilder.preferredName("", "", ""),
        )
    }

    @Test
    fun sharedSpokenReminderPrependsExactlyOneTimeAwareGreeting() {
        assertEquals(
            "Hi, Good Evening, Navin. Backlot. Rajamouli video. This is your important creator reminder.",
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
            "Hi, Good Morning, Creator.",
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
        assertEquals("Hi, Good Morning, Navin.", VoiceGreetingBuilder.greeting("Navin", at(5, 0), zone))
        assertEquals("Hi, Good Morning, Navin.", VoiceGreetingBuilder.greeting("Navin", at(11, 59), zone))
    }

    @Test
    fun afternoonBoundaryMatchesProductRule() {
        assertEquals("Hi, Good Afternoon, Navin.", VoiceGreetingBuilder.greeting("Navin", at(12, 0), zone))
        assertEquals("Hi, Good Afternoon, Navin.", VoiceGreetingBuilder.greeting("Navin", at(16, 59), zone))
    }

    @Test
    fun eveningCoversFivePmAndOvernightHours() {
        assertEquals("Hi, Good Evening, Navin.", VoiceGreetingBuilder.greeting("Navin", at(17, 0), zone))
        assertEquals("Hi, Good Evening, Navin.", VoiceGreetingBuilder.greeting("Navin", at(4, 59), zone))
    }

    private fun at(hour: Int, minute: Int): Long = LocalDateTime.of(2026, 9, 30, hour, minute)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}
