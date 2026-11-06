package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V308SparkActivityPolicyTest {
    private fun item(
        id: String,
        domain: V306ActivityDomain = V306ActivityDomain.CREATOR,
        bucket: V306ActivityBucket = V306ActivityBucket.NEEDS_ACTION,
    ) = V306ActivityItem(
        id = id, domain = domain, bucket = bucket,
        title = "Sensitive user-chosen title",
        detail = "Posted to Android; not seen",
        atMillis = 100L, targetId = id,
    )

    @Test fun noEvidenceProducesNoSparkNotifications() {
        assertTrue(V308SparkActivityPolicy.top(emptyList()).isEmpty())
    }

    @Test fun priorityActionsAreLimitedToThree() {
        val feed = (1..5).map { item("current-$it") } +
            item("old", bucket = V306ActivityBucket.HISTORY)
        val shown = V308SparkActivityPolicy.top(feed)
        assertEquals(3, shown.size)
        assertTrue(shown.all { it.bucket == V306ActivityBucket.NEEDS_ACTION })
        assertEquals(listOf("current-1", "current-2", "current-3"), shown.map { it.id })
    }

    @Test fun receiptsOnlyFallbackUsesTwoActualHistoryRecords() {
        val feed = (1..5).map { item("history-$it", bucket = V306ActivityBucket.HISTORY) }
        assertEquals(2, V308SparkActivityPolicy.top(feed).size)
        assertEquals("history-1", V308SparkActivityPolicy.top(feed).first().id)
    }

    @Test fun oneOccurrenceNeverAppearsTwice() {
        val current = item("creator-current")
        assertEquals(1, V308SparkActivityPolicy.top(listOf(current, current)).size)
    }

    @Test fun personalActivityPreviewNeverExposesTitlesOrDeliveryDetails() {
        val personal = item("private", V306ActivityDomain.PERSONAL)
        val done = item("done", V306ActivityDomain.PERSONAL, V306ActivityBucket.HISTORY)
        assertEquals("Personal reminder needs attention", V308SparkActivityPolicy.displayedTitle(personal))
        assertEquals("Personal activity confirmed", V308SparkActivityPolicy.displayedTitle(done))
        assertFalse(V308SparkActivityPolicy.displayedTitle(personal).contains("Sensitive"))
        assertFalse(V308SparkActivityPolicy.displayedDetail(personal).contains("Posted"))
        assertFalse(V308SparkActivityPolicy.displayedDetail(done).contains("Posted"))
    }

    @Test fun creatorActivityKeepsUsefulEvidenceBackedTitle() {
        val creator = item("film-review")
        assertEquals("Sensitive user-chosen title", V308SparkActivityPolicy.displayedTitle(creator))
        assertEquals("Posted to Android; not seen", V308SparkActivityPolicy.displayedDetail(creator))
    }
}
