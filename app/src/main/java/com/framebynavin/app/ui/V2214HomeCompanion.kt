package com.framebynavin.app.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.framebynavin.app.data.*
import kotlinx.coroutines.delay

/**
 * V221.4: home-only, read-only decision wiring.
 * Not an overlay, background service, notification or replacement for Up Next.
 * The existing V217 guides and original Spark raster renderer remain in use.
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

    // Silence is a correct result. Do not add padding or an empty placeholder.
    if (!quietThisVisit && decision != null) {
        Spacer(Modifier.height(12.dp))
        V221CompanionSuggestionCard(
            decision = decision,
            identity = V144GuidePrefs.currentGuide,
            modifier = modifier,
            onAction = { current ->
                when (current.destination) {
                    V221CompanionDestination.REMINDER_HUB -> onOpenReminders()
                    V221CompanionDestination.IDEA_VAULT -> onOpenIdeas()
                    V221CompanionDestination.PROJECT -> {
                        V2214HomeCompanionPolicy.openableProjectId(current, tasks.toList())
                            ?.let(onOpenProject)
                    }
                    V221CompanionDestination.NONE -> Unit
                }
            },
            onDismiss = { current ->
                quietThisVisit = true
                if (store.dismiss(current.id, System.currentTimeMillis())) {
                    nowMillis = System.currentTimeMillis()
                    refreshVersion += 1
                }
            },
        )
    }
}
