from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f"Missing patch anchor: {label}")
    return text.replace(old, new, 1)


engine_path = Path("app/src/main/java/com/framebynavin/app/data/CreatorOpportunityEngine.kt")
engine = engine_path.read_text()
engine = replace_once(
    engine,
    "enum class CreatorOpportunitySource { LOCAL, YOUTUBE, GEMINI, PLAYBOOK }",
    "enum class CreatorOpportunitySource { LOCAL, YOUTUBE, GEMINI, PLAYBOOK, CREATOR_BRAIN }",
    "brain source",
)
engine = replace_once(
    engine,
    "    val usesPlaybookEvidence: Boolean get() = evidence.any { it.source == CreatorOpportunitySource.PLAYBOOK }\n}",
    "    val usesPlaybookEvidence: Boolean get() = evidence.any { it.source == CreatorOpportunitySource.PLAYBOOK }\n    val usesCreatorBrainEvidence: Boolean get() = evidence.any { it.source == CreatorOpportunitySource.CREATOR_BRAIN }\n}",
    "brain opportunity flag",
)
engine = replace_once(
    engine,
    "    val playbookEvidenceUsed: Boolean get() = visible.any { it.usesPlaybookEvidence }\n    val evidenceSourceCount: Int get() = visible",
    "    val playbookEvidenceUsed: Boolean get() = visible.any { it.usesPlaybookEvidence }\n    val creatorBrainEvidenceUsed: Boolean get() = visible.any { it.usesCreatorBrainEvidence }\n    val evidenceSourceCount: Int get() = visible",
    "brain snapshot flag",
)
engine = replace_once(
    engine,
    " * Alpha 1.3D adds conservative outcome-weighting from the Creator Playbook. A single result never\n * changes ranking, positive history requires at least two evaluated outcomes, and negative history\n * requires at least three. The small playbook delta cannot change an opportunity's NOW/NEXT/LATER\n * horizon; live urgency, readiness and platform evidence stay dominant.",
    " * Alpha 1.4A adds the first Creator Brain memory across Content DNA, content type, platform, hook\n * style and workflow pace. Brain patterns require repeated evaluated outcomes and only add a tightly\n * capped adjustment to recommendations that can be linked to a real project. Current urgency,\n * readiness, YouTube evidence and the broader Creator Playbook remain dominant.",
    "brain docs",
)
engine = replace_once(
    engine,
    "        playbook: CreatorPlaybookSnapshot = CreatorPlaybookSnapshot(),\n        nowMillis: Long = System.currentTimeMillis(),",
    "        playbook: CreatorPlaybookSnapshot = CreatorPlaybookSnapshot(),\n        brain: CreatorBrainSnapshot = CreatorBrainSnapshot(),\n        nowMillis: Long = System.currentTimeMillis(),",
    "brain build arg",
)
engine = replace_once(
    engine,
    "                    targetId = signal.videoId,\n                    videoId = signal.videoId,\n                    confidence = if (signal.baselineMultiple >= 1.5) 86 else 78,",
    "                    targetId = signal.videoId,\n                    taskId = tasks.firstOrNull { task ->\n                        CreatorRecommendationOutcomeEngine.extractYouTubeVideoId(task.publishedUrl) == signal.videoId\n                    }?.id,\n                    videoId = signal.videoId,\n                    confidence = if (signal.baselineMultiple >= 1.5) 86 else 78,",
    "video to project link",
)
brain_weighting = '''        val brainWeighted = outcomeWeighted.map { opportunity ->
            val linkedTask = opportunity.taskId?.let { id -> tasks.firstOrNull { it.id == id } }
                ?: opportunity.videoId?.let { videoId ->
                    tasks.firstOrNull { task ->
                        CreatorRecommendationOutcomeEngine.extractYouTubeVideoId(task.publishedUrl) == videoId
                    }
                }
                ?: return@map opportunity
            val match = CreatorBrainEngine.matchForTask(linkedTask, brain)
            if (match.rankingDelta == 0 || match.patterns.isEmpty()) return@map opportunity

            val positive = match.rankingDelta > 0
            val strategicDelta = (match.rankingDelta / 2).coerceIn(-7, 8)
            opportunity.copy(
                confidence = if (positive) {
                    (opportunity.confidence + 1).coerceAtMost(95)
                } else {
                    (opportunity.confidence - 1).coerceAtLeast(45)
                },
                score = opportunity.score + match.rankingDelta,
                scorecard = opportunity.scorecard.copy(
                    strategicValue = (opportunity.scorecard.strategicValue + strategicDelta).coerceIn(0, 100),
                ),
                evidence = opportunity.evidence + match.patterns.map { pattern ->
                    CreatorOpportunityEvidence(
                        id = "brain.${pattern.id}",
                        label = "Creator Brain ${pattern.state.name.lowercase()} · ${pattern.label} · ${pattern.positiveCount}/${pattern.evaluatedCount} positive",
                        source = CreatorOpportunitySource.CREATOR_BRAIN,
                    )
                },
            )
        }

        val ranked = brainWeighted'''
