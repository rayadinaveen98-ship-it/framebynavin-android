package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.io.ByteArrayInputStream
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
 * The versioned encrypted ZIP must include exact new challenge Done receipts,
 * without changing legacy Personal/V297 data. Executed on cleared Android CI
 * emulator ONLY; never touches user devices, cloud, reminders or account data.
 */
@RunWith(AndroidJUnit4::class)
class Phase1HabitArchiveCoverageGuardDeviceTest {
    @Test fun verifiedVersionTwoArchivePreservesExactChallengeDoneReceipt() = runBlocking {
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
        val confirmed = Phase1HabitChallenge(
            id = "archive-coverage-fixture",
            activity = "Practice guitar",
            startDate = LocalDate.of(2026, 11, 7),
            zoneId = "Asia/Kolkata",
            alertTime = LocalTime.of(7, 30),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
        habits.create(confirmed)
        val before = habits.exportJson()

        val password = "ci-only-passphrase".toCharArray()
        val bytes = ByteArrayOutputStream()
        V300PersonalArchiveSource(app).export(password, bytes)
        assertTrue("Complete versioned ZIP exists", bytes.size() > 0)
        val preview = V300PersonalArchiveSource(app).verify(
            password, ByteArrayInputStream(bytes.toByteArray()),
        )
        assertEquals(1, preview.singleActivityChallenges)
        assertEquals(1, preview.singleActivityDoneReceipts)
        // This version-aware comparison is authenticated and read-only.
        val comparison = Phase1HabitArchiveConflictPreview(app).check(
            ByteArrayInputStream(bytes.toByteArray()), password,
        )
        assertTrue(comparison.archiveHasChallengeCoverage)
        assertEquals(1, comparison.identical)
        assertEquals(1, comparison.importedDoneReceipts)
        assertEquals(0, comparison.conflicts)
        assertFalse(comparison.authorizesRestore)
        V300EncryptedPersonalArchive.stageAndVerify(
            ByteArrayInputStream(bytes.toByteArray()), password, app.cacheDir,
        ).use { staged ->
            assertEquals("Exact snapshot including confirmed instant", before,
                staged.components.singleActivityChallenges)
            assertEquals(confirmed, Phase1HabitCodec.decode(
                requireNotNull(staged.components.singleActivityChallenges)).single())
        }
        // The old V301/B03 encrypted restore pipeline does not include this
        // domain in its journal: it MUST reject the local owner snapshot.
        val legacyRestore = runCatching { V301LocalRecoveryReader(app).read() }
        assertTrue("Legacy multi-store restore must fail closed", legacyRestore.isFailure)
        assertTrue(legacyRestore.exceptionOrNull()?.message.orEmpty().contains(
            "does not cover single-activity challenge data"))
        assertEquals("Original immutable Done receipts survive", before, habits.exportJson())
        assertEquals(confirmed, Phase1HabitStore(app).load().single())
        assertFalse("Never wrote a legacy V297 plan", older.load().isNotEmpty())
        assertTrue("Never wrote Personal reminders", personal.load().isEmpty())
    }
}
