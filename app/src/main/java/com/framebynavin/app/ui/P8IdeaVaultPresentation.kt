package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaStatus
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/** Pure UI counts from the existing Idea Vault list; no analytics or AI claims. */
internal data class P8IdeaVaultSummary(
    val total: Int,
    val inbox: Int,
    val exploring: Int,
    val ready: Int,
    val converted: Int,
    val archived: Int,
) {
    companion object {
        fun from(ideas: List<CreatorIdea>): P8IdeaVaultSummary =
            P8IdeaVaultSummary(
                total = ideas.size,
                inbox = ideas.count { it.status == IdeaStatus.INBOX },
                exploring = ideas.count {
                    it.status == IdeaStatus.WORTH_EXPLORING ||
                        it.status == IdeaStatus.RESEARCHING
                },
                ready = ideas.count { it.status == IdeaStatus.READY_TO_PRODUCE },
                converted = ideas.count { it.status == IdeaStatus.CONVERTED },
                archived = ideas.count { it.status == IdeaStatus.ARCHIVED },
            )
    }
}

internal enum class P8IdeaVaultEmptyMode {
    EMPTY_VAULT, NO_RESULTS, RECENTLY_DELETED_EMPTY
}

internal object P8IdeaVaultEmptyPolicy {
    fun mode(
        showingDeleted: Boolean,
        allIdeasEmpty: Boolean,
        deletedIdeasEmpty: Boolean,
    ): P8IdeaVaultEmptyMode = when {
        showingDeleted && deletedIdeasEmpty -> P8IdeaVaultEmptyMode.RECENTLY_DELETED_EMPTY
        !showingDeleted && allIdeasEmpty -> P8IdeaVaultEmptyMode.EMPTY_VAULT
        else -> P8IdeaVaultEmptyMode.NO_RESULTS
    }
}

/** A small editorial signpost, never a duplicate Inbox or a score. */
@Composable
internal fun P8IdeaVaultOverview(summary: P8IdeaVaultSummary) {
    Surface(
        color = CinemaSurfaceRaised,
        border = BorderStroke(1.dp, CinemaLine),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("p8-idea-vault-overview"),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("YOUR IDEA VAULT", color = RecRed, fontSize = 9.sp,
                letterSpacing = 1.3.sp, fontWeight = FontWeight.SemiBold)
            Text("Capture it. Shape it. Make it.", color = ProjectorIvory,
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                P8Count(summary.inbox, "Inbox", Modifier.weight(1f))
                P8Count(summary.exploring, "Exploring", Modifier.weight(1f))
                P8Count(summary.ready, "Ready", Modifier.weight(1f))
            }
            if (summary.converted > 0 || summary.archived > 0) {
                Text(summary.converted.toString() + " converted · " +
                    summary.archived + " archived",
                    color = MutedText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun P8Count(count: Int, label: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(count.toString(), color = ProjectorIvory,
            fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MutedText, fontSize = 11.sp, maxLines = 1)
    }
}

/** Honest, reversible empty-state actions: no silent data creation. */
@Composable
internal fun P8IdeaVaultEmptyState(
    mode: P8IdeaVaultEmptyMode,
    onNewIdea: () -> Unit,
    onClearFilters: () -> Unit,
    onShowIdeas: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 30.dp)
            .testTag("p8-idea-vault-empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = RecRed)
        Text(
            when (mode) {
                P8IdeaVaultEmptyMode.EMPTY_VAULT -> "Every idea starts somewhere."
                P8IdeaVaultEmptyMode.NO_RESULTS -> "Nothing matches just yet."
                P8IdeaVaultEmptyMode.RECENTLY_DELETED_EMPTY -> "Nothing to restore."
            }, color = ProjectorIvory, fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        )
        Text(
            when (mode) {
                P8IdeaVaultEmptyMode.EMPTY_VAULT ->
                    "A title, a note or a voice thought is enough to begin."
                P8IdeaVaultEmptyMode.NO_RESULTS ->
                    "Try a different search or clear the selected filters."
                P8IdeaVaultEmptyMode.RECENTLY_DELETED_EMPTY ->
                    "Ideas moved to Recently Deleted will appear here for recovery."
            }, color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(5.dp))
        when (mode) {
            P8IdeaVaultEmptyMode.EMPTY_VAULT ->
                Button(onClick = onNewIdea,
                    modifier = Modifier.height(48.dp).testTag("p8-empty-new-idea"),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("NEW IDEA") }
            P8IdeaVaultEmptyMode.NO_RESULTS ->
                OutlinedButton(onClick = onClearFilters,
                    modifier = Modifier.height(48.dp).testTag("p8-empty-clear-filters"),
                ) { Text("CLEAR SEARCH & FILTERS", color = ProjectorIvory) }
            P8IdeaVaultEmptyMode.RECENTLY_DELETED_EMPTY ->
                OutlinedButton(onClick = onShowIdeas,
                    modifier = Modifier.height(48.dp).testTag("p8-empty-show-ideas"),
                ) { Text("SHOW IDEAS", color = ProjectorIvory) }
        }
    }
}
