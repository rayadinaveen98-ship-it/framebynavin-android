package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import com.framebynavin.app.data.CreatorModeRegistry
import com.framebynavin.app.data.CreatorPlatformRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.ProductionStyleRegistry
import com.framebynavin.app.ui.theme.BacklotUX10Tokens
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/** Explicit first-run intent. Never infer a Creator profile from Personal intent. */
internal enum class V393FirstRunFocus(val title: String, val detail: String) {
    CREATOR("Creator", "Plan projects, capture ideas, and publish better."),
    PERSONAL("Personal", "Manage reminders, challenges, and daily life."),
    BOTH("Both", "Bring creative work and personal life together."),
}

internal class V393FirstRunPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "backlot_first_run_intent_v393", Context.MODE_PRIVATE,
    )

    fun selectedFocus(): V393FirstRunFocus? = prefs.getString("focus", null)?.let {
        runCatching { V393FirstRunFocus.valueOf(it) }.getOrNull()
    }

    /**
     * UX12-D2: a new installation starts with a unified, Personal-ready
     * workspace, without asking for Creator/Personal/Both. This is a distinct
     * routing flag: never write a fake PERSONAL choice into the legacy focus.
     * Existing CREATOR/PERSONAL/BOTH selections remain authoritative.
     */
    fun unifiedEntryStarted(): Boolean =
        selectedFocus() == null && prefs.getBoolean("ux12_unified_entry", false)

    fun beginUnifiedEntry(): Boolean {
        if (selectedFocus() != null) return false
        if (unifiedEntryStarted()) return true
        check(prefs.edit()
            .putBoolean("ux12_unified_entry", true)
            .putBoolean("ux02_first_run", true)
            .commit()) { "Unable to save unified Backlot setup route" }
        return true
    }

    /** Commit synchronously before proceeding to avoid returning to this gate on crash. */
    fun saveFocus(focus: V393FirstRunFocus) {
        check(prefs.edit().putString("focus", focus.name)
            .putBoolean("ux02_first_run", true).commit()) {
            "Unable to save first-run choice"
        }
    }

    /** Only journeys explicitly started by UX02 should auto-start its six-step guide. */
    fun startedUx02Journey(): Boolean = prefs.getBoolean("ux02_first_run", false)

    /** Google sign-in to an existing Backlot account is not a new-user journey. */
    fun markReturningAccount() {
        check(prefs.edit().putBoolean("ux02_first_run", false)
            .putBoolean("permission_guide_complete", true).commit()) {
            "Unable to confirm returning-account setup state"
        }
    }

    /** Existing installs with a saved focus must not be sent through a new mandatory gate. */
    fun permissionGuideComplete(): Boolean =
        !prefs.getBoolean("ux02_first_run", false) ||
            prefs.getBoolean("permission_guide_complete", false)

    fun markPermissionGuideComplete() {
        check(prefs.edit().putBoolean("permission_guide_complete", true).commit()) {
            "Unable to save permission guidance choice"
        }
    }

    fun personalFirstEntryShown(): Boolean = prefs.getBoolean("personal_first_entry", false)

    fun markPersonalFirstEntryShown() {
        check(prefs.edit().putBoolean("personal_first_entry", true).commit()) {
            "Unable to save first personal route"
        }
    }
}

/**
 * Legacy UX12-D2 unified-entry preferences remain readable for upgrade
 * compatibility. The original selection screen is now restored for genuine
 * fresh accounts; existing unified journeys are not retroactively reset.
 */
internal object UX12D2FirstRunRoutePolicy {
    fun shouldShowCreatorSetup(
        creatorSetupIncomplete: Boolean,
        legacyFocus: V393FirstRunFocus?,
        unifiedJourney: Boolean,
        creatorWorkRequested: Boolean,
    ): Boolean = creatorSetupIncomplete && (
        creatorWorkRequested ||
            (!unifiedJourney && legacyFocus != V393FirstRunFocus.PERSONAL)
        )
}

