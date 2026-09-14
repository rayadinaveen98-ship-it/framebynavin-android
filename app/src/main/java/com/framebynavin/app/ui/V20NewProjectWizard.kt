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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * New Project V2.1
 *
 * This intentionally stays on ONE scrollable surface. We progressively reveal the next decision
 * only after the creator finishes the current one. Completed decisions collapse to compact,
 * editable summaries instead of becoming seven separate pages.
 */
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
    var revealedStage by rememberSaveable { mutableIntStateOf(0) }
    var activeStage by rememberSaveable { mutableIntStateOf(0) }
    var showAllModes by rememberSaveable { mutableStateOf(false) }
    var showAllTypes by rememberSaveable { mutableStateOf(false) }
    var showAllPlatforms by rememberSaveable { mutableStateOf(false) }
    var customizeStyles by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val now = System.currentTimeMillis()

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

    fun revealNext(from: Int) {
        val next = (from + 1).coerceAtMost(6)
        revealedStage = max(revealedStage, next)
        activeStage = next
    }

    LaunchedEffect(activeStage, revealedStage) {
        if (activeStage > 0) {
            delay(120L)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val basicAttentionPlan = attentionPlan in setOf(
        ProjectAttentionPlan.OFF,
        ProjectAttentionPlan.LIGHT,
        ProjectAttentionPlan.GUIDED,
    )
    val allReady = title.isNotBlank() && archetypeId.isNotBlank() && platform.isNotBlank() &&
        contentType.isNotBlank() && dueAt > now && basicAttentionPlan
    val progress = ((revealedStage + 1).coerceIn(1, 7)) / 7f

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.ArrowBack, "Close new project", tint = ProjectorIvory)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text("NEW PROJECT", color = RecRed, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Text("Build it one decision at a time", color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text("Everything stays on this page.", color = MutedText, fontSize = 8.7.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MutedGold.copy(alpha = .10f),
                        border = BorderStroke(1.dp, MutedGold.copy(alpha = .20f)),
                    ) {
                        Text(
                            "${(revealedStage + 1).coerceAtMost(7)}/7",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = MutedGold,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = RecRed,
                    trackColor = CinemaLine,
                )

                Column(
                    Modifier.weight(1f).verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .padding(bottom = 30.dp),
                ) {
                    // 1 · Project name
                    V20ProgressiveSection(
                        index = 0,
                        label = "PROJECT NAME",
                        title = "What are you making?",
                        summary = title.ifBlank { "Add a project name" },
                        active = activeStage == 0,
                        revealed = true,
                        onEdit = { activeStage = 0 },
                    ) {
                        Text("Start with the name. We’ll reveal the next choice when you’re ready.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. OG universe analysis") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        V20InlineContinue("CHOOSE PROJECT TYPE", title.isNotBlank()) { revealNext(0) }
                    }

                    // 2 · Type + creator mode/style
                    V20ProgressiveSection(
                        index = 1,
                        label = "CONTENT TYPE",
                        title = "What kind of project is it?",
                        summary = chosenTypeLabel,
                        active = activeStage == 1,
                        revealed = revealedStage >= 1,
                        onEdit = { activeStage = 1 },
                    ) {
                        Text("Recommended from your creator setup. You can still choose something different.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                        Spacer(Modifier.height(12.dp))
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(onClick = { showAllTypes = !showAllTypes }) {
                                Text(if (showAllTypes) "RECOMMENDED ONLY" else "SEE ALL TYPES", fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                            TextButton(onClick = { showAllModes = !showAllModes }) {
                                Text(if (showAllModes) "HIDE MODES" else "CHANGE MODE", fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        AnimatedVisibility(showAllModes) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                modeOptions.forEach { modeId ->
                                    val mode = CreatorModeRegistry.definition(modeId)
                                    FilterChip(
                                        selected = CreatorModeRegistry.definition(creatorModeId).id == mode.id,
                                        onClick = { onCreatorModeChange(mode.id); showAllTypes = false },
                                        label = { Text(mode.label, fontSize = 8.3.sp) },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            onClick = { customizeStyles = !customizeStyles },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("CREATIVE STYLE", color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                            FlowRow(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
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
                                        label = { Text(style, fontSize = 8.2.sp) },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        V20InlineContinue("CHOOSE PLATFORM", archetypeId.isNotBlank()) { revealNext(1) }
                    }

                    // 3 · Platform
                    V20ProgressiveSection(
                        index = 2,
                        label = "PLATFORM",
                        title = "Where will you publish it?",
                        summary = platform.ifBlank { "Choose a platform" },
                        active = activeStage == 2,
                        revealed = revealedStage >= 2,
                        onEdit = { activeStage = 2 },
                    ) {
                        Text("Your usual platforms appear first.", color = MutedText, fontSize = 10.sp)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            platformOptions.forEach { value ->
                                FilterChip(selected = platform == value, onClick = { onPlatformChange(value) }, label = { Text(value, fontSize = 9.3.sp) })
                            }
                        }
                        TextButton(onClick = { showAllPlatforms = !showAllPlatforms }) {
                            Text(if (showAllPlatforms) "SHOW MY PLATFORMS" else "OTHER PLATFORMS", fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        V20InlineContinue("CHOOSE FORMAT", platform.isNotBlank()) { revealNext(2) }
                    }

                    // 4 · Format
                    V20ProgressiveSection(
                        index = 3,
                        label = "FORMAT",
                        title = "How are you publishing it?",
                        summary = contentType.ifBlank { "Choose a format" },
                        active = activeStage == 3,
                        revealed = revealedStage >= 3,
                        onEdit = { activeStage = 3 },
                    ) {
                        Text("Only formats supported by $platform are shown.", color = MutedText, fontSize = 10.sp)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            formatOptions.forEach { value ->
                                FilterChip(selected = contentType == value, onClick = { onContentTypeChange(value) }, label = { Text(value, fontSize = 9.3.sp) })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        V20InlineContinue("SET DEADLINE", contentType.isNotBlank()) { revealNext(3) }
                    }

                    // 5 · Deadline
                    V20ProgressiveSection(
                        index = 4,
                        label = "DEADLINE",
                        title = "When do you want it ready?",
                        summary = v20WizardDateTime(dueAt),
                        active = activeStage == 4,
                        revealed = revealedStage >= 4,
                        onEdit = { activeStage = 4 },
                    ) {
                        Text("Set the publish/deadline target. You can adjust it later.", color = MutedText, fontSize = 10.sp)
                        Spacer(Modifier.height(11.dp))
                        Surface(
                            onClick = onPickDue,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CalendarMonth, null, tint = MutedGold, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(v20WizardDateTime(dueAt), color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        V20InlineContinue("CHOOSE SUPPORT", dueAt > now) { revealNext(4) }
                    }

                    // 6 · Support
                    V20ProgressiveSection(
                        index = 5,
                        label = "PROJECT SUPPORT",
                        title = "How much help should FrameByNavin give?",
                        summary = ProjectPulseEngine.planLabel(attentionPlan),
                        active = activeStage == 5,
                        revealed = revealedStage >= 5,
                        onEdit = { activeStage = 5 },
                    ) {
                        Text("Advanced reminder controls stay available in Edit Project later.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                        Spacer(Modifier.height(10.dp))
                        listOf(ProjectAttentionPlan.OFF, ProjectAttentionPlan.LIGHT, ProjectAttentionPlan.GUIDED).forEach { plan ->
                            V20WizardAttentionCard(plan, selected = attentionPlan == plan) { onAttentionPlanChange(plan) }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (attentionPlan == ProjectAttentionPlan.GUIDED) {
                            Text("Recommended · adapts check-ins as the project moves through stages.", color = MutedGold, fontSize = 8.3.sp, lineHeight = 12.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                        V20InlineContinue("REVIEW PROJECT", basicAttentionPlan) { revealNext(5) }
                    }

                    // 7 · Review + create
                    V20ProgressiveSection(
                        index = 6,
                        label = "REVIEW",
                        title = "Ready to create?",
                        summary = "Review & create",
                        active = activeStage == 6,
                        revealed = revealedStage >= 6,
                        onEdit = { activeStage = 6 },
                    ) {
                        Text("Everything stays editable. Create opens the real project workspace immediately.", color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)
                        Spacer(Modifier.height(12.dp))
                        V20WizardReviewRow("PROJECT", title) { activeStage = 0 }
                        V20WizardReviewRow("TYPE", chosenTypeLabel) { activeStage = 1 }
                        V20WizardReviewRow("PLATFORM", platform) { activeStage = 2 }
                        V20WizardReviewRow("FORMAT", contentType) { activeStage = 3 }
                        V20WizardReviewRow("PUBLISH BY", v20WizardDateTime(dueAt)) { activeStage = 4 }
                        V20WizardReviewRow("SUPPORT", ProjectPulseEngine.planLabel(attentionPlan)) { activeStage = 5 }
                        if (productionStyles.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Creative setup · ${productionStyles.joinToString()}", color = MutedText, fontSize = 8.2.sp, lineHeight = 12.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onCreate,
                            enabled = allReady,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        ) {
                            Text("CREATE & OPEN WORKSPACE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V20ProgressiveSection(
    index: Int,
    label: String,
    title: String,
    summary: String,
    active: Boolean,
    revealed: Boolean,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    AnimatedVisibility(visible = revealed) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            if (active) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF111113),
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .38f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(100.dp), color = RecRed.copy(alpha = .12f)) {
                                Text(
                                    "${index + 1}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = RecRed,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(label, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(title, color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black, lineHeight = 22.sp)
                        Spacer(Modifier.height(12.dp))
                        content()
                    }
                }
            } else {
                Surface(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CinemaSurface.copy(alpha = .82f),
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label, color = MutedText, fontSize = 7.4.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                            Text(summary, color = ProjectorIvory, fontSize = 10.3.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("CHANGE", color = MutedGold, fontSize = 7.8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun V20InlineContinue(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(47.dp),
        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Outlined.ArrowDownward, null, modifier = Modifier.size(15.dp))
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
