package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.platform.testTag
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

/** Quick and advanced project creation must obey the same save requirements. */
internal object V153QuickCreatePolicy {
    fun canCreate(
        title: String,
        archetypeId: String,
        platform: String,
        format: String,
        dueAtMillis: Long,
        nowMillis: Long,
        supportReady: Boolean,
    ): Boolean = title.isNotBlank() && archetypeId.isNotBlank() &&
        platform.isNotBlank() && format.isNotBlank() &&
        dueAtMillis > nowMillis && supportReady
}

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
    onPreviewVoice: (VoicePersona) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    reminderSetupReady: Boolean,
    requiresAdvancedPermissions: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    startWithQuickCreate: Boolean = false,
) {
    var revealedStage by rememberSaveable { mutableIntStateOf(0) }
    var activeStage by rememberSaveable { mutableIntStateOf(0) }
    var showAllModes by rememberSaveable { mutableStateOf(false) }
    var showAllTypes by rememberSaveable { mutableStateOf(false) }
    var showAllPlatforms by rememberSaveable { mutableStateOf(false) }
    var customizeStyles by rememberSaveable { mutableStateOf(false) }
    var showReminderAdvanced by rememberSaveable { mutableStateOf(false) }
    // Quick Create initially shows one meaningful reminder summary, not five competing controls.
    var showQuickReminderChoices by rememberSaveable { mutableStateOf(false) }
    // V154: the original seven-step experience is the default. Quick Create is an explicit shortcut.
    var showAdvancedSetup by rememberSaveable(startWithQuickCreate) { mutableStateOf(!startWithQuickCreate) }
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

    val customReady = v20NewProjectSupportReady(attentionPlan, customReminderAt, dueAt, now)
    val supportReady = customReady
    val allReady = V153QuickCreatePolicy.canCreate(
        title = title,
        archetypeId = archetypeId,
        platform = platform,
        format = contentType,
        dueAtMillis = dueAt,
        nowMillis = now,
        supportReady = supportReady,
    )
    val quickReady = V174QuickCreatePolicy.valid(
        title = title,
        archetypeId = archetypeId,
        platform = platform,
        format = contentType,
        dueAtMillis = dueAt,
        reminderAtMillis = customReminderAt,
        attentionPlan = attentionPlan,
        nowMillis = now,
    )
    val progress = ((activeStage + 1).coerceIn(1, 7)) / 7f
    val supportSummary = V156ReminderSetupCopy.summary(attentionPlan, deliveryPreference)


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
                            Text("TYPES", color = RecRed, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Choose a content type", color = ProjectorIvory, fontSize = 21.sp, fontWeight = FontWeight.Black)
                            Text(CreatorModeRegistry.definition(creatorModeId).label, color = MutedGold, fontSize = V173ProjectUiMetrics.supportingText)
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
                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                    selected = archetypeId == archetype.id,
                                    onClick = { onArchetypeChange(archetype.id); showAllTypes = false },
                                    leadingIcon = if (archetypeId == archetype.id) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) } } else null,
                                    label = { Text(label, fontSize = V173ProjectUiMetrics.supportingText) },
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
                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                    selected = archetypeId == archetype.id,
                                    onClick = { onArchetypeChange(archetype.id); showAllTypes = false },
                                    leadingIcon = if (archetypeId == archetype.id) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) } } else null,
                                    label = { Text(label, fontSize = V173ProjectUiMetrics.supportingText) },
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
                        Text("NEW PROJECT", color = RecRed, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Text("New project", color = ProjectorIvory, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MutedGold.copy(alpha = .10f),
                        border = BorderStroke(1.dp, MutedGold.copy(alpha = .20f)),
                    ) {
                        Text(
                            if (showAdvancedSetup) "${(activeStage + 1).coerceIn(1, 7)}/7" else "QUICK",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = MutedGold,
                            fontSize = V173ProjectUiMetrics.supportingText,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
                if (showAdvancedSetup) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = RecRed,
                        trackColor = CinemaLine,
                    )
                }

                Column(
                    Modifier.weight(1f).verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .padding(bottom = 30.dp),
                ) {
                    if (!showAdvancedSetup) {
                        Text(
                            "Just the essentials. You can fine-tune everything later.",
                            color = MutedText,
                            fontSize = V173ProjectUiMetrics.bodyText,
                            lineHeight = 19.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        V20FieldLabel("PROJECT NAME")
                        OutlinedTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            modifier = Modifier.fillMaxWidth().testTag("quick-create-title"),
                            placeholder = { Text("What are you making?") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        V20FieldLabel("PUBLISH TO")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            platformOptions.forEach { value ->
                                FilterChip(
                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                    selected = platform == value,
                                    onClick = { onPlatformChange(value) },
                                    label = { Text(value, fontSize = V173ProjectUiMetrics.choiceText) },
                                )
                            }
                        }
                        TextButton(onClick = { showAllPlatforms = !showAllPlatforms }) {
                            Text(if (showAllPlatforms) "SHOW MY PLATFORMS" else "OTHER PLATFORMS", fontSize = V173ProjectUiMetrics.supportingText)
                        }
                        Spacer(Modifier.height(8.dp))
                        V20FieldLabel("FORMAT")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            formatOptions.forEach { value ->
                                FilterChip(
                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                    selected = contentType == value,
                                    onClick = { onContentTypeChange(value) },
                                    label = { Text(value, fontSize = V173ProjectUiMetrics.choiceText) },
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        V20FieldLabel("DEADLINE")
                        V20DateTimeButton(v20WizardDateTime(dueAt), onPickDue)
                        if (dueAt <= now) {
                            Text("Choose a future deadline to create this project.", color = RecRed, fontSize = V173ProjectUiMetrics.supportingText)
                        }
                        Spacer(Modifier.height(16.dp))
                        V174QuickReminderSummary(
                            summary = supportSummary,
                            expanded = showQuickReminderChoices,
                            onToggle = { showQuickReminderChoices = !showQuickReminderChoices },
                        )
                        // A selected custom plan must never become an unfixable hidden form error.
                        if (V174QuickCreatePolicy.needsCustomReminderTime(attentionPlan)) {
                            Spacer(Modifier.height(10.dp))
                            V20FieldLabel("REMIND ME AT")
                            V20DateTimeButton(v20WizardDateTime(customReminderAt), onPickCustomReminder)
                            if (!customReady) {
                                Text("Choose a reminder after now and no later than the deadline.", color = RecRed, fontSize = V173ProjectUiMetrics.supportingText)
                            }
                        }
                        AnimatedVisibility(visible = showQuickReminderChoices) {
                            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                V156ReminderPlanPicker(
                                    selected = attentionPlan,
                                    onSelected = onAttentionPlanChange,
                                    compact = true,
                                )
                                if (attentionPlan != ProjectAttentionPlan.OFF) {
                                    Spacer(Modifier.height(12.dp))
                                    V156AdvancedReminderSettings(
                                        expanded = showReminderAdvanced,
                                        onToggle = { showReminderAdvanced = !showReminderAdvanced },
                                        preference = deliveryPreference,
                                    ) {
                                        V20FieldLabel("HOW SHOULD BACKLOT ALERT YOU?")
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                                            verticalArrangement = Arrangement.spacedBy(7.dp),
                                        ) {
                                            V20NewProjectReminderStyles.forEach { value ->
                                                FilterChip(
                                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                                    selected = deliveryPreference == value,
                                                    onClick = { onDeliveryPreferenceChange(value) },
                                                    label = { Text(pDeliveryPreferenceLabel(value), fontSize = V173ProjectUiMetrics.supportingText) },
                                                )
                                            }
                                        }
                                        Text(
                                            V156ReminderSetupCopy.alertExplanation(deliveryPreference),
                                            color = MutedText,
                                            fontSize = V173ProjectUiMetrics.supportingText,
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        V141InheritedVoiceCard(
                                            voice = voicePersona,
                                            onPreview = onPreviewVoice,
                                            onOpenSettings = onOpenSettings,
                                        )
                                        if (attentionPlan == ProjectAttentionPlan.CUSTOM) {
                                            Spacer(Modifier.height(12.dp))
                                            V20FieldLabel("IMPORTANCE")
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                                TaskPriority.entries.forEach { value ->
                                                    FilterChip(
                                                        modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                                        selected = priority == value,
                                                        onClick = { onPriorityChange(value) },
                                                        label = { Text(pPriorityLabel(value), fontSize = V173ProjectUiMetrics.supportingText) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (requiresAdvancedPermissions && !reminderSetupReady && attentionPlan != ProjectAttentionPlan.OFF) {
                            TextButton(onClick = onOpenSettings) {
                                Icon(Icons.Outlined.NotificationsActive, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("SET UP REMINDER PERMISSIONS", fontSize = V173ProjectUiMetrics.supportingText)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Content type: $chosenTypeLabel · Edit in More options",
                            color = MutedText,
                            fontSize = V173ProjectUiMetrics.supportingText,
                            lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                    } else {
                        if (startWithQuickCreate) {
                            TextButton(
                                onClick = {
                                    showAdvancedSetup = false
                                    activeStage = 0
                                    revealedStage = 0
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.ArrowBack, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("BACK TO QUICK CREATE", fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        V20ProgressiveSection(
                        index = 0,
                        label = "NAME",
                        valid = title.isNotBlank(),
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
                        label = "TYPE",
                        valid = archetypeId.isNotBlank(),
                        title = "What kind of content?",
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
                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                    selected = archetypeId == archetype.id,
                                    onClick = { onArchetypeChange(archetype.id) },
                                    label = { Text(label, fontSize = V173ProjectUiMetrics.supportingText) },
                                )
                            }
                        }
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(onClick = { showAllTypes = true }) {
                                Text("BROWSE ALL TYPES", fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black)
                            }
                            TextButton(onClick = { showAllModes = !showAllModes }) {
                                Text(if (showAllModes) "HIDE MODES" else "CHANGE MODE", fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black)
                            }
                        }
                        AnimatedVisibility(showAllModes) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                modeOptions.forEach { modeId ->
                                    val mode = CreatorModeRegistry.definition(modeId)
                                    FilterChip(
                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                        selected = CreatorModeRegistry.definition(creatorModeId).id == mode.id,
                                        onClick = { onCreatorModeChange(mode.id); showAllTypes = false },
                                        label = { Text(mode.label, fontSize = V173ProjectUiMetrics.supportingText) },
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
                            Row(Modifier.fillMaxWidth().heightIn(min = V173ProjectUiMetrics.touchTarget).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("YOUR STYLE", color = ProjectorIvory, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Bold)
                                    Text(
                                        productionStyles.takeIf { it.isNotEmpty() }?.joinToString() ?: "Using setup default",
                                        color = MutedText,
                                        fontSize = V173ProjectUiMetrics.bodyText,
                                        maxLines = 3,
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
                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                        selected = selected,
                                        onClick = {
                                            val next = if (selected) productionStyles - style
                                            else if (productionStyles.size < CreatorProfile.MAX_PRODUCTION_STYLES) productionStyles + style
                                            else productionStyles
                                            onProductionStylesChange(next)
                                        },
                                        label = { Text(style, fontSize = V173ProjectUiMetrics.supportingText) },
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
                        valid = platform.isNotBlank(),
                        title = "Where will you publish it?",
                        summary = platform.ifBlank { "Choose a platform" },
                        active = activeStage == 2,
                        revealed = revealedStage >= 2,
                        onEdit = { activeStage = 2 },
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            platformOptions.forEach { value ->
                                FilterChip(modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget), selected = platform == value, onClick = { onPlatformChange(value) }, label = { Text(value, fontSize = V173ProjectUiMetrics.supportingText) })
                            }
                        }
                        TextButton(onClick = { showAllPlatforms = !showAllPlatforms }) {
                            Text(if (showAllPlatforms) "SHOW MY PLATFORMS" else "OTHER PLATFORMS", fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black)
                        }
                        V20InlineContinue("CHOOSE FORMAT", platform.isNotBlank()) { revealNext(2) }
                    }

                    V20ProgressiveSection(
                        index = 3,
                        label = "FORMAT",
                        valid = contentType.isNotBlank(),
                        title = "What format?",
                        summary = contentType.ifBlank { "Choose a format" },
                        active = activeStage == 3,
                        revealed = revealedStage >= 3,
                        onEdit = { activeStage = 3 },
                    ) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            formatOptions.forEach { value ->
                                FilterChip(modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget), selected = contentType == value, onClick = { onContentTypeChange(value) }, label = { Text(value, fontSize = V173ProjectUiMetrics.supportingText) })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        V20InlineContinue("SET DUE DATE", contentType.isNotBlank()) { revealNext(3) }
                    }

                    V20ProgressiveSection(
                        index = 4,
                        label = "DUE DATE",
                        valid = dueAt > now,
                        title = "When is it due?",
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
                                Text(v20WizardDateTime(dueAt), color = ProjectorIvory, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                            }
                        }
                        if (dueAt <= now) {
                            Text("Choose a future deadline to continue.", color = RecRed, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        V20InlineContinue("CHOOSE SUPPORT", dueAt > now) { revealNext(4) }
                    }

                    V20ProgressiveSection(
                        index = 5,
                        label = "REMINDERS",
                        valid = supportReady,
                        title = "Would you like reminders?",
                        summary = supportSummary,
                        active = activeStage == 5,
                        revealed = revealedStage >= 5,
                        onEdit = { activeStage = 5 },
                    ) {
                        V156ReminderPlanPicker(
                            selected = attentionPlan,
                            onSelected = onAttentionPlanChange,
                        )
                        if (attentionPlan == ProjectAttentionPlan.CUSTOM) {
                            Spacer(Modifier.height(8.dp))
                            V20FieldLabel("REMIND ME AT")
                            V20DateTimeButton(v20WizardDateTime(customReminderAt), onPickCustomReminder)
                            if (!customReady) {
                                Spacer(Modifier.height(5.dp))
                                Text("Pick a reminder before your deadline.", color = RecRed, fontSize = 12.sp)
                            }
                        }
                        if (attentionPlan != ProjectAttentionPlan.OFF) {
                            Spacer(Modifier.height(12.dp))
                            V156AdvancedReminderSettings(
                                expanded = showReminderAdvanced,
                                onToggle = { showReminderAdvanced = !showReminderAdvanced },
                                preference = deliveryPreference,
                            ) {
                                V20FieldLabel("HOW SHOULD BACKLOT ALERT YOU?")
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    V20NewProjectReminderStyles.forEach { value ->
                                        FilterChip(
                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                            selected = deliveryPreference == value,
                                            onClick = { onDeliveryPreferenceChange(value) },
                                            label = { Text(pDeliveryPreferenceLabel(value), fontSize = V173ProjectUiMetrics.supportingText) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    V156ReminderSetupCopy.alertExplanation(deliveryPreference),
                                    color = MutedText,
                                    fontSize = V173ProjectUiMetrics.supportingText,
                                    lineHeight = 16.sp,
                                )
                                Spacer(Modifier.height(12.dp))
                                V141InheritedVoiceCard(
                                    voice = voicePersona,
                                    onPreview = onPreviewVoice,
                                    onOpenSettings = onOpenSettings,
                                )
                                if (attentionPlan == ProjectAttentionPlan.CUSTOM) {
                                    Spacer(Modifier.height(12.dp))
                                    V20FieldLabel("IMPORTANCE")
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        TaskPriority.entries.forEach { value ->
                                            FilterChip(
                                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                                                selected = priority == value,
                                                onClick = { onPriorityChange(value) },
                                                label = { Text(pPriorityLabel(value), fontSize = V173ProjectUiMetrics.supportingText) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (requiresAdvancedPermissions && !reminderSetupReady) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                onClick = onOpenSettings,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MutedGold.copy(alpha = .07f),
                                border = BorderStroke(1.dp, MutedGold.copy(alpha = .35f)),
                            ) {
                                Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Settings, null, tint = MutedGold, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Set up reminders", color = ProjectorIvory, fontSize = V173ProjectUiMetrics.bodyText, modifier = Modifier.weight(1f))
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
                        valid = allReady,
                        title = "Check your details",
                        summary = "Review and create",
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
                            Text("Style · ${productionStyles.joinToString()}", color = MutedText, fontSize = V173ProjectUiMetrics.bodyText, lineHeight = 19.sp)
                        }
                        if (!allReady) {
                            Text("Check the highlighted steps before creating this project.", color = RecRed, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onCreate,
                            enabled = allReady,
                            modifier = Modifier.fillMaxWidth().heightIn(min = V173ProjectUiMetrics.actionMinHeight),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        ) {
                            Text("CREATE PROJECT", fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(17.dp))
                        }
                    }
                    }
                }
                if (!showAdvancedSetup) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("quick-create-footer"),
                        color = CinemaSurfaceRaised,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                            Text(
                                "$platform · $contentType · ${v20WizardDateTime(dueAt)}",
                                color = MutedText,
                                fontSize = V173ProjectUiMetrics.supportingText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!quickReady) {
                                Text(
                                    "Add a project name and check the deadline and reminder time.",
                                    color = RecRed,
                                    fontSize = V173ProjectUiMetrics.supportingText,
                                    maxLines = 2,
                                )
                            }
                            Spacer(Modifier.height(7.dp))
                            Button(
                                onClick = onCreate,
                                enabled = quickReady,
                                modifier = Modifier.fillMaxWidth().heightIn(min = V173ProjectUiMetrics.actionMinHeight).testTag("quick-create-submit"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            ) {
                                Text("CREATE PROJECT", fontSize = V173ProjectUiMetrics.actionText, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                            TextButton(
                                onClick = {
                                    showAdvancedSetup = true
                                    activeStage = 0
                                    revealedStage = 0
                                },
                                modifier = Modifier.fillMaxWidth().testTag("quick-create-more-options"),
                            ) {
                                Icon(Icons.Outlined.Tune, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("MORE OPTIONS · TYPE & STYLE", fontSize = V173ProjectUiMetrics.supportingText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun V174QuickReminderSummary(
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp).testTag("quick-create-reminder-summary"),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.NotificationsNone, null, tint = MutedGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("REMINDERS", color = MutedGold, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black)
                Text(summary, color = ProjectorIvory, fontSize = V173ProjectUiMetrics.bodyText, fontWeight = FontWeight.Bold, maxLines = 3)
            }
            Spacer(Modifier.width(7.dp))
            Text(if (expanded) "CLOSE" else "CHANGE", color = RecRed, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun V20ProgressiveSection(
    index: Int,
    label: String,
    valid: Boolean,
    title: String,
    summary: String,
    active: Boolean,
    revealed: Boolean,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bringToStep = remember { BringIntoViewRequester() }
    LaunchedEffect(active, revealed) {
        if (active && revealed) {
            delay(160L)
            bringToStep.bringIntoView()
        }
    }
    AnimatedVisibility(visible = revealed) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("project-step-$index")) {
            if (active) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = CinemaSurfaceRaised,
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .38f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.bringIntoViewRequester(bringToStep), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(100.dp), color = RecRed.copy(alpha = .12f)) {
                                Text(
                                    "${index + 1}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = RecRed,
                                    fontSize = V173ProjectUiMetrics.supportingText,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(label, color = MutedGold, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(title, color = ProjectorIvory, fontSize = V173ProjectUiMetrics.sectionHeadingText, fontWeight = FontWeight.Black, lineHeight = 23.sp)
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
                    Row(Modifier.fillMaxWidth().heightIn(min = V173ProjectUiMetrics.reviewRowMinHeight).padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (valid) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                            null,
                            tint = if (valid) SuccessGreen else RecRed,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label, color = MutedText, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                            Text(summary, color = ProjectorIvory, fontSize = V173ProjectUiMetrics.reviewValueText, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Text(if (valid) "CHANGE" else "REVIEW", color = if (valid) MutedGold else RecRed, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Black)
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
        modifier = Modifier.fillMaxWidth().heightIn(min = V173ProjectUiMetrics.actionMinHeight),
        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(label, fontSize = V173ProjectUiMetrics.actionText, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Outlined.ArrowDownward, null, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun V20FieldLabel(text: String) {
    Text(text, color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 7.dp))
}

@Composable
private fun V20DateTimeButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = V173ProjectUiMetrics.actionMinHeight),
        border = BorderStroke(1.dp, CinemaLine),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = MutedGold, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = ProjectorIvory, fontSize = V173ProjectUiMetrics.bodyText, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("CHANGE", color = RecRed, fontSize = V173ProjectUiMetrics.supportingText)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun V20WizardSummaryChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(100.dp), color = MutedGold.copy(alpha = .10f), border = BorderStroke(1.dp, MutedGold.copy(alpha = .22f))) {
        FlowRow(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.Center) {
            Text("$label · ", color = MutedText, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Bold)
            Text(value, color = MutedGold, fontSize = V173ProjectUiMetrics.reviewValueText, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
internal fun V20WizardReviewRow(label: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp), shape = RoundedCornerShape(15.dp), color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Row(Modifier.fillMaxWidth().heightIn(min = V173ProjectUiMetrics.reviewRowMinHeight).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = MutedText, fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
                Spacer(Modifier.height(2.dp))
                Text(value, color = ProjectorIvory, fontSize = V173ProjectUiMetrics.reviewValueText, fontWeight = FontWeight.Bold, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Outlined.Edit, "Change $label", tint = MutedGold, modifier = Modifier.size(16.dp))
        }
    }
}

private fun v20WizardDateTime(millis: Long): String =
    if (millis <= 0L) "Choose date & time" else SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