/** Original three-way first-run screen, retained for genuine new accounts only. */
@Composable
internal fun V393FirstRunFocusScreen(onChoose: (V393FirstRunFocus) -> Unit) {
    Surface(Modifier.fillMaxSize().testTag("v393-first-run-focus"), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BacklotUX10Tokens.pageGutter, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text("BACKLOT / FIRST RUN", color = MutedGold, fontSize = 10.sp,
                fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text("Make Backlot yours.", color = ProjectorIvory, fontSize = 31.sp,
                lineHeight = 37.sp, fontWeight = FontWeight.Black)
            Text("What would you like help with first? You can use other Backlot features later.",
                color = MutedText, fontSize = 14.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(10.dp))
            V393FirstRunFocus.entries.forEach { focus ->
                Surface(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 102.dp)
                        .clickable { onChoose(focus) }
                        .testTag("v393-focus-${focus.name}"),
                    shape = RoundedCornerShape(BacklotUX10Tokens.cardRadius),
                    // All three are equally valid; don't falsely preselect BOTH.
                    border = BacklotUX10Tokens.quietBorder(CinemaLine),
                    color = CinemaSurfaceRaised,
                ) {
                    Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(focus.title, color = ProjectorIvory, fontSize = 20.sp,
                            fontWeight = FontWeight.Black)
                        Text(focus.detail, color = MutedText,
                            fontSize = 13.sp, lineHeight = 19.sp)
                        Text("CHOOSE ${focus.title.uppercase()}", color = MutedGold,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = .6.sp)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Next: connect Google or continue locally. You can choose Spark or Mira and personalize your workspace later.",
                color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

/** Only fields actually required by CreatorProfile.isComplete; no fake defaults. */
@Composable
internal fun V393QuickCreatorSetup(
    existing: CreatorProfile,
    onFinish: (CreatorProfile) -> Boolean,
) {
    var creatorName by rememberSaveable { mutableStateOf(existing.displayName) }
    var creatorMode by rememberSaveable { mutableStateOf(existing.resolvedPrimaryCreatorMode) }
    var platform by rememberSaveable { mutableStateOf(existing.platforms.firstOrNull().orEmpty()) }
    var goal by rememberSaveable { mutableStateOf(existing.primaryGoal) }
    var productionStyle by rememberSaveable { mutableStateOf(existing.productionStyles.firstOrNull().orEmpty()) }
    var saveError by rememberSaveable { mutableStateOf(false) }
    val styles = remember(creatorMode) { ProductionStyleRegistry.orderedForMode(creatorMode) }
    val goals = listOf(
        "Publish consistently", "Grow my audience",
        "Build a portfolio", "Earn from my work",
    )
    Surface(Modifier.fillMaxSize().testTag("v393-quick-creator-setup"), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = BacklotUX10Tokens.pageGutter, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("BACKLOT / CREATOR SETUP", color = MutedGold,
                fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
            Text("Make your workspace yours.", color = ProjectorIvory,
                fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Choose what, where, why and how you create. You can change these later.", color = MutedText,
                fontSize = 12.sp)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = creatorName,
                    onValueChange = { creatorName = it.take(40) },
                    modifier = Modifier.fillMaxWidth().testTag("v402-backlot-display-name"),
                    label = { Text("Name on Backlot (optional)") },
                    singleLine = true,
                )
                V393QuickChoiceGroup("1 / WHAT DO YOU CREATE?",
                    CreatorModeRegistry.definitions.map { it.label }, creatorMode) { creatorMode = it }
                V393QuickChoiceGroup("2 / WHERE DO YOU PUBLISH?",
                    CreatorPlatformRegistry.supportedPlatforms, platform) { platform = it }
                V393QuickChoiceGroup("3 / WHY DO YOU CREATE?",
                    goals, goal) { goal = it }
                V393QuickChoiceGroup("4 / HOW DO YOU CREATE?",
                    styles, productionStyle) { productionStyle = it }
                Text("Additional modes, weekly targets, and alert permissions can be customized later.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 17.sp)
            }
            if (saveError) Text("Setup could not be saved. Your choices are still here; retry.",
                color = RecRed, fontSize = 12.sp)
            Button(
                onClick = {
                    if (creatorMode.isBlank() || platform.isBlank() || goal.isBlank() || productionStyle.isBlank()) return@Button
                    val profile = existing.copy(
                        displayName = creatorName.trim(),
                        primaryCreatorMode = creatorMode,
                        category = creatorMode,
                        platforms = setOf(platform),
                        primaryGoal = goal,
                        productionStyles = setOf(productionStyle),
                        setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
                    ).normalized()
                    saveError = !onFinish(profile)
                },
                enabled = creatorMode.isNotBlank() && platform.isNotBlank() &&
                    goal.isNotBlank() && productionStyle.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .testTag("v393-creator-start"),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text("CONTINUE TO BACKLOT", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun V393QuickChoiceGroup(
    heading: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    val stackChoices = UX10BPresentationPolicy.stackedFirstRunChoices(
        screenWidthDp = LocalConfiguration.current.screenWidthDp,
        fontScale = LocalDensity.current.fontScale,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(heading, color = MutedGold, fontSize = BacklotUX10Tokens.eyebrow,
            fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
        options.chunked(if (stackChoices) 1 else 2).forEach { pair ->
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { option ->
                    val active = option == selected
                    Surface(
                        onClick = { onSelected(option) },
                        modifier = Modifier.weight(1f)
                            .heightIn(min = 64.dp)
                            .testTag("v393-pick-${heading.take(1)}-${option.hashCode()}"),
                        shape = RoundedCornerShape(BacklotUX10Tokens.compactRadius),
                        color = if (active) RecRed.copy(alpha = .17f) else CinemaSurface,
                        border = if (active) BacklotUX10Tokens.activeBorder(RecRed)
                            else BacklotUX10Tokens.quietBorder(CinemaLine),
                    ) {
                        Box(
                            Modifier.fillMaxWidth().padding(
                                horizontal = 10.dp, vertical = 12.dp
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                option, color = ProjectorIvory,
                                fontSize = BacklotUX10Tokens.body,
                                lineHeight = 18.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                            )
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
