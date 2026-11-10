package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V329ReminderVisualPolicyTest {
    @Test fun fiveApprovedStylesRemainAvailable() {
        assertEquals(5, V329ReminderWorld.entries.size)
        assertEquals(
            listOf("CRIMSON", "LIQUID", "OBSIDIAN", "SIGNAL", "NOCTURNE"),
            V329ReminderWorld.entries.map { it.name },
        )
    }

    @Test fun creatorAndPersonalKeepIndependentAlarmAndVoiceChannels() {
        assertEquals(2, V329ReminderDomain.entries.size)
        assertEquals(2, V329ReminderChannel.entries.size)
        assertEquals(4,
            V329ReminderDomain.entries.flatMap { domain ->
                V329ReminderChannel.entries.map { channel -> domain to channel }
            }.distinct().size)
    }

    @Test fun visualSlidersCannotProduceOutOfRangeMotionWork() {
        val safe = V329ReminderAppearance(
            world = V329ReminderWorld.OBSIDIAN,
            intensity = -2f, density = 9f, speed = -1f,
        ).normalized()
        assertEquals(.15f, safe.intensity)
        assertEquals(1f, safe.density)
        assertEquals(.15f, safe.speed)
        assertTrue(safe.world == V329ReminderWorld.OBSIDIAN)
    }
}
