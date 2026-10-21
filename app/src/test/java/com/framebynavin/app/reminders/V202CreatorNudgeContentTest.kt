package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorContextNudge
import com.framebynavin.app.data.CreatorContextNudgeLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V202CreatorNudgeContentTest {
    private fun nudge(key: String, title: String = "New trailer edit", level: CreatorContextNudgeLevel = CreatorContextNudgeLevel.NOW) =
        CreatorContextNudge(
            key = key,
            taskId = "real-task-1",
            title = title,
            level = level,
            message = "Due within 24h · 40% complete.",
            action = "Finish the next edit.",
            dueAtMillis = 1_800_000_000_000L,
        )

    @Test fun approachingDeadlineUsesRealWorkflowMessageAndAction() {
        val rendered = V202CreatorNudgeContent.forNudge(nudge("risk-24h:real-task-1:edit", level = CreatorContextNudgeLevel.SOON))
        assertTrue(rendered.title.contains("New trailer edit"))
        assertTrue(rendered.body.contains("40% complete"))
        assertTrue(rendered.body.contains("Finish the next edit"))
    }

    @Test fun passedPublishTimeDoesNotPretendPublicationOccurred() {
        val rendered = V202CreatorNudgeContent.forNudge(nudge("overdue:real-task-1:edit"))
        assertTrue(rendered.body.contains("passed its planned publish time"))
        assertFalse(rendered.body.contains("already published"))
    }

    @Test fun actualMissedCheckInHasDistinctWording() {
        val rendered = V202CreatorNudgeContent.forNudge(nudge("missed-reminder:real-task-1:edit"))
        assertTrue(rendered.body.contains("Your check-in for New trailer edit passed"))
        assertTrue(rendered.body.contains("still active"))
    }

    @Test fun priorityReminderMentionsProjectNotInventedGrowthClaims() {
        val rendered = V202CreatorNudgeContent.forNudge(nudge("critical-48h:real-task-1:edit"))
        assertTrue(rendered.title.contains("Priority check-in"))
        assertTrue(rendered.body.contains("New trailer edit"))
        assertFalse(rendered.body.contains("views"))
    }

    @Test fun missingOrLongProjectTitleStaysLegible() {
        val blank = V202CreatorNudgeContent.forNudge(nudge("risk-6h:x", title = "  "))
        assertTrue(blank.title.contains("Your project"))
        val long = V202CreatorNudgeContent.forNudge(nudge("risk-6h:x", title = "A".repeat(150)))
        assertTrue(long.title.length < 100)
        assertTrue(long.title.endsWith("…"))
    }

    @Test fun unknownKeyUsesOnlyOriginalGroundedContent() {
        val rendered = V202CreatorNudgeContent.forNudge(nudge("unexpected:x"))
        assertEquals("Backlot · New trailer edit", rendered.title)
        assertTrue(rendered.body.startsWith("Due within 24h"))
    }
}
