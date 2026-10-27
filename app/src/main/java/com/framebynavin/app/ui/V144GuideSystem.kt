package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    CUTE("Cute", "Warm. Gentle. Always encouraging."),
}

/** The original companion choice experience, restored for first-time creators. */
internal val V145SelectableGuides = listOf(
    V144GuideIdentity.FRAME,
    V144GuideIdentity.NAVI,
    V144GuideIdentity.FUNNY,
    V144GuideIdentity.CUTE,
)

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
    val currentGuide: V144GuideIdentity get() = selectedGuide ?: V144GuideIdentity.FUNNY

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

    fun select(guide: V144GuideIdentity) {
        selectedGuide = guide
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_CHARACTER, guide.name)?.apply()
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

    V217GuideChoiceContent(onSelect = { guide -> V144GuidePrefs.select(guide) })
}

/** Reused production first-run companion UI, independently testable without clearing saved setup. */
@Composable
internal fun V217GuideChoiceContent(onSelect: (V144GuideIdentity) -> Unit) {
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("BACKLOT", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.4.sp)
            Spacer(Modifier.height(7.dp))
            Text("Choose your guide", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Text(
                "Pick the companion you want across setup, onboarding and Backlot. You can change it later in Settings.",
                color = MutedText,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))

            V145SelectableGuides.chunked(2).forEachIndexed { index, guides ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    guides.forEach { guide ->
                        V144GuideChoiceCard(
                            guide = guide,
                            onSelect = { onSelect(guide) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (guides.size == 1) Spacer(Modifier.weight(1f))
                }
                if (index < V145SelectableGuides.chunked(2).lastIndex) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun V144GuideChoiceCard(
    guide: V144GuideIdentity,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val identity = v144GuideIdentityColor(guide)
    Surface(
        modifier = modifier.testTag("v217-choice-${guide.name}"),
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurface,
        border = BorderStroke(1.2.dp, identity.copy(alpha = .76f)),
    ) {
        Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(112.dp),
                shape = RoundedCornerShape(16.dp),
                color = V144GuideStage,
                border = BorderStroke(1.dp, identity.copy(alpha = .25f)),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    V144GuideCharacter(
                        guide = guide,
                        state = CinePulseState.WAVE,
                        modifier = Modifier.size(96.dp),
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(guide.displayName, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(
                guide.description,
                color = MutedText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                minLines = 2,
            )
            Spacer(Modifier.height(7.dp))
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = identity, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("USE ${guide.displayName.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
internal fun V144GuidePicker() {
    val context = LocalContext.current
    V144GuidePrefs.initialize(context)
    val selected = V144GuidePrefs.currentGuide

    Column(Modifier.fillMaxWidth()) {
        Text("GUIDE CHARACTER", color = MutedGold, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(4.dp))
        Text("Choose your creative companion", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text("You can change guides whenever you like.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
        Spacer(Modifier.height(10.dp))

        V145SelectableGuides.chunked(2).forEachIndexed { index, guides ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                guides.forEach { guide ->
                    val active = guide == selected
                    val identity = v144GuideIdentityColor(guide)
                    Surface(
                        modifier = Modifier.weight(1f).clickable { V144GuidePrefs.select(guide) }
                            .testTag("v217-settings-${guide.name}"),
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) CinemaSurfaceRaised else CinemaSurface,
                        border = BorderStroke(1.2.dp, if (active) identity else CinemaLine),
                    ) {
                        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.height(92.dp).fillMaxWidth(),
                                color = V144GuideStage,
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    V144GuideCharacter(
                                        guide = guide,
                                        state = CinePulseState.IDLE,
                                        modifier = Modifier.size(82.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(guide.displayName, color = ProjectorIvory, fontSize = 10.2.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (active) "SELECTED" else "TAP TO USE",
                                color = if (active) identity else MutedText,
                                fontSize = 6.8.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                if (guides.size == 1) Spacer(Modifier.weight(1f))
            }
            if (index < V145SelectableGuides.chunked(2).lastIndex) Spacer(Modifier.height(8.dp))
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
        // The original Spark code path and all five original Spark WebPs are preserved.
        V144GuideIdentity.FUNNY -> V144RasterGuideCharacter(guide, state, modifier, pointRight)
    }
}

private fun v144GuideIdentityColor(guide: V144GuideIdentity): Color = when (guide) {
    V144GuideIdentity.FRAME -> Color(0xFFFFC857)
    V144GuideIdentity.NAVI -> Color(0xFFF04F54)
    V144GuideIdentity.FUNNY -> Color(0xFF8176E8)
    V144GuideIdentity.CUTE -> Color(0xFFE99AAF)
}

private val V144GuideStage: Color get() = CinemaSurfaceRaised
