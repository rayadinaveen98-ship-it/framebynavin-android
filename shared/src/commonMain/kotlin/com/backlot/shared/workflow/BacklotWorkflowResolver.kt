package com.backlot.shared.workflow

/**
 * Cross-platform production workflow resolver.
 *
 * This is the first shared-core vertical slice. Android adapts its persisted CreatorTask into a
 * BacklotProjectDescriptor; iOS will pass the same product-level fields without reimplementing the
 * workflow rules in Swift.
 */
object BacklotWorkflowResolver {
    fun templateFor(project: BacklotProjectDescriptor): BacklotWorkflowTemplate? {
        val dna = project.contentDna.normalized()
        if (dna.isEmpty || dna.archetypeId.isBlank()) return null

        val mode = dna.creatorModeId
        val archetype = dna.archetypeId
        val platform = dna.platform.ifBlank { project.platform.trim() }
        val format = dna.deliveryFormat.ifBlank { project.contentType.trim() }
        val archetypeLabel = dna.archetypeLabel.ifBlank { fallbackLabel(archetype) }

        return when {
            mode == "film_entertainment" && archetype in ANALYTICAL_FILM -> filmAnalysis(platform, format, archetype, archetypeLabel)
            mode == "film_entertainment" && archetype == "review" -> filmReview(platform, format)
            mode == "film_entertainment" && archetype == "highlights" -> cinematicHighlights(platform, format)
            mode == "film_entertainment" && archetype in setOf("news_update", "follow_up", "reaction") -> reportWorkflow("Entertainment Update", platform, format)

            mode == "gaming" && archetype == "gameplay" -> gamingGameplay(platform, format)
            mode == "gaming" && archetype in setOf("tutorial", "walkthrough", "guide") -> gamingGuide(platform, format)
            mode == "gaming" && archetype == "review" -> evidenceReview("Game Review", platform, format, "Play / Test")

            mode == "tech" && archetype in setOf("review", "comparison", "benchmark") -> techReview(platform, format, archetype, archetypeLabel)
            mode == "tech" && archetype in setOf("tutorial", "demo", "explainer") -> teachingWorkflow("Tech $archetypeLabel", platform, format)

            mode == "education" && archetype in EDUCATION_ARCHETYPES -> teachingWorkflow(archetypeLabel, platform, format)
            mode == "news_commentary" && archetype in REPORT_ARCHETYPES -> reportWorkflow(archetypeLabel, platform, format)

            mode == "music_audio" && archetype == "original_work" -> musicOriginal(platform, format)
            mode == "music_audio" && archetype in setOf("cover", "performance") -> musicPerformance(platform, format)
            mode == "music_audio" && archetype == "podcast" -> podcastWorkflow(platform, format)

            mode == "food" && archetype in setOf("recipe", "tutorial") -> foodRecipe(platform, format)
            mode == "travel_outdoors" && archetype in setOf("vlog", "experience") -> travelVlog(platform, format)
            mode == "travel_outdoors" && archetype in setOf("guide", "itinerary", "recommendation") -> travelGuide(platform, format)

            mode == "art_design" && archetype in setOf("process", "speed_process", "original_work", "showcase") -> creativeProcess("Creative Process", platform, format)
            mode == "lifestyle" && archetype in setOf("vlog", "routine", "story", "experience") -> lifestyleStory(platform, format)
            mode == "health_fitness" && archetype in setOf("workout", "routine", "guide", "education") -> fitnessWorkflow(platform, format)
            mode == "business_career" && archetype in setOf("case_study", "analysis", "advice", "opinion") -> businessWorkflow(platform, format, archetype, archetypeLabel)

            archetype in REPORT_ARCHETYPES -> reportWorkflow(archetypeLabel, platform, format)
            archetype in EDUCATION_ARCHETYPES -> teachingWorkflow(archetypeLabel, platform, format)
            archetype == "podcast" -> podcastWorkflow(platform, format)
            else -> null
        }
    }

    private fun filmAnalysis(platform: String, format: String, archetype: String, archetypeLabel: String) = template(
        "v150_film_$archetype", "$archetypeLabel · $platform $format",
        stage("thesis", "Thesis", "Lock the central interpretation or question"),
        stage("research", "Research", "Collect context, references and primary material"),
        stage("scene_evidence", "Scene Evidence", "Map scenes, frames or craft choices that prove the thesis"),
        stage("structure", "Structure", "Order the argument and evidence into a clear journey"),
        stage("script", "Script", "Write and tighten narration around the evidence"),
        stage("voice", "Voice", "Record the final narration or presentation"),
        stage("edit", "Edit", "Build visuals so each example proves the point"),
        stage("package", "Packaging", "Finish title, thumbnail and supporting metadata"),
        stage("published", "Publish", "Publish and verify the live piece"),
        stage("promote", "Promote", "Share the strongest insight and capture the live link"),
    )

