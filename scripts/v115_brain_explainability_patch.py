from pathlib import Path

path = Path('app/src/main/java/com/framebynavin/app/ui/V20OpportunityEngineUi.kt')
text = path.read_text(encoding='utf-8')

old_today = '''    val learningSummary = remember(outcomes) { CreatorRecommendationOutcomeEngine.summary(outcomes) }
    val playbook = remember(outcomes) { CreatorPlaybookEngine.build(outcomes) }
    val brain = remember(outcomes, tasks) { CreatorBrainEngine.build(outcomes, tasks) }
    val brainGuidance = remember(brain, tasks) { CreatorBrainRecommendationEngine.build(brain, tasks) }
    val snapshot = remember(tasks, ideas, analytics, playbook, brain) {
        buildOpportunitySnapshot(context, tasks, ideas, analytics, playbook, brain)
    }
    V20OpportunitySurface(snapshot, learningSummary, playbook, brain, brainGuidance, onOpenProject) { opportunity ->
'''
new_today = '''    val learningSummary = remember(outcomes) { CreatorRecommendationOutcomeEngine.summary(outcomes) }
    val playbook = remember(outcomes) { CreatorPlaybookEngine.build(outcomes) }
    val brainControlStore = remember { CreatorBrainLearningControlStore(context) }
    var brainControlRefresh by remember { mutableIntStateOf(0) }
    val rawBrain = remember(outcomes, tasks) { CreatorBrainEngine.build(outcomes, tasks) }
    val brainControls = remember(brainControlRefresh) { brainControlStore.load() }
    val brain = remember(rawBrain, brainControls) { CreatorBrainLearningControlEngine.apply(rawBrain, brainControls) }
    val brainGuidance = remember(brain, tasks) { CreatorBrainRecommendationEngine.build(brain, tasks) }
    val brainExplainability = remember(rawBrain, outcomes, tasks, brainControls) {
        CreatorBrainExplainabilityEngine.build(rawBrain, outcomes, tasks, brainControls)
    }
    val snapshot = remember(tasks, ideas, analytics, playbook, brain) {
        buildOpportunitySnapshot(context, tasks, ideas, analytics, playbook, brain)
    }
    V20OpportunitySurface(
        snapshot = snapshot,
        learningSummary = learningSummary,
        playbook = playbook,
        brain = brain,
        brainGuidance = brainGuidance,
        brainExplainability = brainExplainability,
        onBrainProject = onOpenProject,
        onBrainControl = { patternId, control ->
            brainControlStore.set(patternId, control)
            brainControlRefresh += 1
        },
    ) { opportunity ->
'''
if old_today not in text:
    raise SystemExit('today anchor not found')
text = text.replace(old_today, new_today, 1)

old_insights = '''    val learningSummary = remember(outcomeState) { CreatorRecommendationOutcomeEngine.summary(outcomeState) }
    val playbook = remember(outcomeState) { CreatorPlaybookEngine.build(outcomeState) }
    val brain = remember(outcomeState, local.tasks) { CreatorBrainEngine.build(outcomeState, local.tasks) }
    val brainGuidance = remember(brain, local.tasks) { CreatorBrainRecommendationEngine.build(brain, local.tasks) }
    val snapshot = remember(local, analytics, playbook, brain) {
        buildOpportunitySnapshot(context, local.tasks, local.ideas, analytics, playbook, brain)
    }
    V20OpportunitySurface(snapshot, learningSummary, playbook, brain, brainGuidance, onBrainProject = null, onAction = null)
'''
new_insights = '''    val learningSummary = remember(outcomeState) { CreatorRecommendationOutcomeEngine.summary(outcomeState) }
    val playbook = remember(outcomeState) { CreatorPlaybookEngine.build(outcomeState) }
    val rawBrain = remember(outcomeState, local.tasks) { CreatorBrainEngine.build(outcomeState, local.tasks) }
    val brainControls = remember(local, analytics) { CreatorBrainLearningControlStore(context).load() }
    val brain = remember(rawBrain, brainControls) { CreatorBrainLearningControlEngine.apply(rawBrain, brainControls) }
    val brainGuidance = remember(brain, local.tasks) { CreatorBrainRecommendationEngine.build(brain, local.tasks) }
    val brainExplainability = remember(rawBrain, outcomeState, local.tasks, brainControls) {
        CreatorBrainExplainabilityEngine.build(rawBrain, outcomeState, local.tasks, brainControls)
    }
    val snapshot = remember(local, analytics, playbook, brain) {
        buildOpportunitySnapshot(context, local.tasks, local.ideas, analytics, playbook, brain)
    }
    V20OpportunitySurface(
        snapshot = snapshot,
        learningSummary = learningSummary,
        playbook = playbook,
        brain = brain,
        brainGuidance = brainGuidance,
        brainExplainability = brainExplainability,
        onBrainProject = null,
        onBrainControl = null,
        onAction = null,
    )
'''
if old_insights not in text:
    raise SystemExit('insights anchor not found')
text = text.replace(old_insights, new_insights, 1)

old_sig = '''private fun V20OpportunitySurface(
    snapshot: CreatorOpportunitySnapshot,
    learningSummary: CreatorRecommendationLearningSummary,
    playbook: CreatorPlaybookSnapshot,
    brain: CreatorBrainSnapshot,
    brainGuidance: List<CreatorBrainRecommendation>,
    onBrainProject: ((String) -> Unit)?,
    onAction: ((CreatorOpportunity) -> Unit)?,
) {
'''
new_sig = '''private fun V20OpportunitySurface(
    snapshot: CreatorOpportunitySnapshot,
    learningSummary: CreatorRecommendationLearningSummary,
    playbook: CreatorPlaybookSnapshot,
    brain: CreatorBrainSnapshot,
    brainGuidance: List<CreatorBrainRecommendation>,
    brainExplainability: CreatorBrainExplainabilitySnapshot,
    onBrainProject: ((String) -> Unit)?,
    onBrainControl: ((String, CreatorBrainLearningControl) -> Unit)?,
    onAction: ((CreatorOpportunity) -> Unit)?,
) {
'''
if old_sig not in text:
    raise SystemExit('surface signature anchor not found')
