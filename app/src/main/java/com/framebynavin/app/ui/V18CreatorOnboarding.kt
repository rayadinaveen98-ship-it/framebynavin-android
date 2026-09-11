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

private val creatorModes = listOf(
    "Film & Entertainment",
    "Gaming",
    "Education",
    "Tech",
    "Lifestyle",
    "Business & Career",
    "Music & Audio",
    "Art & Design",
    "News & Commentary",
    "Food",
    "Travel & Outdoors",
    "Health & Fitness",
    "Other / Hybrid",
)

private val productionStyles = listOf(
    "Voiceover",
    "Talking Head",
    "Gameplay Capture",
    "Screen Recording",
    "Camera / B-roll",
    "Livestream",
    "Audio-only",
    "Animation / Motion",
    "Writing",
    "Mixed / Hybrid",
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
    var primaryMode by rememberSaveable(profile.resolvedPrimaryCreatorMode) {
        mutableStateOf(profile.resolvedPrimaryCreatorMode)
    }
    var secondaryModes by remember(profile.secondaryCreatorModes) {
        mutableStateOf(profile.secondaryCreatorModes)
    }
    var platforms by remember(profile.platforms) { mutableStateOf(profile.platforms) }
    var styles by remember(profile.productionStyles) { mutableStateOf(profile.productionStyles) }
    var selectedGoals by remember(profile.primaryGoal, profile.secondaryGoals) {
        mutableStateOf(profile.activeGoals.toSet())
    }
    var primaryGoal by rememberSaveable(profile.primaryGoal) { mutableStateOf(profile.primaryGoal) }
    var weeklyTarget by rememberSaveable(profile.weeklyPublishingTarget) {
        mutableIntStateOf(profile.weeklyPublishingTarget.coerceIn(1, 14))
    }

    LaunchedEffect(primaryMode) {
        secondaryModes = secondaryModes - primaryMode
    }
    LaunchedEffect(selectedGoals) {
        if (primaryGoal !in selectedGoals) primaryGoal = selectedGoals.firstOrNull().orEmpty()
    }

    val canContinue = when (page) {
        0 -> primaryMode.isNotBlank()
        1 -> platforms.isNotEmpty()
        2 -> styles.isNotEmpty()
        3 -> selectedGoals.isNotEmpty() && primaryGoal in selectedGoals
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
                Text("${page + 1}/5", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { (page + 1) / 5f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = RecRed,
                trackColor = CinemaLine,
            )
            Spacer(Modifier.height(20.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (page) {
                    0 -> {
                        Icon(Icons.Outlined.AutoAwesome, "Creator mode", tint = RecRed, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("What do you create most?", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("Your primary mode sets smart defaults. Secondary modes keep FrameByNavin flexible when your work crosses niches.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(20.dp))
                        V18OnboardingLabel("PRIMARY CREATOR MODE")
                        Spacer(Modifier.height(9.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            creatorModes.forEach { item ->
                                FilterChip(
                                    selected = primaryMode == item,
                                    onClick = { primaryMode = item },
                                    leadingIcon = if (primaryMode == item) { { Icon(Icons.Outlined.Star, null, modifier = Modifier.size(15.dp)) } } else null,
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                        if (primaryMode.isNotBlank()) {
                            Spacer(Modifier.height(22.dp))
                            V18OnboardingLabel("SECONDARY MODES · OPTIONAL")
                            Spacer(Modifier.height(5.dp))
                            Text("Choose up to ${CreatorProfile.MAX_SECONDARY_MODES}. These influence suggestions but never restrict what you can make.", color = MutedText, fontSize = 10.5.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(9.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                creatorModes.filter { it != primaryMode }.forEach { item ->
                                    val selected = item in secondaryModes
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            secondaryModes = when {
                                                selected -> secondaryModes - item
                                                secondaryModes.size < CreatorProfile.MAX_SECONDARY_MODES -> secondaryModes + item
                                                else -> secondaryModes
                                            }
                                        },
                                        label = { Text(item, fontSize = 10.sp) },
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        Icon(Icons.Outlined.Hub, "Creator platforms", tint = MutedGold, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Where do you publish?", color = ProjectorIvory, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("Choose every platform you actively create for. Platforms define delivery formats; your creator mode will define what kind of content is suggested.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(20.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CreatorPlatformRegistry.supportedPlatforms.forEach { item ->
                                val selected = item in platforms
                                FilterChip(
                                    selected = selected,
                                    onClick = { platforms = if (selected) platforms - item else platforms + item },
                                    leadingIcon = if (selected) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                    }

                    2 -> {
                        Icon(Icons.Outlined.MovieCreation, "Production style", tint = MutedGold, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("How do you usually create?", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("This describes production, not your niche. It will let workflows distinguish gameplay capture from screen recording, voiceover, live work and more.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(20.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            productionStyles.forEach { item ->
                                val selected = item in styles
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        styles = when {
                                            selected -> styles - item
                                            styles.size < CreatorProfile.MAX_PRODUCTION_STYLES -> styles + item
                                            else -> styles
                                        }
                                    },
                                    leadingIcon = if (selected) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) } } else null,
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                    }

                    3 -> {
                        Icon(Icons.Outlined.TrackChanges, "Creator goals", tint = RecRed, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("What matters most right now?", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("Choose up to 3 goals. One primary goal breaks soft trade-offs; secondary goals still influence planning and review.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(18.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            creatorGoals.forEach { item ->
                                val selected = item in selectedGoals
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedGoals = when {
                                            selected && selectedGoals.size > 1 -> selectedGoals - item
                                            selected -> selectedGoals
                                            selectedGoals.size < CreatorProfile.MAX_ACTIVE_GOALS -> selectedGoals + item
                                            else -> selectedGoals
                                        }
                                        if (primaryGoal.isBlank() && item in selectedGoals) primaryGoal = item
                                    },
                                    leadingIcon = when {
                                        item == primaryGoal && selected -> { { Icon(Icons.Outlined.Star, null, modifier = Modifier.size(15.dp)) } }
                                        selected -> { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) } }
                                        else -> null
                                    },
                                    label = { Text(item, fontSize = 10.sp) },
                                )
                            }
                        }
                        if (selectedGoals.size > 1) {
                            Spacer(Modifier.height(18.dp))
                            V18OnboardingLabel("PRIMARY FOCUS")
                            Spacer(Modifier.height(7.dp))
                            Text("Choose which selected goal should lead when priorities conflict.", color = MutedText, fontSize = 10.5.sp)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedGoals.sorted().forEach { item ->
                                    FilterChip(
                                        selected = primaryGoal == item,
                                        onClick = { primaryGoal = item },
                                        leadingIcon = if (primaryGoal == item) { { Icon(Icons.Outlined.Star, null, modifier = Modifier.size(15.dp)) } } else null,
                                        label = { Text(item, fontSize = 10.sp) },
                                    )
                                }
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
                        Icon(Icons.Outlined.SettingsSuggest, "System setup", tint = MutedGold, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("System setup", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(7.dp))
                        Text("These Android permissions are optional system capabilities. They are kept separate from your creator identity and goals.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
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
                                Text("YOUR CREATOR PROFILE", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(Modifier.height(5.dp))
                                Text(primaryMode, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (secondaryModes.isNotEmpty()) Text("Also: ${secondaryModes.sorted().joinToString()}", color = MutedText, fontSize = 9.5.sp)
                                Text(platforms.sorted().joinToString(), color = MutedText, fontSize = 9.5.sp)
                                Text(styles.sorted().joinToString(), color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)
                                Spacer(Modifier.height(5.dp))
                                Text("Primary goal · $primaryGoal", color = MutedGold, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                val secondary = selectedGoals - primaryGoal
                                if (secondary.isNotEmpty()) Text("Also: ${secondary.sorted().joinToString()}", color = MutedText, fontSize = 9.5.sp)
                                Text("$weeklyTarget / week", color = MutedText, fontSize = 9.5.sp)
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
                        if (page < 4) {
                            page++
                        } else {
                            onFinish(
                                CreatorProfile(
                                    displayName = profile.displayName,
                                    category = primaryMode,
                                    primaryCreatorMode = primaryMode,
                                    secondaryCreatorModes = secondaryModes,
                                    platforms = platforms,
                                    productionStyles = styles,
                                    primaryGoal = primaryGoal,
                                    secondaryGoals = selectedGoals - primaryGoal,
                                    weeklyPublishingTarget = weeklyTarget,
                                    setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
                                ).normalized(),
                            )
                        }
                    },
                    enabled = canContinue,
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.height(50.dp),
                ) {
                    Text(if (page < 4) "CONTINUE" else "ENTER CREATOR OS", fontWeight = FontWeight.Black)
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
