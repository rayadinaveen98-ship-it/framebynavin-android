package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.framebynavin.app.data.CreatorBlueprintPrompt
import com.framebynavin.app.data.CreatorBlueprintStep
import com.framebynavin.app.data.CreatorContentBlueprint
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.ui.theme.*

@Composable
internal fun V20ContentBlueprintDialog(
    task: CreatorTask,
    blueprint: CreatorContentBlueprint,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DNA BLUEPRINT", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                        Text(task.title, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 36.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, MutedGold.copy(alpha = .38f)),
                    ) {
                        Column(Modifier.padding(17.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(42.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(shape = CircleShape, color = MutedGold.copy(alpha = .12f)) {
                                        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.AutoAwesome, null, tint = MutedGold, modifier = Modifier.size(21.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(blueprint.headline, color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
                                    Text(
                                        "${blueprint.modeLabel} · ${blueprint.archetypeLabel}",
                                        color = MutedGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(blueprint.summary, color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "ADVISORY ONLY · nothing here changes stages, reminders or publication state",
                                color = RecRed,
                                fontSize = 7.8.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = .7.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    V20BlueprintSectionHeader(Icons.Outlined.Route, "RECOMMENDED FLOW", "A content-specific route from idea to release.")
                    Spacer(Modifier.height(9.dp))
                    V20StepList(blueprint.workflow)

                    Spacer(Modifier.height(24.dp))
                    V20BlueprintSectionHeader(Icons.Outlined.DashboardCustomize, "WORKSPACE PROMPTS", "Use these to sharpen the project brief.")
                    Spacer(Modifier.height(9.dp))
                    blueprint.workspacePrompts.forEach { prompt ->
                        V20PromptCard(prompt)
                        Spacer(Modifier.height(7.dp))
                    }

                    Spacer(Modifier.height(17.dp))
                    V20BlueprintSectionHeader(Icons.Outlined.EditNote, "SCRIPT / STRUCTURE", "A starting structure matched to the content type.")
                    Spacer(Modifier.height(9.dp))
                    V20StepList(blueprint.scriptStructure)

                    Spacer(Modifier.height(24.dp))
                    V20BlueprintSectionHeader(Icons.Outlined.Checklist, "QUALITY CHECK", "Things worth verifying before you call the piece finished.")
                    Spacer(Modifier.height(9.dp))
                    Surface(
                        Modifier.fillMaxWidth(),
                        RoundedCornerShape(18.dp),
                        CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            blueprint.checklist.forEachIndexed { index, item ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                                    Text("${index + 1}", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
                                    Text(item, color = ProjectorIvory, fontSize = 9.5.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    V20BlueprintSectionHeader(Icons.Outlined.Build, "USEFUL TOOLS", "Not app requirements — just useful working materials for this DNA.")
                    Spacer(Modifier.height(9.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        blueprint.toolSuggestions.forEach { tool ->
                            SuggestionChip(onClick = {}, label = { Text(tool, fontSize = 8.8.sp) })
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    V20BlueprintSectionHeader(Icons.Outlined.CallSplit, "OUTPUT IDEAS", "Possible outputs from the same core project.")
                    Spacer(Modifier.height(9.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        blueprint.outputIdeas.forEach { output ->
                            AssistChip(onClick = {}, label = { Text(output, fontSize = 8.8.sp) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V20BlueprintSectionHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(12.dp), color = MutedGold.copy(alpha = .10f)) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MutedGold, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = .4.sp)
            Text(subtitle, color = MutedText, fontSize = 8.5.sp)
        }
    }
}

@Composable
private fun V20StepList(steps: List<CreatorBlueprintStep>) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(18.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
            steps.forEachIndexed { index, step ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Surface(shape = CircleShape, color = RecRed.copy(alpha = .13f)) {
                        Box(Modifier.size(27.dp), contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(step.title, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(step.guidance, color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
                    }
                }
                if (index != steps.lastIndex) HorizontalDivider(color = CinemaLine.copy(alpha = .7f))
            }
        }
    }
}

@Composable
private fun V20PromptCard(prompt: CreatorBlueprintPrompt) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(16.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(prompt.label.uppercase(), color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
            Spacer(Modifier.height(3.dp))
            Text(prompt.prompt, color = ProjectorIvory, fontSize = 9.5.sp, lineHeight = 14.sp)
        }
    }
}
