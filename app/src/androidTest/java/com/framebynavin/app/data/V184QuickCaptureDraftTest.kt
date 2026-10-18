package com.framebynavin.app.data

import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.voice.VoiceIdeaRecording
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.io.File

class V184QuickCaptureDraftTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun reset() { HardeningTestEnvironment.requireCiEmulator(); context.getSharedPreferences("quick_capture_draft_v184", 0).edit().clear().commit() }
    @After fun clear() = reset()

    @Test fun textAndFinalizedTakeSurviveStoreRecreationAndCleanup() {
        val audio = File(context.filesDir, "voice_ideas/voice_idea_draft_test.m4a")
        audio.parentFile!!.mkdirs(); audio.writeBytes(byteArrayOf(1, 2, 3))
        try {
            val draft = QuickCaptureDraft(generation = CreatorDataGate.generation(context),
                title = "సీన్ analysis", notes = "lighting", recording = VoiceIdeaRecording(audio.path, 1234))
            QuickCaptureDraftStore(context).save(draft)
            val recovered = QuickCaptureDraftStore(context).load()!!
            assertEquals(draft.copy(recording = draft.recording!!.copy(localPath = audio.canonicalPath)), recovered)
            val storage = VoiceIdeaStorage(audio.parentFile!!, orphanGraceMillis = 0,
                protectedPaths = { setOfNotNull(QuickCaptureDraftStore(context).load()?.recording?.localPath) })
            assertTrue(storage.snapshot(emptyList()).referencedCount >= 1)
            assertTrue(audio.exists())
            QuickCaptureDraftStore(context).discard(recovered, emptySet())
            assertNull(QuickCaptureDraftStore(context).load())
            assertFalse(audio.exists())
        } finally { audio.delete() }
    }

    @Test fun staleDraftIsNotRecoveredOrRewrittenAfterGenerationChange() {
        val store = QuickCaptureDraftStore(context)
        val draft = QuickCaptureDraft(generation = CreatorDataGate.generation(context), title = "old draft")
        store.save(draft)
        // Only emulator fixtures may change creator generation.
        HardeningTestEnvironment.requireCiEmulator()
        CreatorDataGate.invalidate(context)
        assertNull(store.load())
        assertTrue(runCatching { store.save(draft) }.exceptionOrNull() is CreatorWriteConflict)
    }

    @Test fun discardNeverDeletesAnAlreadySavedTakeOrExternalFile() {
        val audio = File(context.filesDir, "voice_ideas/voice_idea_saved_test.m4a")
        audio.parentFile!!.mkdirs(); audio.writeBytes(byteArrayOf(1))
        try {
            val store = QuickCaptureDraftStore(context)
            val draft = QuickCaptureDraft(generation = CreatorDataGate.generation(context), recording = VoiceIdeaRecording(audio.path, 1))
            store.save(draft); store.discard(draft, setOf(audio.path))
            assertTrue(audio.exists())
            val external = File(context.cacheDir, "external-draft.m4a").apply { writeBytes(byteArrayOf(2)) }
            try {
                val other = draft.copy(id = "other", recording = VoiceIdeaRecording(external.path, 1))
                store.save(other)
                assertNull(store.load()!!.recording)
                store.discard(other, emptySet())
                assertTrue(external.exists())
            } finally { external.delete() }
        } finally { audio.delete() }
    }
}
