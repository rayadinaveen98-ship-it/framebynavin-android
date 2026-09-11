package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
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
import com.framebynavin.app.data.CreatorContentBlueprint
import com.framebynavin.app.data.CreatorDeliverableStatus
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.productionProgressPercent
import com.framebynavin.app.ui.theme.*

@Composable
internal fun V19ContentWorkspaceAlpha6Hub(
    task: CreatorTask,
    blueprint: CreatorContentBlueprint,
    onDismiss: () -> Unit,
    onOpenBlueprint: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenScript: () -> Unit,
    onOpenPublish: () -> Unit,
) {
    val workspace = task.workspace
    val published = workspace.deliverables.count { it.status == CreatorDeliverableStatus.PUBLISHED }
    val ready = workspace.deliverables.count { it.status == CreatorDeliverableStatus.READY }
    val scriptMeta = workspace.scriptStudio?.let {
        "${it.beats.size} script beats ready"
    } ?: "Start or continue your script"

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("CLOSE") }
                    Column(Modifier.weight(1f)) {
                        Text("PROJECT WORKSPACE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        Text(task.title, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    Text("${workspace.productionProgressPercent()}%", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                            Text("PROJECT SAVED", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text(scriptMeta, color = MutedText, fontSize = 8.5.sp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Your project", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Plan, write and publish from one place.", color = MutedText, fontSize = 10.sp)

                Spacer(Modifier.height(18.dp))
                Alpha6HubCard(
                    icon = Icons.Outlined.AutoAwesome,
                    eyebrow = "PROJECT STYLE",
                    title = blueprint.headline,
                    body = "A simple guide based on how you make this kind of content.",
                    meta = "${blueprint.modeLabel} · ${blueprint.archetypeLabel}",
                    onClick = onOpenBlueprint,
                )
                Spacer(Modifier.height(10.dp))
                Alpha6HubCard(
                    icon = Icons.Outlined.Dashboard,
                    eyebrow = "PROJECT",
                    title = "Plan & Produce",
                    body = "Brief, research, assets, checklist and outputs.",
                    meta = "${workspace.productionProgressPercent()}% ready · ${workspace.deliverables.size} outputs",
                    onClick = onOpenProject,
                )
                Spacer(Modifier.height(10.dp))
                Alpha6HubCard(
                    icon = Icons.Outlined.EditNote,
                    eyebrow = "SCRIPT",
                    title = "Script Studio",
                    body = "Write your hook, narration, beats and visual notes.",
                    meta = scriptMeta,
                    onClick = onOpenScript,
                )
                Spacer(Modifier.height(10.dp))
                Alpha6HubCard(
                    icon = Icons.Outlined.Publish,
                    eyebrow = "PUBLISH",
                    title = "Publish Studio",
                    body = "Prepare the title, cover, description and final publish check.",
                    meta = "$ready ready · $published published",
                    onClick = onOpenPublish,
                )

                Spacer(Modifier.height(24.dp))
                Text(
                    "Everything stays connected to this project.",
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
