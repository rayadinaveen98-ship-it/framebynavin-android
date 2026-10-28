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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable CI emulator only; never run destructive data replacement on user devices. */
@RunWith(AndroidJUnit4::class)
class V226PersonalBackupTransferTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val zone = ZoneId.of("Asia/Kolkata")
    private val oldToken = "b6a4e3af-7a2b-4b8d-b039-76b91e3406a0"

    private fun record(id: String, title: String): PersonalReminder = PersonalReminder(
        id = id,
        title = title,
        notes = "Private diary notes, never Creator data",
        startDate = LocalDate.now(zone).plusDays(2),
        time = LocalTime.of(9, 0),
        zoneId = zone,
        repeat = PersonalRepeat.DAILY,
        alert = PersonalAlert.NOTIFICATION,
        pendingOccurrenceMillis = System.currentTimeMillis() + 172_800_000L,
        pendingFireAtMillis = System.currentTimeMillis() + 172_800_000L,
        pendingToken = oldToken,
        presentedToken = oldToken,
    )

    @Test fun verifiedRestoreResetsOldActionTokensAndLeavesCreatorProjectsUntouched(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val transfer = PersonalBackupTransfer(app)
        val coordinator = PersonalDeliveryCoordinator(app)
        val oldPersonal = store.load()
        val oldCreator = TaskStore(app).load()
        val original = record("personal:v226-replaced", "Confidential backup title")
        val previouslyActive = record("personal:v226-old", "Original personal entry")
        try {
            store.mutate { listOf(previouslyActive) }
            val raw = PersonalReminderCodec.encode(listOf(original))
            assertEquals(1, transfer.preview(raw).reminderCount)
            val receipt = transfer.restoreApproved(raw)
            assertEquals(1, receipt.importedCount)
            val restored = store.load().single()
            assertEquals(original.title, restored.title)
            assertNotEquals(oldToken, restored.pendingToken)
            assertEquals("", restored.presentedToken)
            assertEquals(oldCreator, TaskStore(app).load())
            assertEquals(
                PersonalApplyResult.STALE,
                coordinator.apply(original.id, original.pendingOccurrenceMillis,
                    oldToken, PersonalDeliveryContract.ACTION_DONE),
            )
            // Re-arming the imported schedule rotates DataStore lastGood,
            // so only the dedicated pre-restore copy is a durable undo point.
            val recoveryRaw = PersonalPreRestoreRecovery(app).latestVerified()
            assertTrue(recoveryRaw != null)
            assertEquals(listOf(previouslyActive), PersonalReminderCodec.decode(requireNotNull(recoveryRaw)))
        } finally {
            coordinator.cancelPending(original.id)
            coordinator.cancelPending(previouslyActive.id)
            PersonalReminderNotifications.cancel(app, original.id)
            PersonalReminderNotifications.cancel(app, previouslyActive.id)
            store.mutate { oldPersonal }
        }
    }

    @Test fun corruptedOrCreatorTypeBackupNeverMutatesPersonalOrCreatorStores(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val transfer = PersonalBackupTransfer(app)
        val before = store.load()
        val creatorBefore = TaskStore(app).load()
        val raw = PersonalReminderCodec.encode(listOf(record("personal:v226-safe", "Secret marker")))
        for (tampered in listOf(raw.replace("Secret marker", "Changed marker"),
            raw.replace("PERSONAL", "CREATOR"))) {
            assertTrue(runCatching { transfer.restoreApproved(tampered) }.isFailure)
            assertEquals(before, store.load())
            assertEquals(creatorBefore, TaskStore(app).load())
        }
    }
}
