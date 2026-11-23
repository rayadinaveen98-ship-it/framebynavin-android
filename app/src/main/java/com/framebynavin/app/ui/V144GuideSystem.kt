package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory

enum class V144GuideIdentity(val displayName: String, val description: String) {
    FRAME("Frame", "Simple. Expressive. Always with you."),
    NAVI("Navi", "Curious. Calm. Creative."),
    FUNNY("Spark", "Lively. Creative. Always moving forward."),
    MIRA("Mira", "Warm. Curious. Ready to cheer you on."),
    CUTE("Cute", "Warm. Gentle. Always encouraging."),
}

/** The original companion choice experience, restored for first-time creators. */
// Two fully independent selectable guides. Legacy Frame/Navi/Cute identifiers
// remain readable so no existing local preference is overwritten on upgrade.
internal val V145SelectableGuides = listOf(V144GuideIdentity.FUNNY, V144GuideIdentity.MIRA)

/** Keep already selected companions intact on upgrades; do not silently overwrite choices. */
internal fun v145NormalizeSavedGuide(value: String?): V144GuideIdentity? =
    value?.let { saved -> V144GuideIdentity.entries.firstOrNull { it.name == saved } }

/**
 * Guide preference remains on the historical key so existing installs migrate in place.
 * Restore the creator's saved Frame / Navi / Spark / Cute selection in place.
 */
object V144GuidePrefs {
    private const val PREFS = "backlot_character_identity_v151"
    private const val KEY_CHARACTER = "selected_character"
    private var appContext: Context? = null

    var selectedGuide by mutableStateOf<V144GuideIdentity?>(null)
        private set

    val hasSelection: Boolean get() = selectedGuide != null
    // Honor legacy saved choice in storage without showing incomplete artwork.
    val currentGuide: V144GuideIdentity get() =
        selectedGuide?.takeIf { it in V145SelectableGuides } ?: V144GuideIdentity.FUNNY

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val prefs = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs?.getString(KEY_CHARACTER, null)
        val normalized = v145NormalizeSavedGuide(saved)
        selectedGuide = normalized
        if (saved != null && normalized != null && normalized.name != saved) {
            prefs.edit().putString(KEY_CHARACTER, normalized.name).apply()
        }
    }

    /** Commit selection before publishing UI state; prevents a relaunch loop on failure. */
    fun select(guide: V144GuideIdentity): Boolean {
        val supported = guide.takeIf { it in V145SelectableGuides } ?: return false
        val context = appContext ?: return false
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_CHARACTER, supported.name).commit()
        if (!saved) return false
        selectedGuide = supported
        return true
    }
}

@Composable
internal fun V144GuideChoiceGate() {
    V144GuideChoiceGate(content = {})
}

@Composable
internal fun V144GuideChoiceGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    V144GuidePrefs.initialize(context)
    if (V144GuidePrefs.hasSelection) {
        content()
        return
    }

    var saveError by remember { mutableStateOf(false) }
    V217GuideChoiceContent(
        onSelect = { guide -> saveError = !V144GuidePrefs.select(guide) },
        saveError = saveError,
    )
}

