package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An encrypted "complete" V300 archive cannot silently omit the independent
 * one-activity 21-day challenge DataStore. Executed on a cleared Android CI
 * emulator ONLY; never touches user devices, cloud, reminders or account data.
 */
@RunWith(AndroidJUnit4::class)
class Phase1HabitArchiveCoverageGuardDeviceTest {
    @Test fun savedChallengeBlocksIncompleteEncryptedZipBeforeFirstByte() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val journal = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!journal.exists()) { "Refuse to run while restore recovery is pending" }

        val habits = Phase1HabitStore(app)
        val older = V297ChallengeStore(app)
        val personal = PersonalReminderStore(app)
        check(habits.load().isEmpty() && older.load().isEmpty() && personal.load().isEmpty()) {
            "Requires a clean disposable emulator; refuse to overwrite existing records"
        }

        val start = LocalDate.of(2026, 11, 7)
        val confirmed = Phase1HabitChallenge(
            id = "archive-coverage-fixture",
            activity = "Practice guitar",
            startDate = start,
            zoneId = "Asia/Kolkata",
            alertTime = LocalTime.of(7, 30),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
        habits.create(confirmed)
        val before = habits.exportJson()

        val bytes = ByteArrayOutputStream()
        val attempt = runCatching {
            V300PersonalArchiveSource(app).export("ci-only-passphrase".toCharArray(), bytes)
        }
        assertTrue("Must fail closed instead of claiming full archive", attempt.isFailure)
        assertTrue(attempt.exceptionOrNull() is IllegalStateException)
        assertTrue(attempt.exceptionOrNull()?.message.orEmpty()
            .contains("Single-activity challenge progress"))
        assertEquals("No misleading incomplete ZIP header", 0, bytes.size())
        assertEquals("Original immutable Done receipts survive", before, habits.exportJson())
        assertEquals(confirmed, Phase1HabitStore(app).load().single())
        assertFalse("Never wrote a legacy V297 plan", older.load().isNotEmpty())
        assertTrue("Never wrote Personal reminders", personal.load().isEmpty())
    }
}
