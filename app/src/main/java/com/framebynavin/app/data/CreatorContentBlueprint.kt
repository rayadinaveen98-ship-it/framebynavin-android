package com.framebynavin.app.data

data class CreatorBlueprintStep(
    val title: String,
    val guidance: String,
)

data class CreatorBlueprintPrompt(
    val label: String,
    val prompt: String,
)

data class CreatorContentBlueprint(
    val dna: CreatorContentDna,
    val modeLabel: String,
    val archetypeLabel: String,
    val headline: String,
    val summary: String,
    val workflow: List<CreatorBlueprintStep>,
    val workspacePrompts: List<CreatorBlueprintPrompt>,
    val scriptStructure: List<CreatorBlueprintStep>,
    val checklist: List<String>,
    val toolSuggestions: List<String>,
    val outputIdeas: List<String>,
)

/**
 * Turns Content DNA into explainable creator guidance.
 *
 * This engine is deliberately deterministic and advisory. It never mutates a project, advances a
 * workflow stage, creates a reminder, marks a deliverable published, or completes a project. The
 * creator remains the authority; DNA simply changes which recommendations are shown.
 */
object CreatorContentBlueprintEngine {
    fun forTask(
        task: CreatorTask,
        profile: CreatorProfile = CreatorProfile(),
    ): CreatorContentBlueprint = forDna(CreatorContentDnaEngine.effective(task, profile))

    fun forDna(input: CreatorContentDna): CreatorContentBlueprint {
        val dna = input.normalized()
        val mode = dna.creatorModeId.takeIf { it.isNotBlank() }?.let(CreatorModeRegistry::definition)
        val archetype = ContentArchetypeRegistry.definition(dna.archetypeId)
        val intent = archetype?.intent ?: CreatorArchetypeIntent.PUBLISH
        val modeLabel = mode?.label ?: "Creator"
        val archetypeLabel = archetype?.let { definition ->
            runCatching { ContentArchetypeRegistry.labelForMode(definition.id, dna.creatorModeId) }
                .getOrDefault(definition.label)
        } ?: "Content"

        val base = intentBlueprint(intent)
        val styleGuidance = dna.productionStyles
            .mapNotNull(ProductionStyleRegistry::definition)
            .distinctBy { it.id }
            .flatMap(::productionGuidance)
        val platformGuidance = platformGuidance(dna.platform, dna.deliveryFormat)

        val checklist = (base.checklist + styleGuidance.flatMap { it.checklist } + platformGuidance.checklist)
            .distinct()
            .take(12)
        val tools = (base.tools + styleGuidance.flatMap { it.tools } + platformGuidance.tools)
            .distinct()
            .take(10)
        val outputs = (base.outputs + platformGuidance.outputs)
            .distinct()
            .take(6)

        val productionSummary = dna.productionStyles.joinToString(" + ").ifBlank { "your chosen production style" }
        val destination = listOf(dna.platform, dna.deliveryFormat).filter { it.isNotBlank() }.joinToString(" · ")
            .ifBlank { "your publishing destination" }

        return CreatorContentBlueprint(
            dna = dna,
            modeLabel = modeLabel,
            archetypeLabel = archetypeLabel,
            headline = "$archetypeLabel blueprint",
            summary = "Built for $modeLabel using $productionSummary for $destination. Use this as a playbook, not a locked workflow.",
            workflow = base.workflow,
            workspacePrompts = base.prompts,
            scriptStructure = base.script,
            checklist = checklist,
            toolSuggestions = tools,
            outputIdeas = outputs,
        )
    }

    private data class IntentBlueprint(
        val workflow: List<CreatorBlueprintStep>,
        val prompts: List<CreatorBlueprintPrompt>,
        val script: List<CreatorBlueprintStep>,
        val checklist: List<String>,
        val tools: List<String>,
        val outputs: List<String>,
    )

    private data class GuidanceBundle(
        val checklist: List<String> = emptyList(),
        val tools: List<String> = emptyList(),
        val outputs: List<String> = emptyList(),
    )

