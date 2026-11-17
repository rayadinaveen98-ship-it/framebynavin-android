package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*

/**
 * UX05.1C: connected, read-only creative board. Every card projects an
 * existing canonical project record. No shadow media store, copied file,
 * fabricated content, automatic stage advancement or mutation on viewing.
 *
 * Editing / curation happens in Stage Desk, Plan, Script Studio or Output Craft.
 */
internal enum class CW51BoardFilter(val label: String) {
    ALL("All"), SOURCES("Sources"), EVIDENCE("Scenes & proof"),
    STORY("Story"), OUTPUTS("Outputs")
}

internal enum class CW51BoardRoute {
    SOURCES, ASSETS, STAGE, SCRIPT, PUBLISH, OUTPUT_CRAFT
}

internal data class CW51BoardItem(
    val key: String,
    val filter: CW51BoardFilter,
    val heading: String,
    val title: String,
    val detail: String,
    val status: String,
    val route: CW51BoardRoute,
    val stageId: String = "",
    val missingMaterial: Boolean = false,
)

internal object CW51CreativeBoardPolicy {
    fun cards(task: CreatorTask): List<CW51BoardItem> {
        val ws = task.workspace
        val template = CreatorWorkflowEngine.templateFor(task)
        val sourceById = ws.references.associateBy { it.id }
        val assetById = ws.assets.associateBy { it.id }
        val entries = ArrayList<CW51BoardItem>()

        ws.references.forEach { source ->
            entries += CW51BoardItem(
                key = "source:${source.id}",
                filter = CW51BoardFilter.SOURCES,
                heading = "PROJECT SOURCE",
                title = source.label.ifBlank { source.url.ifBlank { "Untitled source" } },
                detail = source.url.take(180).ifBlank { "Add the original link in Plan → Sources" },
                status = "Linked source · not automatically verified",
                route = CW51BoardRoute.SOURCES,
            )
        }

        ws.assets.forEach { asset ->
            entries += CW51BoardItem(
                key = "asset:${asset.id}",
                filter = CW51BoardFilter.SOURCES,
                heading = "PROJECT ASSET · ${asset.kind.name}",
                title = asset.label.ifBlank { "Unnamed asset" },
                detail = asset.notes.take(220).ifBlank { "Stored as a link or document reference, not copied media" },
                status = "Availability after restore is not guaranteed",
                route = CW51BoardRoute.ASSETS,
            )
        }

        template.stages.forEach { stage ->
            val work = CreatorStageWorkPolicy.find(ws, template.id, stage.id)
            work?.evidence?.forEach { evidence ->
                val source = sourceById[evidence.referenceId]
                val asset = assetById[evidence.assetId]
                val missing = (evidence.referenceId.isNotBlank() && source == null) ||
                    (evidence.assetId.isNotBlank() && asset == null)
                val connections = buildList {
                    if (source != null) add("Source: " + source.label.ifBlank { source.url })
                    if (asset != null) add("Asset: " + asset.label.ifBlank { "Unnamed asset" })
                    if (missing) add("Earlier linked material unavailable")
                }
                entries += CW51BoardItem(
                    key = "evidence:${template.id}:${stage.id}:${evidence.id}",
                    filter = CW51BoardFilter.EVIDENCE,
                    heading = stage.label.uppercase(),
                    title = evidence.claim.ifBlank { "Untitled observation" },
                    detail = listOfNotNull(
                        evidence.timecode.takeIf { it.isNotBlank() }?.let { "Timecode: $it" },
                        evidence.notes.takeIf { it.isNotBlank() },
                        connections.joinToString(" · ").takeIf { it.isNotBlank() },
                    ).joinToString("\n").take(360),
                    status = if (missing) "Review unavailable link" else "Saved evidence · not independently verified",
                    route = CW51BoardRoute.STAGE,
                    stageId = stage.id,
                    missingMaterial = missing,
                )
            }
        }

        ws.scriptStudio?.beats?.forEach { beat ->
            entries += CW51BoardItem(
                key = "beat:${beat.id}",
                filter = CW51BoardFilter.STORY,
                heading = "SCRIPT BEAT",
                title = beat.label.ifBlank { beat.purpose.ifBlank { "Untitled beat" } },
                detail = listOf(beat.purpose, beat.narration, beat.visualNotes, beat.bRollNotes)
                    .firstOrNull { it.isNotBlank() }.orEmpty().take(250),
                status = "Saved script status: ${beat.status.name.replace('_', ' ').lowercase()}",
                route = CW51BoardRoute.SCRIPT,
            )
        }

        ws.deliverables.forEach { output ->
            val genuinelyRecorded = output.status == CreatorDeliverableStatus.PUBLISHED &&
                output.publishedAtMillis > 0L
            entries += CW51BoardItem(
                key = "output:${output.id}",
                filter = CW51BoardFilter.OUTPUTS,
                heading = "DELIVERABLE · ${output.platform}",
                title = output.title.ifBlank { output.format },
                detail = listOf(output.format, output.thumbnailConcept)
                    .filter { it.isNotBlank() }.joinToString(" · ").take(240),
                status = if (genuinelyRecorded) "Creator-recorded published"
                    else if (output.status == CreatorDeliverableStatus.READY) "Ready · not published"
                    else "Planned · not published",
                route = CW51BoardRoute.PUBLISH,
            )
        }

        ws.editVersions.forEach { cut ->
            val output = ws.deliverables.firstOrNull { it.id == cut.deliverableId }
            entries += CW51BoardItem(
                key = "cut:${cut.id}",
                filter = CW51BoardFilter.OUTPUTS,
                heading = "CUT VERSION",
                title = cut.label,
                detail = listOfNotNull(
                    output?.title?.ifBlank { output.format }?.takeIf { it.isNotBlank() },
                    cut.notes.takeIf { it.isNotBlank() },
                ).joinToString(" · ").take(240),
                status = "Saved cut review: ${cut.status.name.lowercase()}",
                route = CW51BoardRoute.OUTPUT_CRAFT,
                missingMaterial = output == null,
            )
        }

        return entries
    }
}

