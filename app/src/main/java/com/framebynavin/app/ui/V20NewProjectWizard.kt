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

internal val V20NewProjectSupportPlans: List<ProjectAttentionPlan> = ProjectAttentionPlan.entries
internal val V20NewProjectReminderStyles: List<ReminderDeliveryPreference> = ReminderDeliveryPreference.entries

internal fun v20NewProjectSupportReady(
    plan: ProjectAttentionPlan,
    reminderAtMillis: Long,
    dueAtMillis: Long,
    nowMillis: Long,
): Boolean = plan != ProjectAttentionPlan.CUSTOM ||
    (reminderAtMillis > nowMillis && reminderAtMillis <= dueAtMillis)

/**
 * New Project V2.2
 *
 * The complete project setup remains available during creation. The interaction is progressive:
 * one decision is active at a time, completed decisions collapse to editable summaries, and the
 * next card is revealed only after the current required choice is valid.
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
    deliveryPreference: ReminderDeliveryPreference,
    onDeliveryPreferenceChange: (ReminderDeliveryPreference) -> Unit,
    customReminderAt: Long,
    onPickCustomReminder: () -> Unit,
    priority: TaskPriority,
    onPriorityChange: (TaskPriority) -> Unit,
    voicePersona: VoicePersona,
    onVoicePersonaChange: (VoicePersona) -> Unit,
    onPreviewVoice: (VoicePersona) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    reminderSetupReady: Boolean,
    requiresAdvancedPermissions: Boolean,
    onOpenSettings: () -> Unit,
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
    val typeOptions = recommendedTypes.take(4)
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

    val customReady = v20NewProjectSupportReady(attentionPlan, customReminderAt, dueAt, now)
    val supportReady = customReady
    val allReady = title.isNotBlank() && archetypeId.isNotBlank() && platform.isNotBlank() &&
        contentType.isNotBlank() && dueAt > now && supportReady
    val progress = ((revealedStage + 1).coerceIn(1, 7)) / 7f
    val supportSummary = buildString {
        append(ProjectPulseEngine.planLabel(attentionPlan))
        if (attentionPlan != ProjectAttentionPlan.OFF) {
            append(" · ")
            append(pDeliveryPreferenceLabel(deliveryPreference))
        }
    }


    if (showAllTypes) {
        Dialog(
            onDismissRequest = { showAllTypes = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("CONTENT TYPES", color = RecRed, fontSize = 8.4.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Choose the right format", color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Black)
                            Text(CreatorModeRegistry.definition(creatorModeId).label, color = MutedGold, fontSize = 9.sp)
                        }
                        IconButton(onClick = { showAllTypes = false }) {
                            Icon(Icons.Outlined.Close, "Close content types", tint = ProjectorIvory)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        V20FieldLabel("RECOMMENDED")
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recommendedTypes.forEach { archetype ->
                                val label = runCatching { ContentArchetypeRegistry.labelForMode(archetype.id, creatorModeId) }.getOrDefault(archetype.label)
                                FilterChip(
                                    selected = archetypeId == archetype.id,
                                    onClick = { onArchetypeChange(archetype.id); showAllTypes = false },
                                    leadingIcon = if (archetypeId == archetype.id) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) } } else null,
                                    label = { Text(label, fontSize = 9.sp) },
                                )
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        V20FieldLabel("ALL TYPES")
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ContentArchetypeRegistry.definitions.filter { option -> recommendedTypes.none { it.id == option.id } }.forEach { archetype ->
                                val label = runCatching { ContentArchetypeRegistry.labelForMode(archetype.id, creatorModeId) }.getOrDefault(archetype.label)
                                FilterChip(
                                    selected = archetypeId == archetype.id,
                                    onClick = { onArchetypeChange(archetype.id); showAllTypes = false },
                                    leadingIcon = if (archetypeId == archetype.id) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) } } else null,
                                    label = { Text(label, fontSize = 9.sp) },
                                )
                            }
                        }
                        Spacer(Modifier.height(30.dp))
                    }
                }
            }
        }
    }

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
                        Text("Create project", color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black)
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
                    V20ProgressiveSection(
                        index = 0,
                        label = "PROJECT NAME",
                        title = "What are you making?",
                        summary = title.ifBlank { "Add a project name" },
                        active = activeStage == 0,
                        revealed = true,
                        onEdit = { activeStage = 0 },
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Project title") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        V20InlineContinue("CHOOSE PROJECT TYPE", title.isNotBlank()) { revealNext(0) }
                    }

                    V20ProgressiveSection(
                        index = 1,
                        label = "CONTENT TYPE",
                        title = "What kind of project is it?",
                        summary = chosenTypeLabel,
                        active = activeStage == 1,
                        revealed = revealedStage >= 1,
                        onEdit = { activeStage = 1 },
                    ) {
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
                            TextButton(onClick = { showAllTypes = true }) {
                                Text("BROWSE ALL TYPES", fontSize = 8.sp, fontWeight = FontWeight.Black)
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
                                        productionStyles.takeIf { it.isNotEmpty() }?.joinToString() ?: "Using setup default",
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

                    V20ProgressiveSection(
                        index = 2,
                        label = "PLATFORM",
                        title = "Where will you publish it?",
                        summary = platform.ifBlank { "Choose a platform" },
                        active = activeStage == 2,
                        revealed = revealedStage >= 2,
                        onEdit = { activeStage = 2 },
                    ) {
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

                    V20ProgressiveSection(
                        index = 3,
                        label = "FORMAT",
                        title = "How are you publishing it?",
                        summary = contentType.ifBlank { "Choose a format" },
                        active = activeStage == 3,
                        revealed = revealedStage >= 3,
                        onEdit = { activeStage = 3 },
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            formatOptions.forEach { value ->
                                FilterChip(selected = contentType == value, onClick = { onContentTypeChange(value) }, label = { Text(value, fontSize = 9.3.sp) })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        V20InlineContinue("SET DEADLINE", contentType.isNotBlank()) { revealNext(3) }
                    }

                    V20ProgressiveSection(
                        index = 4,
                        label = "DEADLINE",
                        title = "When do you want it ready?",
                        summary = v20WizardDateTime(dueAt),
                        active = activeStage == 4,
                        revealed = revealedStage >= 4,
                        onEdit = { activeStage = 4 },
                    ) {
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

                    V20ProgressiveSection(
                        index = 5,
                        label = "PROJECT SUPPORT",
                        title = "How much help should Backlot give?",
                        summary = supportSummary,
                        active = activeStage == 5,
                        revealed = revealedStage >= 5,
                        onEdit = { activeStage = 5 },
                    ) {
                        V20NewProjectSupportPlans.forEach { plan ->
                            V20WizardAttentionCard(plan, selected = attentionPlan == plan) { onAttentionPlanChange(plan) }
                            Spacer(Modifier.height(8.dp))
                        }

                        if (attentionPlan != ProjectAttentionPlan.OFF) {
                            Spacer(Modifier.height(4.dp))
                            V20FieldLabel("REMINDER STYLE")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                V20NewProjectReminderStyles.forEach { value ->
                                    FilterChip(
                                        selected = deliveryPreference == value,
                                        onClick = { onDeliveryPreferenceChange(value) },
                                        label = { Text(pDeliveryPreferenceLabel(value), fontSize = 9.sp) },
                                    )
                                }
                            }
                        }

                        if (attentionPlan != ProjectAttentionPlan.OFF) {
                            Spacer(Modifier.height(14.dp))
                            V140VoiceStudioPicker(
                                selected = voicePersona,
                                onSelected = onVoicePersonaChange,
                                onPreview = onPreviewVoice,
                            )
                        }

                        if (attentionPlan == ProjectAttentionPlan.CUSTOM) {
                            Spacer(Modifier.height(14.dp))
                            V20FieldLabel("REMINDER TIME")
                            V20DateTimeButton(v20WizardDateTime(customReminderAt), onPickCustomReminder)
                            Spacer(Modifier.height(12.dp))
                            V20FieldLabel("IMPORTANCE")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                TaskPriority.entries.forEach { value ->
                                    FilterChip(
                                        selected = priority == value,
                                        onClick = { onPriorityChange(value) },
                                        label = { Text(pPriorityLabel(value), fontSize = 9.sp) },
                                    )
                                }
                            }
                            if (!customReady) {
                                Spacer(Modifier.height(6.dp))
                                Text("Choose a reminder between now and the deadline.", color = RecRed, fontSize = 8.5.sp)
                            }
                        }

                        if (requiresAdvancedPermissions && !reminderSetupReady) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                onClick = onOpenSettings,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF17130F),
                                border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f)),
                            ) {
                                Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Settings, null, tint = MutedGold, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Finish reminder setup", color = ProjectorIvory, fontSize = 9.5.sp, modifier = Modifier.weight(1f))
                                    Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        V20FieldLabel("NOTES · OPTIONAL")
                        OutlinedTextField(
                            value = notes,
                            onValueChange = onNotesChange,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp),
                            placeholder = { Text("Angle, reference, anything worth remembering…") },
                            shape = RoundedCornerShape(16.dp),
                        )

                        Spacer(Modifier.height(12.dp))
                        V20InlineContinue("REVIEW PROJECT", supportReady) { revealNext(5) }
                    }

                    V20ProgressiveSection(
                        index = 6,
                        label = "REVIEW",
                        title = "Ready to create?",
                        summary = "Review & create",
                        active = activeStage == 6,
                        revealed = revealedStage >= 6,
                        onEdit = { activeStage = 6 },
                    ) {
                        V20WizardReviewRow("PROJECT", title) { activeStage = 0 }
                        V20WizardReviewRow("TYPE", chosenTypeLabel) { activeStage = 1 }
                        V20WizardReviewRow("PLATFORM", platform) { activeStage = 2 }
                        V20WizardReviewRow("FORMAT", contentType) { activeStage = 3 }
                        V20WizardReviewRow("PUBLISH BY", v20WizardDateTime(dueAt)) { activeStage = 4 }
                        V20WizardReviewRow("SUPPORT", supportSummary) { activeStage = 5 }
                        if (attentionPlan == ProjectAttentionPlan.CUSTOM) {
                            V20WizardReviewRow("REMINDER", v20WizardDateTime(customReminderAt)) { activeStage = 5 }
                            V20WizardReviewRow("IMPORTANCE", pPriorityLabel(priority)) { activeStage = 5 }
                        }
                        if (notes.isNotBlank()) {
                            V20WizardReviewRow("NOTES", notes) { activeStage = 5 }
                        }
                        if (productionStyles.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Creative style · ${productionStyles.joinToString()}", color = MutedText, fontSize = 8.2.sp, lineHeight = 12.sp)
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
private fun V20FieldLabel(text: String) {
    Text(text, color = MutedGold, fontSize = 8.3.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 7.dp))
}

@Composable
private fun V20DateTimeButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        border = BorderStroke(1.dp, CinemaLine),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = MutedGold, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = ProjectorIvory, modifier = Modifier.weight(1f))
        Text("CHANGE", color = RecRed, fontSize = 8.sp)
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
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) RecRed.copy(alpha = .10f) else CinemaSurface,
        border = BorderStroke(1.dp, if (selected) RecRed.copy(alpha = .75f) else CinemaLine),
    ) {
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
