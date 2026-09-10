package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CreatorContentProjectRc1InstrumentedTest {

    @Test
    fun contentProjectRoundTripsWithoutCollapsingIndependentStates() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = CreatorBackupManager(context)
        val store = TaskStore(context)
        val original = manager.createBackup()
        val projectId = "rc1-project-${UUID.randomUUID()}"
        val deliverableId = "rc1-deliverable-${UUID.randomUUID()}"
        val publishedAt = System.currentTimeMillis() - 60_000L

        val studio = CreatorScriptStudio(
            projectId = projectId,
            revision = 4L,
            status = CreatorScriptStatus.READY_TO_RECORD,
            hooks = listOf(
                CreatorHookIdea(id = "hook-a", text = "Why does this frame feel inevitable?", selected = true),
                CreatorHookIdea(id = "hook-b", text = "The shot tells us before the dialogue does."),
            ),
            titles = listOf(
                CreatorTitleIdea(id = "title-a", text = "The Frame That Tells the Story", selected = true),
                CreatorTitleIdea(id = "title-b", text = "Visual Storytelling Hidden in Plain Sight"),
            ),
            beats = listOf(
                CreatorScriptBeat(
                    id = "beat-1",
                    label = "Setup",
                    purpose = "Establish the emotional question",
                    narration = "The scene looks simple until you notice where the character is placed.",
                    visualNotes = "Hold on the wide frame before the cut.",
                    bRollNotes = "Use two supporting frames from the same sequence.",
                    onScreenText = "Composition before dialogue",
                    status = CreatorScriptBeatStatus.LOCKED,
                ),
                CreatorScriptBeat(
                    id = "beat-2",
                    label = "Payoff",
                    purpose = "Connect composition to emotion",
                    narration = "The blocking turns distance into the real conflict.",
                    status = CreatorScriptBeatStatus.READY,
                ),
            ),
            creatorNotes = "Keep the VO restrained and let the frames breathe.",
        )

        val published = CreatorDeliverable(
            id = deliverableId,
            platform = "YouTube",
            format = "Long video",
            title = "The Frame That Tells the Story",
            status = CreatorDeliverableStatus.PUBLISHED,
            description = "A visual storytelling breakdown.",
            tags = "cinema, cinematography, storytelling",
            thumbnailConcept = "Character isolated inside negative space",
            publishedAtMillis = publishedAt,
            publishedUrl = "https://example.test/video",
            titleVariants = listOf(
                CreatorVariantIdea(id = "tv-1", text = "The Frame That Tells the Story"),
                CreatorVariantIdea(id = "tv-2", text = "Why This Shot Works"),
            ),
            thumbnailVariants = listOf(
                CreatorVariantIdea(id = "thumb-1", text = "Negative-space frame + minimal text"),
            ),
            publishGate = listOf(
                CreatorPublishGateItem(id = "gate-1", title = "Final export checked", status = CreatorPublishGateStatus.DONE),
                CreatorPublishGateItem(id = "gate-2", title = "Rights/source check", status = CreatorPublishGateStatus.DONE),
            ),
            publicationHistory = listOf(
                CreatorPublicationEvent(
                    id = "publication-1",
                    kind = CreatorPublicationEventKind.PUBLISHED,
                    atMillis = publishedAt,
                    titleSnapshot = "The Frame That Tells the Story",
                    thumbnailSnapshot = "Character isolated inside negative space",
                    descriptionSnapshot = "A visual storytelling breakdown.",
                    tagsSnapshot = "cinema, cinematography, storytelling",
                    url = "https://example.test/video",
                )
            ),
        )

        val workspace = CreatorContentWorkspace(
            revision = 9L,
            audience = "Telugu and English cinema viewers",
            viewerProblem = "They notice a strong scene but cannot explain why it works.",
            promise = "Translate the craft into a simple emotional explanation.",
            angle = "Composition and blocking before dialogue.",
            hook = studio.selectedHook(),
            script = studio.compiledNarration(),
            references = listOf(
                CreatorProjectReference(id = "ref-1", label = "Director interview", url = "https://example.test/interview"),
            ),
            checklist = listOf(
                CreatorChecklistItem(id = "check-1", title = "Research", status = CreatorChecklistStatus.DONE),
                CreatorChecklistItem(id = "check-2", title = "Record VO", status = CreatorChecklistStatus.TODO),
            ),
            assets = listOf(
                CreatorProjectAsset(
                    id = "asset-1",
                    label = "Reference still",
                    location = "content://framebynavin.test/reference-still",
                    kind = CreatorAssetKind.IMAGE,
                    notes = "Do not duplicate media bytes into project JSON.",
                )
            ),
            deliverables = listOf(published),
            learnings = "Lead with the emotional observation, then explain the technique.",
            scriptStudio = studio,
        )

        val task = CreatorTask(
            id = projectId,
            title = "RC1 Content Project",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Today",
            status = TaskStatus.PLANNED,
            progress = 42,
            workspace = workspace,
        )

        val tombstoneId = "rc1-tombstone-${UUID.randomUUID()}"
        val tombstone = CreatorTask(
            id = tombstoneId,
            title = "Deleted weekly content",
            platform = "Instagram",
            contentType = "Reel",
            dueLabel = "Today",
            status = TaskStatus.SKIPPED,
            origin = CreatorTaskOrigin.WEEKLY,
            scheduleOccurrenceKey = "weekly-${UUID.randomUUID()}",
            archivedAtMillis = -1L,
            workspace = workspace.copy(scriptStudio = studio.copy(projectId = tombstoneId)),
        )

        try {
            store.mutate { current -> current.filterNot { it.id == projectId || it.id == tombstoneId } + task + tombstone }
            val portable = manager.createBackup()
            val tasks = JSONArray(JSONObject(portable).getString("tasks"))
            val portableProject = (0 until tasks.length())
                .map(tasks::getJSONObject)
                .first { it.getString("id") == projectId }
            val portableTombstone = (0 until tasks.length())
                .map(tasks::getJSONObject)
                .first { it.getString("id") == tombstoneId }

            val portableWorkspace = portableProject.getJSONObject("workspace")
            assertTrue(portableWorkspace.has("scriptStudio"))
            assertFalse(portableWorkspace.isNull("scriptStudio"))
            val scrubbed = portableTombstone.getJSONObject("workspace")
            assertTrue(scrubbed.isNull("scriptStudio"))
            assertEquals(0, scrubbed.getJSONArray("deliverables").length())
            assertEquals(0, scrubbed.getJSONArray("assets").length())

            store.updateTask(projectId) { current ->
                current.copy(status = TaskStatus.DONE, workspace = CreatorContentWorkspace())
            }
            manager.restore(portable)

            val restored = store.load().first { it.id == projectId }
            assertEquals(TaskStatus.PLANNED, restored.status)
            assertEquals(42, restored.progress)
            assertEquals("Telugu and English cinema viewers", restored.workspace.audience)
            assertEquals(CreatorChecklistStatus.DONE, restored.workspace.checklist.first().status)
            assertEquals("content://framebynavin.test/reference-still", restored.workspace.assets.single().location)

            val restoredDeliverable = restored.workspace.deliverables.single { it.id == deliverableId }
            assertEquals(CreatorDeliverableStatus.PUBLISHED, restoredDeliverable.status)
            assertEquals(publishedAt, restoredDeliverable.publishedAtMillis)
            assertEquals(CreatorPublicationEventKind.PUBLISHED, restoredDeliverable.publicationHistory.single().kind)
            assertEquals("https://example.test/video", restoredDeliverable.publicationHistory.single().url)
            assertEquals(TaskStatus.PLANNED, restored.status)

            val restoredStudio = requireNotNull(restored.workspace.scriptStudio)
            assertEquals(CreatorScriptStatus.READY_TO_RECORD, restoredStudio.status)
            assertEquals(4L, restoredStudio.revision)
            assertEquals("Why does this frame feel inevitable?", restoredStudio.selectedHook())
            assertEquals("The Frame That Tells the Story", restoredStudio.selectedTitle())
            assertEquals(2, restoredStudio.beats.size)
            assertEquals(CreatorScriptBeatStatus.LOCKED, restoredStudio.beats.first().status)
            assertTrue(restored.workspace.script.contains("blocking turns distance"))
        } finally {
            CreatorDataGate.nonCancellable { manager.restore(original) }
        }
    }
}
