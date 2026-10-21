@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorModeRegistry
import com.framebynavin.app.data.CreatorPlatformRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.ui.theme.*

/**
 * A deliberately short first-run creator setup. Sign-in and recovery remain
 * separate and optional; the saved creator profile remains backward compatible.
 * Permission setup happens contextually when a reminder needs it.
 */
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
    onFinish: (CreatorProfile) -> Boolean,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var primaryMode by rememberSaveable(profile.resolvedPrimaryCreatorMode) {
        mutableStateOf(profile.resolvedPrimaryCreatorMode)
    }
    var platformSelections by rememberSaveable {
        mutableStateOf(ArrayList(profile.platforms.sorted()))
    }
    val platforms = platformSelections.toSet()
    val canContinue = V207FirstRunPolicy.canContinue(page, primaryMode, platforms) && !saving

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "BACKLOT", color = RecRed, fontSize = 11.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp,
                    )
                    Text("Set up your space", color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Text(
                    "${page + 1}/${V207FirstRunPolicy.PAGE_COUNT}",
                    color = MutedText, fontSize = 12.sp,
                    modifier = Modifier.testTag("v207-first-run-progress"),
                )
            }
            Spacer(Modifier.height(15.dp))
            LinearProgressIndicator(
                progress = { (page + 1) / V207FirstRunPolicy.PAGE_COUNT.toFloat() },
                color = RecRed,
                trackColor = CinemaLine,
                modifier = Modifier.fillMaxWidth().height(4.dp),
            )
            Spacer(Modifier.height(30.dp))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (page) {
                    0 -> {
                        Icon(Icons.Outlined.AutoAwesome, "Creator type", tint = RecRed, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("What do you create?", color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(20.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            CreatorModeRegistry.labels.forEach { mode ->
                                FilterChip(
                                    selected = primaryMode == mode,
                                    onClick = { primaryMode = mode },
                                    modifier = Modifier.heightIn(min = 46.dp),
                                    label = { Text(mode, fontSize = 12.sp) },
                                    leadingIcon = if (primaryMode == mode) {
                                        { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                )
                            }
                        }
                    }
                    1 -> {
                        Icon(Icons.Outlined.Hub, "Publishing platforms", tint = MutedGold, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Where do you publish?", color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(8.dp))
                        Text("Pick at least one.", color = MutedText, fontSize = 13.sp)
                        Spacer(Modifier.height(18.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            CreatorPlatformRegistry.supportedPlatforms.forEach { platform ->
                                val selected = platform in platforms
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        platformSelections = ArrayList(
                                            if (selected) platforms - platform else platforms + platform
                                        )
                                    },
                                    modifier = Modifier.heightIn(min = 46.dp),
                                    label = { Text(platform, fontSize = 12.sp) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                )
                            }
                        }
                    }
                    else -> {
                        Icon(Icons.Outlined.RocketLaunch, "Ready to create", tint = RecRed, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("You're ready to create.", color = ProjectorIvory, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(17.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth().testTag("v207-first-run-review"),
                            shape = RoundedCornerShape(18.dp), color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                                V207ReviewRow("CREATOR", primaryMode)
                                V207ReviewRow("PLATFORMS", platforms.sorted().joinToString())
                                V207ReviewRow("STARTING FOCUS", V207FirstRunPolicy.resolvedGoal(profile))
                            }
                        }
                        Spacer(Modifier.height(13.dp))
                        Text(
                            "You can change your focus, production style and reminder preferences in Settings.",
                            color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            V197OnboardingActions(
                page = page,
                hasProductionStyles = false,
                enabled = canContinue,
                onBack = { if (page > 0) { page--; error = false } },
                onPrimary = {
                    if (page < V207FirstRunPolicy.LAST_PAGE) {
                        page++
                        error = false
                    } else if (!saving) {
                        saving = true
                        error = false
                        val complete = runCatching {
                            onFinish(V207FirstRunPolicy.completeProfile(profile, primaryMode, platforms))
                        }.getOrDefault(false)
                        if (!complete) {
                            error = true
                            saving = false
                        }
                    }
                },
                lastPage = V207FirstRunPolicy.LAST_PAGE,
            )
            if (error) {
                Spacer(Modifier.height(9.dp))
                Text("Could not save creator setup. Try again.", color = RecRed, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun V207ReviewRow(label: String, value: String) {
    Column {
        Text(label, color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
