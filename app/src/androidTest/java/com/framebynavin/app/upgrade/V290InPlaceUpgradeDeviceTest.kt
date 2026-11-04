package com.framebynavin.app.upgrade

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.PersonalPurpose
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.PersonalRepeat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic *emulator-only* two-stage upgrade fixture.
 *
 * CI installs the already-verified V288 personalStable APK, runs seedOnV288,
 * then adb install -r (NO pm clear / NO uninstall) of the signed V290
 * personalStable APK and runs verifyOnV290.
 *
 * We intentionally create no Android alarms, notifications, real reminders,
 * linked accounts or cloud changes. Both phases must run on a CI emulator.
 * The fixture's synthetic completed-event history is for migration testing,
 * not a claim of a real owner pressing Done.
 */
@RunWith(AndroidJUnit4::class)
class V290InPlaceUpgradeDeviceTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private val personalId = "ci-v288-v290-personal-preservation"
    private val creatorId = "ci-v288-v290-creator-preservation"
    private val zone = ZoneId.of("Asia/Kolkata")
    private val receipt = PersonalCompletionEvent(
        reminderId = personalId,
        occurrenceMillis = 1_783_040_400_000L,
        doneAtMillis = 1_783_040_460_000L,
    )

    private fun requireInstalledVersion(version: Long) {
        val code = app.packageManager
            .getPackageInfo(app.packageName, 0).longVersionCode
        assertEquals("Wrong signed owner APK version in upgrade verification", version, code)
    }

    @Test fun seedOnV288() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        requireInstalledVersion(288L)

        val personal = PersonalReminder(
            id = personalId,
            title = "Upgrade fixture: morning care",
            notes = "Reminder should survive V288 to V290, no cloud data.",
            startDate = LocalDate.of(2026, 11, 10),
            time = LocalTime.of(8, 30),
            zoneId = zone,
            repeat = PersonalRepeat.DAILY,
            alert = PersonalAlert.VOICE,
            purpose = PersonalPurpose.PERSONAL_GOAL,
            snoozeMinutes = 15,
            voiceRepeatCount = 3,
            autoRetryEnabled = false,
        ).checked()
        val store = PersonalReminderStore(app)
        assertFalse("Fixture must run on a freshly cleared CI app", store.load().any { it.id == personalId })
        store.create(personal)
        store.mutate(replacementHistory = listOf(receipt)) { it }

        val creator = CreatorTask(
            id = creatorId,
            title = "Upgrade fixture: original filmmaker analysis",
            platform = "YouTube",
            contentType = "Video",
            dueLabel = "Future",
            status = TaskStatus.WORKING,
            progress = 25,
            priority = TaskPriority.IMPORTANT,
            notes = "Creator project must remain separate from Personal.",
            reminderEnabled = false,
        )
        val creatorStore = TaskStore(app)
        assertFalse(creatorStore.load().any { it.id == creatorId })
        creatorStore.mutate { tasks -> tasks + creator }

        assertTrue(store.load().any { it.id == personalId })
        assertTrue(creatorStore.load().any { it.id == creatorId })
        assertEquals(receipt, store.loadCompletionHistory().single { it.reminderId == personalId })
    }

    @Test fun verifyOnV290() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        requireInstalledVersion(290L)

        val personal = PersonalReminderStore(app).load().singleOrNull { it.id == personalId }
        assertTrue("Personal reminder lost during V288 to V290 in-place update", personal != null)
        val record = requireNotNull(personal)
        assertEquals("Upgrade fixture: morning care", record.title)
        assertEquals("Reminder should survive V288 to V290, no cloud data.", record.notes)
        assertEquals(LocalDate.of(2026, 11, 10), record.startDate)
        assertEquals(LocalTime.of(8, 30), record.time)
        assertEquals(zone, record.zoneId)
        assertEquals(PersonalRepeat.DAILY, record.repeat)
        assertEquals(PersonalAlert.VOICE, record.alert)
        assertEquals(PersonalPurpose.PERSONAL_GOAL, record.purpose)
        assertEquals(15, record.snoozeMinutes)
        assertEquals(3, record.voiceRepeatCount)
        assertFalse(record.autoRetryEnabled)

        assertEquals(
            receipt,
            PersonalReminderStore(app).loadCompletionHistory().singleOrNull {
                it.reminderId == personalId
            }
        )

        val creator = TaskStore(app).load().singleOrNull { it.id == creatorId }
        assertTrue("Creator project lost during V288 to V290 in-place update", creator != null)
        val project = requireNotNull(creator)
        assertEquals("Upgrade fixture: original filmmaker analysis", project.title)
        assertEquals("YouTube", project.platform)
        assertEquals("Creator project must remain separate from Personal.", project.notes)
        assertEquals(TaskStatus.WORKING, project.status)
        assertEquals(25, project.progress)
        assertFalse(project.reminderEnabled)
    }
}
