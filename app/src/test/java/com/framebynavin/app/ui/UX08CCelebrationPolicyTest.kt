package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorRewardEngine
import com.framebynavin.app.data.CreatorRewardEventType
import com.framebynavin.app.data.CreatorRewardLedgerEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-only tests. NOT EXECUTED until the approved consolidated UX01–UX10
 * Gradle + Android + physical-device acceptance campaign.
 */
class UX08CCelebrationPolicyTest {
    private val now = 1_800_000_000_000L

    @Test
    fun `existing authentic credited Creator event types qualify within the live window`() {
        val events = listOf(
            CreatorRewardEngine.ideaDailyCapture("idea1", now),
            CreatorRewardEngine.ideaConverted("idea1", "project1", now),
            CreatorRewardEngine.stageCompleted("project1", "STORY", "Story", now),
            CreatorRewardEngine.projectPublished("project1", "My short film", now),
            CreatorRewardLedgerEntry(
                eventKey = "post-publish:checkpoint1",
                type = CreatorRewardEventType.POST_PUBLISH_COMPLETED,
                subjectId = "checkpoint1",
                projectId = "project1",
                label = "Performance reviewed",
                xp = 10,
                momentum = 10,
                occurredAtMillis = now,
            ),
        )
        assertTrue(events.all { UX08CCelebrationPolicy.verifiedFeedback(it, now) })
    }

    @Test
    fun `forged mismatched keys or empty owners do not produce a celebration`() {
        val original = CreatorRewardEngine.stageCompleted(
            "project1", "CAMERA", "Camera", now,
        )
        assertFalse(
            UX08CCelebrationPolicy.verifiedFeedback(
                original.copy(eventKey = "stage:other:CAMERA"), now,
            )
        )
        assertFalse(
            UX08CCelebrationPolicy.verifiedFeedback(
                original.copy(projectId = ""), now,
            )
        )
        assertFalse(
            UX08CCelebrationPolicy.verifiedFeedback(
                original.copy(xp = 0), now,
            )
        )
        assertFalse(UX08CCelebrationPolicy.verifiedFeedback(null, now))
    }

    @Test
    fun `old imported-history timestamps and future timestamps cannot replay`() {
        val valid = CreatorRewardEngine.projectPublished("project1", "Title", now)
        assertFalse(
            UX08CCelebrationPolicy.verifiedFeedback(
                valid.copy(occurredAtMillis = now -
                    UX08CCelebrationPolicy.MAX_EVENT_AGE_MS - 1), now,
            )
        )
        assertFalse(
            UX08CCelebrationPolicy.verifiedFeedback(
                valid.copy(occurredAtMillis = now + 1), now,
            )
        )
        assertTrue(
            UX08CCelebrationPolicy.verifiedFeedback(
                valid.copy(occurredAtMillis = now -
                    UX08CCelebrationPolicy.MAX_EVENT_AGE_MS), now,
            )
        )
    }

    @Test
    fun `seen keys remain idempotent and bounded across screen recreation`() {
        var seen = emptyList<String>()
        seen = UX08CCelebrationPolicy.addSeenKey(seen, "first")
        assertEquals(listOf("first"), seen)
        seen = UX08CCelebrationPolicy.addSeenKey(seen, "first")
        assertEquals(listOf("first"), seen)
        (1..40).forEach { seen = UX08CCelebrationPolicy.addSeenKey(seen, "key$it") }
        assertEquals(UX08CCelebrationPolicy.MAX_RECENT_KEYS, seen.size)
        assertFalse("first" in seen)
        assertEquals("key40", seen.last())
    }

    @Test
    fun `visible duration includes fade and queue waits beyond it`() {
        assertEquals(
            3_000L,
            UX08CCelebrationPolicy.VISIBLE_HOLD_MS +
                UX08CCelebrationPolicy.EXIT_MS,
        )
        assertTrue(
            UX08CCelebrationPolicy.FEEDBACK_QUEUE_SLOT_MS >
                UX08CCelebrationPolicy.VISIBLE_HOLD_MS +
                    UX08CCelebrationPolicy.EXIT_MS,
        )
    }
}
