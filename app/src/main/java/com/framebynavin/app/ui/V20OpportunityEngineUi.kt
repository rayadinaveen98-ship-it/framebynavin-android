package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class OpportunityLocalState(
    val tasks: List<CreatorTask> = emptyList(),
    val ideas: List<CreatorIdea> = emptyList(),
)

@Composable
internal fun V20OpportunityEngineCard(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onOpenIdeaVault: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val analyticsStore = remember { YouTubeAnalyticsStore(context) }
    val outcomeStore = remember { CreatorRecommendationOutcomeStore(context) }
    var learningRefresh by remember { mutableIntStateOf(0) }
    val analytics = remember(tasks, ideas) { analyticsStore.loadAny() }
    val performanceSignals = remember(analytics) { buildPerformanceSignals(analytics) }
    val outcomes = remember(tasks, analytics, learningRefresh) {
        outcomeStore.reconcile(tasks, performanceSignals)
    }
    val learningSummary = remember(outcomes) { CreatorRecommendationOutcomeEngine.summary(outcomes) }
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
        outcomeStore.recordAction(opportunity)
        learningRefresh += 1
        when (opportunity.targetKind) {
            CreatorOpportunityTargetKind.INSIGHTS -> onOpenInsights()
            CreatorOpportunityTargetKind.IDEA_VAULT -> onOpenIdeaVault()
            CreatorOpportunityTargetKind.PROJECT -> onOpenProject(opportunity.targetId)
        }
    }
}

/** Opportunity guidance belongs in Insights; Today stays focused on current work. */
@Composable
internal fun V20OpportunityEngineInsightsCard(analytics: YouTubeAnalyticsSnapshot, onCreateProject: (() -> Unit)? = null) {
    val context = LocalContext.current.applicationContext
    val local by produceState(
        initialValue = OpportunityLocalState(),
        key1 = context,
        key2 = analytics.fetchedAtMillis,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                CreatorDataGate.readyTransaction(context) {
                    OpportunityLocalState(
                        tasks = TaskStore(context).load(),
                        ideas = IdeaVaultStore(context).load(),
                    )
                }
            }.getOrDefault(OpportunityLocalState())
        }
    }
    val outcomeState = remember(local, analytics) {
        val store = CreatorRecommendationOutcomeStore(context)
        store.reconcile(local.tasks, buildPerformanceSignals(analytics))
    }
    val learningSummary = remember(outcomeState) { CreatorRecommendationOutcomeEngine.summary(outcomeState) }
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
        actionLabelOverride = if (onCreateProject != null) "START THIS MOVE" else null,
        onAction = onCreateProject?.let { create -> { _: CreatorOpportunity -> create() } },
    )
}

private fun buildPerformanceSignals(
    analytics: YouTubeAnalyticsSnapshot?,
): List<CreatorPlatformOpportunitySignal> {
    val performances = analytics?.let(YouTubeInsightEngine::videoPerformance).orEmpty()
    return performances.take(12).map { performance ->
        CreatorPlatformOpportunitySignal(
            videoId = performance.video.videoId,
            title = performance.video.title,
            periodViews = performance.video.periodViews,
            baselineMultiple = performance.baselineMultiple,
            viewSharePercent = performance.viewSharePercent,
        )
    }
}

private fun buildOpportunitySnapshot(
    context: android.content.Context,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    analytics: YouTubeAnalyticsSnapshot?,
    playbook: CreatorPlaybookSnapshot,
    brain: CreatorBrainSnapshot,
): CreatorOpportunitySnapshot {
    val pulse = YouTubePulseStore(context).build24HourReport()
    val youtubeAlerts = YouTubeOpportunityEngine.build(pulse, ideas)
    val performanceSignals = buildPerformanceSignals(analytics)
    val aiStore = CreatorAiReportStore(context)
    val aiSignals = performanceSignals.take(8).mapNotNull { performance ->
        aiStore.load(performance.videoId)?.let { report ->
            CreatorAiOpportunitySignal(
                videoId = performance.videoId,
                citedEvidenceCount = report.citedEvidenceIds.size,
            )
        }
    }
    return CreatorOpportunityEngine.build(
        tasks = tasks,
        ideas = ideas,
        youtubeAlerts = youtubeAlerts,
        performanceSignals = performanceSignals,
        aiSignals = aiSignals,
        playbook = playbook,
        brain = brain,
    )
}

