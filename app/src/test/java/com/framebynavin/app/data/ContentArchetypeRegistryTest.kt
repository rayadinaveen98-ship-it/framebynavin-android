package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentArchetypeRegistryTest {
    @Test fun everyCreatorModeSuggestionUsesACanonicalArchetype() {
        assertTrue(ContentArchetypeRegistry.validateModeRegistry().isEmpty())
    }

    @Test fun sameCanonicalReviewGetsModeSpecificLanguage() {
        assertEquals("Movie Review", ContentArchetypeRegistry.labelForMode("review", "Film & Entertainment"))
        assertEquals("Game Review", ContentArchetypeRegistry.labelForMode("review", "Gaming"))
        assertEquals("Product Review", ContentArchetypeRegistry.labelForMode("review", "Tech"))
        assertEquals("review", ContentArchetypeRegistry.definition("Movie Review")?.id)
    }

    @Test fun platformFormatsAreNotPretendedToBeArchetypes() {
        assertEquals(null, ContentArchetypeRegistry.definition("YouTube Long-form"))
        assertEquals(null, ContentArchetypeRegistry.definition("Instagram Reel"))
    }

    @Test fun filmAndGamingSuggestionsRemainDistinctButShareCanonicalIds() {
        val film = ContentArchetypeRegistry.suggestedForMode("Film & Entertainment").map { it.id }.toSet()
        val gaming = ContentArchetypeRegistry.suggestedForMode("Gaming").map { it.id }.toSet()
        assertTrue("review" in film && "review" in gaming)
        assertTrue("gameplay" in gaming)
        assertTrue("gameplay" !in film)
    }
}
