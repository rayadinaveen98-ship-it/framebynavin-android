package com.framebynavin.app.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudRecoveryPolicyTest {
    private fun point(id: String, time: Long, projects: Int = 0, ideas: Int = 0, weekly: Int = 0) =
        CloudRestorePoint(id, "daily", time, "2026-09-06", "test", projects, ideas, weekly, 0)

    @Test
    fun newerEmptySnapshot_neverBeatsOlderCreatorWork() {
        val emptyNewest = point("empty", 300, projects = 0)
        val useful = point("useful", 200, projects = 4, ideas = 2)
        assertEquals("useful", CloudRecoveryPolicy.recommended(listOf(emptyNewest, useful))?.id)
    }

    @Test
    fun newestMeaningfulSnapshot_winsAmongUsefulCopies() {
        val old = point("old", 100, projects = 1)
        val newer = point("newer", 200, ideas = 1)
        assertEquals("newer", CloudRecoveryPolicy.recommended(listOf(old, newer))?.id)
    }

    @Test
    fun allEmptySnapshots_stillReturnNewestWithoutInventingWork() {
        val old = point("old", 100)
        val newer = point("newer", 200)
        assertFalse(CloudRecoveryPolicy.hasCreatorWork(newer))
        assertEquals("newer", CloudRecoveryPolicy.recommended(listOf(old, newer))?.id)
        assertTrue(CloudRecoveryPolicy.recommended(emptyList()) == null)
    }
}
