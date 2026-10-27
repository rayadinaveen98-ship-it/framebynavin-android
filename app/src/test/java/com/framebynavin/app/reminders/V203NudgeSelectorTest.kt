package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorContextNudge
import com.framebynavin.app.data.CreatorContextNudgeLevel
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V203NudgeSelectorTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(day: Int, hour: Int) =
        LocalDateTime.of(2026, 10, day, hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun task(key: String, level: CreatorContextNudgeLevel = CreatorContextNudgeLevel.NOW, id: String = key) =
        CreatorContextNudge(key, id, "Project $key", level, "A real task is due", "Open project", at(24, 18))

    private fun choose(
        choices: List<CreatorContextNudge>,
        now: Long = at(21, 12),
        globalAt: Long = 0L,
        globalKey: String = "",
        perKey: Map<String, Long> = emptyMap(),
        quietEnabled: Boolean = false,
        quietStart: Int = 22 * 60,
        quietEnd: Int = 8 * 60,
    ) = V203NudgeSelector.choose(
        candidates = choices,
        nowMillis = now,
        lastGlobalShownAtMillis = globalAt,
        lastGlobalKey = globalKey,
        lastShownAtForKey = { perKey[it] ?: 0L },
        quietHoursEnabled = quietEnabled,
        quietStartMinute = quietStart,
        quietEndMinute = quietEnd,
        zone = zone,
    )

    @Test fun rotationCannotBypass72HourSameProjectLimit() {
        // A was posted 48h ago, then B was posted 24h ago.
        // V201's last-key-only tracking mistakenly allowed A immediately.
        val winner = choose(
            listOf(task("A"), task("C")),
            globalAt = at(20, 12),
            globalKey = "B",
            perKey = mapOf("A" to at(19, 12), "B" to at(20, 12)),
        )
        assertEquals("C", winner?.key)
    }

    @Test fun respect24HourGlobalLimitWithUnrelatedCandidate() {
        assertNull(choose(listOf(task("C")), globalAt = at(20, 13), globalKey = "B"))
        assertEquals("C", choose(listOf(task("C")), globalAt = at(20, 12), globalKey = "B")?.key)
    }

    @Test fun preserveRankingWhileSkippingSuppressedOrReadyProjects() {
        val winner = choose(
            listOf(task("ready", CreatorContextNudgeLevel.READY), task("A"), task("B")),
            globalAt = at(20, 12),
            globalKey = "C",
            perKey = mapOf("A" to at(20, 9)),
        )
        assertEquals("B", winner?.key)
    }

    @Test fun doNotSendOutsideDaytimeOrCustomQuietHours() {
        assertNull(choose(listOf(task("a")), now = at(21, 20)))
        assertNull(choose(listOf(task("a")), quietEnabled = true, quietStart = 11 * 60, quietEnd = 13 * 60))
        assertEquals("a", choose(listOf(task("a")), quietEnabled = true, quietStart = 13 * 60, quietEnd = 15 * 60)?.key)
    }

    @Test fun clockRollbackFailsClosedAlsoForPerKeyHistory() {
        assertNull(choose(listOf(task("a")), perKey = mapOf("a" to at(22, 12))))
        assertNull(choose(listOf(task("a")), globalAt = at(22, 12), globalKey = "b"))
    }

    @Test fun staleOrIncompleteCandidatesAreNotNudged() {
        assertNull(choose(listOf(task(" ", id = "real-id"), task("valid", id = " "))))
        assertNull(choose(listOf(task("done", level = CreatorContextNudgeLevel.READY))))
    }

    @Test fun storageKeysHideIdentifiersAndAreStableAcrossRechecks() {
        val k = V203NudgeHistory.storageKey("risk-24h:real-project-id:edit")
        assertTrue(k.startsWith("key_v203_"))
        assertTrue(k.length == "key_v203_".length + 64)
        assertTrue(!k.contains("real-project-id"))
        assertEquals(k, V203NudgeHistory.storageKey("risk-24h:real-project-id:edit"))
        assertNotEquals(k, V203NudgeHistory.storageKey("risk-24h:other:edit"))
    }
}