    private fun intentBlueprint(intent: CreatorArchetypeIntent): IntentBlueprint = when (intent) {
        CreatorArchetypeIntent.REVIEW -> blueprint(
            workflow = listOf(
                step("Define the verdict", "Decide what the audience should understand before collecting supporting points."),
                step("Set useful criteria", "Choose the few criteria that actually matter to this audience."),
                step("Collect proof", "Gather examples, footage, screenshots, quotes or measurements for each major claim."),
                step("Build the argument", "Order positives, limitations and trade-offs so the verdict feels earned."),
                step("Create & package", "Produce the piece, then make the title/cover communicate the strongest useful promise."),
                step("Publish & learn", "Verify the live output and note what viewers agreed with, challenged or asked next."),
            ),
            prompts = commonPrompts(
                problem = "What decision, expectation or confusion is the viewer bringing into this review?",
                promise = "What will the viewer know or be able to decide by the end?",
                angle = "What is your specific lens instead of a generic list of pros and cons?",
                hook = "What surprising verdict, tension or question earns attention immediately?",
            ),
            script = listOf(
                step("Hook / verdict tension", "Open with the strongest question, claim or contradiction."),
                step("Context", "Give only the background needed to understand the review."),
                step("Criteria", "Tell the viewer what you are judging and why it matters."),
                step("Evidence blocks", "Pair each major opinion with a concrete example."),
                step("Trade-offs", "Acknowledge where the opposite audience may reasonably disagree."),
                step("Verdict", "Land on a clear conclusion and who this is or is not for."),
            ),
            checklist = listOf(
                "Every major opinion has a concrete example or reason",
                "The verdict is clear without pretending the review is universal",
                "Strengths and limitations are separated from personal preference",
                "The opening does not spend too long on background",
            ),
            tools = listOf("Evidence / reference list", "Comparison notes", "Title + cover variants"),
            outputs = listOf("Main review", "Short verdict cut", "Key takeaway post"),
        )

        CreatorArchetypeIntent.ANALYZE -> blueprint(
            workflow = listOf(
                step("Frame the question", "Write one question or thesis that the entire piece will answer."),
                step("Research", "Collect primary examples first, then supporting context and references."),
                step("Map evidence", "Group evidence by idea rather than by the order you found it."),
                step("Build the thesis", "Choose the strongest interpretation and identify where it could be challenged."),
                step("Script the journey", "Move from question to evidence to insight instead of dumping observations."),
                step("Produce & verify", "Make visuals prove the analysis and re-check claims before publishing."),
            ),
            prompts = commonPrompts(
                problem = "What does the audience notice but not fully understand yet?",
                promise = "What deeper understanding should they leave with?",
                angle = "What is the central thesis connecting your evidence?",
                hook = "What question, contradiction or overlooked detail opens the analysis?",
            ),
            script = listOf(
                step("Question / hook", "Introduce the puzzle, contradiction or idea worth investigating."),
                step("Thesis", "State the core interpretation clearly enough that it can be tested."),
                step("Context", "Give the minimum background needed for the evidence to make sense."),
                step("Evidence sequence", "Build 2–5 evidence blocks, each advancing the thesis."),
                step("Counterpoint", "Address the strongest reasonable alternate reading or limitation."),
                step("Payoff", "Return to the opening question with a sharper conclusion."),
            ),
            checklist = listOf(
                "The thesis can be stated in one or two sentences",
                "Examples demonstrate the point instead of decorating it",
                "Fact, interpretation and opinion are distinguishable",
                "The conclusion adds insight instead of only summarizing",
            ),
            tools = listOf("Research board", "Evidence map", "Scene / example log", "Argument outline"),
            outputs = listOf("Main analysis", "One-insight short", "Evidence carousel / thread"),
        )

        CreatorArchetypeIntent.TEACH -> blueprint(
            workflow = listOf(
                step("Define the outcome", "Describe what the learner should be able to do or understand after this."),
                step("Check prerequisites", "Identify what the learner needs to know before step one."),
                step("Break into steps", "Use the smallest useful sequence that still reaches the outcome."),
                step("Prepare examples", "Show a concrete example for every concept that could feel abstract."),
                step("Teach & demonstrate", "Alternate explanation with visible application."),
                step("Verify learning", "End with a recap, exercise, result check or next practice step."),
            ),
            prompts = commonPrompts(
                problem = "What exactly is the learner struggling to understand or do?",
                promise = "What concrete skill or understanding will they gain?",
                angle = "What is the simplest teaching path for this audience level?",
                hook = "What useful result can you preview immediately?",
            ),
            script = listOf(
                step("Outcome first", "Show or state the result before explaining the process."),
                step("Prerequisites", "Cover only what is needed before the first action."),
                step("Step-by-step teaching", "One idea or action at a time, in dependency order."),
                step("Example / demo", "Apply the lesson to a real example."),
                step("Common mistake", "Call out the failure mode most likely to block the learner."),
                step("Recap / practice", "Summarize the method and give the viewer a next action."),
            ),
            checklist = listOf(
                "The promised outcome is visible or testable",
                "No step assumes knowledge that was never introduced",
                "Examples match the audience's likely real use case",
                "A learner can tell what to do next after finishing",
            ),
            tools = listOf("Lesson outline", "Demo project / example", "Practice checklist"),
            outputs = listOf("Main lesson", "Quick tip cut", "Reference checklist"),
        )

        CreatorArchetypeIntent.EXPLAIN -> blueprint(
            workflow = listOf(
                step("Define the confusion", "Write the exact thing the audience does not yet understand."),
                step("Find the simplest model", "Choose one mental model or analogy that clarifies the mechanism."),
                step("Verify the facts", "Separate confirmed facts from assumptions, interpretation or developing details."),
                step("Structure cause → effect", "Order the explanation so each idea makes the next one easier."),
                step("Visualize", "Use diagrams, examples, clips or demonstrations where words alone would be slower."),
                step("Land the implication", "Explain why this matters and what the viewer should understand now."),
            ),
            prompts = commonPrompts(
                problem = "What part of this topic is confusing, misunderstood or easy to oversimplify?",
                promise = "What will become clear by the end?",
                angle = "What simple model best explains the complexity without distorting it?",
                hook = "What surprising consequence or misconception makes this worth understanding?",
            ),
            script = listOf(
                step("Why this matters", "Open with the consequence, misconception or question."),
                step("Plain-language answer", "Give the short answer before expanding it."),
                step("Mechanism", "Explain how it works in a logical sequence."),
                step("Example", "Make the mechanism concrete with one strong example."),
                step("Nuance", "Add the caveat that prevents the explanation becoming misleading."),
                step("Takeaway", "Restate the idea in a memorable final form."),
            ),
            checklist = listOf(
                "Jargon is either removed or explained before it is used",
                "The short answer appears before the deep detail",
                "The example truly matches the mechanism being explained",
                "Important uncertainty or caveats are not hidden",
            ),
            tools = listOf("Fact sheet", "Source list", "Visual explanation plan"),
            outputs = listOf("Main explainer", "60-second summary", "Diagram / summary post"),
        )

        CreatorArchetypeIntent.REPORT -> blueprint(
            workflow = listOf(
                step("Verify first", "Confirm the core event with primary or high-quality independent sources."),
                step("Separate known / unknown", "Write what is confirmed, developing and still unverified."),
                step("Build context", "Add only the history or background needed to understand the update."),
                step("Write cleanly", "Lead with the actual new information, then evidence and implications."),
                step("Publish with labels", "Make uncertainty visible instead of burying it in the copy."),
                step("Prepare follow-up", "Note what could change and what source would confirm the next update."),
            ),
            prompts = commonPrompts(
                problem = "What does the audience need to know right now, without speculation being mixed into fact?",
                promise = "What verified understanding will they get from this update?",
                angle = "What is actually new compared with what was already known?",
                hook = "What is the clearest accurate statement of the development?",
            ),
            script = listOf(
                step("Confirmed update", "Lead with the verified new information."),
                step("Source / evidence", "Show where the claim comes from."),
                step("Context", "Explain the minimum background needed."),
                step("What it means", "Describe likely implications without presenting inference as fact."),
                step("What is unknown", "State unresolved or disputed details explicitly."),
                step("Next watchpoint", "Tell the audience what development would materially change the story."),
            ),
            checklist = listOf(
                "Core factual claims are sourced before publication",
                "Rumor, inference and confirmed fact are visibly separated",
                "Old information is not presented as a new development",
                "A correction or follow-up path is clear if facts change",
            ),
            tools = listOf("Source ledger", "Fact / uncertainty split", "Timeline", "Follow-up watchlist"),
            outputs = listOf("Verified update", "Context explainer", "Follow-up post"),
        )

        CreatorArchetypeIntent.GUIDE -> blueprint(
            workflow = listOf(
                step("Define the destination", "State the decision or outcome the audience is trying to reach."),
                step("Map constraints", "Identify budget, time, skill, location or other constraints that change the advice."),
                step("Build the path", "Order recommendations so the audience knows what to do first, next and last."),
                step("Add alternatives", "Include useful branches for audiences with different needs."),
                step("Demonstrate", "Show the recommendation working where possible."),
                step("Finish with action", "Give the audience a simple next step or decision rule."),
            ),
            prompts = commonPrompts(
                problem = "What decision or process is the audience trying to navigate?",
                promise = "What outcome will this guide help them reach?",
                angle = "What constraints or audience type make this guide specifically useful?",
                hook = "What expensive, confusing or frustrating mistake can this guide prevent?",
            ),
            script = listOf(
                step("Destination", "Define the result and who the guide is for."),
                step("Decision rule", "Give the simplest rule that shapes the rest of the guide."),
                step("Path / steps", "Walk through the recommended sequence."),
                step("Alternatives", "Show when another option is a better fit."),
                step("Mistakes", "Call out avoidable traps."),
                step("Next action", "End with the exact first move the viewer can take."),
            ),
            checklist = listOf(
                "Recommendations state who they are for",
                "Constraints and trade-offs are visible",
                "Steps are ordered by dependency rather than convenience",
                "The final next action is specific",
            ),
            tools = listOf("Decision tree", "Step checklist", "Comparison table"),
            outputs = listOf("Main guide", "Quick-start checklist", "Recommendation short"),
        )

        CreatorArchetypeIntent.ENTERTAIN -> blueprint(
            workflow = listOf(
                step("Define the experience", "Choose the emotion, tension, surprise or fun the audience should feel."),
                step("Find the moments", "Identify the strongest beats before filling the space between them."),
                step("Shape escalation", "Make each beat earn the next rather than repeating the same energy."),
                step("Capture / create", "Prioritize usable moments, reactions and transitions."),
                step("Edit for rhythm", "Cut dead time aggressively while preserving setup needed for payoff."),
                step("Package the payoff", "Make the title/cover promise the experience without spoiling it."),
            ),
            prompts = commonPrompts(
                problem = "What feeling or experience is the audience coming here for?",
                promise = "What memorable payoff will make the time feel worth it?",
                angle = "What rule, perspective or personality makes this version distinctive?",
                hook = "What immediate moment signals the entertainment value?",
            ),
            script = listOf(
                step("Cold open", "Tease a strong moment, challenge or outcome."),
                step("Setup", "Explain the premise fast enough to start the experience."),
                step("Escalating beats", "Build increasingly interesting moments or complications."),
                step("Peak moment", "Deliver the strongest emotional or entertainment payoff."),
                step("Resolution", "Let the audience feel the result without dragging the ending."),
                step("Button", "End on a memorable final beat, callback or next tease."),
            ),
            checklist = listOf(
                "The premise is understandable early",
                "Repeated beats are cut or escalated",
                "The strongest moment has enough setup to land",
                "Packaging promises the experience without misleading",
            ),
            tools = listOf("Beat sheet", "Moment markers", "Retention edit pass"),
            outputs = listOf("Main entertainment piece", "Best-moment short", "Behind-the-scenes cut"),
        )

        CreatorArchetypeIntent.DOCUMENT -> blueprint(
            workflow = listOf(
                step("Choose the story lens", "Decide what this experience means beyond simply recording what happened."),
                step("Plan story anchors", "Identify beginning state, turning points and desired ending."),
                step("Capture texture", "Collect actions, details, environment, reactions and transitions."),
                step("Find the story", "Review material and choose the moments that reveal change or meaning."),
                step("Build narration", "Use narration only where the footage cannot carry context itself."),
                step("Edit for meaning", "Shape chronology around the emotional or informational arc."),
            ),
            prompts = commonPrompts(
                problem = "Why would this real experience matter to someone who was not there?",
                promise = "What will the audience feel, learn or understand from following the journey?",
                angle = "What personal lens turns documentation into a story?",
                hook = "What moment hints that something meaningful, difficult or surprising is ahead?",
            ),
            script = listOf(
                step("Present moment", "Open inside a concrete moment rather than with a long explanation."),
                step("Context", "Explain where we are, what you want and what is at stake."),
                step("Journey beats", "Use moments that show movement, friction or discovery."),
                step("Turning point", "Highlight what changed your understanding or direction."),
                step("Reflection", "Say what the experience meant without over-explaining every feeling."),
                step("After", "Show where things landed or what remains unresolved."),
            ),
            checklist = listOf(
                "The story has a lens beyond chronological footage",
                "Important transitions and context are captured",
                "Reflection is connected to specific moments",
                "The ending shows change, consequence or a meaningful open question",
            ),
            tools = listOf("Story beat log", "Shot list", "Daily / session notes"),
            outputs = listOf("Main story / vlog", "Moment short", "Photo / quote recap"),
        )

        CreatorArchetypeIntent.PERFORM -> blueprint(
            workflow = listOf(
                step("Define the performance goal", "Choose the take, arrangement or live experience you want to capture."),
                step("Rehearse", "Resolve performance issues before adding production complexity."),
                step("Plan capture", "Lock camera, audio, lighting and backup recording needs."),
                step("Record / perform", "Protect the strongest complete take while capturing optional coverage."),
                step("Polish", "Edit, mix or grade without removing the character of the performance."),
                step("Package & release", "Present the performance clearly and credit collaborators or source material where needed."),
            ),
            prompts = commonPrompts(
                problem = "What should the audience feel or notice in this performance?",
                promise = "What kind of performance experience are you delivering?",
                angle = "What interpretation, arrangement or setting makes this version yours?",
                hook = "What strongest moment can preview the performance immediately?",
            ),
            script = listOf(
                step("Opening moment", "Start with a confident musical, visual or verbal entry."),
                step("Build", "Let energy, complexity or intimacy develop."),
                step("Signature moment", "Protect the moment that best represents the performance."),
                step("Finish", "End decisively and leave enough space for the performance to land."),
            ),
            checklist = listOf(
                "Primary performance quality is stronger than the production gimmick",
                "Audio capture has a backup where practical",
                "The strongest full take is protected before experimental coverage",
                "Credits and rights-sensitive details are reviewed before release",
            ),
            tools = listOf("Rehearsal notes", "Capture checklist", "Take log", "Audio QC pass"),
            outputs = listOf("Full performance", "Signature-moment clip", "Process / rehearsal cut"),
        )

        CreatorArchetypeIntent.CREATE -> blueprint(
            workflow = listOf(
                step("Define the creative brief", "Lock the intent, constraints and finish line before polishing."),
                step("Explore", "Generate a small set of meaningfully different directions."),
                step("Choose a direction", "Select based on the brief, not just novelty."),
                step("Build", "Create the complete work before endless micro-polish."),
                step("Refine", "Use one focused quality pass for craft, consistency and presentation."),
                step("Present", "Show the final work with enough context for the audience to understand the intent."),
            ),
            prompts = commonPrompts(
                problem = "What creative need, idea or feeling is this work responding to?",
                promise = "What experience should the finished work create?",
                angle = "What constraint, style or concept gives this work identity?",
                hook = "What detail best represents the final creative promise?",
            ),
            script = listOf(
                step("Intent", "State or show what the work is trying to do."),
                step("Process / development", "Reveal only the decisions that help the audience appreciate the result."),
                step("Final work", "Give the finished piece enough uninterrupted space to land."),
                step("Reflection", "Capture what changed, what worked and what you would carry forward."),
            ),
            checklist = listOf(
                "The finish line is defined before polishing begins",
                "Exploration includes genuinely different options",
                "Final presentation supports rather than distracts from the work",
                "A reusable learning is captured after completion",
            ),
            tools = listOf("Creative brief", "Reference board", "Version / iteration log"),
            outputs = listOf("Finished work", "Process piece", "Before / after or detail cut"),
        )

        CreatorArchetypeIntent.CONVERSE -> blueprint(
            workflow = listOf(
                step("Choose the conversation promise", "Decide what useful tension, perspective or question the conversation will explore."),
                step("Prepare prompts", "Use open questions and follow-ups, not a rigid interrogation script."),
                step("Research context", "Know enough to challenge, clarify and avoid wasting the guest or audience's time."),
                step("Record the conversation", "Listen for unexpected threads worth following."),
                step("Shape the edit", "Protect coherent ideas while removing repetition and dead space."),
                step("Package the strongest idea", "Lead with the conversation's most valuable tension or insight."),
            ),
            prompts = commonPrompts(
                problem = "What question or tension deserves a real conversation instead of a monologue?",
                promise = "What perspective should the audience gain by listening?",
                angle = "What makes this conversation different from the obvious questions?",
                hook = "What sharp question or answer best exposes the central tension?",
            ),
            script = listOf(
                step("Opening question", "Start with a question that quickly reveals stakes or perspective."),
                step("Context", "Give the audience only the background needed to follow."),
                step("Core threads", "Group the conversation around a few meaningful themes."),
                step("Challenge / tension", "Clarify disagreement, trade-offs or uncertainty respectfully."),
                step("Synthesis", "End with what changed, remains unresolved or deserves further thought."),
            ),
            checklist = listOf(
                "Questions invite specific answers rather than generic talking points",
                "Important claims are challenged or clarified when needed",
                "The edit preserves meaning and context",
                "Packaging represents the conversation fairly",
            ),
            tools = listOf("Guest / topic brief", "Question bank", "Timestamp markers"),
            outputs = listOf("Full conversation", "Insight clip", "Quote / takeaway post"),
        )

        CreatorArchetypeIntent.PUBLISH -> blueprint(
            workflow = listOf(
                step("Define the message", "Write the one thing this piece needs to communicate or achieve."),
                step("Draft", "Create a complete first version before polishing details."),
                step("Review", "Check clarity, factual accuracy, presentation and audience fit."),
                step("Package", "Prepare platform-specific title, cover, caption or metadata."),
                step("Publish", "Verify the live result rather than assuming the upload succeeded."),
                step("Learn", "Capture response, friction and the next improvement."),
            ),
            prompts = commonPrompts(
                problem = "Why does the audience need this piece?",
                promise = "What should they get from it?",
                angle = "What makes this version worth publishing?",
                hook = "What is the clearest opening expression of the value?",
            ),
            script = listOf(
                step("Opening", "Lead with value, tension or the main idea."),
                step("Core", "Develop the idea with the minimum structure needed."),
                step("Proof / example", "Support the message with something concrete."),
                step("Close", "Finish with a clear takeaway or next action."),
            ),
            checklist = listOf(
                "The purpose is clear before publishing",
                "The live version is checked after publication",
                "Metadata matches the actual content",
                "At least one learning is captured after release",
            ),
            tools = listOf("Draft outline", "Publish checklist", "Post-publish note"),
            outputs = listOf("Primary piece", "Short derivative", "Takeaway post"),
        )
    }

