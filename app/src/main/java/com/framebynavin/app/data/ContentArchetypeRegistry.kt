package com.framebynavin.app.data

enum class CreatorArchetypeIntent {
    REVIEW,
    ANALYZE,
    TEACH,
    EXPLAIN,
    REPORT,
    ENTERTAIN,
    DOCUMENT,
    GUIDE,
    PERFORM,
    CREATE,
    CONVERSE,
    PUBLISH,
}

data class CreatorContentArchetype(
    val id: String,
    val label: String,
    val intent: CreatorArchetypeIntent,
    val description: String,
    val aliases: Set<String> = emptySet(),
)

/**
 * Canonical description of WHAT a creator is making.
 *
 * Archetypes deliberately do not encode platform or production method. "Review" is one archetype;
 * Film can present it as Movie Review, Gaming as Game Review and Tech as Product Review. Platform
 * and production style remain separate Content DNA dimensions.
 */
object ContentArchetypeRegistry {
    val definitions: List<CreatorContentArchetype> = listOf(
        archetype("review", "Review", CreatorArchetypeIntent.REVIEW, "Evaluate something and communicate a clear verdict.", "movie review", "game review", "product review"),
        archetype("analysis", "Analysis", CreatorArchetypeIntent.ANALYZE, "Break down meaning, craft, systems, evidence or causes.", "cinematic analysis", "lore analysis", "tech analysis"),
        archetype("essay", "Video Essay", CreatorArchetypeIntent.ANALYZE, "Build a sustained argument or interpretation around one central idea."),
        archetype("deep_dive", "Deep Dive", CreatorArchetypeIntent.ANALYZE, "Explore a subject with more context and depth than a standard explainer."),
        archetype("case_study", "Case Study", CreatorArchetypeIntent.ANALYZE, "Use a specific real example to explain decisions, outcomes and lessons."),
        archetype("comparison", "Comparison", CreatorArchetypeIntent.REVIEW, "Compare two or more options against useful criteria."),
        archetype("benchmark", "Benchmark", CreatorArchetypeIntent.REVIEW, "Test performance against a repeatable set of measures."),

        archetype("tutorial", "Tutorial", CreatorArchetypeIntent.TEACH, "Teach the audience how to accomplish a concrete outcome.", "guide tutorial"),
        archetype("lesson", "Lesson", CreatorArchetypeIntent.TEACH, "Teach one structured concept or skill."),
        archetype("course_module", "Course Module", CreatorArchetypeIntent.TEACH, "Teach one part of a larger ordered curriculum."),
        archetype("study_guide", "Study Guide", CreatorArchetypeIntent.TEACH, "Organize material for understanding, revision or practice."),
        archetype("short_tip", "Short Tip", CreatorArchetypeIntent.TEACH, "Deliver one useful teaching point quickly."),
        archetype("education", "Educational Content", CreatorArchetypeIntent.TEACH, "Teach or clarify a subject without requiring a tutorial format."),
        archetype("guide", "Guide", CreatorArchetypeIntent.GUIDE, "Help the audience make a decision or navigate an experience."),
        archetype("walkthrough", "Walkthrough", CreatorArchetypeIntent.GUIDE, "Lead the audience through a sequence step by step."),
        archetype("itinerary", "Itinerary", CreatorArchetypeIntent.GUIDE, "Organize places or activities into a useful travel sequence."),
        archetype("recipe", "Recipe", CreatorArchetypeIntent.GUIDE, "Guide the audience through ingredients, preparation and result."),
        archetype("workout", "Workout", CreatorArchetypeIntent.GUIDE, "Guide the audience through an exercise session or training block."),
        archetype("routine", "Routine", CreatorArchetypeIntent.GUIDE, "Show a repeatable sequence of actions or habits."),

        archetype("explainer", "Explainer", CreatorArchetypeIntent.EXPLAIN, "Make a topic, event or mechanism easier to understand."),
        archetype("demo", "Demo", CreatorArchetypeIntent.EXPLAIN, "Demonstrate a product, process, feature or result."),
        archetype("news_update", "News Update", CreatorArchetypeIntent.REPORT, "Report a verified change or developing story.", "breaking update", "tech news", "gaming news"),
        archetype("follow_up", "Follow-up", CreatorArchetypeIntent.REPORT, "Update an earlier story when facts, outcomes or context change."),
        archetype("reaction", "Reaction", CreatorArchetypeIntent.CONVERSE, "Respond to an event, release or piece of media with clear context."),
        archetype("opinion", "Opinion", CreatorArchetypeIntent.CONVERSE, "Present a creator's argued point of view."),
        archetype("advice", "Advice", CreatorArchetypeIntent.GUIDE, "Offer practical guidance informed by experience or evidence."),
        archetype("recommendation", "Recommendation", CreatorArchetypeIntent.GUIDE, "Recommend something and explain why it may fit the audience."),

        archetype("gameplay", "Gameplay Video", CreatorArchetypeIntent.ENTERTAIN, "Build content around captured gameplay and the creator's experience."),
        archetype("challenge", "Challenge", CreatorArchetypeIntent.ENTERTAIN, "Create around a rule, constraint or goal with an observable outcome."),
        archetype("highlights", "Highlights", CreatorArchetypeIntent.ENTERTAIN, "Curate the strongest moments from a larger source.", "cinematic moments", "cinematic compilation"),
        archetype("vlog", "Vlog", CreatorArchetypeIntent.DOCUMENT, "Document an experience or period from the creator's point of view."),
        archetype("experience", "Experience", CreatorArchetypeIntent.DOCUMENT, "Document and interpret a firsthand experience."),
        archetype("story", "Story", CreatorArchetypeIntent.DOCUMENT, "Tell a coherent narrative around an event, person or journey."),

        archetype("live_stream", "Livestream", CreatorArchetypeIntent.PERFORM, "Create live, audience-facing content in real time.", "live stream"),
        archetype("performance", "Performance", CreatorArchetypeIntent.PERFORM, "Capture or broadcast a prepared creative performance."),
        archetype("cover", "Cover", CreatorArchetypeIntent.PERFORM, "Create a performance or reinterpretation of an existing work."),
        archetype("podcast", "Podcast", CreatorArchetypeIntent.CONVERSE, "Create an audio or video conversation, monologue or episodic discussion."),

        archetype("original_work", "Original Work", CreatorArchetypeIntent.CREATE, "Create and present an original creative work."),
        archetype("process", "Process", CreatorArchetypeIntent.DOCUMENT, "Show how a creative result is made."),
        archetype("speed_process", "Speed Process", CreatorArchetypeIntent.DOCUMENT, "Compress a longer creative process into a fast visual sequence."),
        archetype("showcase", "Showcase", CreatorArchetypeIntent.PUBLISH, "Present completed work, a portfolio or a collection."),

        archetype("newsletter", "Newsletter", CreatorArchetypeIntent.PUBLISH, "Publish a recurring written edition for an audience."),
        archetype("article", "Article", CreatorArchetypeIntent.PUBLISH, "Publish structured written content."),
        archetype("post", "Post", CreatorArchetypeIntent.PUBLISH, "Publish a standalone social or professional post."),
        archetype("video", "Video", CreatorArchetypeIntent.PUBLISH, "Create a general video when no more specific archetype is needed."),
        archetype("short", "Short-form", CreatorArchetypeIntent.PUBLISH, "Create a concise short-form piece when no more specific archetype is needed."),
        archetype("audio", "Audio", CreatorArchetypeIntent.PUBLISH, "Create a general audio piece when no more specific archetype is needed."),
    )

