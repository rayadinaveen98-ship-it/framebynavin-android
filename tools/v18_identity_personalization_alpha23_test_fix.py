from pathlib import Path

path = Path("app/src/test/java/com/framebynavin/app/ui/V18CreatorJourneyAlpha12Test.kt")
text = path.read_text()
old = '''    @Test
    fun ideaTaxonomyIsCreatorNeutralAtTheUiBoundary() {
        val labels = IdeaCategory.entries.map(IdeaVaultLabels::category)
        val joined = labels.joinToString(" ").lowercase()
        assertFalse(joined.contains("cinematic"))
        assertFalse(joined.contains("frame"))
        assertFalse(joined.contains("scene"))
        assertEquals("Deep Dive", IdeaVaultLabels.category(IdeaCategory.CINEMATIC_ANALYSIS))
    }
'''
new = '''    @Test
    fun genericCreatorsDoNotReceiveFilmSpecificTaxonomy() {
        val genericCategories = IdeaVaultLabels.categoriesFor(
            com.framebynavin.app.data.CreatorProfile(category = "Education")
        )
        assertFalse(genericCategories.contains(IdeaCategory.CINEMATIC_ANALYSIS))
        assertFalse(genericCategories.contains(IdeaCategory.EVERY_CINEMATIC_MOMENT))
        assertFalse(genericCategories.contains(IdeaCategory.FRAME_OF_TODAY))
        assertFalse(genericCategories.contains(IdeaCategory.FRAME_BREAKDOWN))
        assertFalse(genericCategories.contains(IdeaCategory.WHY_THIS_SCENE_WORKS))
        assertEquals("Behind the Scenes", IdeaVaultLabels.category(IdeaCategory.BEHIND_THE_SCENES))
        assertEquals("Deep Dive", IdeaVaultLabels.category(IdeaCategory.CINEMATIC_ANALYSIS))
    }
'''
if old not in text:
    raise SystemExit("Alpha23 legacy taxonomy test target not found")
path.write_text(text.replace(old, new, 1))
print("Applied Alpha23 creator taxonomy test compatibility fix")
