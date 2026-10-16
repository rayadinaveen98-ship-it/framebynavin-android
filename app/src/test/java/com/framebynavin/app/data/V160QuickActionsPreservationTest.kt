package com.framebynavin.app.data

import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** R1 regression: recurring templates do not own or overwrite generated creator projects. */
class V160QuickActionsPreservationTest {
    @get:Rule val folder = TemporaryFolder()

    private val settings = CreatorOsSettings(onboardingComplete = true)
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = LocalDate.of(2030, 1, 7).atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
    private val slot = WeeklyScheduleSlot(
        id = "weekly-v160", title = "My original film", dayOfWeek = DayOfWeek.TUESDAY,
        hour = 19, minute = 0, platform = "YouTube", contentType = "Short",
        reminderMode = ReminderMode.SIMPLE,
    )

    @Test fun changingOrDisablingTemplateNeverAltersAnAlreadyGeneratedProject() {
        val original = CreatorAutoPlanEngine.merge(emptyList(), listOf(slot), settings, now, 7).created.single()
        val customized = original.copy(
            title = "Creator edited title",
            priority = TaskPriority.CRITICAL,
            status = TaskStatus.WORKING,
            notes = "Never overwrite my creative notes",
            dueAtMillis = original.dueAtMillis + 1_800_000L,
            reminderAtMillis = original.reminderAtMillis + 600_000L,
        )
        val changedSlot = slot.copy(
            title = "Renamed weekly template",
            platform = "Instagram",
            contentType = "Reel",
            hour = 21,
            priority = TaskPriority.NORMAL,
            reminderMode = ReminderMode.NONE,
        )
        val merged = CreatorAutoPlanEngine.merge(listOf(customized), listOf(changedSlot), settings, now, 7)
        // New occurrence identity is preserved (same slot id and date). The creator's
        // already-created record must remain byte-for-byte identical.
        assertEquals(customized, merged.tasks.single())
        assertTrue(merged.created.isEmpty())

        val disabled = CreatorAutoPlanEngine.merge(listOf(customized), listOf(slot.copy(enabled = false)), settings, now, 7)
        assertEquals(listOf(customized), disabled.tasks)
        assertTrue(disabled.created.isEmpty())
    }

    @Test fun aDeletedWeeklyTemplateDoesNotDeleteItsPreviouslyGeneratedProjects() {
        val project = CreatorAutoPlanEngine.merge(emptyList(), listOf(slot), settings, now, 7).created.single()
        val result = CreatorAutoPlanEngine.merge(listOf(project), emptyList(), settings, now, 7)
        assertEquals(listOf(project), result.tasks)
        assertTrue(result.created.isEmpty())
    }

    @Test fun readableOwnedVoiceRecordingCanBeIncluded() {
        val root = folder.newFolder("voice_ideas")
        val owned = File(root, "voice_idea_safe-123.m4a").apply { writeBytes(byteArrayOf(4, 5, 6)) }
        assertEquals(owned.canonicalFile, PortableVoiceExportPolicy.requireReadableOwnedRecording(root, owned.path))
    }

    @Test fun missingOrEmptyRecordingsMustNeverBeMarkedAsBackedUp() {
        val root = folder.newFolder("voice_ideas")
        assertThrows(IllegalArgumentException::class.java) {
            PortableVoiceExportPolicy.requireReadableOwnedRecording(root, File(root, "voice_idea_missing.m4a").path)
        }
        val empty = File(root, "voice_idea_zero.m4a").apply { createNewFile() }
        assertThrows(IllegalArgumentException::class.java) {
            PortableVoiceExportPolicy.requireReadableOwnedRecording(root, empty.path)
        }
    }

    @Test fun foreignAndUnfinishedVoicePathsAreRejected() {
        val root = folder.newFolder("voice_ideas")
        val outside = folder.newFile("voice_idea_outside.m4a").apply { writeBytes(byteArrayOf(1)) }
        val inProgress = File(root, "voice_idea_unfinished.recording.m4a").apply {
            writeBytes(byteArrayOf(1, 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PortableVoiceExportPolicy.requireReadableOwnedRecording(root, outside.path)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PortableVoiceExportPolicy.requireReadableOwnedRecording(root, inProgress.path)
        }
    }
}
