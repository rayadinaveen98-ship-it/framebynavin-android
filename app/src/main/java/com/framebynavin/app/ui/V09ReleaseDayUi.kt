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
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 38.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("RELEASE DAY", color = RecRed, fontSize = 8.5.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Black)
                    Text("Move while it matters", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
                Box(Modifier.size(40.dp).background(RecRed.copy(alpha = .12f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Bolt, null, tint = RecRed, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.height(17.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(21.dp), CinemaSurfaceRaised, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(RecRed.copy(alpha = .12f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Movie, null, tint = RecRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Text("ONE MOMENT. SEVERAL POSTS.", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Choose what deserves an immediate response and what can wait.", color = MutedText, fontSize = 9.3.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            V09ReleaseLabel("WHAT HAPPENED?")
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Example: Spirit trailer") },
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(Modifier.height(16.dp))
            V09ReleaseLabel("TYPE")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ReleaseEventType.entries.forEach { type ->
                    FilterChip(selected = eventType == type, onClick = { eventType = type }, label = { Text(ReleaseDayEngine.eventLabel(type), fontSize = 8.7.sp) })
                }
            }

            Spacer(Modifier.height(16.dp))
            V09ReleaseLabel("HOW FAST?")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ReleaseUrgency.entries.forEach { value ->
                    val label = when (value) {
                        ReleaseUrgency.NOW -> "NOW"
                        ReleaseUrgency.TODAY -> "TODAY"
                        ReleaseUrgency.LATER -> "LATER"
                    }
                    FilterChip(selected = urgency == value, onClick = { urgency = value }, label = { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold) }, modifier = Modifier.weight(1f))
                }
            }
            Text(
                when (urgency) {
                    ReleaseUrgency.NOW -> "Prioritises the fastest reaction windows."
                    ReleaseUrgency.TODAY -> "Keeps everything inside today's conversation."
                    ReleaseUrgency.LATER -> "Save the opportunity without rushing production."
                },
                color = MutedText,
                fontSize = 9.2.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(18.dp))
            V09ReleaseLabel("WHAT DO YOU WANT TO MAKE?")
            ReleaseOutput.entries.forEach { output ->
                val selected = output in outputs
                val description = when (output) {
                    ReleaseOutput.X_POST -> "Fast written reaction"
                    ReleaseOutput.INSTAGRAM_STORY -> "Quick visual update"
                    ReleaseOutput.INSTAGRAM_REEL -> "Reaction Reel"
                    ReleaseOutput.YOUTUBE_SHORT -> "Short video response"
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable { outputs = if (selected) outputs - output else outputs + output },
                    shape = RoundedCornerShape(17.dp),
                    color = if (selected) Color(0xFF19120F) else CinemaSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) RecRed.copy(alpha = .6f) else CinemaLine),
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = selected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = RecRed))
                        Spacer(Modifier.width(5.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ReleaseDayEngine.outputLabel(output), color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Text(description, color = MutedText, fontSize = 9.sp)
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().clickable { deepDive = !deepDive },
                shape = RoundedCornerShape(17.dp),
                color = if (deepDive) Color(0xFF15130E) else CinemaSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (deepDive) MutedGold.copy(alpha = .55f) else CinemaLine),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = deepDive, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = MutedGold))
                    Spacer(Modifier.width(5.dp))
                    Column {
                        Text("SAVE A DEEP-DIVE IDEA", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Keep a future analysis angle in Idea Vault.", color = MutedText, fontSize = 9.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            V09ReleaseLabel("YOUR ANGLE · OPTIONAL")
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp),
                placeholder = { Text("What stands out? What should your take focus on?") },
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(Modifier.height(19.dp))
            Button(
                onClick = {
                    result = onLaunch(ReleaseBurstRequest(topic = topic.trim(), eventType = eventType, details = details.trim(), urgency = urgency, outputs = outputs, saveDeepDiveIdea = deepDive))
                },
                enabled = topic.isNotBlank() && (outputs.isNotEmpty() || deepDive),
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Outlined.Bolt, null); Spacer(Modifier.width(7.dp)); Text("CREATE RESPONSE PLAN", fontWeight = FontWeight.Black, fontSize = 10.sp)
            }

            result?.let { launch ->
                Spacer(Modifier.height(13.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), Color(0xFF101812), border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = .38f))) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ready to make", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                            Text("${launch.createdProjects} project${if (launch.createdProjects == 1) "" else "s"}${if (launch.ideaSaved) " + one idea saved" else ""}.", color = MutedText, fontSize = 9.4.sp)
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
    Text(text, color = MutedGold, fontSize = 8.5.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 7.dp))
}
