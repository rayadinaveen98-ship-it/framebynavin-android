package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V289SpacesRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator only: real DataStore and Room, no owner data/cloud. */
@RunWith(AndroidJUnit4::class)
class PersonalLinkedReferenceGuardDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun missingCloudIdsNeverDeletePersonalRecordOrStrandLocalSpaceAndGroup() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val reminders = PersonalReminderStore(app)
        assertTrue("Run only on a cleared emulator", reminders.load().isEmpty())
        val stored = PersonalReminder(
            id = "synthetic-owner-keep",
            title = "My Existing Reminder",
            startDate = LocalDate.of(2026, 12, 20),
            time = LocalTime.of(10, 0),
            createdAtMillis = 100L,
            updatedAtMillis = 100L,
        )
        reminders.create(stored)
        val group = V298ReminderGroup(
            name = "My Private Routine",
            reminderIds = listOf(stored.id),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val groupStore = V298ReminderGroupStore(app)
        groupStore.create(group)
        val spaceId = UUID.randomUUID().toString()
        val db = V289SpacesDatabase.open(app)
        try {
            db.spaces().insertSpace(V289SpaceEntity(
                id = spaceId, title = "My Space", type = "CUSTOM",
                iconKey = "sparkles", goalText = "Progress",
                createdAtMillis = 100L, archivedAtMillis = null, displayOrder = 0,
            ))
            assertTrue(V289SpacesRepository(db.spaces()).linkExistingReminder(
                V289SpaceReminderLink(spaceId, stored.id, System.currentTimeMillis()),
                setOf(stored.id),
            ))
        } finally { db.close() }
        val cloud = PersonalReminder(
            id = "incoming-other-identity",
            title = "Cloud-only reminder",
            startDate = LocalDate.of(2026, 12, 21),
            time = LocalTime.of(11, 0),
            createdAtMillis = 100L,
            updatedAtMillis = 100L,
        )
        val transfer = PersonalBackupTransfer(app)
        val missing = PersonalCompletionBackupCodec.encode(listOf(cloud), emptyList())
        try {
            transfer.restoreApproved(missing)
            fail("Personal replacement must not strand existing links")
        } catch (e: PersonalLinkProtectionException) {
            assertTrue(e.message.orEmpty().contains("Nothing was restored"))
        }
        assertEquals(listOf(stored), reminders.load())
        assertEquals(listOf(stored.id),
            groupStore.load().single().reminderIds)
        val afterRejected = V289SpacesDatabase.open(app)
        try {
            assertEquals(stored.id, afterRejected.spaces()
                .linksForSpace(spaceId).single().reminderId)
        } finally { afterRejected.close() }

        // A copy containing referenced IDs is valid and may be restored only
        // through the canonical Personal command while references remain.
        val safe = PersonalCompletionBackupCodec.encode(
            listOf(stored.copy(title = "Renamed, same identity")), emptyList())
        val receipt = transfer.restoreApproved(safe)
        assertEquals(1, receipt.importedCount)
        assertEquals(stored.id, reminders.load().single().id)
        assertEquals("Renamed, same identity", reminders.load().single().title)
        assertEquals(stored.id, groupStore.load().single().reminderIds.single())
        val afterApproved = V289SpacesDatabase.open(app)
        try {
            assertEquals(stored.id, afterApproved.spaces()
                .linksForSpace(spaceId).single().reminderId)
        } finally { afterApproved.close() }
    }
}