engine = replace_once(
    engine,
    "        val ranked = outcomeWeighted",
    brain_weighting,
    "brain weighting",
)
engine_path.write_text(engine)


ui_path = Path("app/src/main/java/com/framebynavin/app/ui/V20OpportunityEngineUi.kt")
ui = ui_path.read_text()
ui = replace_once(
    ui,
    "    val playbook = remember(outcomes) { CreatorPlaybookEngine.build(outcomes) }\n    val snapshot = remember(tasks, ideas, analytics, playbook) {\n        buildOpportunitySnapshot(context, tasks, ideas, analytics, playbook)\n    }\n    V20OpportunitySurface(snapshot, learningSummary, playbook) { opportunity ->",
    "    val playbook = remember(outcomes) { CreatorPlaybookEngine.build(outcomes) }\n    val brain = remember(outcomes, tasks) { CreatorBrainEngine.build(outcomes, tasks) }\n    val snapshot = remember(tasks, ideas, analytics, playbook, brain) {\n        buildOpportunitySnapshot(context, tasks, ideas, analytics, playbook, brain)\n    }\n    V20OpportunitySurface(snapshot, learningSummary, playbook, brain) { opportunity ->",
    "today brain wiring",
)
ui = replace_once(
    ui,
    "    val playbook = remember(outcomeState) { CreatorPlaybookEngine.build(outcomeState) }\n    val snapshot = remember(local, analytics, playbook) {\n        buildOpportunitySnapshot(context, local.tasks, local.ideas, analytics, playbook)\n    }\n    V20OpportunitySurface(snapshot, learningSummary, playbook, onAction = null)",
    "    val playbook = remember(outcomeState) { CreatorPlaybookEngine.build(outcomeState) }\n    val brain = remember(outcomeState, local.tasks) { CreatorBrainEngine.build(outcomeState, local.tasks) }\n    val snapshot = remember(local, analytics, playbook, brain) {\n        buildOpportunitySnapshot(context, local.tasks, local.ideas, analytics, playbook, brain)\n    }\n    V20OpportunitySurface(snapshot, learningSummary, playbook, brain, onAction = null)",
    "insights brain wiring",
)
ui = replace_once(
    ui,
    "    analytics: YouTubeAnalyticsSnapshot?,\n    playbook: CreatorPlaybookSnapshot,\n): CreatorOpportunitySnapshot {",
    "    analytics: YouTubeAnalyticsSnapshot?,\n    playbook: CreatorPlaybookSnapshot,\n    brain: CreatorBrainSnapshot,\n): CreatorOpportunitySnapshot {",
    "snapshot brain arg",
)
ui = replace_once(
    ui,
    "        aiSignals = aiSignals,\n        playbook = playbook,\n    )",
    "        aiSignals = aiSignals,\n        playbook = playbook,\n        brain = brain,\n    )",
    "engine brain pass",
)
ui = replace_once(
    ui,
    "    learningSummary: CreatorRecommendationLearningSummary,\n    playbook: CreatorPlaybookSnapshot,\n    onAction: ((CreatorOpportunity) -> Unit)?,",
    "    learningSummary: CreatorRecommendationLearningSummary,\n    playbook: CreatorPlaybookSnapshot,\n    brain: CreatorBrainSnapshot,\n    onAction: ((CreatorOpportunity) -> Unit)?,",
    "surface brain arg",
)
brain_ui = '''            if (brain.patterns.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("CREATOR BRAIN · EARLY MEMORY", color = MutedGold, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                Spacer(Modifier.height(5.dp))
                brain.patterns.take(4).forEach { pattern ->
                    V20CreatorBrainPatternRow(pattern)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "Creator Brain learns Content DNA, content type, platform, hook style and workflow patterns only from evaluated outcomes. Three results can become Emerging; four are required for Proven or Caution.",
                    color = MutedText.copy(alpha = .78f),
                    fontSize = 6.8.sp,
                    lineHeight = 10.sp,
                )
            }

'''
ui = replace_once(
    ui,
    "            Spacer(Modifier.height(10.dp))\n            Text(\n                when {",
    brain_ui + "            Spacer(Modifier.height(10.dp))\n            Text(\n                when {",
    "brain UI section",
)
ui = replace_once(
    ui,
    "                when {\n                    snapshot.playbookEvidenceUsed && snapshot.aiEvidenceUsed -> \"Ranking is led by current creator + YouTube evidence, then lightly informed by repeated outcome history and optional Gemini evidence.\"",
    "                when {\n                    snapshot.creatorBrainEvidenceUsed -> \"Current evidence stays dominant. Repeated Creator Brain patterns add only a small, capped adjustment when a recommendation matches a real project.\"\n                    snapshot.playbookEvidenceUsed && snapshot.aiEvidenceUsed -> \"Ranking is led by current creator + YouTube evidence, then lightly informed by repeated outcome history and optional Gemini evidence.\"",
    "brain ranking note",
)
brain_row = '''
@Composable
private fun V20CreatorBrainPatternRow(pattern: CreatorBrainPattern) {
    val accent = when (pattern.state) {
        CreatorBrainPatternState.PROVEN -> SuccessGreen
        CreatorBrainPatternState.EMERGING -> MutedGold
        CreatorBrainPatternState.CAUTION -> RecRed
        CreatorBrainPatternState.LEARNING -> MutedText
    }
    val stateLabel = pattern.state.name
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .12f)) {
            Text(stateLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = accent, fontSize = 5.7.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(pattern.label, color = ProjectorIvory.copy(alpha = .86f), fontSize = 7.8.sp, fontWeight = FontWeight.Bold)
            Text("${pattern.positiveCount}/${pattern.evaluatedCount} positive · avg ${String.format(\"%.2f\", pattern.averageBaselineMultiple)}× baseline", color = MutedText, fontSize = 6.4.sp)
        }
        if (pattern.rankingDelta != 0) {
            Text(
                if (pattern.rankingDelta > 0) "+${pattern.rankingDelta}" else pattern.rankingDelta.toString(),
                color = accent,
                fontSize = 7.2.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
'''
ui = replace_once(
    ui,
    "\n@Composable\nprivate fun V20HorizonList(",
    brain_row + "\n@Composable\nprivate fun V20HorizonList(",
    "brain row",
)
ui = replace_once(
    ui,
    "    CreatorOpportunitySource.PLAYBOOK -> \"PLAYBOOK\"\n}",
    "    CreatorOpportunitySource.PLAYBOOK -> \"PLAYBOOK\"\n    CreatorOpportunitySource.CREATOR_BRAIN -> \"CREATOR BRAIN\"\n}",
    "brain source label",
)
ui = replace_once(
    ui,
    "    CreatorOpportunitySource.PLAYBOOK -> SuccessGreen\n}",
    "    CreatorOpportunitySource.PLAYBOOK -> SuccessGreen\n    CreatorOpportunitySource.CREATOR_BRAIN -> MutedGold\n}",
    "brain source color",
)
ui_path.write_text(ui)


build_path = Path("app/build.gradle.kts")
build = build_path.read_text()
build = build.replace("versionCode = 109", "versionCode = 111", 1)
build = build.replace(
    'versionName = "2.0.0-alpha1.3c-recommendation-outcome-learning"',
    'versionName = "2.0.0-alpha1.4a-creator-brain-foundation"',
    1,
)
build_path.write_text(build)
