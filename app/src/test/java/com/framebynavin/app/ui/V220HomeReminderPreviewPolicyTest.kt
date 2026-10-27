package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V220HomeReminderPreviewPolicyTest {
    private val now = 1_700_000_000_000L

    private fun task(
        id: String,
        at: Long,
        status: TaskStatus = TaskStatus.PLANNED,
        enabled: Boolean = true,
        mode: ReminderMode = ReminderMode.ALARM,
    ) = CreatorTask(
        id = id, title = "Film $id", platform = "YouTube",
        contentType = "Video", dueLabel = "Today",
        status = status, reminderEnabled = enabled,
        reminderMode = mode, reminderAtMillis = at,
    )

    @Test fun nearestUpcomingIsSelectedFromRealEnabledActiveReminders() {
        val tasks = listOf(task("later", now + 80_000), task("soon", now + 1_000),
            task("off", now + 100, enabled = false),
            task("done", now + 200, status = TaskStatus.DONE),
            task("none", now + 300, mode = ReminderMode.NONE))
        val item = V220HomeReminderPreviewPolicy.pick(tasks, now)!!
        assertEquals("soon", item.task.id)
        assertFalse(item.remindingNow)
    }

    @Test fun anActiveSessionTakesPriorityOverUpcomingButNotAnArchivedProject() {
        val tasks = listOf(task("future", now + 1_000),
            task("live", now - 60_000, status = TaskStatus.WORKING),
            task("completed", now - 20_000, status = TaskStatus.DONE))
        val item = V220HomeReminderPreviewPolicy.pick(
            tasks, now, setOf("live", "completed"),
        )!!
        assertEquals("live", item.task.id)
        assertTrue(item.remindingNow)
    }

    @Test fun pastStaleAlertsAreNeverShownAsNextAndEmptyIsHonest() {
        assertNull(V220HomeReminderPreviewPolicy.pick(
            listOf(task("expired", now - 60_000),
                task("disabled", now + 1_000, enabled = false)), now,
        ))
        assertNull(V220HomeReminderPreviewPolicy.pick(emptyList(), now))
    }

    @Test fun tiesAreDeterministicAndExactNowIsValid() {
        val result = V220HomeReminderPreviewPolicy.pick(
            listOf(task("z", now), task("a", now)), now,
        )!!
        assertEquals("a", result.task.id)
    }

    @Test fun aStaleSessionIdCannotInventAnAlertForAnUnrelatedTask() {
        val result = V220HomeReminderPreviewPolicy.pick(
            listOf(task("inactive", now - 10_000)), now, setOf("missing"),
        )
        assertNull(result)
    }
}