/** Practical card collection, not an infinite canvas or media editing engine. */
@Composable
internal fun CW51CreativeBoard(
    task: CreatorTask,
    onBack: () -> Unit,
    onOpenPlanSection: (String) -> Unit,
    onOpenStage: (String) -> Unit,
    onOpenScript: () -> Unit,
    onOpenPublish: () -> Unit,
    onOpenOutputCraft: () -> Unit,
) {
    val all = remember(task) { CW51CreativeBoardPolicy.cards(task) }
    var selected by rememberSaveable(task.id) { mutableStateOf(CW51BoardFilter.ALL.name) }
    val filter = CW51BoardFilter.entries.firstOrNull { it.name == selected }
        ?: CW51BoardFilter.ALL
    val visible = if (filter == CW51BoardFilter.ALL) all else all.filter { it.filter == filter }
    val template = CreatorWorkflowEngine.templateFor(task)
    val canEdit = task.archivedAtMillis == 0L &&
        task.status != TaskStatus.DONE && task.status != TaskStatus.SKIPPED

    fun open(item: CW51BoardItem) {
        when (item.route) {
            CW51BoardRoute.SOURCES -> onOpenPlanSection("RESEARCH")
            CW51BoardRoute.ASSETS -> onOpenPlanSection("ASSETS")
            CW51BoardRoute.STAGE -> onOpenStage(item.stageId)
            CW51BoardRoute.SCRIPT -> onOpenScript()
            CW51BoardRoute.PUBLISH -> onOpenPublish()
            CW51BoardRoute.OUTPUT_CRAFT -> onOpenOutputCraft()
        }
    }

    WorkspaceAdaptiveFrame {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back to project",
                        tint = ProjectorIvory)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("CONNECTED CREATIVE BOARD", fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = MutedGold)
                    Text(task.title, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, color = ProjectorIvory)
                }
            }
            Text(
                "${all.size} saved cards · ${template.stages.size} workflow stages",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp),
                color = MutedText, fontSize = 12.sp,
            )
            Text(
                "Browse connected project materials. Edit the originals in their tools; this view never duplicates or changes them.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp),
                color = MutedText, fontSize = 12.sp, lineHeight = 17.sp,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CW51BoardFilter.entries.forEach { choice ->
                    FilterChip(
                        selected = choice == filter,
                        onClick = { selected = choice.name },
                        label = { Text(choice.label) },
                        modifier = Modifier.heightIn(min = 48.dp)
                            .testTag("ux051c-filter-${choice.name.lowercase()}"),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("ux051c-connected-board"),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (visible.isEmpty()) item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("Nothing collected here yet", color = ProjectorIvory,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(7.dp))
                            Text(
                                if (canEdit) "Add sources, evidence, script beats or outputs through their existing project tools."
                                else "This project has no saved records in the selected collection.",
                                color = MutedText, fontSize = 12.sp,
                            )
                            if (canEdit) {
                                Spacer(Modifier.height(12.dp))
                                TextButton(onClick = {
                                    when (filter) {
                                        CW51BoardFilter.EVIDENCE -> onOpenStage(CreatorWorkflowEngine.currentStage(task).id)
                                        CW51BoardFilter.STORY -> onOpenScript()
                                        CW51BoardFilter.OUTPUTS -> onOpenPlanSection("DELIVERABLES")
                                        else -> onOpenPlanSection("RESEARCH")
                                    }
                                }) { Text("OPEN WORKING TOOL", color = RecRed) }
                            }
                        }
                    }
                }
                items(visible, key = { it.key }) { item ->
                    Surface(
                        onClick = { open(item) },
                        modifier = Modifier.fillMaxWidth().testTag("ux051c-card-${item.filter.name.lowercase()}"),
                        shape = RoundedCornerShape(16.dp),
                        color = CinemaSurfaceRaised,
                        border = BorderStroke(1.dp, CinemaLine.copy(alpha = .75f)),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when (item.filter) {
                                        CW51BoardFilter.SOURCES -> Icons.Outlined.Collections
                                        CW51BoardFilter.EVIDENCE -> Icons.Outlined.FactCheck
                                        CW51BoardFilter.STORY -> Icons.Outlined.EditNote
                                        CW51BoardFilter.OUTPUTS -> Icons.Outlined.Movie
                                        CW51BoardFilter.ALL -> Icons.Outlined.Dashboard
                                    },
                                    contentDescription = null, tint = MutedGold,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(item.heading, color = MutedGold,
                                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                    modifier = Modifier.weight(1f), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                                    tint = RecRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.height(9.dp))
                            Text(item.title, color = ProjectorIvory, fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 3,
                                overflow = TextOverflow.Ellipsis)
                            if (item.detail.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(item.detail, color = MutedText, fontSize = 12.sp,
                                    lineHeight = 17.sp, maxLines = 5,
                                    overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(9.dp))
                            Text(item.status, color = if (item.missingMaterial) RecRed else MutedGold,
                                fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
