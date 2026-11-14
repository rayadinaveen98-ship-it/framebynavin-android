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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    /** Commit synchronously before proceeding to avoid returning to this gate on crash. */
    fun saveFocus(focus: V393FirstRunFocus) {
        check(prefs.edit().putString("focus", focus.name).commit()) {
            "Unable to save first-run choice"
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
 * First real app decision: no forced account connection, notification permissions,
 * or arbitrary Creator goal assumptions. Existing completed accounts skip this gate.
 */
@Composable
internal fun V393FirstRunFocusScreen(onChoose: (V393FirstRunFocus) -> Unit) {
    Surface(Modifier.fillMaxSize().testTag("v393-first-run-focus"), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 23.dp, vertical = 23.dp),
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
                    Modifier.fillMaxWidth().clickable { onChoose(focus) }
                        .testTag("v393-focus-${focus.name}"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (focus == V393FirstRunFocus.BOTH)
                        RecRed.copy(alpha = .58f) else CinemaLine),
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
            Text("Next: choose Spark or Mira. You can start locally; account connection and reminder permissions come later.",
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
    var creatorMode by rememberSaveable { mutableStateOf(existing.resolvedPrimaryCreatorMode) }
    var platform by rememberSaveable { mutableStateOf(existing.platforms.firstOrNull().orEmpty()) }
    var goal by rememberSaveable { mutableStateOf(existing.primaryGoal) }
    var saveError by rememberSaveable { mutableStateOf(false) }
    val goals = listOf(
        "Publish consistently", "Grow my audience",
        "Build a portfolio", "Earn from my work",
    )
    Surface(Modifier.fillMaxSize().testTag("v393-quick-creator-setup"), color = CinemaBlack) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("BACKLOT / CREATOR SETUP", color = MutedGold,
                fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
            Text("Three quick choices.", color = ProjectorIvory,
                fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("The rest can wait until you need it.", color = MutedText,
                fontSize = 12.sp)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                V393QuickChoiceGroup("1 / WHAT DO YOU CREATE?",
                    CreatorModeRegistry.definitions.map { it.label }, creatorMode) { creatorMode = it }
                V393QuickChoiceGroup("2 / WHERE DO YOU PUBLISH?",
                    CreatorPlatformRegistry.supportedPlatforms, platform) { platform = it }
                V393QuickChoiceGroup("3 / WHAT IS YOUR MAIN GOAL?",
                    goals, goal) { goal = it }
                Text("Production styles, targets, account connection, and alert permissions can be customized later.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 17.sp)
            }
            if (saveError) Text("Setup could not be saved. Your choices are still here; retry.",
                color = RecRed, fontSize = 12.sp)
            Button(
                onClick = {
                    if (creatorMode.isBlank() || platform.isBlank() || goal.isBlank()) return@Button
                    val profile = existing.copy(
                        primaryCreatorMode = creatorMode,
                        category = creatorMode,
                        platforms = setOf(platform),
                        primaryGoal = goal,
                        setupSchemaVersion = CreatorProfile.CURRENT_SCHEMA_VERSION,
                    ).normalized()
                    saveError = !onFinish(profile)
                },
                enabled = creatorMode.isNotBlank() && platform.isNotBlank() && goal.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .testTag("v393-creator-start"),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                shape = RoundedCornerShape(15.dp),
            ) {
                Text("CREATE MY FIRST PROJECT", fontWeight = FontWeight.Black)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(heading, color = MutedGold, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
        options.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { option ->
                    val active = option == selected
                    Surface(
                        modifier = Modifier.weight(1f).height(55.dp)
                            .clickable { onSelected(option) }
                            .testTag("v393-pick-${heading.take(1)}-${option.hashCode()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) RecRed.copy(alpha = .17f) else CinemaSurface,
                        border = BorderStroke(1.dp, if (active) RecRed else CinemaLine),
                    ) {
                        Box(Modifier.fillMaxSize().padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center) {
                            Text(option, color = ProjectorIvory,
                                fontSize = 11.5.sp, lineHeight = 15.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 2)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
