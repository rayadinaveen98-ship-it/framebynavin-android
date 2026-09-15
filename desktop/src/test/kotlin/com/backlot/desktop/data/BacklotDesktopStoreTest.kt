package com.backlot.desktop.data

import com.backlot.desktop.model.CreatorProject
import com.backlot.desktop.model.DesktopSnapshot
import com.backlot.desktop.model.Idea
import com.backlot.desktop.model.ProjectStage
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BacklotDesktopStoreTest {
    @Test
    fun roundTripsCreatorWorkspace() {
        val root = Files.createTempDirectory("backlot-desktop-test")
        val store = BacklotDesktopStore(root)
        val snapshot = DesktopSnapshot(
            ideas = listOf(Idea(id = "idea-1", title = "Opening hook", note = "Start with the question", createdAt = "2026-09-15T00:00:00Z")),
            projects = listOf(
                CreatorProject(
                    id = "project-1",
                    title = "Director breakdown",
                    stage = ProjectStage.EDIT,
                    dueDate = "2026-09-18",
                    note = "Tighten first minute",
                    updatedAt = "2026-09-15T01:00:00Z",
                )
            ),
        )

        store.save(snapshot)
        val restored = store.load()

        assertTrue(Files.exists(store.dataPath()))
        assertEquals(snapshot, restored)
    }
}
