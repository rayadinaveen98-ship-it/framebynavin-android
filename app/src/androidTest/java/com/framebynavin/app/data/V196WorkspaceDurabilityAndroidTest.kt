package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Storage-contract tests with real Android JSON and SharedPreferences implementations.
 * They do not modify installed creator projects or the app's durable TaskStore.
 */
@RunWith(AndroidJUnit4::class)
class V196WorkspaceDurabilityAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun complexProjectRoundTripsWithPublicationHistoryScriptAssetsAndReminders() {
        val project = CreatorTask(
            id = "qa-" + UUID.randomUUID(),
            title = "Color and Blocking",
            platform = "YouTube", contentType = "Long-form", dueLabel = "Today",
            reminderEnabled = true, reminderAtMillis = 2_000_000L,
            workspace = CreatorContentWorkspace(
                revision = 43L,
                audience = "Cinema students",
                viewerProblem = "How does a frame guide attention?",
                angle = "Shot structure",
                references = (0 until 12).map { i ->
                    CreatorProjectReference(id = "ref-$i", label = "Source $i", url = "https://example.org/$i")
                },
                assets = (0 until 24).map { i ->
                    CreatorProjectAsset(id = "asset-$i", label = "Storyboard $i",
                        location = "content://com.example.provider/document/$i", kind = CreatorAssetKind.IMAGE)
                },
                scriptStudio = CreatorScriptStudio(projectId = "temp", revision = 8L,
                    titles = listOf(CreatorTitleIdea(id = "selected", text = "Camera language", selected = true)),
                    beats = (0 until 30).map { i ->
                        CreatorScriptBeat(id = "beat-$i", label = "Beat $i", narration = "Narration for beat $i")
                    }),
                deliverables = listOf(
                    CreatorDeliverable(
                        id = "main", platform = "YouTube", format = "Long video",
                        title = "Color and Blocking", status = CreatorDeliverableStatus.PUBLISHED,
                        publishedAtMillis = 123_000L, publishedUrl = "https://example.org/watch",
                        publicationHistory = listOf(
                            CreatorPublicationEvent(id = "event-a", atMillis = 123_000L,
                                titleSnapshot = "Color and Blocking", url = "https://example.org/watch"),
                        ),
                    ),
                    CreatorDeliverable(id = "short", platform = "Instagram", format = "Reel",
                        title = "Reel", parentDeliverableId = "main"),
                ),
            ),
        )
        val store = TaskStore(context)
        val encoded = store.encodeRecoveryTask(project)
        val restored = store.decodeRecoveryTask(encoded)
        assertEquals(project.id, restored.id)
        assertEquals(43L, restored.workspace.revision)
        assertEquals(12, restored.workspace.references.size)
        assertEquals(24, restored.workspace.assets.size)
        assertEquals(30, restored.workspace.scriptStudio?.beats?.size)
        assertEquals("Camera language", restored.workspace.scriptStudio?.selectedTitle())
        assertEquals(2, restored.workspace.deliverables.size)
        assertEquals("main", restored.workspace.deliverables[1].parentDeliverableId)
        assertEquals("event-a", restored.workspace.deliverables[0].publicationHistory.single().id)
        assertEquals("https://example.org/watch", restored.workspace.deliverables[0].publishedUrl)
        assertEquals(project.reminderAtMillis, restored.reminderAtMillis)
        assertEquals(project.reminderEnabled, restored.reminderEnabled)
    }

    @Test fun draftJournalsAreRevisionBoundAndCleanupDoesNotTouchCanonicalProject() {
        val projectId = "qa-draft-" + UUID.randomUUID()
        val drafts = CreatorEditorDraftStore(context)
        try {
            val plan = CreatorContentWorkspace(revision = 42L, audience = "Audience")
            drafts.saveProject(projectId, 42L, plan)
            assertEquals("Audience", drafts.loadProject(projectId, 42L)?.audience)
            assertNull(drafts.loadProject(projectId, 43L))

            val script = CreatorScriptStudio(projectId = projectId, revision = 5L,
                beats = listOf(CreatorScriptBeat(id = "beat", label = "Opening", narration = "Original")))
            drafts.saveScript(projectId, 42L, 5L, script)
            assertEquals("Original", drafts.loadScript(projectId, 42L, 5L)?.compiledNarration())
            assertNull(drafts.loadScript(projectId, 42L, 6L))
            assertNull(drafts.loadScript(projectId, 43L, 5L))

            val output = CreatorDeliverable(id = "planned", platform = "YouTube", format = "Long video")
            drafts.savePublish(projectId, 42L, listOf(output))
            assertEquals("planned", drafts.loadPublish(projectId, 42L)?.single()?.id)
            assertNull(drafts.loadPublish(projectId, 43L))
        } finally {
            drafts.clearAllForProject(projectId)
        }
        assertNull(drafts.loadProject(projectId, 42L))
        assertNull(drafts.loadScript(projectId, 42L, 5L))
        assertNull(drafts.loadPublish(projectId, 42L))
    }
}