/** Founder-approved P5: one featured companion, authentic pose art, deliberate confirmation. */
@Composable
internal fun V217GuideChoiceContent(
    onSelect: (V144GuideIdentity) -> Unit,
    saveError: Boolean = false,
) {
    var previewName by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(V145SelectableGuides.first().name)
    }
    var poseName by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(P5CompanionPose.WELCOME.name)
    }
    val candidate = P5CompanionSelectionPolicy.previewable(previewName)
    val pose = P5CompanionPose.valueOf(poseName)

    Surface(Modifier.fillMaxSize().testTag("p5-first-run-companion-choice"), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("BACKLOT / YOUR COMPANION", color = MutedGold, fontSize = 10.sp,
                letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text("Meet your companion.", color = ProjectorIvory,
                fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center)
            Text("Choose the character who feels right for you. " +
                "Change your choice anytime in Settings.",
                color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(5.dp))
            P5CompanionPortraitStage(
                guide = candidate, pose = pose,
                modifier = Modifier.fillMaxWidth().height(265.dp),
            )
            Text(candidate.displayName, color = ProjectorIvory,
                fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
            Text(candidate.description, color = MutedText,
                fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
            P5CompanionChoices(
                active = candidate,
                onPreview = { next ->
                    previewName = next.name
                    poseName = P5CompanionPose.WELCOME.name
                },
                testPrefix = "v217-choice-",
            )
            P5PoseChoices(pose = pose, onChange = { poseName = it.name })
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSelect(candidate) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .testTag("v217-choose-button-" + candidate.name),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProjectorIvory, contentColor = CinemaBlack),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("CONTINUE WITH " + candidate.displayName.uppercase(),
                    fontWeight = FontWeight.SemiBold)
            }
            if (saveError) {
                Text("Couldn't save this companion. Please try again.",
                    color = ProjectorIvory, fontSize = 12.sp,
                    textAlign = TextAlign.Center)
            }
            Text("Your companion does not send messages or change reminders " +
                "without your action.", color = MutedText, fontSize = 10.sp,
                lineHeight = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

/** The Settings picker uses the same hero assets; preview never persists a choice. */
@Composable
internal fun V144GuidePicker() {
    val context = LocalContext.current
    V144GuidePrefs.initialize(context)
    val saved = V144GuidePrefs.currentGuide
    var previewName by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(saved.name)
    }
    var poseName by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(P5CompanionPose.WELCOME.name)
    }
    var error by androidx.compose.runtime.remember { mutableStateOf(false) }
    val candidate = P5CompanionSelectionPolicy.previewable(previewName)
    val pose = P5CompanionPose.valueOf(poseName)
    Column(Modifier.fillMaxWidth().testTag("p5-settings-companion"),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("YOUR COMPANION", color = MutedGold, fontSize = 10.sp,
            letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
        Text("Make Backlot yours.", color = ProjectorIvory, fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold)
        Text("Preview Spark and Mira. Your current companion won't change " +
            "until you confirm.", color = MutedText, fontSize = 12.sp, lineHeight = 17.sp)
        P5CompanionPortraitStage(guide = candidate, pose = pose,
            modifier = Modifier.fillMaxWidth().height(230.dp))
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(candidate.displayName, color = ProjectorIvory,
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(if (candidate == saved) "CURRENT COMPANION" else "PREVIEW ONLY",
                color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        P5CompanionChoices(active = candidate, onPreview = {
            previewName = it.name
            poseName = P5CompanionPose.WELCOME.name
            error = false
        }, testPrefix = "v217-settings-")
        P5PoseChoices(pose = pose, onChange = { poseName = it.name })
        Button(
            onClick = {
                error = !V144GuidePrefs.select(candidate)
            },
            enabled = candidate != saved,
            modifier = Modifier.fillMaxWidth().height(50.dp)
                .testTag("p5-confirm-selected-companion"),
            colors = ButtonDefaults.buttonColors(
                containerColor = ProjectorIvory, contentColor = CinemaBlack),
        ) { Text(if (candidate == saved) "CURRENTLY SELECTED" else
            "USE " + candidate.displayName.uppercase(), fontWeight = FontWeight.SemiBold) }
        if (error) Text("Couldn't save your companion. Try again.",
            color = ProjectorIvory, fontSize = 12.sp)
    }
}

/** Reuses original Spark/Mira sprites in exactly the existing aspect ratio. */
@Composable
private fun P5CompanionPortraitStage(
    guide: V144GuideIdentity,
    pose: P5CompanionPose,
    modifier: Modifier,
) {
    androidx.compose.animation.Crossfade(
        targetState = guide to pose,
        modifier = modifier.testTag("p5-character-stage"),
        animationSpec = androidx.compose.animation.core.tween(220),
        label = "p5CompanionFocus",
    ) { (character, characterPose) ->
        Surface(Modifier.fillMaxSize(),
            color = CinemaSurfaceRaised,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, CinemaLine)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                V144GuideCharacter(
                    guide = character,
                    state = P5CompanionSelectionPolicy.characterState(characterPose),
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                )
            }
        }
    }
}

/** Preview selection only; never mutate V144GuidePrefs from a tab tap. */
@Composable
private fun P5CompanionChoices(
    active: V144GuideIdentity,
    onPreview: (V144GuideIdentity) -> Unit,
    testPrefix: String,
) {
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        V145SelectableGuides.forEach { guide ->
            val focused = guide == active
            Surface(
                onClick = { onPreview(guide) },
                modifier = Modifier.weight(1f).height(54.dp)
                    .testTag(testPrefix + guide.name)
                    .semantics { contentDescription =
                        "Preview " + guide.displayName },
                shape = RoundedCornerShape(14.dp),
                color = if (focused) CinemaSurfaceRaised else CinemaSurface,
                border = BorderStroke(1.5.dp,
                    if (focused) v144GuideIdentityColor(guide) else CinemaLine),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(guide.displayName, color = ProjectorIvory,
                        fontSize = 13.sp, fontWeight = if (focused)
                            FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

/** The gesture makes one expressive pose visible at a time, without fake lip-sync. */
@Composable
private fun P5PoseChoices(
    pose: P5CompanionPose,
    onChange: (P5CompanionPose) -> Unit,
) {
    Row(Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        P5CompanionPose.entries.forEach { option ->
            Surface(
                onClick = { onChange(option) },
                modifier = Modifier.weight(1f).height(38.dp)
                    .testTag("p5-pose-" + option.name),
                shape = RoundedCornerShape(11.dp),
                color = if (pose == option) CinemaSurfaceRaised else CinemaSurface,
                border = BorderStroke(1.dp,
                    if (pose == option) MutedGold else CinemaLine),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(option.label, color =
                        if (pose == option) ProjectorIvory else MutedText,
                        fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun V144GuideCharacter(
    guide: V144GuideIdentity = V144GuidePrefs.currentGuide,
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    when (guide) {
        V144GuideIdentity.FRAME, V144GuideIdentity.NAVI, V144GuideIdentity.CUTE ->
            V217LiveCompanionCharacter(guide, state, modifier, pointRight)
        // Spark and Mira each retain their own approved transparent sprite packs.
        V144GuideIdentity.FUNNY, V144GuideIdentity.MIRA ->
            V144RasterGuideCharacter(guide, state, modifier, pointRight)
    }
}

private fun v144GuideIdentityColor(guide: V144GuideIdentity): Color = when (guide) {
    V144GuideIdentity.FRAME -> Color(0xFFFFC857)
    V144GuideIdentity.NAVI -> Color(0xFFF04F54)
    V144GuideIdentity.FUNNY -> Color(0xFF8176E8)
    V144GuideIdentity.MIRA -> Color(0xFFE995AF)
    V144GuideIdentity.CUTE -> Color(0xFFE99AAF)
}

private val V144GuideStage: Color get() = CinemaSurfaceRaised
