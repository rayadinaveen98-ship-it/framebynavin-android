package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V20NewProjectWizard(
    profile: CreatorProfile,
    title: String,
    onTitleChange: (String) -> Unit,
    creatorModeId: String,
    onCreatorModeChange: (String) -> Unit,
    archetypeId: String,
    onArchetypeChange: (String) -> Unit,
    productionStyles: Set<String>,
    onProductionStylesChange: (Set<String>) -> Unit,
    platform: String,
    onPlatformChange: (String) -> Unit,
    contentType: String,
    onContentTypeChange: (String) -> Unit,
    dueAt: Long,
    onPickDue: () -> Unit,
    attentionPlan: ProjectAttentionPlan,
    onAttentionPlanChange: (ProjectAttentionPlan) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var showAllModes by rememberSaveable { mutableStateOf(false) }
    var showAllTypes by rememberSaveable { mutableStateOf(false) }
    var showAllPlatforms by rememberSaveable { mutableStateOf(false) }
    var customizeStyles by rememberSaveable { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val stepCount = 7

    val selectedModeIds = remember(profile, creatorModeId) {
        buildList {
            add(CreatorModeRegistry.definition(profile.resolvedPrimaryCreatorMode).id)
            profile.secondaryCreatorModes.forEach { add(CreatorModeRegistry.definition(it).id) }
            if (creatorModeId.isNotBlank()) add(CreatorModeRegistry.definition(creatorModeId).id)
        }.filter { it.isNotBlank() }.distinct()
    }
    val modeOptions = if (showAllModes) CreatorModeRegistry.definitions.map { it.id } else selectedModeIds
    val recommendedTypes = remember(creatorModeId) { ContentArchetypeRegistry.suggestedForMode(creatorModeId) }
    val typeOptions = if (showAllTypes) ContentArchetypeRegistry.definitions else recommendedTypes
    val selectedPlatforms = remember(profile, platform) { CreatorPlatformRegistry.orderedSelected(profile, include = platform) }
    val platformOptions = if (showAllPlatforms) CreatorPlatformRegistry.supportedPlatforms else selectedPlatforms
    val formatOptions = remember(platform) { CreatorPlatformRegistry.formats(platform) }
    val styleOptions = remember(creatorModeId) { ProductionStyleRegistry.orderedForMode(creatorModeId) }
    val chosenTypeLabel = runCatching { ContentArchetypeRegistry.labelForMode(archetypeId, creatorModeId) }
        .getOrElse { ContentArchetypeRegistry.definition(archetypeId)?.label ?: "Content" }

    val canContinue = when (step) {
        0 -> title.isNotBlank()
        1 -> archetypeId.isNotBlank()
        2 -> platform.isNotBlank()
        3 -> contentType.isNotBlank()
        4 -> dueAt > now
        5 -> attentionPlan in setOf(ProjectAttentionPlan.OFF, ProjectAttentionPlan.LIGHT, ProjectAttentionPlan.GUIDED)
        else -> title.isNotBlank() && dueAt > now
    }

    val heading = when (step) {
        0 -> "What are you making?"
        1 -> "What kind of project is it?"
        2 -> "Where will you publish it?"
        3 -> "Choose the format"
        4 -> "When do you want it ready?"
        5 -> "How much help should FrameByNavin give?"
        else -> "Everything look right?"
    }
    val eyebrow = when (step) {
        0 -> "PROJECT NAME"
        1 -> "CONTENT TYPE"
        2 -> "PLATFORM"
        3 -> "FORMAT"
        4 -> "DEADLINE"
        5 -> "PROJECT SUPPORT"
        else -> "REVIEW"
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { if (step == 0) onDismiss() else step-- }) {
                        Icon(Icons.Outlined.ArrowBack, if (step == 0) "Close" else "Previous step", tint = ProjectorIvory)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NEW PROJECT · ${step + 1}/$stepCount", color = RecRed, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Text(heading, color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2)
                    }
                }

                LinearProgressIndicator(
                    progress = { (step + 1) / stepCount.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = RecRed,
                    trackColor = CinemaLine,
                )

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Text(eyebrow, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))

                    when (step) {
                        0 -> {
                            Text("Start with only the name. We'll shape everything else one decision at a time.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(18.dp))
                            OutlinedTextField(
                                value = title,
                                onValueChange = onTitleChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. OG universe analysis") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                            )
                        }

                        1 -> {
                            Text("Recommended from your creator setup. You can still choose something different.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(14.dp))
                            V20WizardSummaryChip("CREATOR MODE", CreatorModeRegistry.definition(creatorModeId).label)
                            Spacer(Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                typeOptions.forEach { archetype ->
                                    val label = runCatching { ContentArchetypeRegistry.labelForMode(archetype.id, creatorModeId) }.getOrDefault(archetype.label)
                                    FilterChip(
                                        selected = archetypeId == archetype.id,
                                        onClick = { onArchetypeChange(archetype.id) },
                                        label = { Text(label, fontSize = 9.sp) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showAllTypes = !showAllTypes }) {
                                Text(if (showAllTypes) "SHOW RECOMMENDED TYPES" else "SEE ALL CONTENT TYPES", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                            TextButton(onClick = { showAllModes = !showAllModes }) {
                                Text(if (showAllModes) "HIDE OTHER CREATOR MODES" else "CHANGE CREATOR MODE", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                            if (showAllModes) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    modeOptions.forEach { modeId ->
                                        val mode = CreatorModeRegistry.definition(modeId)
                                        FilterChip(
                                            selected = CreatorModeRegistry.definition(creatorModeId).id == mode.id,
                                            onClick = { onCreatorModeChange(mode.id); showAllTypes = false },
                                            label = { Text(mode.label, fontSize = 8.5.sp) },
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                onClick = { customizeStyles = !customizeStyles },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = CinemaSurface,
                                border = BorderStroke(1.dp, CinemaLine),
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("CREATIVE STYLE", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            productionStyles.takeIf { it.isNotEmpty() }?.joinToString() ?: "Using your setup default",
                                            color = MutedText,
                                            fontSize = 8.sp,
                                            maxLines = 2,
                                        )
                                    }
                                    Icon(if (customizeStyles) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, tint = MutedText)
                                }
                            }
                            AnimatedVisibility(customizeStyles) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        styleOptions.forEach { style ->
                                            val selected = style in productionStyles
                                            FilterChip(
                                                selected = selected,
                                                onClick = {
                                                    val next = if (selected) productionStyles - style
                                                    else if (productionStyles.size < CreatorProfile.MAX_PRODUCTION_STYLES) productionStyles + style
                                                    else productionStyles
                                                    onProductionStylesChange(next)
                                                },
                                                label = { Text(style, fontSize = 8.4.sp) },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            Text("Your setup platforms appear first. Open the full list only when this project is different.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(14.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                platformOptions.forEach { value ->
                                    FilterChip(selected = platform == value, onClick = { onPlatformChange(value) }, label = { Text(value, fontSize = 9.5.sp) })
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showAllPlatforms = !showAllPlatforms }) {
                                Text(if (showAllPlatforms) "SHOW MY PLATFORMS" else "OTHER PLATFORMS", fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        3 -> {
                            Text("Only formats supported by $platform are shown here.", color = MutedText, fontSize = 10.sp)
                            Spacer(Modifier.height(14.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                formatOptions.forEach { value ->
                                    FilterChip(selected = contentType == value, onClick = { onContentTypeChange(value) }, label = { Text(value, fontSize = 9.5.sp) })
                                }
                            }
                        }

                        4 -> {
                            Text("Set the publish/deadline target. You can always adjust it later.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                onClick = onPickDue,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = CinemaSurface,
                                border = BorderStroke(1.dp, CinemaLine),
                            ) {
                                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CalendarMonth, null, tint = MutedGold, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(v20WizardDateTime(dueAt), color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        5 -> {
                            Text("Keep creation simple now. Advanced voice, alarm and custom reminder controls stay available in Edit Project later.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(14.dp))
                            listOf(ProjectAttentionPlan.OFF, ProjectAttentionPlan.LIGHT, ProjectAttentionPlan.GUIDED).forEach { plan ->
                                V20WizardAttentionCard(plan, selected = attentionPlan == plan) { onAttentionPlanChange(plan) }
                                Spacer(Modifier.height(8.dp))
                            }
                            if (attentionPlan == ProjectAttentionPlan.GUIDED) {
                                Text("Recommended · FrameByNavin adapts check-ins as the project moves through stages.", color = MutedGold, fontSize = 8.5.sp, lineHeight = 12.sp)
                            }
                        }

                        else -> {
                            Text("These are the choices that will create your project. Tap any row to change it.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                            Spacer(Modifier.height(14.dp))
                            V20WizardReviewRow("PROJECT", title) { step = 0 }
                            V20WizardReviewRow("TYPE", chosenTypeLabel) { step = 1 }
                            V20WizardReviewRow("PLATFORM", platform) { step = 2 }
                            V20WizardReviewRow("FORMAT", contentType) { step = 3 }
                            V20WizardReviewRow("PUBLISH BY", v20WizardDateTime(dueAt)) { step = 4 }
                            V20WizardReviewRow("SUPPORT", ProjectPulseEngine.planLabel(attentionPlan)) { step = 5 }
                            if (productionStyles.isNotEmpty()) {
                                Spacer(Modifier.height(7.dp))
                                Text("Using your creative setup · ${productionStyles.joinToString()}", color = MutedText, fontSize = 8.2.sp, lineHeight = 12.sp)
                            }
                            Spacer(Modifier.height(14.dp))
                            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color(0xFF15130F), border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f))) {
                                Column(Modifier.padding(13.dp)) {
                                    Text("NEXT", color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                                    Spacer(Modifier.height(3.dp))
                                    Text("Create it and FrameByNavin will open this project's workspace immediately.", color = ProjectorIvory, fontSize = 9.5.sp, lineHeight = 14.sp)
                                }
                            }
                        }
                    }
                }

                Surface(color = Color(0xF20B0B0C), tonalElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (step > 0) {
                            OutlinedButton(
                                onClick = { step-- },
                                modifier = Modifier.weight(.7f).height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, CinemaLine),
                            ) { Text("BACK", fontSize = 9.sp, fontWeight = FontWeight.Black) }
                        }
                        Button(
                            onClick = { if (step < stepCount - 1) step++ else onCreate() },
                            enabled = canContinue,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        ) {
                            Text(if (step < stepCount - 1) "CONTINUE" else "CREATE & OPEN", fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(5.dp))
                            Icon(if (step < stepCount - 1) Icons.Outlined.ArrowForward else Icons.Outlined.PlayArrow, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V20WizardSummaryChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .10f), border = BorderStroke(1.dp, MutedGold.copy(alpha = .22f))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$label · ", color = MutedText, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MutedGold, fontSize = 8.2.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V20WizardReviewRow(label: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp), shape = RoundedCornerShape(15.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = MutedText, fontSize = 7.2.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
                Spacer(Modifier.height(2.dp))
                Text(value, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.Edit, "Change $label", tint = MutedGold, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun V20WizardAttentionCard(plan: ProjectAttentionPlan, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = if (selected) RecRed.copy(alpha = .10f) else CinemaSurface, border = BorderStroke(1.dp, if (selected) RecRed.copy(alpha = .75f) else CinemaLine)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (plan) {
                    ProjectAttentionPlan.OFF -> Icons.Outlined.NotificationsOff
                    ProjectAttentionPlan.LIGHT -> Icons.Outlined.NotificationsNone
                    ProjectAttentionPlan.GUIDED -> Icons.Outlined.NotificationsActive
                    ProjectAttentionPlan.URGENT -> Icons.Outlined.Alarm
                    ProjectAttentionPlan.CUSTOM -> Icons.Outlined.Tune
                },
                null,
                tint = if (selected) RecRed else MutedGold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(ProjectPulseEngine.planLabel(plan), color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Text(ProjectPulseEngine.planDescription(plan), color = MutedText, fontSize = 8.7.sp, lineHeight = 12.sp)
            }
            if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = RecRed, modifier = Modifier.size(18.dp))
        }
    }
}

private fun v20WizardDateTime(millis: Long): String =
    if (millis <= 0L) "Choose date & time" else SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
