package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStore
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** CI-emulator-only: confirms Personal DONE, export, restore, and recovery are independent of Creator data. */
@RunWith(AndroidJUnit4::class)
class V282PersonalProgressRestoreTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun acceptedDoneIsBackedUpAndRecoveredWithoutCreatorMutation(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val transfer = PersonalBackupTransfer(app)
        val oldSnapshot = PersonalCompletionBackupCodec.decode(store.exportPersonalJson())
        val creatorBefore = TaskStore(app).load()
        val id = "v282:personal-history"
        val token = "66666666-7777-8888-9999-aaaaaaaaaaaa"
        val occurrence = System.currentTimeMillis() + 5L * 86_400_000L
        val coordinator = PersonalDeliveryCoordinator(app)
        try {
            store.mutate(replacementHistory = emptyList()) { emptyList() }
            val future = PersonalReminder(
                id = id, title = "Private action", notes = "Never in Creator cloud",
                startDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(5),
                time = LocalTime.of(10, 30),
                pendingOccurrenceMillis = occurrence,
                pendingFireAtMillis = occurrence,
                pendingToken = token,
                presentedToken = token,
                presentedAtMillis = System.currentTimeMillis(),
            )
            store.create(future)
            assertEquals(PersonalApplyResult.ACCEPTED, coordinator.apply(
                id, occurrence, token, PersonalDeliveryContract.ACTION_DONE,
            ))
            assertEquals(PersonalApplyResult.STALE, coordinator.apply(
                id, occurrence, token, PersonalDeliveryContract.ACTION_DONE,
            ))
            val history = store.loadCompletionHistory()
            assertEquals(1, history.size)
            assertEquals(occurrence, history.single().occurrenceMillis)
            val backed = transfer.export()
            assertEquals(1, transfer.preview(backed).completionCount)
            assertEquals(history, PersonalCompletionBackupCodec.decode(backed).completions)
            store.mutate(replacementHistory = emptyList()) { emptyList() }
            assertTrue(store.loadCompletionHistory().isEmpty())
            assertEquals(1, transfer.restoreApproved(backed).importedCount)
            assertEquals(history, store.loadCompletionHistory())
            assertEquals(creatorBefore, TaskStore(app).load())
            val recovery = PersonalPreRestoreRecovery(app).latestVerified()
            assertTrue(recovery != null)
            assertTrue(PersonalCompletionBackupCodec.decode(requireNotNull(recovery)).completions.isEmpty())
            assertEquals(0, transfer.restorePreviousApproved().importedCount)
            assertTrue(store.loadCompletionHistory().isEmpty())
            assertEquals(creatorBefore, TaskStore(app).load())
        } finally {
            coordinator.cancelPending(id)
            PersonalReminderNotifications.cancel(app, id)
            store.mutate(replacementHistory = oldSnapshot.completions) { oldSnapshot.reminders }
        }
    }

    @Test fun corruptedHistoryDoesNotReplacePersonalRecords(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val transfer = PersonalBackupTransfer(app)
        val before = store.exportPersonalJson()
        val tampered = before.replace("payloadSha256", "tamperedSha256")
        assertTrue(runCatching { transfer.restoreApproved(tampered) }.isFailure)
        assertEquals(before, store.exportPersonalJson())
    }
}
