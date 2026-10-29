package com.framebynavin.app.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.framebynavin.app.data.*
import kotlinx.coroutines.delay

/**
 * V222.1: the same V221.4 evidence/dismissal/action policy, now hosted inside
 * a 48dp Home-header portrait and a tap-to-open sheet instead of a tall inline
 * card below the creator's actual work. No new evidence, inferred delivery,
 * background service, notification or reminder mutations.
 */
internal object V2214HomeCompanionPolicy {
    fun unorganizedCount(ideas: List<CreatorIdea>): Int = ideas.count {
        !it.isTrashed && it.status == IdeaStatus.INBOX && it.projectTaskId.isBlank()
    }

    /** Never route a stale suggestion to a deleted/finished/archived project. */
    fun openableProjectId(
        decision: V221CompanionDecision,
        tasks: List<CreatorTask>,
    ): String? {
        if (decision.destination != V221CompanionDestination.PROJECT) return null
        return decision.taskId?.takeIf { id ->
            id.isNotBlank() && tasks.any { task ->
                task.id == id && task.archivedAtMillis <= 0L &&
                    (task.status == TaskStatus.PLANNED || task.status == TaskStatus.WORKING)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun V2214HomeCompanion(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onOpenReminders: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenIdeas: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val store = remember(context) { V221CompanionDismissalStore(context.applicationContext) }
    val evidence = remember(context) { V221CompanionEvidenceReader(context.applicationContext) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    // Not now must close the card for this Home visit, even if another lower
    // priority candidate exists. Persisted dismissal guards future visits.
    var quietThisVisit by remember { mutableStateOf(false) }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = (context as? ComponentActivity)?.lifecycle

    // Recheck when returning from a reminder, another app, or a deep link.
    // No polling or background process when Home is not on screen.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                nowMillis = System.currentTimeMillis()
                refreshVersion += 1
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    // Reminder acknowledgement can change without any change to the task list.
    // Refresh only while this Home component is composed, never in the background.
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            nowMillis = System.currentTimeMillis()
            refreshVersion += 1
        }
    }

    // Materialize snapshots from actual observable VM data so Home refreshes
    // after saves, changes, deletions, restores and idea status transitions.
    val taskSnapshot = tasks.toList()
    val ideaSnapshot = ideas.toList()
    val decision = remember(taskSnapshot, ideaSnapshot, nowMillis, refreshVersion, enabled) {
        runCatching {
            val contextSnapshot = evidence.snapshot(
                tasks = taskSnapshot,
                nowMillis = nowMillis,
                unorganizedIdeaCount = V2214HomeCompanionPolicy.unorganizedCount(ideaSnapshot),
            )
            V221CompanionBehaviorPolicy.select(
                context = contextSnapshot,
                enabled = enabled && store.enabled(),
                dismissals = store.records(),
            )
        }.getOrNull() // Fail closed when evidence/preferences cannot be read.
    }

    // The actual settings preference owns visibility; off means no avatar,
    // sheet or unnecessary suggested-action affordance on Home.
    if (!enabled || !store.enabled()) return

    val identity = V144GuidePrefs.currentGuide
    V2221CompanionPortrait(
        guide = identity,
        hasSuggestion = decision != null && !quietThisVisit,
        onClick = { sheetOpen = true },
        modifier = modifier,
    )

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            containerColor = CinemaSurfaceRaised,
            modifier = Modifier.testTag("v2221-companion-sheet"),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Suggestion card already shows Spark and the next action; no duplicate header.
                // A suppressed/unavailable decision must never be replaced by
                // fabricated project or reminder evidence.
                val visibleDecision = decision?.takeUnless { quietThisVisit }
                if (visibleDecision != null) {
                    V221CompanionSuggestionCard(
                        decision = visibleDecision,
                        identity = identity,
                        onAction = { current ->
                            sheetOpen = false
                            when (current.destination) {
                                V221CompanionDestination.REMINDER_HUB -> onOpenReminders()
                                V221CompanionDestination.IDEA_VAULT -> onOpenIdeas()
                                V221CompanionDestination.PROJECT ->
                                    V2214HomeCompanionPolicy.openableProjectId(
                                        current, tasks.toList(),
                                    )?.let(onOpenProject)
                                V221CompanionDestination.NONE -> Unit
                            }
                        },
                        onDismiss = { current ->
                            sheetOpen = false
                            quietThisVisit = true
                            if (store.dismiss(current.id, System.currentTimeMillis())) {
                                nowMillis = System.currentTimeMillis()
                                refreshVersion += 1
                            }
                        },
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .testTag("v2221-companion-empty")
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        V144GuideCharacter(
                            guide = identity,
                            state = CinePulseState.REST,
                            modifier = Modifier.size(78.dp),
                        )
                        Text("Nothing needs your attention right now.",
                            color = MutedText, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/** Approved character artwork, rendered static in a 72dp circular Home control.
 * Neither Spark nor the three approved V217 character illustrations are redrawn.
 */
@Composable
internal fun V2221CompanionPortrait(
    guide: V144GuideIdentity,
    hasSuggestion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawable = if (guide == V144GuideIdentity.FUNNY) {
        v144GuideDrawable(guide, CinePulseState.IDLE)
    } else {
        v217ConceptDrawable(guide)
    }
    Surface(
        onClick = onClick,
        modifier = modifier.size(72.dp)
            .testTag("v2221-companion-avatar")
            .semantics { contentDescription = "Open ${guide.displayName} companion" },
        shape = CircleShape,
        color = CinemaSurfaceRaised,
        border = BorderStroke(if (hasSuggestion) 1.7.dp else 1.dp,
            if (hasSuggestion) MutedGold else CinemaLine),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(drawable),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(64.5.dp).clip(CircleShape),
            )
        }
    }
}
