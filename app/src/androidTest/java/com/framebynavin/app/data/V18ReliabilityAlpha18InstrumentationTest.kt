package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18ReliabilityAlpha18InstrumentationTest {
    @Test
    fun taskStore_serializesConcurrentReadModifyWrite() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        HardeningTestEnvironment.requireCiEmulator()
        val storeA = TaskStore(context)
        val storeB = TaskStore(context)
        val original = HardeningTestEnvironment.stage("capture original tasks") { storeA.load() }
        try {
            HardeningTestEnvironment.stage("seed atomic task") { storeA.save(listOf(task())) }
            HardeningTestEnvironment.stage("parallel read-modify-write") {
                coroutineScope {
                    launch(Dispatchers.Default) {
                        storeA.updateTask("atomic") { it.copy(progress = 45) }
                    }
                    launch(Dispatchers.Default) {
                        storeB.updateTask("atomic") { it.copy(notes = "second mutation survived") }
                    }
                }
            }
            val result = HardeningTestEnvironment.stage("read committed tasks") {
                storeA.load().single { it.id == "atomic" }
            }
            assertEquals(45, result.progress)
            assertEquals("second mutation survived", result.notes)
        } finally {
            withContext(NonCancellable) {
                HardeningTestEnvironment.stage("restore original tasks") { storeA.save(original) }
            }
        }
    }

    @Test
    fun taskStore_rejectsDuplicateIdsOnImportValidation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val store = TaskStore(context)
        val raw = "[{\"id\":\"dup\",\"title\":\"A\"},{\"id\":\"dup\",\"title\":\"B\"}]"
        assertThrows(IllegalArgumentException::class.java) { store.validateJson(raw) }
    }

    private fun task() = CreatorTask(
        id = "atomic",
        title = "Atomic update",
        platform = "YouTube",
        contentType = "Video",
        dueLabel = "Today",
        status = TaskStatus.PLANNED,
    )
}