    private fun commonPrompts(
        problem: String,
        promise: String,
        angle: String,
        hook: String,
    ) = listOf(
        CreatorBlueprintPrompt("Audience", "Who specifically should care about this, and what do they already know?"),
        CreatorBlueprintPrompt("Viewer problem", problem),
        CreatorBlueprintPrompt("Promise", promise),
        CreatorBlueprintPrompt("Angle", angle),
        CreatorBlueprintPrompt("Hook", hook),
    )

    private fun productionGuidance(style: CreatorProductionStyleDefinition): List<GuidanceBundle> = when (style.kind) {
        CreatorProductionKind.NARRATION -> listOf(GuidanceBundle(
            checklist = listOf("Read the script aloud before final recording", "Plan visuals against narration beats", "Run a final voice / music intelligibility check"),
            tools = listOf("Narration script", "Visual / B-roll map", "Audio cleanup + loudness QC"),
        ))
        CreatorProductionKind.PRESENTER -> listOf(GuidanceBundle(
            checklist = listOf("Camera, eyeline, framing and background are intentional", "Record a clean audio test before the full take", "Talking points allow natural delivery instead of robotic reading"),
            tools = listOf("Talking-point card", "Camera + lighting check", "Audio test"),
        ))
        CreatorProductionKind.GAMEPLAY -> listOf(GuidanceBundle(
            checklist = listOf("Capture settings and frame rate are tested first", "Mark useful gameplay moments while recording", "UI, notifications and private account details are safe to show"),
            tools = listOf("Gameplay capture", "Moment / timestamp log", "Commentary track"),
        ))
        CreatorProductionKind.SCREEN -> listOf(GuidanceBundle(
            checklist = listOf("Screen resolution and text size are readable on the target platform", "Private tabs, keys, messages and notifications are hidden", "Cursor movement supports rather than distracts from the explanation"),
            tools = listOf("Screen recording plan", "Demo state / sample data", "Cursor + zoom pass"),
        ))
        CreatorProductionKind.CAMERA -> listOf(GuidanceBundle(
            checklist = listOf("A-roll and essential B-roll have a simple shot list", "Continuity and coverage are sufficient before leaving the location", "Critical shots have a backup take"),
            tools = listOf("Shot list", "B-roll checklist", "Continuity notes"),
        ))
        CreatorProductionKind.LIVE -> listOf(GuidanceBundle(
            checklist = listOf("Run of show and key transitions are prepared", "Connection, audio and backup recording are tested", "Moderation / audience interaction rules are clear"),
            tools = listOf("Run of show", "Live tech check", "Backup recording"),
        ))
        CreatorProductionKind.AUDIO -> listOf(GuidanceBundle(
            checklist = listOf("Mic level and room noise are checked before recording", "Edits preserve natural pacing", "Final loudness and clipping are checked on headphones and speakers"),
            tools = listOf("Audio session notes", "Noise / edit pass", "Loudness QC"),
        ))
        CreatorProductionKind.MOTION -> listOf(GuidanceBundle(
            checklist = listOf("Storyboard or animatic proves timing before detailed polish", "Reusable visual assets are organized", "Motion reinforces the idea instead of competing with it"),
            tools = listOf("Storyboard / animatic", "Asset list", "Motion timing pass"),
        ))
        CreatorProductionKind.WRITING -> listOf(GuidanceBundle(
            checklist = listOf("The outline has one purpose per section", "Links, names, claims and quotations are verified", "A final mobile reading pass checks rhythm and formatting"),
            tools = listOf("Outline", "Source / link check", "Proofreading pass"),
        ))
        CreatorProductionKind.HYBRID -> listOf(GuidanceBundle(
            checklist = listOf("Each production method has a clear job", "Audio / visual handoffs are planned before editing", "The piece still feels like one coherent experience"),
            tools = listOf("Hybrid production map", "Asset handoff checklist", "Continuity pass"),
        ))
    }