text = text.replace(old_sig, new_sig, 1)

memory_anchor = '''            if (brain.patterns.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("CREATOR BRAIN · MEMORY", color = MutedGold, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
'''
explain_block = '''            if (brainExplainability.explanations.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MutedGold.copy(alpha = .14f))
                Spacer(Modifier.height(10.dp))
                Text("CREATOR BRAIN · EXPLAINABILITY", color = MutedGold, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${brainExplainability.activeCount} active · ${brainExplainability.pausedCount} paused · ${brainExplainability.retiredCount} retired",
                    color = ProjectorIvory.copy(alpha = .82f),
                    fontSize = 7.2.sp,
                )
                Spacer(Modifier.height(5.dp))
                brainExplainability.explanations.take(4).forEach { explanation ->
                    V20CreatorBrainExplanationRow(explanation, onBrainControl)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "Pause or retire changes Brain influence only. Historical outcomes stay intact, and RESTORE re-enables the original time-aware evidence.",
                    color = MutedText.copy(alpha = .78f),
                    fontSize = 6.8.sp,
                    lineHeight = 10.sp,
                )
            }

'''
if memory_anchor not in text:
    raise SystemExit('memory anchor not found')
text = text.replace(memory_anchor, explain_block + memory_anchor, 1)

row_anchor = '''@Composable
private fun V20CreatorBrainPatternRow(pattern: CreatorBrainPattern) {
'''
explain_row = '''@Composable
private fun V20CreatorBrainExplanationRow(
    explanation: CreatorBrainPatternExplanation,
    onControl: ((String, CreatorBrainLearningControl) -> Unit)?,
) {
    var expanded by remember(explanation.patternId) { mutableStateOf(false) }
    val accent = when (explanation.control) {
        CreatorBrainLearningControl.PAUSED -> MutedGold
        CreatorBrainLearningControl.RETIRED -> RecRed
        CreatorBrainLearningControl.ACTIVE -> when (explanation.state) {
            CreatorBrainPatternState.PROVEN -> SuccessGreen
            CreatorBrainPatternState.EMERGING -> MutedGold
            CreatorBrainPatternState.CAUTION -> RecRed
            CreatorBrainPatternState.LEARNING -> MutedText
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        color = CinemaSurfaceRaised,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 9.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .12f)) {
                    Text(
                        if (explanation.control == CreatorBrainLearningControl.ACTIVE) explanation.state.name else explanation.control.name,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        color = accent,
                        fontSize = 5.7.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(explanation.label, color = ProjectorIvory.copy(alpha = .9f), fontSize = 7.9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${explanation.sampleSize} evaluated · ${explanation.positiveRatePercent}% positive · ${explanation.freshness.name.lowercase()} · ${explanation.trajectory.name.lowercase()}",
                        color = MutedText,
                        fontSize = 6.3.sp,
                    )
                }
                Text(
                    if (explanation.effectiveRankingDelta > 0) "+${explanation.effectiveRankingDelta}" else explanation.effectiveRankingDelta.toString(),
                    color = accent,
                    fontSize = 7.1.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(7.dp))
                Text(explanation.why, color = ProjectorIvory.copy(alpha = .82f), fontSize = 6.8.sp, lineHeight = 10.sp)
                Spacer(Modifier.height(4.dp))
                Text(explanation.freshnessReason, color = MutedText, fontSize = 6.4.sp, lineHeight = 9.5.sp)
                Spacer(Modifier.height(3.dp))
                Text(explanation.trajectoryReason, color = MutedText, fontSize = 6.4.sp, lineHeight = 9.5.sp)
                if (explanation.evidence.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("WHAT TAUGHT THE BRAIN", color = MutedGold, fontSize = 5.8.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
                    Spacer(Modifier.height(3.dp))
                    explanation.evidence.take(3).forEach { evidence ->
                        Text(
                            "• ${evidence.title} · ${evidence.verdict.name.lowercase()} · ${String.format("%.2f", evidence.baselineMultiple)}× baseline${if (evidence.videoId.isBlank()) "" else " · ${evidence.videoId}"}",
                            color = MutedText,
                            fontSize = 6.2.sp,
                            lineHeight = 9.sp,
                        )
                    }
                }
                if (onControl != null) {
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (explanation.control == CreatorBrainLearningControl.ACTIVE) {
                            OutlinedButton(
                                onClick = { onControl(explanation.patternId, CreatorBrainLearningControl.PAUSED) },
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
                            ) { Text("PAUSE", fontSize = 6.2.sp, fontWeight = FontWeight.Black) }
                            OutlinedButton(
                                onClick = { onControl(explanation.patternId, CreatorBrainLearningControl.RETIRED) },
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
                            ) { Text("RETIRE", fontSize = 6.2.sp, fontWeight = FontWeight.Black) }
                        } else {
                            OutlinedButton(
                                onClick = { onControl(explanation.patternId, CreatorBrainLearningControl.ACTIVE) },
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
                            ) { Text("RESTORE", fontSize = 6.2.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
    }
}

'''
if row_anchor not in text:
    raise SystemExit('pattern row anchor not found')
text = text.replace(row_anchor, explain_row + row_anchor, 1)

path.write_text(text, encoding='utf-8')