@Composable
private fun V20OpportunitySurface(
    snapshot: CreatorOpportunitySnapshot,
    learningSummary: CreatorRecommendationLearningSummary,
    playbook: CreatorPlaybookSnapshot,
    brain: CreatorBrainSnapshot,
    brainGuidance: List<CreatorBrainRecommendation>,
    brainExplainability: CreatorBrainExplainabilitySnapshot,
    onBrainProject: ((String) -> Unit)?,
    onBrainControl: ((String, CreatorBrainLearningControl) -> Unit)?,
    actionLabelOverride: String? = null,
    onAction: ((CreatorOpportunity) -> Unit)?,
) {
    val primary = snapshot.now.firstOrNull() ?: snapshot.primary ?: return
    var expanded by remember(primary.id) { mutableStateOf(false) }

    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(14.dp)) {
            Text("Next Move", color = ProjectorIvory, fontSize = 15.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(primary.title, color = ProjectorIvory, fontSize = 13.sp,
                fontWeight = FontWeight.Medium, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            if (onAction != null) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onAction(primary) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed)) {
                    Text(actionLabelOverride ?: primary.actionLabel,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            V190Disclosure("Ideas & patterns", expanded) { expanded = !expanded }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                if (snapshot.now.size > 1)
                    V20HorizonList("Now", snapshot.now.drop(1), RecRed, onAction)
                if (snapshot.next.isNotEmpty())
                    V20HorizonList("Next", snapshot.next, MutedGold, onAction)
                if (snapshot.later.isNotEmpty())
                    V20HorizonList("Later", snapshot.later, MutedText, onAction)
                if (learningSummary.acted > 0) {
                    Spacer(Modifier.height(9.dp))
                    Text("${learningSummary.acted} tried · ${learningSummary.projectsCreated} projects · ${learningSummary.evaluated} reviewed",
                        color = MutedText, fontSize = 11.sp)
                }
                if (playbook.patterns.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Patterns", color = ProjectorIvory, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                    playbook.patterns.take(3).forEach { V20PlaybookPatternRow(it) }
                }
                if (brainGuidance.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Suggestions", color = ProjectorIvory, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                    brainGuidance.take(3).forEach {
                        V20CreatorBrainRecommendationRow(it, onBrainProject)
                    }
                }
                if (brainExplainability.explanations.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Pattern Controls", color = ProjectorIvory, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                    brainExplainability.explanations.take(4).forEach {
                        V20CreatorBrainExplanationRow(it, onBrainControl)
                    }
                }
                if (brain.patterns.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Learned Patterns", color = ProjectorIvory, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold)
                    brain.patterns.take(3).forEach { V20CreatorBrainPatternRow(it) }
                }
            }
        }
    }
}

