package com.framebynavin.app.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.io.File

class V185IdeaProjectConversionTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = context.getSharedPreferences("idea_conversion_v185", 0)
    @Before fun reset() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        prefs.edit().clear().commit()
        TaskStore(context).save(emptyList()); IdeaVaultStore(context).save(emptyList())
        CreatorRewardStore(context).importJson("[]")
        Unit
    }
    @After fun cleanup() = reset()
    private fun request(id: String = "idea", plan: ProjectAttentionPlan = ProjectAttentionPlan.OFF) =
        IdeaProjectConversionRequest(id, "YouTube", "Short", System.currentTimeMillis() + 86_400_000L,
            plan, CreatorDataGate.generation(context))
    private suspend fun seed(idea: CreatorIdea = CreatorIdea(id = "idea", title = "Camera movement", notes = "Keep this context")) {
        IdeaVaultStore(context).save(listOf(idea))
    }

    @Test fun interruptionAtEveryWriteRecoversOneProjectLinkAndRewardWithOriginalAudio() = runBlocking {
        val audio = File(context.filesDir, "voice_ideas/voice_idea_conversion_test.m4a").apply {
            parentFile!!.mkdirs(); writeBytes(byteArrayOf(1, 2, 3))
        }
        try {
            for (stage in listOf("prepared", "project", "link", "reward")) {
                reset()
                seed(CreatorIdea(id = "idea", title = "Camera movement", notes = "తెలుగు notes", audioLocalPath = audio.path))
                val req = request()
                val failure = runCatching { IdeaProjectConversion(context) { if (it == stage) error("interrupted") }.convert(req) }
                assertTrue(failure.isFailure)
                CreatorDataGate.readyTransaction(context) { }
                CreatorDataGate.readyTransaction(context) { }
                val task = TaskStore(context).load().single()
                val idea = IdeaVaultStore(context).load().single()
                assertEquals(task.id, idea.projectTaskId); assertEquals(IdeaStatus.CONVERTED, idea.status)
                assertEquals("తెలుగు notes", idea.notes); assertEquals(audio.path, idea.audioLocalPath); assertTrue(audio.exists())
                assertTrue(task.notes.contains("తెలుగు notes")); assertFalse(task.reminderEnabled)
                assertEquals(ProjectAttentionPlan.OFF, task.attentionPlan)
                assertEquals(task.id, IdeaProjectConversion(context).convert(req))
                assertEquals(1, TaskStore(context).load().size)
                val reward = CreatorRewardStore(context).load().single()
                assertEquals("idea-converted:idea", reward.eventKey); assertEquals(task.id, reward.projectId)
                assertFalse(prefs.contains("pending"))
            }
        } finally { audio.delete() }
    }

    @Test fun concurrentRetriesShareOneProjectAndPersistExplicitReminderChoice() = runBlocking {
        seed()
        assertEquals(ProjectAttentionPlan.OFF, IdeaProjectConversion(context).rememberedPlan())
        val req = request(plan = ProjectAttentionPlan.LIGHT)
        val ids = coroutineScope { (1..8).map { async(Dispatchers.Default) { IdeaProjectConversion(context).convert(req) } }.awaitAll() }
        assertEquals(1, ids.toSet().size)
        val task = TaskStore(context).load().single()
        assertEquals(ProjectAttentionPlan.LIGHT, task.attentionPlan); assertTrue(task.reminderEnabled)
        assertEquals(ReminderMode.SIMPLE, task.reminderMode)
        assertEquals(ProjectAttentionPlan.LIGHT, IdeaProjectConversion(context).rememberedPlan())
        assertEquals(1, CreatorRewardStore(context).load().size)
    }

    @Test fun restoreGenerationRejectsPendingConversionAndOldRequest() = runBlocking {
        seed(); val old = request()
        assertTrue(runCatching { IdeaProjectConversion(context) { if (it == "project") error("interrupted") }.convert(old) }.isFailure)
        CreatorDataGate.transaction {
            CreatorDataGate.invalidate(context)
            TaskStore(context).save(emptyList())
            seed(CreatorIdea(id = "restored", title = "Restored work"))
            CreatorRewardStore(context).importJson("[]")
        }
        CreatorDataGate.readyTransaction(context) { }
        assertTrue(TaskStore(context).load().isEmpty()); assertTrue(CreatorRewardStore(context).load().isEmpty())
        assertEquals("restored", IdeaVaultStore(context).load().single().id)
        assertTrue(runCatching { IdeaProjectConversion(context).convert(old) }.exceptionOrNull() is CreatorWriteConflict)
        assertFalse(prefs.contains("pending"))
    }

    @Test fun removedArchivedOrMissingLinkedProjectNeverCreatesAReplacement() = runBlocking {
        for (idea in listOf(
            CreatorIdea(id = "idea", title = "Deleted", trashedAtMillis = 1),
            CreatorIdea(id = "idea", title = "Archived", status = IdeaStatus.ARCHIVED),
            CreatorIdea(id = "idea", title = "Linked", status = IdeaStatus.CONVERTED, projectTaskId = "missing"),
        )) {
            seed(idea)
            assertTrue(runCatching { IdeaProjectConversion(context).convert(request()) }.isFailure)
            assertTrue(TaskStore(context).load().isEmpty()); assertFalse(prefs.contains("pending"))
            assertEquals(idea, IdeaVaultStore(context).load().single())
        }
    }

    @Test fun olderPartialConversionAdoptsOriginalProjectWithoutOverwritingWork() = runBlocking {
        seed()
        val original = CreatorTask(id = "original", title = "Edited project title", platform = "YouTube", contentType = "Short",
            dueLabel = "Tomorrow", notes = "Existing script", origin = CreatorTaskOrigin.IDEA_VAULT, sourceRefId = "idea")
        TaskStore(context).save(listOf(original))
        assertEquals("original", IdeaProjectConversion(context).convert(request()))
        assertEquals("Existing script", TaskStore(context).load().single().notes)
        assertEquals("Edited project title", TaskStore(context).load().single().title)
        assertEquals("original", IdeaVaultStore(context).load().single().projectTaskId)
        assertEquals(1, CreatorRewardStore(context).load().size)
    }

    @Test fun pastDeadlineFailsWithoutWritingAndKeepsSelectedPreference() = runBlocking {
        seed()
        assertTrue(runCatching { IdeaProjectConversion(context).convert(request().copy(dueAtMillis = 1)) }.isFailure)
        assertFalse(prefs.contains("pending")); assertTrue(TaskStore(context).load().isEmpty())
        assertEquals("", IdeaVaultStore(context).load().single().projectTaskId)
        assertEquals(ProjectAttentionPlan.OFF, IdeaProjectConversion(context).rememberedPlan())
    }
}
