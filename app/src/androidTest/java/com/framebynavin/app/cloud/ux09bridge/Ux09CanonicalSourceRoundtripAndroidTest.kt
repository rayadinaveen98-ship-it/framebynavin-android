package com.framebynavin.app.cloud.ux09bridge

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.TaskStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic objects passed through the ACTUAL existing Creator encoders
 * and decoders, without DataStore write, remote call, sync or user record.
 */
@RunWith(AndroidJUnit4::class)
class Ux09CanonicalSourceRoundtripAndroidTest {
    private val app = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun actualTaskStoreSimpleProjectIsLosslessAfterOnlyLocalFieldsExcluded() {
        val store = TaskStore(app)
        val task = CreatorTask(
            id = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
            title = "Synthetic filmmaking lesson",
            platform = "YouTube",
            contentType = "Video",
            dueLabel = "Tomorrow",
            notes = "All portable text should survive",
            alarmSoundUri = "content://local/not-for-cloud",
            snoozeCount = 2,
            workingUntilMillis = 500L,
        )
        val result = Ux09CanonicalReadinessPolicy.inspectProject(
            task, store.encodeRecoveryTask(task), store::decodeRecoveryTask,
        )
        assertTrue("Existing TaskStore lost fields: " + result, result is Ux09CanonicalReadinessPolicy.Result.Ready)
        val ready = result as Ux09CanonicalReadinessPolicy.Result.Ready
        assertEquals(task.id, ready.entityId)
        assertTrue(ready.bytes <= 16_384)
        assertTrue(!ready.payloadJson.contains("content://local"))
        assertEquals("All portable text should survive",
            org.json.JSONObject(ready.payloadJson).getString("notes"))
    }

    @Test fun actualIdeaVaultSimpleIdeaIsLosslessAfterOnlyLocalFieldsExcluded() {
        val store = IdeaVaultStore(app)
        val idea = CreatorIdea(
            id = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
            title = "Synthetic animation idea",
            notes = "Keep this entire idea description",
            topic = "Frame composition",
            createdAtMillis = 1200L,
            updatedAtMillis = 1200L,
            tags = listOf("frame", "animation"),
        )
        val result = Ux09CanonicalReadinessPolicy.inspectIdea(
            idea, store.encodeRecoveryIdea(idea), store::decodeRecoveryIdea,
        )
        assertTrue("Existing IdeaVaultStore lost fields: " + result, result is Ux09CanonicalReadinessPolicy.Result.Ready)
        val ready = result as Ux09CanonicalReadinessPolicy.Result.Ready
        assertEquals(idea.id, ready.entityId)
        assertTrue(ready.bytes <= 16_384)
        assertEquals("Keep this entire idea description",
            org.json.JSONObject(ready.payloadJson).getString("notes"))
    }
}
