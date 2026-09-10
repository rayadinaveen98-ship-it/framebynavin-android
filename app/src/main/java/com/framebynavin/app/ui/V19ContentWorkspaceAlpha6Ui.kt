package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.productionProgressPercent
import com.framebynavin.app.ui.theme.*

@Composable
internal fun V19ContentWorkspaceAlpha6Hub(
    task: CreatorTask,
    onDismiss: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenScript: () -> Unit,
    onOpenPublish: () -> Unit,
) {
    val workspace = task.workspace
    val published = workspace.deliverables.count { it.status == CreatorDeliverableStatus.PUBLISHED }
    val ready = workspace.deliverables.count { it.status == CreatorDeliverableStatus.READY }
    val scriptMeta = workspace.scriptStudio?.let {
        "Embedded · S${it.revision} · ${it.beats.size} beats · backup/restore ready"
    } ?: "Legacy-compatible · embeds automatically on open/save/export"

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("CLOSE") }
                    Column(Modifier.weight(1f)) {
                        Text("CONTENT PROJECT 2.0 · RC1", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        Text(task.title, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Text("R${workspace.revision}", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudDone, null, tint = MutedGold)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("RELEASE-CANDIDATE DATA", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text(scriptMeta, color = MutedText, fontSize = 8.5.sp)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("CREATOR WORKSPACE", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("One project from planning to publishing. RC1 is feature-frozen while lifecycle, migration and recovery are verified.", color = MutedText, fontSize = 10.sp)

                Spacer(Modifier.height(18.dp))
                Alpha6HubCard(
                    icon = Icons.Outlined.Dashboard,
                    eyebrow = "PROJECT COMMAND CENTER",
                    title = "Plan & Produce",
                    body = "Brief, research, assets, production steps, templates, deliverables and learnings.",
                    meta = "${workspace.productionProgressPercent()}% production · ${workspace.deliverables.size} outputs",
                    onClick = onOpenProject,
                )
                Spacer(Modifier.height(10.dp))
                Alpha6HubCard(
                    icon = Icons.Outlined.EditNote,
                    eyebrow = "CONSOLIDATED SCRIPT",
                    title = "Script Studio",
                    body = "Hooks, title ideas, beats, narration, visuals, B-roll, notes and recording readiness.",
                    meta = scriptMeta,
                    onClick = onOpenScript,
                )
                Spacer(Modifier.height(10.dp))
                Alpha6HubCard(
                    icon = Icons.Outlined.Publish,
                    eyebrow = "PUBLISH WORKFLOW",
                    title = "Publish Studio",
                    body = "Final metadata, title/cover variants, pre-publish gates, publication history and repurposing.",
                    meta = "$ready ready · $published published",
                    onClick = onOpenPublish,
                )

                Spacer(Modifier.weight(1f))
                Text(
                    "Reminder acknowledgement, deliverable publication and project completion remain independent states.",
                    color = MutedText,
                    fontSize = 8.5.sp,
                    modifier = Modifier.padding(bottom = 18.dp),
                )
            }
        }
    }
}

@Composable
private fun Alpha6HubCard(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    body: String,
    meta: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MutedGold, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(eyebrow, color = RecRed, fontSize = 7.8.sp, fontWeight = FontWeight.Black)
                Text(title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(body, color = MutedText, fontSize = 9.5.sp)
                Spacer(Modifier.height(5.dp))
                Text(meta, color = MutedGold, fontSize = 8.3.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
