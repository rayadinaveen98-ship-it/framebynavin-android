package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitStore
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable CI emulator only. Exercises the real unified owner transaction:
 * export -> authenticate -> prepare -> protect original -> reserve H4 ->
 * Creator -> Personal v2 -> both-domain final verification -> journal release.
 *
 * This test exports a target with synthetic incoming Personal records and a
 * Creator Focus receipt, then makes the current phone an intentionally smaller
 * ORIGINAL dataset. It verifies non-conflicting Personal ADDITIONS, preserved
 * existing 21-day Done receipts, and Creator replacement. It does NOT prove
 * partial-write rollback, arbitrary hardware, cloud deletion or UI readiness.
 *
 * Never calls ActivityManager.clearApplicationUserData() or deletes any
 * pre-existing owner media / recovery evidence. Uses a fresh CI emulator.
 */
@RunWith(AndroidJUnit4::class)
class V384UnifiedRestoreEndToEndAndroidTest {
    @Test fun importedPersonalAdditionsAndCreatorHistorySurviveUnifiedForwardRestore() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        assertFalse("Disposable emulator must start without H4 recovery evidence",
            V344UnifiedRestoreJournal.hasPendingFor(app))
        val passphrase = "emulator-only-safe-test-" + UUID.randomUUID().toString()
        val secret = passphrase.toCharArray()
        val zip = File(app.cacheDir, "v384-ci-owner-" + UUID.randomUUID() + ".zip")
        try {
            withTimeout(300_000L) {
                // Test fixtures belong ONLY to a fresh CI emulator. Enforce
                // no preexisting owner reminder, habit or Focus history.
                val reminders = PersonalReminderStore(app)
                val habits = Phase1HabitStore(app)
                val focus = CreatorFocusSessionStore(app)
                check(reminders.load().isEmpty() && habits.load().isEmpty() &&
                    focus.load().history.isEmpty() && focus.load().active == null) {
                    "Unified fixture must start on a clean disposable emulator"
                }
                val reminder = PersonalReminder(
                    id = "v384-personal-ci-" + UUID.randomUUID(),
                    title = "Synthetic owner's reading reminder",
                    startDate = LocalDate.of(2026, 11, 13),
                    time = LocalTime.of(21, 30),
                    createdAtMillis = 1_700_000_000_000L,
                    updatedAtMillis = 1_700_000_000_000L,
                )
                val doneAt = Instant.parse("2026-11-07T04:00:00Z")
                val habit = Phase1HabitChallenge(
                    id = "v384-21day-ci-" + UUID.randomUUID(),
                    activity = "Synthetic reading challenge",
                    startDate = LocalDate.of(2026, 11, 7),
                    zoneId = "Asia/Kolkata",
                ).confirmToday(doneAt)
                reminders.create(reminder)
                habits.create(habit)
                // Incoming data will exist in the SELECTED target ZIP but
                // will be absent from the ORIGINAL phone at restore time.
                val incomingReminder = reminder.copy(
                    id = "v384-incoming-ci-" + UUID.randomUUID(),
                    title = "Incoming owner reminder"
                )
                val incomingHabit = habit.copy(
                    id = "v384-incoming-habit-ci-" + UUID.randomUUID(),
                    activity = "Incoming single-activity habit",
                    confirmations = emptyMap(),
                )
                reminders.create(incomingReminder)
                habits.create(incomingHabit)
                val focusSession = CreatorFocusSessionPolicy.create(
                    projectId = "v384-owner-focus",
                    stageId = "script",
                    targetMinutes = 25,
                    atWallMillis = 1_700_000_000_000L,
                    id = "v384-focus-ci",
                )
                val focusStarted = CreatorFocusSessionPolicy.start(focusSession, 1_000L, 2)
                val focusFinished = CreatorFocusSessionPolicy.finish(
                    focusStarted, 61_000L, 2, 1_700_000_061_000L,
                )
                focus.importAfterApprovedRestore(CreatorFocusSessionCodec.encode(
                    CreatorFocusLedger(history = listOf(focusFinished)),
                ))
                val manager = V341UnifiedBackupManager(app)
                zip.outputStream().buffered().use { manager.export(secret, it) }
                assertTrue(zip.length() > 0L)
                zip.inputStream().buffered().use { manager.verify(secret, it) }
                // Simulate a genuinely different destination phone with
                // ORIGINAL records retained and target-only records absent.
                reminders.delete(incomingReminder.id)
                habits.deleteReviewed(incomingHabit.id, incomingHabit)
                focus.importAfterApprovedRestore(CreatorFocusSessionCodec.encode(
                    CreatorFocusLedger()
                ))
                assertEquals(listOf(reminder.id), reminders.load().map { it.id })
                assertEquals(listOf(habit.id), habits.load().map { it.id })
                assertTrue(focus.load().history.isEmpty())
                val review = zip.inputStream().buffered().use {
                    manager.inspectRestore(secret, it)
                }
                // Incoming 21-day plans explicitly need OWNER review, but
                // non-conflicting additions are safe to prepare after consent.
                assertTrue(review.personalDiff.needsOwnerReview)
                assertEquals(0, review.personalDiff.conflicts)
                assertTrue("The selected Personal ZIP must add real missing records",
                    review.personalDiff.proposedAdditions >= 2)
                assertTrue(V385UnifiedPersonalTargetReviewPolicy
                    .conflictFreeAndVerified(review.personalDiff))

                zip.inputStream().buffered().use { chosen ->
                    V342UnifiedRestorePreparationStore(app).prepare(
                        chosen, secret, ownerRequested = true,
                    )
                }
                V343UnifiedRecoveryEscrow(app).retainOriginal(
                    ownerConfirmed = true, passphrase = secret,
                )
                val readOnly = V344UnifiedRestorePrecommit(app).inspectFullOwnerProof(
                    secret, ownerReviewed = true,
                )
                assertTrue(readOnly.preflight.sourceAndEscrowAuthenticated)

                val reserved = V349UnifiedRestoreReservation(app).reserveOnly(
                    passphrase = secret, ownerConfirmedPreparation = true,
                )
                assertTrue(reserved.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES)

                val creator = V357UnifiedCreatorForwardStage(app).executeInternalOnly(
                    ownerConfirmedStart = true, passphrase = secret,
                )
                assertTrue(creator.checkpointReached ==
                    V344UnifiedRestoreStep.CREATOR_WRITTEN)

                val personal = V358UnifiedPersonalForwardStage(app).executeInternalOnly(
                    passphrase = secret,
                    ownerConfirmedPersonalAdditions = true,
                    acknowledgedNonPortableAlarmsAndSongs = true,
                )
                assertTrue(personal.checkpointReached ==
                    V344UnifiedRestoreStep.PERSONAL_WRITTEN)

                V381UnifiedVerifiedForwardCompletion(app)
                    .verifyForwardAndRecordInternalOnly(
                        freshOwnerConsent = true, passphrase = secret,
                    )
                V379UnifiedReleaseCoordinator(app).releaseOwnerVerifiedInternalOnly(
                    freshOwnerConsent = true, passphrase = secret,
                )
                assertFalse("Verified owner release must archive blocking H4 journal",
                    V344UnifiedRestoreJournal.hasPendingFor(app))
                // Verify restored owner content, not only an absent journal.
                val restoredReminder = reminders.load().single { it.id == reminder.id }
                assertEquals(reminder.title, restoredReminder.title)
                assertEquals(reminder.startDate, restoredReminder.startDate)
                assertEquals(reminder.time, restoredReminder.time)
                val restoredIncomingReminder =
                    reminders.load().single { it.id == incomingReminder.id }
                assertEquals(incomingReminder.title, restoredIncomingReminder.title)
                val restoredHabit = habits.load().single { it.id == habit.id }
                assertEquals(habit.confirmations, restoredHabit.confirmations)
                assertEquals(doneAt, restoredHabit.confirmations.values.single())
                val restoredIncomingHabit =
                    habits.load().single { it.id == incomingHabit.id }
                assertEquals(incomingHabit.activity, restoredIncomingHabit.activity)
                assertTrue(restoredIncomingHabit.confirmations.isEmpty())
                assertEquals(2, reminders.load().size)
                assertEquals(2, habits.load().size)
                val restoredFocus = focus.load().history.single { it.id == focusFinished.id }
                assertEquals(focusFinished.accruedActiveMillis,
                    restoredFocus.accruedActiveMillis)
            }
        } finally {
            secret.fill('\u0000')
            zip.delete()
        }
    }
}
