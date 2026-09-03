package com.framebynavin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V09ReleaseDayScreen(
    onClose: () -> Unit,
    onLaunch: (ReleaseBurstRequest) -> ReleaseLaunchResult,
) {
    var topic by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf(ReleaseEventType.TRAILER) }
    var urgency by remember { mutableStateOf(ReleaseUrgency.NOW) }
    var outputs by remember { mutableStateOf(setOf(ReleaseOutput.X_POST, ReleaseOutput.INSTAGRAM_STORY)) }
    var deepDive by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<ReleaseLaunchResult?>(null) }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 38.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("RELEASE DAY MODE", color = RecRed, fontSize = 9.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                    Text("React while it matters", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
                Icon(Icons.Outlined.Bolt, null, tint = RecRed, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                Modifier.fillMaxWidth(),
                RoundedCornerShape(22.dp),
                CinemaSurfaceRaised,
                border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
            ) {
                Column(Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).background(RecRed.copy(alpha = .15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Movie, null, tint = RecRed, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("ONE EVENT → MULTIPLE OUTPUTS", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Each output becomes a real Studio project.", color = MutedText, fontSize = 9.5.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(17.dp))
            V09ReleaseLabel("WHAT HAPPENED?")
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Movie / event / topic") },
                placeholder = { Text("Example: Spirit trailer") },
            )

            Spacer(Modifier.height(14.dp))
            V09ReleaseLabel("EVENT TYPE")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ReleaseEventType.entries.forEach { type ->
                    FilterChip(
                        selected = eventType == type,
                        onClick = { eventType = type },
                        label = { Text(ReleaseDayEngine.eventLabel(type), fontSize = 8.7.sp) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            V09ReleaseLabel("URGENCY")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ReleaseUrgency.entries.forEach { value ->
                    val label = when (value) {
                        ReleaseUrgency.NOW -> "NOW"
                        ReleaseUrgency.TODAY -> "TODAY"
                        ReleaseUrgency.LATER -> "LATER"
                    }
                    FilterChip(
                        selected = urgency == value,
                        onClick = { urgency = value },
                        label = { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                when (urgency) {
                    ReleaseUrgency.NOW -> "Fastest windows: X ~10m, Story ~15m, Reel ~90m, Short ~2.5h."
                    ReleaseUrgency.TODAY -> "Balanced same-day response windows."
                    ReleaseUrgency.LATER -> "Keeps the opportunity but gives production more room."
                },
                color = MutedText,
                fontSize = 9.3.sp,
                modifier = Modifier.padding(top = 5.dp),
            )

            Spacer(Modifier.height(16.dp))
            V09ReleaseLabel("CREATE OUTPUTS")
            ReleaseOutput.entries.forEach { output ->
                val selected = output in outputs
                val description = when (output) {
                    ReleaseOutput.X_POST -> "Draft → Review → Publish"
                    ReleaseOutput.INSTAGRAM_STORY -> "Create → Review → Publish"
                    ReleaseOutput.INSTAGRAM_REEL -> "Script → Voice → Edit → Cover → Upload"
                    ReleaseOutput.YOUTUBE_SHORT -> "Script → Voice → Edit → Cover → Upload"
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable {
                        outputs = if (selected) outputs - output else outputs + output
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) Color(0xFF19120F) else CinemaSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) RecRed.copy(alpha = .65f) else CinemaLine),
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = selected, onCheckedChange = null)
                        Spacer(Modifier.width(4.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ReleaseDayEngine.outputLabel(output), color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Text(description, color = MutedText, fontSize = 9.sp)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().clickable { deepDive = !deepDive },
                shape = RoundedCornerShape(16.dp),
                color = if (deepDive) Color(0xFF15130E) else CinemaSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (deepDive) MutedGold.copy(alpha = .6f) else CinemaLine),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = deepDive, onCheckedChange = null)
                    Spacer(Modifier.width(5.dp))
                    Column {
                        Text("SAVE DEEP-DIVE IDEA", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Keeps a future long-form analysis candidate in Idea Vault.", color = MutedText, fontSize = 9.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            V09ReleaseLabel("CONTEXT / NOTES")
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp),
                label = { Text("Optional") },
                placeholder = { Text("What changed, your angle, important context…") },
            )

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    result = onLaunch(
                        ReleaseBurstRequest(
                            topic = topic.trim(),
                            eventType = eventType,
                            details = details.trim(),
                            urgency = urgency,
                            outputs = outputs,
                            saveDeepDiveIdea = deepDive,
                        )
                    )
                },
                enabled = topic.isNotBlank() && (outputs.isNotEmpty() || deepDive),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                shape = RoundedCornerShape(15.dp),
            ) {
                Icon(Icons.Outlined.Bolt, null)
                Spacer(Modifier.width(7.dp))
                Text("LAUNCH RELEASE RESPONSE", fontWeight = FontWeight.Black, fontSize = 10.5.sp)
            }

            result?.let { launch ->
                Spacer(Modifier.height(13.dp))
                Surface(
                    Modifier.fillMaxWidth(),
                    RoundedCornerShape(17.dp),
                    Color(0xFF101812),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = .4f)),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Response launched", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                            Text(
                                "${launch.createdProjects} Studio project${if (launch.createdProjects == 1) "" else "s"}${if (launch.ideaSaved) " + deep-dive idea saved" else ""}.",
                                color = MutedText,
                                fontSize = 9.5.sp,
                            )
                        }
                        TextButton(onClick = onClose) { Text("DONE", color = SuccessGreen) }
                    }
                }
            }
        }
    }
}

@Composable
private fun V09ReleaseLabel(text: String) {
    Text(text, color = MutedGold, fontSize = 8.5.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
}
