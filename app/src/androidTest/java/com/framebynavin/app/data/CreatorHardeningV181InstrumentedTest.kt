package com.framebynavin.app.data

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.cloud.CloudLocalStore
import com.framebynavin.app.cloud.CloudSyncManager
import com.framebynavin.app.cloud.CloudOperationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CreatorHardeningV181InstrumentedTest {
    @Test fun backupRoundTripAndRestoreGeneration() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = HardeningTestEnvironment.stage("capture recovery baseline") { manager.createBackup() }
        val id = "hardening-test-${UUID.randomUUID()}"
        try {
            val created = CreatorTask(id=id,title="Before",platform="YouTube",contentType="Long-form",dueLabel="Today")
            TaskStore(context).mutate { it + created }
            val before = manager.createBackup()
            assertEquals(4, manager.validate(before).schemaVersion)
            val backupRoot = org.json.JSONObject(before)
            assertTrue(backupRoot.has("youtubeProjectLinks"))
            assertTrue(backupRoot.has("youtubeMilestones"))
            assertTrue(backupRoot.has("payloadSha256"))
            val generation = CreatorDataGate.generation(context)
            TaskStore(context).updateTask(id) { it.copy(title="After") }
            HardeningTestEnvironment.stage("restore test snapshot") { manager.restore(before) }
            assertEquals("Before", TaskStore(context).load().first { it.id==id }.title)
            assertTrue(CreatorDataGate.generation(context) > generation)
            assertThrows(CreatorWriteConflict::class.java) {
                runBlocking { TaskStore(context).applyDelta(listOf(created), listOf(created.copy(title="Stale")), generation) }
            }
            assertTrue(manager.recoveryCopies().isNotEmpty())
        } finally {
            CreatorDataGate.nonCancellable {
                HardeningTestEnvironment.stage("restore original snapshot") { manager.restore(original) }
            }
        }
    }

    @Test fun cloudIsManualOnlyEvenWhenAnOldPreferenceEnabledIt() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val local=CloudLocalStore(context)
        local.setEnabled(true)
        assertFalse(local.settings().enabled)
        assertTrue(CloudSyncManager(context).syncNow(false) is CloudOperationResult.Skipped)
    }
}