@Composable
private fun V20DecisionScorecard(scorecard: CreatorOpportunityScorecard) {
    val dimensions = listOf(
        "URGENCY" to scorecard.urgency,
        "MOMENTUM" to scorecard.momentum,
        "READY" to scorecard.readiness,
        "EVIDENCE" to scorecard.evidenceStrength,
        "VALUE" to scorecard.strategicValue,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        dimensions.forEach { (label, value) ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = CinemaSurfaceRaised,
            ) {
                Column(Modifier.padding(horizontal = 5.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value.toString(), color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(label, color = MutedText, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun V20PlaybookPatternRow(pattern: CreatorPlaybookPattern) {
    val accent = when (pattern.state) {
        CreatorPlaybookState.PROVEN -> SuccessGreen
        CreatorPlaybookState.CAUTION -> RecRed
        CreatorPlaybookState.LEARNING -> MutedText
    }
    val stateLabel = when (pattern.state) {
        CreatorPlaybookState.PROVEN -> "PROVEN"
        CreatorPlaybookState.CAUTION -> "CAUTION"
        CreatorPlaybookState.LEARNING -> "LEARNING"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .12f)) {
            Text(stateLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(pattern.label, color = ProjectorIvory.copy(alpha = .86f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${pattern.positiveCount}/${pattern.evaluatedCount} positive · avg ${String.format("%.2f", pattern.averageBaselineMultiple)}× baseline", color = MutedText, fontSize = 11.sp)
        }
        if (pattern.rankingDelta != 0) {
            Text(
                if (pattern.rankingDelta > 0) "+${pattern.rankingDelta}" else pattern.rankingDelta.toString(),
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun V20CreatorBrainRecommendationRow(
    recommendation: CreatorBrainRecommendation,
    onOpenProject: ((String) -> Unit)?,
) {
    val accent = when (recommendation.kind) {
        CreatorBrainRecommendationKind.LEAN_IN -> SuccessGreen
        CreatorBrainRecommendationKind.TEST_MORE -> MutedGold
        CreatorBrainRecommendationKind.WATCH -> MutedGold
        CreatorBrainRecommendationKind.AVOID_FOR_NOW -> RecRed
        CreatorBrainRecommendationKind.REFRESH_EVIDENCE -> MutedText
    }
    val label = when (recommendation.kind) {
        CreatorBrainRecommendationKind.LEAN_IN -> "LEAN IN"
        CreatorBrainRecommendationKind.TEST_MORE -> "TEST"
        CreatorBrainRecommendationKind.WATCH -> "WATCH"
        CreatorBrainRecommendationKind.AVOID_FOR_NOW -> "REWORK"
        CreatorBrainRecommendationKind.REFRESH_EVIDENCE -> "REFRESH"
    }
    val clickable = recommendation.taskId.isNotBlank() && onOpenProject != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = clickable) { onOpenProject?.invoke(recommendation.taskId) }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .12f)) {
            Text(label, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(recommendation.title, color = ProjectorIvory.copy(alpha = .9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(recommendation.body, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("${recommendation.confidence}% evidence confidence · ${recommendation.patternIds.size} Brain pattern${if (recommendation.patternIds.size == 1) "" else "s"}", color = accent.copy(alpha = .86f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (clickable) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Outlined.ArrowForward, null, tint = accent, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Spacer(Modifier.width(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(explanation.label, color = ProjectorIvory.copy(alpha = .9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${explanation.sampleSize} evaluated · ${explanation.positiveRatePercent}% positive · ${explanation.freshness.name.lowercase()} · ${explanation.trajectory.name.lowercase()}",
                        color = MutedText,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    if (explanation.effectiveRankingDelta > 0) "+${explanation.effectiveRankingDelta}" else explanation.effectiveRankingDelta.toString(),
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(7.dp))
                Text(explanation.why, color = ProjectorIvory.copy(alpha = .82f), fontSize = 11.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text(explanation.freshnessReason, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(3.dp))
                Text(explanation.trajectoryReason, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)
                if (explanation.evidence.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("WHAT TAUGHT THE BRAIN", color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
                    Spacer(Modifier.height(3.dp))
                    explanation.evidence.take(3).forEach { evidence ->
                        Text(
                            "• ${evidence.title} · ${evidence.verdict.name.lowercase()} · ${String.format("%.2f", evidence.baselineMultiple)}× baseline${if (evidence.videoId.isBlank()) "" else " · ${evidence.videoId}"}",
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
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
                            ) { Text("PAUSE", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                            OutlinedButton(
                                onClick = { onControl(explanation.patternId, CreatorBrainLearningControl.RETIRED) },
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
                            ) { Text("RETIRE", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                        } else {
                            OutlinedButton(
                                onClick = { onControl(explanation.patternId, CreatorBrainLearningControl.ACTIVE) },
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
                            ) { Text("RESTORE", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                }
            }
        }
    }
}

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
            Text(stateLabel, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(pattern.label, color = ProjectorIvory.copy(alpha = .86f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${pattern.positiveCount}/${pattern.evaluatedCount} positive · avg ${String.format("%.2f", pattern.averageBaselineMultiple)}× baseline", color = MutedText, fontSize = 11.sp)
        }
        if (pattern.rankingDelta != 0) {
            Text(
                if (pattern.rankingDelta > 0) "+${pattern.rankingDelta}" else pattern.rankingDelta.toString(),
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun V20HorizonList(
    label: String,
    opportunities: List<CreatorOpportunity>,
    accent: Color,
    onAction: ((CreatorOpportunity) -> Unit)?,
) {
    Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
    Spacer(Modifier.height(5.dp))
    opportunities.take(2).forEach { opportunity ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onAction != null) { onAction?.invoke(opportunity) }
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(5.dp).background(accent, RoundedCornerShape(100.dp)))
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    opportunity.title,
                    color = ProjectorIvory.copy(alpha = .88f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Decision ${opportunity.scorecard.weightedScore} · ${opportunity.evidence.size} evidence",
                    color = MutedText,
                    fontSize = 11.sp,
                )
            }
            if (onAction != null) {
                Icon(Icons.Outlined.ArrowForward, null, tint = accent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

private fun opportunitySourceLabel(source: CreatorOpportunitySource): String = when (source) {
    CreatorOpportunitySource.LOCAL -> "YOUR DATA"
    CreatorOpportunitySource.YOUTUBE -> "YOUTUBE"
    CreatorOpportunitySource.GEMINI -> "GEMINI EVIDENCE"
    CreatorOpportunitySource.PLAYBOOK -> "PLAYBOOK"
    CreatorOpportunitySource.CREATOR_BRAIN -> "CREATOR BRAIN"
}

private fun opportunitySourceColor(source: CreatorOpportunitySource): Color = when (source) {
    CreatorOpportunitySource.LOCAL -> ProjectorIvory
    CreatorOpportunitySource.YOUTUBE -> RecRed
    CreatorOpportunitySource.GEMINI -> MutedGold
    CreatorOpportunitySource.PLAYBOOK -> SuccessGreen
    CreatorOpportunitySource.CREATOR_BRAIN -> MutedGold
}