    private fun platformGuidance(platform: String, format: String): GuidanceBundle {
        val p = platform.trim().lowercase()
        val f = format.trim().lowercase()
        return when {
            p == "youtube" && f == "long-form" -> GuidanceBundle(
                checklist = listOf("Title and thumbnail communicate one shared promise", "First 30 seconds earn the deeper setup", "Description, chapters and links are ready before publish"),
                tools = listOf("Thumbnail + title variants", "Retention opening pass", "YouTube metadata checklist"),
                outputs = listOf("YouTube long-form", "Short / Reel derivative", "Community / social takeaway"),
            )
            p == "youtube" && f == "short" -> verticalShortGuidance("YouTube Short")
            p == "instagram" && f == "reel" -> verticalShortGuidance("Instagram Reel")
            p == "facebook" && f == "reel" -> verticalShortGuidance("Facebook Reel")
            p == "instagram" && f == "post" -> GuidanceBundle(
                checklist = listOf("The first frame works without the caption", "Carousel order has a clear visual hierarchy", "Caption adds context instead of repeating the artwork"),
                tools = listOf("Feed crop check", "Carousel sequence", "Caption / CTA pass"),
                outputs = listOf("Instagram post", "Story share"),
            )
            p == "podcast" -> GuidanceBundle(
                checklist = listOf("Episode title promises a specific conversation value", "Description includes useful context and links", "A clip-worthy moment is marked during edit"),
                tools = listOf("Episode metadata", "Chapter / timestamp notes", "Clip markers"),
                outputs = listOf("Podcast episode", "Podcast clip", "Quote / insight post"),
            )
            p == "blog / newsletter" -> GuidanceBundle(
                checklist = listOf("Subject / headline reflects the actual value", "Links and formatting are verified", "Opening paragraph earns the rest of the read"),
                tools = listOf("Headline variants", "Link check", "Mobile reading pass"),
                outputs = listOf("Article / newsletter", "Social excerpt"),
            )
            else -> GuidanceBundle()
        }
    }

    private fun verticalShortGuidance(label: String) = GuidanceBundle(
        checklist = listOf("The first 1–2 seconds establish the hook", "Important text stays inside vertical safe areas", "Captions are readable without covering the subject", "The ending lands cleanly or loops intentionally"),
        tools = listOf("9:16 safe-area check", "Caption pass", "Hook variants"),
        outputs = listOf(label, "Cross-platform vertical cut"),
    )

    private fun blueprint(
        workflow: List<CreatorBlueprintStep>,
        prompts: List<CreatorBlueprintPrompt>,
        script: List<CreatorBlueprintStep>,
        checklist: List<String>,
        tools: List<String>,
        outputs: List<String>,
    ) = IntentBlueprint(workflow, prompts, script, checklist, tools, outputs)

    private fun step(title: String, guidance: String) = CreatorBlueprintStep(title, guidance)
}
