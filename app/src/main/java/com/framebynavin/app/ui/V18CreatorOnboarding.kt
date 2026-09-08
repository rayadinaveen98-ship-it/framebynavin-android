package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorPlatformRegistry
import com.framebynavin.app.ui.theme.*

private val creatorCategories = listOf(
    "Film & Entertainment",
    "Gaming",
    "Education",
    "Tech",
    "Lifestyle",
    "Business",
    "Music",
    "Art & Design",
    "News & Commentary",
    "Other",
)

private val creatorGoals = listOf(
    "Publish consistently",
    "Grow an audience",
    "Improve content quality",
    "Build a creator business",
    "Launch a project",
    "Stay organized",
)

@Composable
internal fun V18CreatorOnboarding(
    profile: CreatorProfile,
    notificationsReady: Boolean,
    preciseTimingReady: Boolean,
    fullScreenReady: Boolean,
    batteryReady: Boolean,
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onFinish: (CreatorProfile) -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var category by rememberSaveable(profile.category) { mutableStateOf(profile.category) }
    var platforms by remember(profile.platforms) { mutableStateOf(profile.platforms) }
    var goal by rememberSaveable(profile.primaryGoal) { mutableStateOf(profile.primaryGoal) }
    var weeklyTarget by rememberSaveable(profile.weeklyPublishingTarget) {
        mutableIntStateOf(profile.weeklyPublishingTarget.coerceIn(1, 14))
    }

    val canContinue = when (page) {
        0 -> category.isNotBlank()
        1 -> platforms.isNotEmpty()
        2 -> goal.isNotBlank()
        else -> true
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FRAMEBYNAVIN", color = RecRed, fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Black)
                    Text("Creator setup", color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Text("${page + 1}/4", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { (page + 1) / 4f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = RecRed,
                trackColor = CinemaLine,
            )
            Spacer(Modifier.height(20.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (page) {
                    0 -> {
                        Icon(Icons.Outlined.AutoAwesome, "Creator type", tint = RecRed, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Shape the system around your work.", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("These choices now change project defaults, platform formats, workflows, planning language and Insights priorities.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(20.dp))
                        V18OnboardingLabel("WHAT DO YOU CREATE?")
                        Spacer(Modifier.height(9.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            creatorCategories.forEach { item ->
                                FilterChip(
                                    selected = category == item,
                                    onClick = { category = item },
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                    }

                    1 -> {
                        Icon(Icons.Outlined.Hub, "Creator platforms", tint = MutedGold, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Where do you publish?", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("Choose every platform you actively create for. FrameByNavin will prioritize their formats and workflows across Create and Idea Vault.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(20.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CreatorPlatformRegistry.supportedPlatforms.forEach { item ->
                                val selected = item in platforms
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        platforms = if (selected) platforms - item else platforms + item
                                    },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                    }

                    2 -> {
                        Icon(Icons.Outlined.TrackChanges, "Creator goal", tint = RecRed, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("What matters most right now?", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("This becomes the default lens for planning. It is a preference, not a permanent label.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(18.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            creatorGoals.forEach { item ->
                                FilterChip(
                                    selected = goal == item,
                                    onClick = { goal = item },
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        V18OnboardingLabel("REALISTIC PUBLISHING TARGET")
                        Spacer(Modifier.height(6.dp))
                        Text("How many pieces would you like to publish in a normal week?", color = MutedText, fontSize = 11.sp)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 5, 7).forEach { value ->
                                FilterChip(
                                    selected = weeklyTarget == value,
                                    onClick = { weeklyTarget = value },
                                    label = { Text("$value / week", fontSize = 10.sp) },
                                )
                            }
                        }
                    }

                    else -> {
                        Icon(Icons.Outlined.NotificationsActive, "Reminder setup", tint = MutedGold, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Make reminders dependable.", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("These Android permissions are optional, but enabling them gives project reminders their full reliability.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(18.dp))
                        V18OnboardingPermissionRow("Notifications", notificationsReady, onNotifications)
                        V18OnboardingPermissionRow("Exact reminder timing", preciseTimingReady, onPreciseTiming)
                        V18OnboardingPermissionRow("Full-screen alerts", fullScreenReady, onFullScreen)
                        V18OnboardingPermissionRow("Allow background reminders", batteryReady, onBattery)
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("YOUR SETUP", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(Modifier.height(5.dp))
                                Text("$category · ${platforms.sorted().joinToString()}", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp)
                                Text("$goal · $weeklyTarget / week", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) {
                    TextButton(onClick = { page-- }) {
                        Icon(Icons.Outlined.ArrowBack, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("BACK", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (page < 3) {
                            page++
                        } else {
                            onFinish(
                                CreatorProfile(
                                    displayName = profile.displayName,
                                    category = category,
                                    platforms = platforms,
                                    primaryGoal = goal,
                                    weeklyPublishingTarget = weeklyTarget,
                                ).normalized(),
                            )
                        }
                    },
                    enabled = canContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.height(50.dp),
                ) {
                    Text(if (page < 3) "CONTINUE" else "ENTER CREATOR OS", fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun V18OnboardingLabel(text: String) {
    Text(text, color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
}

@Composable
private fun V18OnboardingPermissionRow(label: String, ready: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, if (ready) SuccessGreen.copy(alpha = .45f) else CinemaLine),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ready) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                null,
                tint = if (ready) SuccessGreen else MutedText,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(label, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (ready) "READY" else "SET UP", color = if (ready) SuccessGreen else RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}
