package com.framebynavin.app.youtube

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeMilestonePolicyTest {
    private val hour = 3_600_000L
    private val published = 1_000_000L

    @Test
    fun `nothing is due before 24 hours`() {
        assertEquals(emptyList<Int>(), YouTubeMilestonePolicy.dueMilestones(published, published + 23 * hour, emptySet()))
    }

    @Test
    fun `24 hour milestone becomes due at boundary`() {
        assertEquals(listOf(24), YouTubeMilestonePolicy.dueMilestones(published, published + 24 * hour, emptySet()))
    }

    @Test
    fun `late sync captures every missing milestone reached`() {
        assertEquals(listOf(24, 168, 672), YouTubeMilestonePolicy.dueMilestones(published, published + 700 * hour, emptySet()))
    }

    @Test
    fun `already captured checkpoints are never duplicated`() {
        assertEquals(listOf(168), YouTubeMilestonePolicy.dueMilestones(published, published + 200 * hour, setOf(24)))
    }
}
