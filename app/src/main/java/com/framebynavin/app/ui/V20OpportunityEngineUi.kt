package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    val analytics = remember(tasks, ideas) { analyticsStore.loadAny() }
    val snapshot = remember(tasks, ideas, analytics) {
        buildOpportunitySnapshot(context, tasks, ideas, analytics)
    }
    V20OpportunitySurface(snapshot) { primary ->
        when (primary.targetKind) {
            CreatorOpportunityTargetKind.INSIGHTS -> onOpenInsights()
            CreatorOpportunityTargetKind.IDEA_VAULT -> onOpenIdeaVault()
            CreatorOpportunityTargetKind.PROJECT -> onOpenProject(primary.targetId)
        }
    }
}

/** Read-only Opportunity Engine proof surface inside Insights until the Today route is wired. */
@Composable
internal fun V20OpportunityEngineInsightsCard(analytics: YouTubeAnalyticsSnapshot) {
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
    val snapshot = remember(local, analytics) {
        buildOpportunitySnapshot(context, local.tasks, local.ideas, analytics)
    }
    V20OpportunitySurface(snapshot, onAction = null)
}

private fun buildOpportunitySnapshot(
    context: android.content.Context,
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    analytics: YouTubeAnalyticsSnapshot?,
): CreatorOpportunitySnapshot {
    val pulse = YouTubePulseStore(context).build24HourReport()
    val youtubeAlerts = YouTubeOpportunityEngine.build(pulse, ideas)
    val performances = analytics?.let(YouTubeInsightEngine::videoPerformance).orEmpty()
    val performanceSignals = performances.take(5).map { performance ->
        CreatorPlatformOpportunitySignal(
            videoId = performance.video.videoId,
            title = performance.video.title,
            periodViews = performance.video.periodViews,
            baselineMultiple = performance.baselineMultiple,
            viewSharePercent = performance.viewSharePercent,
        )
    }
    val aiStore = CreatorAiReportStore(context)
    val aiSignals = performances.take(8).mapNotNull { performance ->
        aiStore.load(performance.video.videoId)?.let { report ->
            CreatorAiOpportunitySignal(
                videoId = performance.video.videoId,
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
    )
}

@Composable
private fun V20OpportunitySurface(
    snapshot: CreatorOpportunitySnapshot,
    onAction: ((CreatorOpportunity) -> Unit)?,
) {
    val primary = snapshot.primary ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF171412),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .34f)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).background(MutedGold.copy(alpha = .12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("OPPORTUNITY ENGINE", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                    Text("What should you do next?", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                Surface(shape = RoundedCornerShape(100.dp), color = CinemaSurfaceRaised) {
                    Text(
                        "${primary.confidence}%",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = if (primary.confidence >= 80) SuccessGreen else MutedGold,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(primary.kicker, color = RecRed, fontSize = 7.8.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
            Spacer(Modifier.height(4.dp))
            Text(primary.title, color = ProjectorIvory, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(primary.body, color = MutedText, fontSize = 9.4.sp, lineHeight = 14.sp)

            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                primary.evidence.map { it.source }.distinct().forEach { source ->
                    Surface(shape = RoundedCornerShape(100.dp), color = opportunitySourceColor(source).copy(alpha = .12f)) {
                        Text(
                            opportunitySourceLabel(source),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = opportunitySourceColor(source),
                            fontSize = 6.9.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            primary.evidence.take(2).forEach { evidence ->
                Text("• ${evidence.label}", color = MutedText, fontSize = 7.8.sp, lineHeight = 11.sp)
            }

            if (onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onAction(primary) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Text(primary.actionLabel, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }

            if (snapshot.alternatives.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("OTHER SIGNALS", color = MutedText, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                Spacer(Modifier.height(5.dp))
                snapshot.alternatives.forEach { alternative ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(5.dp).background(MutedGold, RoundedCornerShape(100.dp)))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            alternative.title,
                            modifier = Modifier.weight(1f),
                            color = ProjectorIvory.copy(alpha = .82f),
                            fontSize = 8.4.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("${alternative.confidence}%", color = MutedText, fontSize = 7.2.sp)
                    }
                }
            }

            Spacer(Modifier.height(9.dp))
            Text(
                if (snapshot.aiEvidenceUsed) "Local + YouTube evidence lead the ranking. A saved Gemini autopsy only strengthens an already-grounded signal."
                else "Built from your creator state and available platform evidence. Gemini is optional and not required.",
                color = MutedText.copy(alpha = .72f),
                fontSize = 6.9.sp,
                lineHeight = 10.sp,
            )
        }
    }
}

private fun opportunitySourceLabel(source: CreatorOpportunitySource): String = when (source) {
    CreatorOpportunitySource.LOCAL -> "YOUR DATA"
    CreatorOpportunitySource.YOUTUBE -> "YOUTUBE"
    CreatorOpportunitySource.GEMINI -> "GEMINI EVIDENCE"
}

private fun opportunitySourceColor(source: CreatorOpportunitySource): Color = when (source) {
    CreatorOpportunitySource.LOCAL -> ProjectorIvory
    CreatorOpportunitySource.YOUTUBE -> RecRed
    CreatorOpportunitySource.GEMINI -> MutedGold
}