    private fun filmReview(platform: String, format: String) = template(
        "v150_film_review", "Movie Review · $platform $format",
        stage("watch_notes", "Watch Notes", "Capture reactions, evidence and important moments"),
        stage("verdict", "Verdict + Angle", "Decide the review's clear point of view and audience promise"),
        stage("evidence", "Evidence", "Choose examples that support strengths, limitations and trade-offs"),
        stage("spoiler_plan", "Spoiler Plan", "Decide what can be discussed and label spoiler sections"),
        stage("script", "Script", "Build the review around verdict, evidence and audience fit"),
        stage("edit", "Edit", "Assemble footage, narration and supporting proof"),
        stage("package", "Packaging", "Create title, thumbnail and description"),
        stage("published", "Publish", "Publish and verify the review"),
        stage("promote", "Promote", "Share a spoiler-safe takeaway and live link"),
    )

    private fun cinematicHighlights(platform: String, format: String) = template(
        "v150_film_highlights", "Cinematic Moments · $platform $format",
        stage("source", "Source", "Confirm the source material and usable footage"),
        stage("select", "Select Moments", "Choose only the strongest cinematic moments"),
        stage("sequence", "Sequence", "Build rhythm, escalation and a satisfying order"),
        stage("sound_grade", "Sound + Grade", "Polish audio, grade and transitions"),
        stage("qc", "Quality Check", "Check pacing, duplicates, framing and export quality"),
        stage("package", "Packaging", "Prepare title, thumbnail and metadata"),
        stage("published", "Publish", "Publish and verify the compilation"),
        stage("promote", "Promote", "Share a standout moment and capture the live link"),
    )

    private fun gamingGameplay(platform: String, format: String) = template(
        "v150_gaming_gameplay", "Gameplay · $platform $format",
        stage("session_plan", "Session Plan", "Choose the goal, challenge or experience for this session"),
        stage("capture", "Capture", "Record clean gameplay and creator reactions"),
        stage("select", "Select Moments", "Pull the strongest wins, fails, reveals and transitions"),
        stage("commentary", "Commentary", "Add context, narration or reactions where they improve the story"),
        stage("edit", "Edit", "Shape the session into a paced viewing experience"),
        stage("package", "Packaging", "Finish title, thumbnail and metadata"),
        stage("published", "Publish", "Publish and verify the gameplay video"),
        stage("promote", "Promote", "Share the strongest clip or hook"),
    )

    private fun gamingGuide(platform: String, format: String) = template(
        "v150_gaming_guide", "Game Guide · $platform $format",
        stage("problem", "Problem", "Define the exact player problem or objective"),
        stage("test", "Reproduce + Test", "Reproduce the situation and verify the solution"),
        stage("steps", "Steps", "Lock the shortest reliable sequence for the player"),
        stage("proof", "Capture Proof", "Capture gameplay that clearly demonstrates each step"),
        stage("script", "Narration", "Explain the method, caveats and common mistakes"),
        stage("edit", "Edit", "Pair each instruction with the right proof"),
        stage("package", "Packaging", "Make the outcome clear in title and cover"),
        stage("published", "Publish", "Publish and verify the guide"),
    )

    private fun techReview(platform: String, format: String, archetype: String, archetypeLabel: String) = template(
        "v150_tech_$archetype", "$archetypeLabel · $platform $format",
        stage("test_plan", "Test Plan", "Define audience, comparison baseline and measurable criteria"),
        stage("setup", "Setup", "Prepare the product, environment and repeatable test conditions"),
        stage("evidence", if (archetype == "benchmark") "Benchmarks" else "Evidence", "Run tests and capture measurements, demos and real-use evidence"),
        stage("verdict", "Verdict", "Turn evidence into clear strengths, limits and audience fit"),
        stage("script", "Script", "Structure claims so every major verdict has proof"),
        stage("capture", "B-roll + Demo", "Capture product visuals, screens and demonstrations"),
        stage("edit", "Edit", "Combine evidence, narration and visuals without hiding trade-offs"),
        stage("package", "Packaging", "Create precise title, cover and supporting metadata"),
        stage("published", "Publish", "Publish and verify the review"),
    )

    private fun teachingWorkflow(name: String, platform: String, format: String) = template(
        "v150_teach_${slug(name)}", "$name · $platform $format",
        stage("learning_goal", "Learning Goal", "Define what the learner should understand or do by the end"),
        stage("prerequisites", "Prerequisites", "Identify required knowledge, tools or setup"),
        stage("outline", "Outline", "Break the lesson into dependency-ordered steps"),
        stage("example", "Example + Demo", "Prepare concrete examples that prove each important concept"),
        stage("record", "Record", "Record the explanation or demonstration"),
        stage("visuals", "Supporting Visuals", "Add screens, diagrams, captions or examples where useful"),
        stage("review", "Learning Check", "Verify the promised outcome is actually teachable from the content"),
        stage("published", "Publish", "Publish and verify the lesson"),
    )

