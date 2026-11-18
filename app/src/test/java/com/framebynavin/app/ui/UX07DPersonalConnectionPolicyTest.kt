package com.framebynavin.app.ui

import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V298ReminderGroup
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-only UX07D regression cases. Execution is explicitly postponed
 * until the founder-directed consolidated UX01–UX10 campaign.
 */
class UX07DPersonalConnectionPolicyTest {
    private fun reminder(id: String, title: String, paused: Boolean = false) =
        PersonalReminder(
            id = id,
            title = title,
            startDate = LocalDate.of(2026, 11, 17),
            time = LocalTime.of(9, 0),
            paused = paused,
        )

    @Test
    fun `group searching is case insensitive, trimmed and nonmutating`() {
        val items = listOf(
            V298ReminderGroup(id = "g1", name = "Morning"),
            V298ReminderGroup(id = "g2", name = "Reading"),
        )
        assertEquals(items, UX07DPersonalConnectionPolicy.matchingGroups(items, ""))
        assertEquals(
            listOf("Reading"),
            UX07DPersonalConnectionPolicy.matchingGroups(items, "  READ  ").map { it.name },
        )
        assertTrue(items.all { it.reminderIds.isEmpty() })
    }

    @Test
    fun `Space link search includes only unlinked original Personal reminders`() {
        val records = listOf(
            reminder("id-b", "Writing"),
            reminder("id-c", "reading", paused = true),
            reminder("id-a", "Reading a novel"),
        )
        assertEquals(
            listOf("id-c", "id-a"),
            UX07DPersonalConnectionPolicy.availableSpaceReminders(
                records, setOf("id-b"), "rea",
            ).map { it.id },
        )
        assertEquals(3, records.size)
        assertEquals("Writing", records[0].title)
    }

    @Test
    fun `bulk pause preview separates changing unchanged missing and shared IDs`() {
        val first = V298ReminderGroup(
            id = "first", name = "Morning",
            reminderIds = listOf("a", "b", "missing"),
        )
        val second = V298ReminderGroup(
            id = "second", name = "Home",
            reminderIds = listOf("a", "c"),
        )
        val records = listOf(
            reminder("a", "Walk"),
            reminder("b", "Read", paused = true),
            reminder("c", "Drink water"),
        )
        val snapshot = UX07DPersonalConnectionPolicy.bulkImpact(
            first, listOf(first, second), records, targetPaused = true,
        )
        assertEquals(3, snapshot.linked)
        assertEquals(1, snapshot.changing)
        assertEquals(1, snapshot.unchanged)
        assertEquals(1, snapshot.missing)
        assertEquals(1, snapshot.sharedWithOtherGroups)
        // Only a preview: no original Personal record changes here.
        assertEquals(false, records.first().paused)
    }

    @Test
    fun `bulk resume preview counts only the original members needing resume`() {
        val group = V298ReminderGroup(
            id = "group", name = "Evening", reminderIds = listOf("a", "b"),
        )
        val records = listOf(
            reminder("a", "Journal", paused = true),
            reminder("b", "Stretch"),
        )
        val result = UX07DPersonalConnectionPolicy.bulkImpact(
            group, listOf(group), records, targetPaused = false,
        )
        assertEquals(1, result.changing)
        assertEquals(1, result.unchanged)
        assertEquals(0, result.missing)
        assertEquals(0, result.sharedWithOtherGroups)
    }
}
