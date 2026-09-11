package com.framebynavin.app.data

/** A mode-specific label for a canonical content archetype. */
data class CreatorModeArchetypeSuggestion(
    val archetypeId: String,
    val label: String,
)

data class CreatorModeVocabulary(
    val subjectLabel: String,
    val sourceLabel: String,
    val captureLabel: String,
)

data class CreatorModeDefinition(
    val id: String,
    val label: String,
    val description: String,
    val aliases: Set<String> = emptySet(),
    val suggestedArchetypes: List<CreatorModeArchetypeSuggestion>,
    val suggestedProductionStyles: List<String>,
    val vocabulary: CreatorModeVocabulary,
)

/**
 * Canonical creator-mode taxonomy.
 *
 * Modes are recommendation bundles, never permission gates. A Gaming creator can still create a
 * film essay; a Film creator can still create a podcast. Project-level Content DNA will always
 * have final authority over these account defaults.
 */
object CreatorModeRegistry {
    val definitions: List<CreatorModeDefinition> = listOf(
        CreatorModeDefinition(
            id = "film_entertainment",
            label = "Film & Entertainment",
            description = "Reviews, analysis, cinematic moments, recommendations and entertainment commentary.",
            suggestedArchetypes = listOf(
                suggestion("review", "Movie Review"),
                suggestion("analysis", "Cinematic Analysis"),
                suggestion("highlights", "Cinematic Moments"),
                suggestion("recommendation", "Movie Recommendation"),
                suggestion("explainer", "Trailer / Teaser Breakdown"),
                suggestion("tutorial", "Filmmaking / Craft Breakdown"),
                suggestion("news_update", "Entertainment News / Reaction"),
                suggestion("essay", "Video Essay"),
            ),
            suggestedProductionStyles = listOf("Voiceover", "Camera / B-roll", "Talking Head", "Animation / Motion"),
            vocabulary = CreatorModeVocabulary("film / series", "scene / source", "footage / frames"),
        ),
        CreatorModeDefinition(
            id = "gaming",
            label = "Gaming",
            description = "Gameplay, reviews, guides, walkthroughs, streams, highlights and game analysis.",
            suggestedArchetypes = listOf(
                suggestion("review", "Game Review"),
                suggestion("gameplay", "Gameplay Video"),
                suggestion("walkthrough", "Walkthrough"),
                suggestion("tutorial", "Guide / Tutorial"),
                suggestion("challenge", "Challenge"),
                suggestion("highlights", "Highlights"),
                suggestion("live_stream", "Livestream"),
                suggestion("news_update", "Gaming News / Update"),
                suggestion("analysis", "Lore / Story Analysis"),
            ),
            suggestedProductionStyles = listOf("Gameplay Capture", "Livestream", "Voiceover", "Talking Head"),
            vocabulary = CreatorModeVocabulary("game", "session / patch", "gameplay"),
        ),
        CreatorModeDefinition(
            id = "education",
            label = "Education",
            description = "Lessons, tutorials, explainers, study content and structured learning material.",
            suggestedArchetypes = listOf(
                suggestion("tutorial", "Tutorial"),
                suggestion("lesson", "Lesson"),
                suggestion("explainer", "Explainer"),
                suggestion("course_module", "Course Module"),
                suggestion("study_guide", "Study Guide"),
                suggestion("short_tip", "Short Tip"),
            ),
            suggestedProductionStyles = listOf("Screen Recording", "Talking Head", "Voiceover", "Writing"),
            vocabulary = CreatorModeVocabulary("topic", "reference / example", "demo / lesson"),
        ),
        CreatorModeDefinition(
            id = "tech",
            label = "Tech",
            description = "Product reviews, comparisons, demos, tutorials, benchmarks and technology updates.",
            suggestedArchetypes = listOf(
                suggestion("review", "Product Review"),
                suggestion("comparison", "Comparison"),
                suggestion("tutorial", "Tutorial"),
                suggestion("demo", "Demo"),
                suggestion("benchmark", "Benchmark"),
                suggestion("news_update", "Tech News"),
                suggestion("explainer", "Tech Explainer"),
            ),
            suggestedProductionStyles = listOf("Screen Recording", "Camera / B-roll", "Talking Head", "Voiceover"),
            vocabulary = CreatorModeVocabulary("product / technology", "spec / source", "demo / B-roll"),
        ),
        CreatorModeDefinition(
            id = "lifestyle",
            label = "Lifestyle",
            description = "Vlogs, routines, stories, challenges, recommendations and personal experiences.",
            suggestedArchetypes = listOf(
                suggestion("vlog", "Vlog"),
                suggestion("routine", "Routine"),
                suggestion("story", "Personal Story"),
                suggestion("recommendation", "Recommendation"),
                suggestion("challenge", "Challenge"),
            ),
            suggestedProductionStyles = listOf("Camera / B-roll", "Talking Head", "Voiceover", "Mixed / Hybrid"),
            vocabulary = CreatorModeVocabulary("experience", "reference / plan", "footage"),
        ),
        CreatorModeDefinition(
            id = "business_career",
            label = "Business & Career",
            description = "Case studies, professional education, career advice, analysis and business content.",
            aliases = setOf("Business"),
            suggestedArchetypes = listOf(
                suggestion("case_study", "Case Study"),
                suggestion("advice", "Advice"),
                suggestion("analysis", "Business Analysis"),
                suggestion("explainer", "Educational Post"),
                suggestion("newsletter", "Newsletter"),
                suggestion("opinion", "Professional Opinion"),
            ),
            suggestedProductionStyles = listOf("Talking Head", "Writing", "Screen Recording", "Voiceover"),
            vocabulary = CreatorModeVocabulary("business / career topic", "case / evidence", "example / presentation"),
        ),
        CreatorModeDefinition(
            id = "music_audio",
            label = "Music & Audio",
            description = "Original music, covers, performances, production breakdowns, tutorials and podcasts.",
            aliases = setOf("Music"),
            suggestedArchetypes = listOf(
                suggestion("original_work", "Original Song"),
                suggestion("cover", "Cover"),
                suggestion("performance", "Performance"),
                suggestion("analysis", "Production Breakdown"),
                suggestion("tutorial", "Music Tutorial"),
                suggestion("podcast", "Podcast"),
            ),
            suggestedProductionStyles = listOf("Audio-only", "Camera / B-roll", "Screen Recording", "Livestream"),
            vocabulary = CreatorModeVocabulary("track / episode", "reference / take", "audio / performance"),
        ),
        CreatorModeDefinition(
            id = "art_design",
            label = "Art & Design",
            description = "Artwork, design process, tutorials, showcases, portfolios and creative breakdowns.",
            suggestedArchetypes = listOf(
                suggestion("original_work", "Artwork / Design"),
                suggestion("process", "Process Video"),
                suggestion("speed_process", "Speedpaint / Timelapse"),
                suggestion("tutorial", "Tutorial"),
                suggestion("analysis", "Design Breakdown"),
                suggestion("showcase", "Portfolio / Showcase"),
            ),
            suggestedProductionStyles = listOf("Screen Recording", "Camera / B-roll", "Voiceover", "Animation / Motion"),
            vocabulary = CreatorModeVocabulary("piece / design", "reference", "process / canvas"),
        ),
        CreatorModeDefinition(
            id = "news_commentary",
            label = "News & Commentary",
            description = "Verified updates, explainers, reactions, deep dives, opinion and follow-up coverage.",
            suggestedArchetypes = listOf(
                suggestion("news_update", "Breaking Update"),
                suggestion("explainer", "Explainer"),
                suggestion("reaction", "Reaction"),
                suggestion("deep_dive", "Deep Dive"),
                suggestion("opinion", "Opinion"),
                suggestion("follow_up", "Follow-up"),
            ),
            suggestedProductionStyles = listOf("Talking Head", "Voiceover", "Screen Recording", "Writing"),
            vocabulary = CreatorModeVocabulary("story", "source / evidence", "clip / statement"),
        ),
        CreatorModeDefinition(
            id = "food",
            label = "Food",
            description = "Recipes, reviews, tutorials, food stories and restaurant experiences.",
            suggestedArchetypes = listOf(
                suggestion("recipe", "Recipe"),
                suggestion("review", "Food / Restaurant Review"),
                suggestion("tutorial", "Cooking Tutorial"),
                suggestion("story", "Food Story"),
                suggestion("recommendation", "Recommendation"),
            ),
            suggestedProductionStyles = listOf("Camera / B-roll", "Voiceover", "Talking Head", "Mixed / Hybrid"),
            vocabulary = CreatorModeVocabulary("dish / place", "recipe / source", "food footage"),
        ),
        CreatorModeDefinition(
            id = "travel_outdoors",
            label = "Travel & Outdoors",
            description = "Travel vlogs, guides, experiences, recommendations and itineraries.",
            suggestedArchetypes = listOf(
                suggestion("vlog", "Travel Vlog"),
                suggestion("guide", "Travel Guide"),
                suggestion("experience", "Experience"),
                suggestion("recommendation", "Recommendation"),
                suggestion("itinerary", "Itinerary"),
            ),
            suggestedProductionStyles = listOf("Camera / B-roll", "Voiceover", "Talking Head", "Mixed / Hybrid"),
            vocabulary = CreatorModeVocabulary("place / trip", "plan / source", "travel footage"),
        ),
        CreatorModeDefinition(
            id = "health_fitness",
            label = "Health & Fitness",
            description = "Workouts, routines, educational guides, progress stories and fitness content.",
            suggestedArchetypes = listOf(
                suggestion("workout", "Workout"),
                suggestion("guide", "Guide"),
                suggestion("routine", "Routine"),
                suggestion("education", "Educational Content"),
                suggestion("story", "Progress Story"),
            ),
            suggestedProductionStyles = listOf("Camera / B-roll", "Talking Head", "Voiceover", "Mixed / Hybrid"),
            vocabulary = CreatorModeVocabulary("routine / topic", "reference / plan", "demonstration"),
        ),
        CreatorModeDefinition(
            id = "other_hybrid",
            label = "Other / Hybrid",
            description = "Flexible creator mode for mixed work that does not fit a single niche.",
            aliases = setOf("Other"),
            suggestedArchetypes = listOf(
                suggestion("video", "Video"),
                suggestion("short", "Short-form"),
                suggestion("post", "Post"),
                suggestion("audio", "Audio"),
                suggestion("article", "Written Content"),
                suggestion("live_stream", "Livestream"),
            ),
            suggestedProductionStyles = listOf("Mixed / Hybrid", "Voiceover", "Talking Head", "Writing"),
            vocabulary = CreatorModeVocabulary("topic", "source", "content"),
        ),
    )

    val labels: List<String> get() = definitions.map { it.label }

    fun definition(value: String): CreatorModeDefinition {
        val normalized = value.trim()
        return definitions.firstOrNull { mode ->
            mode.id.equals(normalized, ignoreCase = true) ||
                mode.label.equals(normalized, ignoreCase = true) ||
                mode.aliases.any { it.equals(normalized, ignoreCase = true) }
        } ?: definitions.last()
    }

    fun canonicalLabel(value: String): String =
        if (value.isBlank()) "" else definition(value).label

    fun suggestedArchetypes(value: String): List<CreatorModeArchetypeSuggestion> =
        definition(value).suggestedArchetypes

    fun suggestedProductionStyles(value: String): List<String> =
        definition(value).suggestedProductionStyles

    private fun suggestion(id: String, label: String) = CreatorModeArchetypeSuggestion(id, label)
}