    private fun reportWorkflow(name: String, platform: String, format: String) = template(
        "v150_report_${slug(name)}", "$name · $platform $format",
        stage("sources", "Source Collection", "Collect the primary source and strong independent context"),
        stage("verify", "Verify", "Confirm core facts before writing or recording"),
        stage("known_unknown", "Known / Unknown", "Separate confirmed, developing and unverified details"),
        stage("context", "Context", "Add only the background needed to understand what changed"),
        stage("script", "Script", "Lead with the new verified information and its significance"),
        stage("published", "Publish", "Publish with uncertainty clearly labelled where necessary"),
        stage("follow_up", "Follow-up", "Track what could materially change or require correction"),
    )

    private fun musicOriginal(platform: String, format: String) = template(
        "v150_music_original", "Original Music · $platform $format",
        stage("concept", "Concept", "Lock the emotion, idea or musical direction"),
        stage("write", "Writing", "Develop lyrics, melody, harmony or core musical material"),
        stage("arrange", "Arrangement", "Build the structure, instrumentation and dynamics"),
        stage("record", "Record", "Capture final vocals and instruments"),
        stage("mix", "Mix + Master", "Balance, polish and master the final audio"),
        stage("artwork", "Artwork + Metadata", "Prepare cover, credits and release metadata"),
        stage("published", "Release", "Publish or distribute and verify the release"),
        stage("promote", "Promote", "Prepare clips, performance moments or launch posts"),
    )

    private fun musicPerformance(platform: String, format: String) = template(
        "v150_music_performance", "Performance · $platform $format",
        stage("select", "Select Piece", "Lock the song, section and performance concept"),
        stage("rehearse", "Rehearse", "Prepare arrangement, performance and timing"),
        stage("setup", "Audio + Camera Setup", "Test sound, framing, light and monitoring"),
        stage("record", "Record", "Capture the final performance"),
        stage("edit", "Edit + Mix", "Choose the best take and finish picture and sound"),
        stage("package", "Packaging", "Prepare cover, title and credits"),
        stage("published", "Publish", "Publish and verify the performance"),
    )

    private fun podcastWorkflow(platform: String, format: String) = template(
        "v150_podcast", "Podcast · $platform $format",
        stage("promise", "Episode Promise", "Lock the audience promise, guest or central question"),
        stage("research", "Research", "Prepare sources, guest context and talking points"),
        stage("outline", "Run of Show", "Build the episode flow and transitions"),
        stage("record", "Record", "Capture the final conversation or monologue"),
        stage("edit", "Edit + QC", "Clean audio, pacing and factual corrections"),
        stage("metadata", "Metadata", "Prepare title, description, chapters and artwork"),
        stage("published", "Publish", "Publish and verify the episode"),
        stage("clips", "Clips", "Select moments for useful derivative clips"),
    )

    private fun foodRecipe(platform: String, format: String) = template(
        "v150_food_recipe", "Recipe · $platform $format",
        stage("ingredients", "Ingredients", "Finalize quantities, substitutions and required tools"),
        stage("method", "Method", "Verify the cooking sequence and critical timings"),
        stage("shot_plan", "Shot Plan", "Plan the visual moments viewers must clearly see"),
        stage("capture", "Cook + Capture", "Cook while capturing usable steps and final result"),
        stage("edit", "Edit", "Build a clear, appetizing and followable sequence"),
        stage("metadata", "Recipe Metadata", "Add ingredients, method, timings and useful notes"),
        stage("published", "Publish", "Publish and verify the recipe"),
    )

    private fun travelVlog(platform: String, format: String) = template(
        "v150_travel_vlog", "Travel Vlog · $platform $format",
        stage("trip_plan", "Trip Plan", "Lock the experience, route and story opportunity"),
        stage("locations", "Locations", "Prioritize places and moments worth capturing"),
        stage("shot_plan", "Shot List", "Plan essential establishing, detail and transition shots"),
        stage("capture", "Capture", "Record the journey, reactions and useful context"),
        stage("story", "Story Assembly", "Find the narrative throughline from the captured material"),
        stage("edit", "Edit", "Shape pace, geography, sound and emotion"),
        stage("package", "Packaging", "Prepare title, cover and location context"),
        stage("published", "Publish", "Publish and verify the vlog"),
    )

