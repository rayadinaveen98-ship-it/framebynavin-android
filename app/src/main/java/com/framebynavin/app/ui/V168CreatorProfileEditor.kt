@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*

private val v168Goals = listOf(
    "Publish consistently", "Grow an audience", "Improve content quality",
    "Build a creator business", "Launch a project", "Stay organized",
)

@Composable
internal fun V168CreatorProfileEditor(
    profile: CreatorProfile,
    onCancel: () -> Unit,
    onSave: (CreatorProfile) -> Boolean,
) {
    var mode by remember(profile) { mutableStateOf(profile.resolvedPrimaryCreatorMode) }
    var extraModes by remember(profile) { mutableStateOf(profile.secondaryCreatorModes) }
    var platforms by remember(profile) { mutableStateOf(profile.platforms) }
    var styles by remember(profile) { mutableStateOf(profile.productionStyles) }
    var goal by remember(profile) { mutableStateOf(profile.primaryGoal) }
    var extraGoals by remember(profile) { mutableStateOf(profile.secondaryGoals) }
    var target by remember(profile) { mutableIntStateOf(profile.weeklyPublishingTarget) }
    var moreOptions by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    val draft = CreatorProfileEditDraft(mode, extraModes, platforms, styles, goal, extraGoals, target)
    val error = CreatorProfileEditV168Policy.validate(draft)

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = ProjectorIvory)
                }
                Column(Modifier.weight(1f)) {
                    Text("MY PROFILE", color = RecRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Edit creator details", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            Text("Change what Backlot suggests. Your saved projects and weekly schedule stay as they are.",
                color = MutedText, fontSize = 12.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(start = 10.dp, bottom = 12.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                V168EditLabel("WHAT DO YOU MAKE?", "Pick your main creative focus.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    (CreatorModeRegistry.labels + mode).filter { it.isNotBlank() }.distinct().forEach { choice ->
                        FilterChip(
                            selected = choice == mode,
                            onClick = { mode = choice; extraModes = extraModes - choice },
                            label = { Text(choice, fontSize = 11.sp) },
                        )
                    }
                }
                Spacer(Modifier.height(19.dp))
                V168EditLabel("WHERE DO YOU POST?", "Choose at least one platform.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    (CreatorPlatformRegistry.definitions.map { it.name } + platforms).distinct().forEach { choice ->
                        FilterChip(
                            selected = choice in platforms,
                            onClick = { platforms = if (choice in platforms) platforms - choice else platforms + choice },
                            label = { Text(choice, fontSize = 11.sp) },
                        )
                    }
                }
                Spacer(Modifier.height(19.dp))
                V168EditLabel("YOUR MAIN GOAL", "What matters most right now?")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    (v168Goals + goal + extraGoals).filter { it.isNotBlank() }.distinct().forEach { choice ->
                        FilterChip(
                            selected = choice == goal,
                            onClick = { goal = choice; extraGoals = extraGoals - choice },
                            label = { Text(choice, fontSize = 11.sp) },
                        )
                    }
                }
                Spacer(Modifier.height(19.dp))
                V168EditLabel("POSTING TARGET", "Your goal per week, not a required quota.")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { target = (target - 1).coerceAtLeast(1) },
                        enabled = target > 1) { Text("−", fontSize = 17.sp) }
                    Spacer(Modifier.width(17.dp))
                    Text("$target per week", color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(17.dp))
                    OutlinedButton(onClick = { target = (target + 1).coerceAtMost(14) },
                        enabled = target < 14) { Text("+", fontSize = 17.sp) }
                }
                Spacer(Modifier.height(15.dp))
                TextButton(onClick = { moreOptions = !moreOptions }) {
                    Text(if (moreOptions) "HIDE EXTRA DETAILS" else "MORE OPTIONS · CREATOR TYPES & STYLES",
                        color = MutedGold, fontSize = 11.sp)
                }
                if (moreOptions) {
                    Spacer(Modifier.height(7.dp))
                    V168EditLabel("OTHER CREATOR TYPES", "Optional · up to 3.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        (CreatorModeRegistry.labels + extraModes).filter { it.isNotBlank() && it != mode }.distinct().forEach { choice ->
                            FilterChip(
                                selected = choice in extraModes,
                                onClick = {
                                    extraModes = when {
                                        choice in extraModes -> extraModes - choice
                                        extraModes.size < CreatorProfile.MAX_SECONDARY_MODES -> extraModes + choice
                                        else -> extraModes
                                    }
                                },
                                label = { Text(choice, fontSize = 11.sp) },
                            )
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    V168EditLabel("HOW DO YOU CREATE?", "Optional · select your production styles.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        (ProductionStyleRegistry.orderedForMode(mode) + styles).distinct().forEach { choice ->
                            FilterChip(
                                selected = choice in styles,
                                onClick = {
                                    styles = when {
                                        choice in styles -> styles - choice
                                        styles.size < CreatorProfile.MAX_PRODUCTION_STYLES -> styles + choice
                                        else -> styles
                                    }
                                },
                                label = { Text(choice, fontSize = 11.sp) },
                            )
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    V168EditLabel("OTHER GOALS", "Optional · up to 2.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        (v168Goals + extraGoals).filter { it.isNotBlank() && it != goal }.distinct().forEach { choice ->
                            FilterChip(
                                selected = choice in extraGoals,
                                onClick = {
                                    extraGoals = when {
                                        choice in extraGoals -> extraGoals - choice
                                        extraGoals.size < CreatorProfile.MAX_ACTIVE_GOALS - 1 -> extraGoals + choice
                                        else -> extraGoals
                                    }
                                },
                                label = { Text(choice, fontSize = 11.sp) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            if (error != null) Text(error, color = RecRed, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
            if (saveFailed) Text("Could not save creator details. Nothing was reset. Try again.",
                color = RecRed, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(49.dp),
                    border = BorderStroke(1.dp, CinemaLine)) { Text("CANCEL", fontSize = 12.sp) }
                Button(
                    onClick = {
                        saveFailed = !onSave(CreatorProfileEditV168Policy.apply(profile, draft))
                    },
                    enabled = error == null,
                    modifier = Modifier.weight(1.4f).height(49.dp).testTag("v168-save-profile"),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("SAVE CHANGES", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun V168EditLabel(title: String, subtitle: String) {
    Text(title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Text(subtitle, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)
    Spacer(Modifier.height(7.dp))
}
