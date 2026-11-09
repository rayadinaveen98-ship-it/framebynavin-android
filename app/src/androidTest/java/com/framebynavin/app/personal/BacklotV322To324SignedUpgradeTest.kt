package com.framebynavin.app.personal

import android.content.Context
import android.util.AtomicFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorContentWorkspace
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs one named method on the actual installed V322 signed APK BEFORE install -r V324,
 * then verifies the exact owner schedule + 21-day Day-1 receipt + project AFTER upgrade.
 * The test APK is the V324 signed personalStable AndroidTest variant; it uses only
 * APIs that already existed in V322 for the pre-upgrade phase.
 *
 * Disposable Android 15 CI emulator only. No uninstall / pm clear between phases.
 */
@RunWith(AndroidJUnit4::class)
class BacklotV322To324SignedUpgradeTest {
    private val app: Context get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val evidence: File get() = File(app.filesDir, "qa-v322-v323-owner-data-fixture.json")
    private val creatorId = "qa-v322-v323-creator-project"
    private val note = "Preserve this owner project and its script after signed upgrade"
    private val zone = ZoneId.of("Asia/Kolkata")
    private val date = LocalDate.of(2026, 11, 9)
    private val receipt = Instant.parse("2026-11-09T05:00:00Z")

    private fun installedVersion(): Long = app.packageManager
        .getPackageInfo(app.packageName, 0).longVersionCode

    @Test fun phase1SeedRealV322ScheduleWalkingChallengeAndCreatorData(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals("Seed only on verified signed V322", 322L, installedVersion())
        assertFalse("Do not overwrite earlier unfinished upgrade proof", evidence.exists())
        val personal = PersonalReminderStore(app)
        val habits = Phase1HabitStore(app)
        val creator = TaskStore(app)
        val templates = PersonalRebuildSchedulePack.reminders(date, zone).map {
            // Quiet fixture: record all 16 actual V322 schedule definitions
            // but never trigger an alarm during CI.
            it.copy(paused = true).checked()
        }
        assertEquals(16, templates.size)
        assertTrue(personal.load().none { it.id.startsWith(PersonalRebuildSchedulePack.PREFIX) })
        assertTrue(habits.load().none { it.id == PersonalRebuildSchedulePack.CHALLENGE_ID })
        assertTrue(creator.load().none { it.id == creatorId })

        personal.mutate { existing -> existing + templates }
        val walking = Phase1HabitChallenge(
            id = PersonalRebuildSchedulePack.CHALLENGE_ID,
            activity = "Morning walking", startDate = date, zoneId = zone.id,
        ).checked()
        habits.create(walking)
        habits.confirmToday(walking.id, receipt)
        creator.mutate { old -> old + CreatorTask(
            id = creatorId, title = "Frame breakdown survives V324",
            platform = "YouTube", contentType = "Long-form", dueLabel = "Tomorrow",
            status = TaskStatus.WORKING, notes = note,
            workspace = CreatorContentWorkspace(script = "Scene analysis — saved before V324"),
        ) }

        val baseline = JSONObject()
            .put("personal", personal.exportPersonalJson())
            .put("habit", habits.exportJson())
            .put("creatorId", creatorId)
            .put("versionCode", installedVersion())
        val atomic = AtomicFile(evidence)
        val stream = atomic.startWrite()
        try {
            stream.write(baseline.toString().toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
        assertTrue(evidence.isFile && evidence.length() > 100L)
    }

    @Test fun phase2VerifyV324InPlaceInstallPreservedEveryScheduleRecordAndDay1Receipt(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals("Must be signed V324 update, not still old V322", 324L, installedVersion())
        verifyUnmodifiedOwnerRecords()
    }

    @Test fun phase3VerifyAfterV324NormalActivityLaunch(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals(324L, installedVersion())
        verifyUnmodifiedOwnerRecords()
    }

    private suspend fun verifyUnmodifiedOwnerRecords() {
        assertTrue("Private app fixture missing: app data may have been cleared", evidence.isFile)
        val expected = JSONObject(AtomicFile(evidence).openRead().bufferedReader().use { it.readText() })
        assertEquals(322L, expected.getLong("versionCode"))
        val personal = PersonalReminderStore(app)
        val original = PersonalCompletionBackupCodec.decode(expected.getString("personal")).reminders
            .filter { it.id.startsWith(PersonalRebuildSchedulePack.PREFIX) }
        val actual = personal.load().filter { it.id.startsWith(PersonalRebuildSchedulePack.PREFIX) }
        assertEquals(16, original.size)
        assertEquals("All 16 configured reminders must survive upgrade exactly", original, actual)
        assertTrue("Wake alarm was lost", actual.any { it.id.endsWith("wake") })
        assertTrue("Four-day work schedule was lost", actual.any { it.id.endsWith("app-work") })
        assertEquals(7, actual.count { it.id.contains("hobby-") })
        val before = Phase1HabitCodec.decode(expected.getString("habit"))
            .single { it.id == PersonalRebuildSchedulePack.CHALLENGE_ID }
        val now = Phase1HabitStore(app).load()
            .single { it.id == PersonalRebuildSchedulePack.CHALLENGE_ID }
        assertEquals("Challenge identity/activity/date and Day 1 proof changed", before, now)
        assertEquals(receipt, now.confirmations[date])
        val creator = TaskStore(app).load().single { it.id == expected.getString("creatorId") }
        assertEquals(TaskStatus.WORKING, creator.status)
        assertEquals(note, creator.notes)
        assertEquals("Scene analysis — saved before V324", creator.workspace.script)
    }
}