    private fun travelGuide(platform: String, format: String) = template(
        "v150_travel_guide", "Travel Guide · $platform $format",
        stage("audience", "Audience + Goal", "Define who the guide is for and what trip decision it solves"),
        stage("research", "Research", "Verify locations, timings, access, costs and constraints"),
        stage("route", "Route + Priorities", "Build a useful order and decision rules"),
        stage("capture", "Capture Proof", "Collect visuals that support each recommendation"),
        stage("script", "Guide Script", "Explain trade-offs, alternatives and practical details"),
        stage("edit", "Edit", "Keep geography and recommendations easy to follow"),
        stage("published", "Publish", "Publish and verify the guide"),
    )

    private fun creativeProcess(name: String, platform: String, format: String) = template(
        "v150_creative_process", "$name · $platform $format",
        stage("brief", "Creative Brief", "Lock the goal, constraints and intended result"),
        stage("references", "References", "Collect inspiration without losing the original direction"),
        stage("create", "Create", "Produce the work while preserving useful process moments"),
        stage("refine", "Refine", "Review composition, craft and presentation"),
        stage("process_edit", "Process Story", "Turn the making process into a coherent audience journey"),
        stage("package", "Presentation", "Prepare final visuals, title and context"),
        stage("published", "Publish", "Publish and verify the work"),
    )

    private fun lifestyleStory(platform: String, format: String) = template(
        "v150_lifestyle_story", "Lifestyle Story · $platform $format",
        stage("intent", "Story Intent", "Choose the experience, feeling or takeaway worth sharing"),
        stage("plan", "Plan", "Identify key moments without scripting away authenticity"),
        stage("capture", "Capture", "Record the experience, details and honest reactions"),
        stage("story", "Story", "Find the beginning, change and payoff in the material"),
        stage("edit", "Edit", "Shape pace while keeping the creator voice natural"),
        stage("package", "Packaging", "Prepare title, cover and audience promise"),
        stage("published", "Publish", "Publish and verify the story"),
    )

    private fun fitnessWorkflow(platform: String, format: String) = template(
        "v150_fitness", "Fitness Content · $platform $format",
        stage("outcome", "Outcome", "Define the routine, skill or educational outcome"),
        stage("safety", "Safety + Context", "Check prerequisites, form cues and important limitations"),
        stage("plan", "Routine Plan", "Order movements or teaching points clearly"),
        stage("demo", "Demonstration", "Capture correct execution and useful angles"),
        stage("explain", "Explanation", "Add cues, modifications and progression guidance"),
        stage("review", "Review", "Check clarity and avoid unsupported health claims"),
        stage("published", "Publish", "Publish and verify the content"),
    )

    private fun businessWorkflow(platform: String, format: String, archetype: String, archetypeLabel: String) = template(
        "v150_business_$archetype", "$archetypeLabel · $platform $format",
        stage("question", "Business Question", "Lock the decision, problem or claim being explored"),
        stage("evidence", "Evidence", "Collect cases, data, examples and relevant constraints"),
        stage("framework", "Framework", "Organize the evidence into a useful model or argument"),
        stage("draft", "Draft", "Write with a clear audience takeaway and practical implications"),
        stage("proof", "Proof", "Check claims, numbers, links and context"),
        stage("package", "Packaging", "Prepare title, visual or opening that reflects the real value"),
        stage("published", "Publish", "Publish and verify the content"),
    )

    private fun evidenceReview(name: String, platform: String, format: String, firstStage: String) = template(
        "v150_review_${slug(name)}", "$name · $platform $format",
        stage("test", firstStage, "Spend enough time with the subject to form a defensible verdict"),
        stage("criteria", "Criteria", "Choose the criteria that matter to this audience"),
        stage("evidence", "Evidence", "Capture examples for major strengths and limitations"),
        stage("verdict", "Verdict", "Decide who this is and is not for"),
        stage("script", "Script", "Build the review so evidence earns the conclusion"),
        stage("edit", "Edit", "Pair claims with the strongest supporting proof"),
        stage("published", "Publish", "Publish and verify the review"),
    )

    private fun template(id: String, label: String, vararg stages: BacklotWorkflowStage) =
        BacklotWorkflowTemplate(id = id, label = label, stages = stages.toList())

    private fun stage(id: String, label: String, action: String) = BacklotWorkflowStage(id, label, action)

    private fun fallbackLabel(archetype: String): String =
        archetype.replace('_', ' ').replaceFirstChar { it.uppercase() }

    private fun slug(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private val ANALYTICAL_FILM = setOf("analysis", "essay", "deep_dive", "explainer")
    private val EDUCATION_ARCHETYPES = setOf("tutorial", "lesson", "course_module", "study_guide", "short_tip", "education", "explainer", "guide", "walkthrough")
    private val REPORT_ARCHETYPES = setOf("news_update", "follow_up", "reaction")
}
