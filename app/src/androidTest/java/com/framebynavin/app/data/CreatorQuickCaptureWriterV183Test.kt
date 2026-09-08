package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CreatorQuickCaptureWriterV183Test {
    @Test fun concurrentCapturesPreserveExistingAndBothNewIdeas(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val store = IdeaVaultStore(context)
        try {
            val existing = CreatorIdea(id = UUID.randomUUID().toString(), title = "Existing research")
            store.mutate { listOf(existing) + it }
            val epoch = CreatorDataGate.generation(context)
            val first = CreatorIdea(id = UUID.randomUUID().toString(), title = "First capture")
            val second = CreatorIdea(id = UUID.randomUUID().toString(), title = "Second capture")
            withTimeout(15_000) {
                val a = async(Dispatchers.Default) { store.capture(first, epoch) }
                val b = async(Dispatchers.Default) { store.capture(second, epoch) }
                a.await(); b.await()
            }
            val latest = store.load()
            assertEquals(existing, latest.first { it.id == existing.id })
            assertEquals(first, latest.first { it.id == first.id })
            assertEquals(second, latest.first { it.id == second.id })
            assertEquals(3, latest.count { it.id in setOf(existing.id, first.id, second.id) })
        } finally {
            withContext(NonCancellable) { manager.restore(original) }
        }
    }

    @Test fun staleCaptureCannotReplayAfterRestore(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val store = IdeaVaultStore(context)
        try {
            val existing = CreatorIdea(id = UUID.randomUUID().toString(), title = "Restore baseline")
            store.mutate { listOf(existing) + it }
            val checkpoint = manager.createBackup()
            val oldEpoch = CreatorDataGate.generation(context)
            store.mutate { current -> current.map { if (it.id == existing.id) it.copy(title = "Newer edit") else it } }
            manager.restore(checkpoint)
            val stale = CreatorIdea(id = UUID.randomUUID().toString(), title = "Queued before restore")
            val failure = runCatching { store.capture(stale, oldEpoch) }.exceptionOrNull()
            assertTrue(failure is CreatorWriteConflict)
            assertEquals("Restore baseline", store.load().first { it.id == existing.id }.title)
            assertFalse(store.load().any { it.id == stale.id })
        } finally {
            withContext(NonCancellable) { manager.restore(original) }
        }
    }

    @Test fun duplicateCaptureCannotReplaceAnExistingIdea(): Unit = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val original = manager.createBackup()
        val store = IdeaVaultStore(context)
        try {
            val existing = CreatorIdea(id = UUID.randomUUID().toString(), title = "Original title")
            store.mutate { listOf(existing) + it }
            val failure = runCatching {
                store.capture(existing.copy(title = "Overwrite attempt"), CreatorDataGate.generation(context))
            }.exceptionOrNull()
            assertTrue(failure is CreatorWriteConflict)
            assertEquals(existing, store.load().first { it.id == existing.id })
        } finally {
            withContext(NonCancellable) { manager.restore(original) }
        }
    }
}
