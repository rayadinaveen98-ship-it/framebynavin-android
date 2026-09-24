package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorMediaSourceRecommendationTest {
    @Test
    fun `collapsed onboarding shows only top five recommended sources`() {
        val selection = CreatorMediaSourceRecommendationEngine.build(
            catalog = (1..12).map { index ->
                source("telugu-$index", "Telugu Source $index", setOf("Telugu"), CreatorMediaSourceAuthority.OFFICIAL, 13 - index)
            },
            preferredLanguages = setOf("Telugu"),
        )

        assertEquals(5, selection.recommended.size)
        assertEquals(5, selection.visible(expanded = false).size)
        assertEquals(12, selection.visible(expanded = true).size)
        assertTrue(selection.hasMore)
    }

    @Test
    fun `selected languages filter unrelated language-specific sources`() {
        val selection = CreatorMediaSourceRecommendationEngine.build(
            catalog = listOf(
                source("telugu", "Telugu Official", setOf("Telugu"), CreatorMediaSourceAuthority.OFFICIAL),
                source("tamil", "Tamil Official", setOf("Tamil"), CreatorMediaSourceAuthority.OFFICIAL),
                source("india", "India Trade", emptySet(), CreatorMediaSourceAuthority.TRADE),
            ),
            preferredLanguages = setOf("telugu"),
        )

        assertEquals(listOf("telugu", "india"), selection.allEligible.map { it.id })
        assertFalse(selection.allEligible.any { it.id == "tamil" })
    }

    @Test
    fun `official same-language source outranks cross-language trade source`() {
        val selection = CreatorMediaSourceRecommendationEngine.build(
            catalog = listOf(
                source("trade", "Trade", emptySet(), CreatorMediaSourceAuthority.TRADE, 20),
                source("official", "Official", setOf("Tamil"), CreatorMediaSourceAuthority.OFFICIAL),
            ),
            preferredLanguages = setOf("Tamil"),
        )

        assertEquals("official", selection.recommended.first().id)
    }

    @Test
    fun `inactive and duplicate source ids are never surfaced`() {
        val selection = CreatorMediaSourceRecommendationEngine.build(
            catalog = listOf(
                source("one", "First", setOf("Telugu"), CreatorMediaSourceAuthority.OFFICIAL),
                source("ONE", "Duplicate", setOf("Telugu"), CreatorMediaSourceAuthority.OFFICIAL),
                source("off", "Inactive", setOf("Telugu"), CreatorMediaSourceAuthority.OFFICIAL, active = false),
            ),
            preferredLanguages = setOf("Telugu"),
        )

        assertEquals(1, selection.allEligible.size)
        assertEquals("one", selection.allEligible.single().id)
        assertFalse(selection.hasMore)
    }

    private fun source(
        id: String,
        label: String,
        languages: Set<String>,
        authority: CreatorMediaSourceAuthority,
        weight: Int = 0,
        active: Boolean = true,
    ) = CreatorMediaSource(
        id = id,
        label = label,
        languages = languages,
        authority = authority,
        recommendationWeight = weight,
        active = active,
    )
}
