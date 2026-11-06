package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real stores, synthetic data, future-only reminder, CI emulator required. */
@RunWith(AndroidJUnit4::class)
class V310CreateAndLinkDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun oneSaveProducesOneReminderWithActualSpaceAndGroupReferences() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val reminders = PersonalReminderStore(app)
        assertTrue("Must run on a disposable empty emulator", reminders.load().isEmpty())
        val spaceId = UUID.randomUUID().toString()
        val group = V298ReminderGroup(name = "My custom routine")
        val db = V289SpacesDatabase.open(app)
        try {
            db.spaces().insertSpace(
                V289SpaceEntity(
                    id = spaceId, title = "My Skin Care", type = "SKIN_CARE",
                    iconKey = "sun", goalText = "Progress photos",
                    createdAtMillis = System.currentTimeMillis(),
                    archivedAtMillis = null, displayOrder = 0,
                )
            )
        } finally { db.close() }
        val groupStore = V298ReminderGroupStore(app)
        groupStore.create(group)
        val draft = PersonalReminderDraft(
            title = "Morning sunscreen",
            startDate = LocalDate.now().plusDays(31),
            time = LocalTime.of(10, 10),
            zoneId = ZoneId.systemDefault(),
        )
        val created = V310CreateAndLink(app).create(
            draft,
            V310LinkTargets(spaceId = spaceId, groupId = group.id),
        )
        assertTrue(created.linkProblems.isEmpty())
        assertTrue(created.linkedSpace)
        assertTrue(created.linkedGroup)
        assertEquals(1, reminders.load().size)
        assertEquals(created.reminder.id, reminders.load().single().id)
        assertEquals(created.reminder.id,
            groupStore.load().single { it.id == group.id }.reminderIds.single())

        val check = V289SpacesDatabase.open(app)
        try {
            val link = check.spaces().linksForSpace(spaceId).single()
            assertEquals(created.reminder.id, link.reminderId)
            assertEquals(spaceId, link.spaceId)
            check.spaces().setArchivedAt(spaceId, System.currentTimeMillis())
        } finally { check.close() }

        val before = reminders.load()
        try {
            V310CreateAndLink(app).create(
                draft.copy(title = "Second reminder must not be created"),
                V310LinkTargets(spaceId = spaceId),
            )
            throw AssertionError("Archived Space accepted as a live link")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("archived"))
        }
        assertEquals(before, reminders.load())
        assertFalse(before.isEmpty())
    }
}
