package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class CreatorWeeklyPlanPreviewV164Test {
    private val zone = ZoneId.systemDefault()
    private val now = LocalDate.of(2026, 10, 16).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
    private val nextDay = LocalDate.of(2026, 10, 17).dayOfWeek

    private fun slot(id: String, enabled: Boolean = true) = WeeklyScheduleSlot(
        id = id, title = "Creator's weekly short", dayOfWeek = nextDay,
        hour = 19, minute = 0, platform = "YouTube", contentType = "Short",
        enabled = enabled, reminderMode = ReminderMode.NONE,
    )

    private fun task(key: String, state: TaskStatus = TaskStatus.SKIPPED) = CreatorTask(
        id = "creator-owned", title = "Creator changed my title",
        platform = "YouTube", contentType = "Short", dueLabel = "",
        status = state, scheduleSlotId = "s", scheduleOccurrenceKey = key,
        archivedAtMillis = 15L,
    )

    @Test fun previewIsReadOnlyAndSkippedArchivedOccurrencesStayOwned() {
        val slots = listOf(slot("s"))
        val original = CreatorWeeklyPlanPreviewEngine.build(slots, emptyList(), now)
        assertTrue(original.upcoming.isNotEmpty())
        val existing = task(original.upcoming.first().key)
        val withWork = CreatorWeeklyPlanPreviewEngine.build(slots, listOf(existing), now)
        assertEquals(original.upcoming.size, withWork.upcoming.size)
        assertEquals(original.missing.size - 1, withWork.missing.size)
        assertEquals(1, withWork.alreadyCreated)
        assertEquals("Creator changed my title", existing.title)
        assertTrue(withWork.missing.none { it.key == existing.scheduleOccurrenceKey })
    }

    @Test fun disabledAndEmptySchedulesPreviewNothing() {
        assertTrue(CreatorWeeklyPlanPreviewEngine.build(emptyList(), emptyList(), now).upcoming.isEmpty())
        assertTrue(CreatorWeeklyPlanPreviewEngine.build(listOf(slot("disabled", false)), emptyList(), now).missing.isEmpty())
    }

    @Test fun noPreviewDateGoesPastFourteenLocalDays() {
        val preview = CreatorWeeklyPlanPreviewEngine.build(listOf(slot("s")), emptyList(), now)
        val last = LocalDate.of(2026, 10, 16).plusDays(13)
        assertTrue(preview.upcoming.all { !it.date.isAfter(last) })
    }

    @Test fun genericProfileStarterIsSmallAndCreatorSpecific() {
        val profile = CreatorProfile(
            category = "Education", platforms = setOf("YouTube"),
            weeklyPublishingTarget = 3, primaryGoal = "Teach weekly",
        )
        val created = CreatorWeeklyPresetPolicy.previewFor(profile)
        assertEquals(3, created.size)
        assertTrue(created.all { it.platform == "YouTube" })
        assertTrue(created.all { it.title.startsWith("My weekly post") })
        assertTrue(created.all { it.reminderMode == ReminderMode.NONE })
        assertFalse(created.any { it.id.startsWith("fbn_v4_") })
        assertTrue(CreatorWeeklyPresetPolicy.willReplaceCurrent(emptyList(), profile))
    }

    @Test fun filmPresetUsesTheExistingLegacyCompatibleScheduleOnlyIfChosen() {
        val film = CreatorProfile(category = "Film & Entertainment", platforms = setOf("YouTube"))
        assertEquals(WeeklyScheduleEngine.defaultSlots(), CreatorWeeklyPresetPolicy.previewFor(film))
    }
}