    private val byId = definitions.associateBy { it.id }

    val ids: Set<String> get() = byId.keys

    fun definition(idOrAlias: String): CreatorContentArchetype? {
        val value = idOrAlias.trim()
        if (value.isBlank()) return null
        return byId[value.lowercase()] ?: definitions.firstOrNull { definition ->
            definition.label.equals(value, ignoreCase = true) ||
                definition.aliases.any { it.equals(value, ignoreCase = true) }
        }
    }

    fun requireDefinition(id: String): CreatorContentArchetype =
        byId[id] ?: error("Unknown creator content archetype: $id")

    /** Mode-specific copy is presentation only; the canonical archetype ID stays stable. */
    fun labelForMode(archetypeId: String, creatorMode: String): String {
        val override = CreatorModeRegistry.definition(creatorMode).suggestedArchetypes
            .firstOrNull { it.archetypeId == archetypeId }
            ?.label
        return override ?: requireDefinition(archetypeId).label
    }

    fun suggestedForMode(creatorMode: String): List<CreatorContentArchetype> =
        CreatorModeRegistry.definition(creatorMode).suggestedArchetypes
            .map { requireDefinition(it.archetypeId) }
            .distinctBy { it.id }

    fun validateModeRegistry(): List<String> = buildList {
        CreatorModeRegistry.definitions.forEach { mode ->
            mode.suggestedArchetypes.forEach { suggestion ->
                if (suggestion.archetypeId !in ids) add("${mode.label}: ${suggestion.archetypeId}")
            }
        }
    }

    private fun archetype(
        id: String,
        label: String,
        intent: CreatorArchetypeIntent,
        description: String,
        vararg aliases: String,
    ) = CreatorContentArchetype(id, label, intent, description, aliases.toSet())
}
