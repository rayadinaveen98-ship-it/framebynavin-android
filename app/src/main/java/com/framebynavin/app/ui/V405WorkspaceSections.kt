package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import java.text.DateFormat
import java.util.Date

/** UX05B: current-stage canonical work, never fabricated work duration. */
@Composable
internal fun V405CurrentStageGlance(glance: V405StageGlance) {
    Column(
        Modifier.fillMaxWidth().testTag("ux05-current-stage-glance"),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("STAGE MATERIALS", color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(
            "${glance.checklistDone} stage tasks done · ${glance.checklistOpen} open",
            color = ProjectorIvory, fontSize = 13.sp,
        )
        Text(
            "${glance.evidenceCount} evidence records · ${glance.linkedMaterialCount} linked items" +
                if (glance.notesPresent) " · notes saved" else "",
            color = MutedText, fontSize = 11.sp,
        )
        if (glance.reviewed) Text(
            "Stage notes reviewed · workflow advancement is separate",
            color = MutedGold, fontSize = 11.sp,
        )
    }
}

/** UX05G: distinguish saved status, cut versions and actual publication records. */
@Composable
internal fun V405OutputFacts(overview: V405WorkspaceOverview) {
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp).testTag("ux05-output-facts"),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("OUTPUT STATUS", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            "${overview.outputPlanned} planned · ${overview.outputReady} ready · " +
                "${overview.outputPublished} recorded published",
            color = MutedText, fontSize = 12.sp,
        )
        Text(
            "${overview.scriptBeats} script beats · ${overview.recordedCuts} logged cut versions",
            color = MutedText, fontSize = 12.sp,
        )
        Text(
            "Ready or cut-approved does not mean published. Publication is a creator-confirmed record, " +
                "not automatically verified by Backlot against the live platform.",
            color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
        )
    }
}

/** UX05G: transition receipts, not claims of focused work or external publication. */
@Composable
internal fun V405StageApprovalHistory(task: CreatorTask, overview: V405WorkspaceOverview) {
    val workflow = CreatorWorkflowEngine.templateFor(task)
    val receipts = task.workspace.stageTransitions
        .filter { it.workflowTemplateId == workflow.id }
        .sortedByDescending { it.approvedAtMillis }
        .take(3)

    Column(
        Modifier.fillMaxWidth().testTag("ux05-approval-history"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("STAGE HISTORY", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (overview.hasOriginIdea) Text(
            "Origin · Converted from Idea Vault. The original idea remains linked.",
            color = MutedGold, fontSize = 11.sp, lineHeight = 16.sp,
        )
        if (receipts.isEmpty()) {
            Text(
                "No stage approvals recorded here yet. Older projects may have workflow " +
                    "progress without a saved approval receipt.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
            )
        } else receipts.forEachIndexed { index, receipt ->
            val stageLabel = workflow.stages.firstOrNull { it.id == receipt.stageId }?.label
                ?: receipt.stageId
            Row(Modifier.fillMaxWidth()) {
                Text("✓", color = MutedGold, fontSize = 13.sp)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Approved · $stageLabel", color = ProjectorIvory,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(receipt.approvedAtMillis)),
                        color = MutedText, fontSize = 10.sp,
                    )
                }
            }
            if (index < receipts.lastIndex) {
                HorizontalDivider(color = CinemaLine.copy(alpha = .35f))
            }
        }
        Text(
            "Recorded approvals are not time-tracking or publication evidence.",
            color = MutedText, fontSize = 10.sp, lineHeight = 15.sp,
        )
    }
}
