package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One isolated, sequential integration test with REAL Android DataStore on a
 * disposable, CLEARED emulator. Not intended for an owner phone or prod account.
 * No alarms, cloud, notifications, photos, or live restore operations are used.
 */
@RunWith(AndroidJUnit4::class)
class Phase1HabitStoreDeviceTest {
    @Test fun realDataStorePreservesReceiptsAndLegacyOwnersAcrossSafeChanges() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val journal = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!journal.exists()) { "Refuse tests during an incomplete owner recovery" }
        val store = Phase1HabitStore(app)
        val legacy = V297ChallengeStore(app)
        val personal = PersonalReminderStore(app)
        check(store.load().isEmpty() && legacy.load().isEmpty() && personal.load().isEmpty()) {
            "This test requires a clean disposable emulator; do not overwrite records"
        }
        val start = LocalDate.of(2026, 11, 7)
        val at = Instant.parse("2026-11-07T04:00:00Z")
        val original = Phase1HabitChallenge(
            id = "emulator-habit-1",
            activity = "Read 20 minutes",
            startDate = start,
            zoneId = "Asia/Kolkata",
            alertTime = LocalTime.of(7, 30),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).checked()
        assertEquals(listOf(original), store.create(original))
        // A separate store instance reads the persisted primary, not Compose state.
        assertEquals(original, Phase1HabitStore(app).load().single())
        val done = store.confirmToday(original.id, at).single()
        assertEquals(at, done.confirmations[start])
        assertEquals(done, store.confirmToday(original.id, at.plusSeconds(60)).single())
        assertEquals(done, Phase1HabitStore(app).load().single())
        val paused = store.setPaused(original.id, true).single()
        assertTrue(paused.paused)
        assertEquals(done.confirmations, paused.confirmations)
        val previous = store.validatedPreviousRevision()
        assertNotNull("Previous valid revision retained before pause", previous)
        assertEquals(done, previous!!.single())

        // A same-ID conflict is now a HARD ERROR, not a silent skip. The
        // entire attempted import must leave local receipts and unrelated new
        // records unchanged; it must not partially append the second ID.
        val maliciousSameId = original.copy(activity = "Replace the real activity")
        val additional = original.copy(id = "emulator-habit-2", activity = "Stretch")
        val importPayload = Phase1HabitCodec.encode(listOf(maliciousSameId, additional))
        assertEquals(listOf(maliciousSameId, additional), store.validateImport(importPayload))
        assertEquals(listOf(paused), store.load())
        val rejected = runCatching { store.importMissingAfterApproval(importPayload) }
        assertTrue("Conflicting import must require owner review", rejected.isFailure)
        assertTrue(rejected.exceptionOrNull() is IllegalArgumentException)
        assertTrue(rejected.exceptionOrNull()?.message.orEmpty().contains("Owner review"))
        assertEquals("No partial append after conflict", listOf(paused), store.load())
        assertEquals(at, Phase1HabitStore(app).load().single().confirmations[start])

        // An *identical* collision can remain idempotent, and a genuinely
        // distinct challenge can be added without changing the original Done
        // receipt or pause state.
        val safePayload = Phase1HabitCodec.encode(listOf(paused, additional))
        val merged = store.importMissingAfterApproval(safePayload)
        assertEquals(2, merged.size)
        assertEquals(paused, merged.single { it.id == original.id })
        assertEquals(additional, merged.single { it.id == additional.id })
        val snapshot = Phase1HabitStore(app).load()
        assertEquals(merged, snapshot)
        assertEquals(snapshot, Phase1HabitCodec.decode(store.exportJson()))
        assertEquals(emptyList<V297Challenge>(), legacy.load())
        assertTrue(personal.load().isEmpty())

        // Neither malformed input nor an active recovery journal may change
        // the saved archive or its explicit Done receipts.
        val beforeCorruption = store.load()
        val bad = importPayload.replace("Stretch", "Changed maliciously")
        val corrupt = runCatching { store.importMissingAfterApproval(bad) }
        assertTrue("Corrupt import must be rejected", corrupt.isFailure)
        assertEquals(beforeCorruption, store.load())

        try {
            check(journal.mkdir()) { "Could not create synthetic recovery marker" }
            val blocked = runCatching {
                store.create(original.copy(id = "emulator-blocked-habit"))
            }
            assertTrue("Pending recovery must reject writes", blocked.isFailure)
            assertTrue(blocked.exceptionOrNull() is IllegalStateException)
            assertEquals(beforeCorruption, store.load())
            assertFalse(store.load().any { it.id == "emulator-blocked-habit" })
        } finally {
            journal.deleteRecursively() // Only the synthetic empty CI marker.
        }
        assertEquals(beforeCorruption, Phase1HabitStore(app).load())
        assertEquals(emptyList<V297Challenge>(), legacy.load())
        assertTrue(personal.load().isEmpty())
    }
}
