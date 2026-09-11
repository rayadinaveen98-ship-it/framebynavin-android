package com.framebynavin.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorContentDna
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentDnaPersistenceV96Test {
    @Test
    fun explicitContentDnaSurvivesPortableRoundTrip() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = TaskStore(context)
        val before = store.exportJson()
        try {
            val dna = CreatorContentDna(
                creatorModeId = "gaming",
                archetypeId = "review",
                productionStyles = setOf("Gameplay Capture", "Voiceover"),
                platform = "YouTube",
                deliveryFormat = "Long-form",
            )
            store.save(listOf(CreatorTask(id = "v96", title = "V96", platform = "YouTube", contentType = "Long-form", dueLabel = "Today", dueAtMillis = 1L, contentDna = dna)))
            val portable = store.exportJson()
            assertTrue(portable.contains("contentDna"))
            val restored = store.importJson(portable).single()
            assertEquals("gaming", restored.contentDna.creatorModeId)
            assertEquals("review", restored.contentDna.archetypeId)
            assertEquals(setOf("Gameplay Capture", "Voiceover"), restored.contentDna.productionStyles)
        } finally {
            store.importJson(before)
        }
    }
}
