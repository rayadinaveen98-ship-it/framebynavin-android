package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

/** Android proof that a Focus receipt survives a real Creator archive restore. */
@RunWith(AndroidJUnit4::class)
class CreatorFocusBackupV10Test {
    @Test fun creatorBackupV10IncludesVerifiedFocusHistoryAndRestoresIt(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = CreatorFocusSessionStore(context)
        val before = store.exportJson()
        try {
            val created = CreatorFocusSessionPolicy.create(
                projectId = "focus-backup-test",
                stageId = "script",
                targetMinutes = 25,
                atWallMillis = 1_700_000_000_000L,
                id = "focus-backup-v10",
            )
            val started = CreatorFocusSessionPolicy.start(created, 1000L, 2)
            val finished = CreatorFocusSessionPolicy.finish(started, 61_000L, 2, 1_700_000_061_000L)
            store.importAfterApprovedRestore(
                CreatorFocusSessionCodec.encode(CreatorFocusLedger(history = listOf(finished)))
            )
            val manager = CreatorBackupManager(context)
            val archived = manager.createBackup()
            val root = JSONObject(archived)
            assertEquals(10, manager.validate(archived).schemaVersion)
            assertTrue(root.has("creatorFocusSessions"))
            assertTrue(root.getJSONObject("manifest").getJSONArray("included")
                .toString().contains("creatorFocusSessions"))
            assertEquals(60_000L,
                CreatorFocusSessionCodec.decode(root.getString("creatorFocusSessions"))
                    .history.single().accruedActiveMillis)

            store.importAfterApprovedRestore(CreatorFocusSessionCodec.encode(CreatorFocusLedger()))
            assertTrue(store.load().history.isEmpty())
            manager.restore(archived)
            assertEquals("focus-backup-test", store.load().history.single().projectId)
            assertEquals(60_000L, store.load().history.single().accruedActiveMillis)

            // Corruption is rejected before any restore changes app data.
            val tampered = JSONObject(archived).put("creatorFocusSessions",
                CreatorFocusSessionCodec.encode(CreatorFocusLedger())).toString()
            assertThrows(IllegalArgumentException::class.java) { manager.validate(tampered) }
            assertEquals(1, store.load().history.size)
        } finally {
            store.importAfterApprovedRestore(before)
        }
    }

    @Test fun portableFocusArchiveNeverCarriesActiveDeviceClockAcrossBackup(): Unit {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = CreatorFocusSessionStore(context)
        val before = store.exportJson()
        try {
            val created = CreatorFocusSessionPolicy.create(
                projectId = "focus-boot-anchor", stageId = "research",
                targetMinutes = 15, atWallMillis = 1_700_000_000_000L,
                id = "focus-recovery-v10"
            )
            store.importAfterApprovedRestore(CreatorFocusSessionCodec.encode(CreatorFocusLedger()))
            store.create(created)
            store.update(created.id) { CreatorFocusSessionPolicy.start(it, 1_000L, 4) }
            store.update(created.id) { CreatorFocusSessionPolicy.checkpoint(it, 21_000L, 4) }
            val backup = CreatorFocusSessionCodec.decode(store.exportPortableJson())
            assertEquals(CreatorFocusState.PAUSED, backup.active?.state)
            assertEquals(-1L, backup.active?.segmentStartedElapsedMillis)
            assertEquals(20_000L, backup.active?.accruedActiveMillis)
            assertTrue(backup.active?.recoveredAfterReboot == true)

            // A portable export must NEVER pause a running local session.
            assertEquals(CreatorFocusState.RUNNING, store.load().active?.state)
            assertFalse(store.load().active?.recoveredAfterReboot == true)
        } finally {
            store.importAfterApprovedRestore(before)
        }
    }
}
