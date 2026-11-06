package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spark dismissal is only local presentation, never a reminder acknowledgement. */
class V3081SparkDismissalPolicyTest {
    private fun item(id: String, bucket: V306ActivityBucket = V306ActivityBucket.NEEDS_ACTION) =
        V306ActivityItem(
            id = id, domain = V306ActivityDomain.PERSONAL, bucket = bucket,
            title = "Private diary", detail = "Presented by Android",
            atMillis = 500L, targetId = "personal-id",
        )

    @Test fun dismissalKeyIsStableHashedAndDoesNotExposeReminderId() {
        val first = item("personal:action:super-private-skin-care-id:170")
        val key = V308SparkActivityPolicy.dismissalId(first)
        assertEquals(key, V308SparkActivityPolicy.dismissalId(first))
        assertTrue(key.startsWith("spark-activity:"))
        assertEquals("spark-activity:".length + 64, key.length)
        assertFalse(key.contains("skin-care"))
        assertFalse(key.contains("super-private"))
    }

    @Test fun differentOccurrencesHaveDifferentDismissalIds() {
        assertNotEquals(
            V308SparkActivityPolicy.dismissalId(item("personal:action:same:100")),
            V308SparkActivityPolicy.dismissalId(item("personal:action:same:200")),
        )
    }

    @Test fun singleDismissalHidesOnlyOneSparkItemAndKeepsOtherActions() {
        val items = (1..4).map { item("action-$it") }
        val dismissed = setOf(V308SparkActivityPolicy.dismissalId(items.first()))
        val visible = V308SparkActivityPolicy.top(items, dismissed)
        assertEquals(listOf("action-2", "action-3", "action-4"), visible.map { it.id })
        // The canonical activity list is unaffected and retains all current actions.
        assertEquals(4, items.size)
        assertTrue(items.any { it.id == "action-1" })
    }

    @Test fun dismissedActiveItemsNeverBecomeFakeHistoryCompletions() {
        val active = item("unresolved")
        val history = item("prior", V306ActivityBucket.HISTORY)
        val visible = V308SparkActivityPolicy.top(
            listOf(active, history),
            setOf(V308SparkActivityPolicy.dismissalId(active)),
        )
        assertEquals(1, visible.size)
        assertEquals(V306ActivityBucket.HISTORY, visible.single().bucket)
        assertFalse(visible.any { it.id == active.id })
    }

    @Test fun noActiveAndNoHistoryIsEmptyWithoutSyntheticFallbacks() {
        assertTrue(V308SparkActivityPolicy.top(emptyList(), setOf("anything")).isEmpty())
    }

    @Test fun dismissalDoesNotAlterUnderlyingActivityCenterFeed() {
        val items = listOf(item("creator:action:1"), item("personal:action:1"))
        val ignored = setOf(V308SparkActivityPolicy.dismissalId(items[0]))
        assertEquals(1, V308SparkActivityPolicy.top(items, ignored).size)
        assertEquals(2, V306ActivityCenterPolicy.filter(items, V306ActivityFilter.ALL).size)
        assertEquals(2, V306ActivityCenterPolicy.filter(items, V306ActivityFilter.NEEDS_ACTION).size)
    }
}
