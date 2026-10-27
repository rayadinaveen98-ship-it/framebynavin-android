package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V221CompanionCooldownPolicyTest {
    private val now = 1_700_000_000_000L
    private val hour = 3_600_000L

    private fun task(id: String, due: Long = now - hour) = CreatorTask(
        id = id, title = "Creator project $id", platform = "YouTube",
        contentType = "Film", dueLabel = "Today", dueAtMillis = due,
    )
    private fun context(tasks: List<CreatorTask>) = V221CompanionContext(tasks, now)

    @Test fun disabledModeAlwaysReturnsSilenceEvenWithUrgentWork() {
        assertNull(V221CompanionBehaviorPolicy.select(
            context(listOf(task("urgent"))), false, emptyList(),
        ))
    }

    @Test fun enabledModeUsesRealV2211Decision() {
        val value = V221CompanionBehaviorPolicy.select(
            context(listOf(task("urgent"))), true, emptyList(),
        )!!
        assertEquals("project-overdue:urgent", value.id)
    }

    @Test fun dismissalSuppressesCurrentSuggestionAndFallsBackToNextRealOne() {
        val tasks = listOf(task("first", now - 2 * hour), task("second", now - hour))
        val records = V221CompanionCooldownPolicy.dismiss(emptyList(),
            "project-overdue:first", now)
        val selected = V221CompanionBehaviorPolicy.select(context(tasks), true, records)!!
        assertEquals("project-overdue:second", selected.id)
    }

    @Test fun dismissedMessageReturnsOnlyAfterActualExpiry() {
        val rows = V221CompanionCooldownPolicy.dismiss(emptyList(), "decision-a", now)
        assertTrue("decision-a" in V221CompanionCooldownPolicy.active(rows, now))
        assertTrue("decision-a" in V221CompanionCooldownPolicy.active(
            rows, now + V221CompanionCooldownPolicy.DEFAULT_COOLDOWN_MILLIS - 1))
        assertFalse("decision-a" in V221CompanionCooldownPolicy.active(
            rows, now + V221CompanionCooldownPolicy.DEFAULT_COOLDOWN_MILLIS))
    }

    @Test fun noFutureOrInvalidClockCanLockSuggestionsForever() {
        val future = V221CompanionDismissal("a", now + hour, now + 12 * hour)
        val malformed = V221CompanionDismissal("b", now, now - 1)
        assertTrue(V221CompanionCooldownPolicy.active(listOf(future,malformed), now).isEmpty())
        assertTrue(V221CompanionCooldownPolicy.active(listOf(future), 0).isEmpty())
        val tooLong = V221CompanionDismissal("c", now, now + 36 * hour)
        assertTrue(V221CompanionCooldownPolicy.active(listOf(tooLong), now).isEmpty())
    }

    @Test fun duplicateDismissalsReplaceOlderEntryAndRemainBounded() {
        val first = V221CompanionCooldownPolicy.dismiss(emptyList(), "same", now)
        val again = V221CompanionCooldownPolicy.dismiss(first, "same", now + hour)
        assertEquals(1, again.size)
        assertEquals(now + hour, again.first().dismissedAtMillis)
        val full = (0..100).fold(emptyList<V221CompanionDismissal>()) { current, index ->
            V221CompanionCooldownPolicy.dismiss(current, "decision-$index", now)
        }
        assertEquals(80, full.size)
        assertFalse(full.any { it.decisionId == "decision-0" })
        assertEquals("decision-100", full.last().decisionId)
    }

    @Test fun maliciousOrBrokenInputsDoNotCreateSuppression() {
        assertTrue(V221CompanionCooldownPolicy.dismiss(emptyList(), "", now).isEmpty())
        assertTrue(V221CompanionCooldownPolicy.dismiss(emptyList(), "a".repeat(161), now).isEmpty())
        assertTrue(V221CompanionCooldownPolicy.dismiss(emptyList(), "ok", -1).isEmpty())
        assertTrue(V221CompanionCooldownPolicy.dismiss(emptyList(), "ok", now, 1).isEmpty())
        assertTrue(V221CompanionCooldownPolicy.dismiss(emptyList(), "ok", Long.MAX_VALUE).isEmpty())
    }

    @Test fun userAndEngineSuppressedIdsAreCombinedNotOverridden() {
        val ctx = context(listOf(task("first"), task("second"))).copy(
            suppressedDecisionIds = setOf(
                "project-overdue:first", "project-review:first",
            ))
        // The engine has both urgent AND lower-priority fallback decisions
        // for planned projects. Silence only after all four are suppressed.
        val rows = V221CompanionCooldownPolicy.dismiss(
            V221CompanionCooldownPolicy.dismiss(
                emptyList(), "project-overdue:second", now,
            ),
            "project-review:second", now,
        )
        assertNull(V221CompanionBehaviorPolicy.select(ctx, true, rows))
    }

    @Test fun suggestionsAreReproducibleAndInputIsNeverChanged() {
        val task = task("safe")
        val list = mutableListOf(task)
        val before = list.toList()
        val a = V221CompanionBehaviorPolicy.select(context(list),true,emptyList())
        val b = V221CompanionBehaviorPolicy.select(context(list),true,emptyList())
        assertEquals(a,b)
        assertEquals(before,list)
    }
}
