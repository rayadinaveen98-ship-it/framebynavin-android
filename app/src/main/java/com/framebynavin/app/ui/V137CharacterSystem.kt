package com.framebynavin.app.ui

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.framebynavin.app.ui.theme.RecRed
import kotlin.math.abs
import kotlin.math.sin

enum class BacklotCharacter(val displayName: String, val description: String) {
    FRAME("Frame", "Minimal, expressive and built from the Backlot frame."),
    NAVI("Navi", "Calm, curious and drawn like a quiet studio friend."),
}

/** One creator-selected guide identity across onboarding, empty states and helper moments. */
object BacklotCharacterPrefs {
    private const val PREFS = "backlot_character_identity"
    private const val KEY_CHARACTER = "selected_character"
    private var appContext: Context? = null

    var selectedCharacter by mutableStateOf<BacklotCharacter?>(null)
        private set

    val hasSelection: Boolean get() = selectedCharacter != null
    val currentCharacter: BacklotCharacter get() = selectedCharacter ?: BacklotCharacter.FRAME

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val saved = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CHARACTER, null)
        selectedCharacter = saved?.let { value ->
            BacklotCharacter.entries.firstOrNull { it.name == value }
        }
    }

    fun select(character: BacklotCharacter) {
        selectedCharacter = character
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_CHARACTER, character.name)?.apply()
    }
}

@Composable
internal fun V137CharacterChoiceGate(content: @Composable () -> Unit) {
    if (BacklotCharacterPrefs.hasSelection) {
        content()
        return
    }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("BACKLOT", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.4.sp)
            Spacer(Modifier.height(7.dp))
            Text("Choose your guide", color = ProjectorIvory, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Text(
                "Pick the companion you want to see across Backlot. You can change it anytime in Settings.",
                color = MutedText,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BacklotCharacter.entries.forEach { character ->
                    V137CharacterChoiceCard(
                        character = character,
                        onSelect = { BacklotCharacterPrefs.select(character) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun V137CharacterChoiceCard(
    character: BacklotCharacter,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            BacklotGuideCharacter(
                character = character,
                state = if (character == BacklotCharacter.FRAME) CinePulseState.WAVE else CinePulseState.THINK,
                modifier = Modifier.size(width = 112.dp, height = 136.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(character.displayName, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(
                character.description,
                color = MutedText,
                fontSize = 8.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                minLines = 3,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (character == BacklotCharacter.FRAME) MutedGold else RecRed),
                shape = RoundedCornerShape(13.dp),
            ) {
                Text("USE ${character.displayName.uppercase()}", fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
internal fun V137CharacterPicker() {
    val selected = BacklotCharacterPrefs.currentCharacter
    Column(Modifier.fillMaxWidth()) {
        Text("GUIDE CHARACTER", color = MutedGold, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(4.dp))
        Text("Choose who stays with you.", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text("Your selection follows you through guides and assistant moments.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BacklotCharacter.entries.forEach { character ->
                val active = character == selected
                Surface(
                    modifier = Modifier.weight(1f).clickable { BacklotCharacterPrefs.select(character) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) CinemaSurfaceRaised else CinemaSurface,
                    border = BorderStroke(1.dp, if (active) MutedGold else CinemaLine),
                ) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(92.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            BacklotGuideCharacter(character, CinePulseState.IDLE, Modifier.size(width = 76.dp, height = 92.dp))
                        }
                        Text(character.displayName, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                        Text(if (active) "SELECTED" else "TAP TO USE", color = if (active) MutedGold else MutedText, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Deterministic flat-vector renderer. Both characters are built from Compose Canvas primitives;
 * there are no generated character assets, glow effects or 3D dependencies.
 */
@Composable
internal fun BacklotGuideCharacter(
    character: BacklotCharacter = BacklotCharacterPrefs.currentCharacter,
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val motion = rememberInfiniteTransition(label = "backlotGuideMotion")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (state == CinePulseState.WALK) 1050 else 3400, easing = LinearEasing)),
        label = "guidePhase",
    )
    val breathe by motion.animateFloat(
        initialValue = .985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(2100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "guideBreath",
    )

    Canvas(modifier) {
        when (character) {
            BacklotCharacter.FRAME -> drawFrameCharacter(state, pointRight, phase, breathe)
            BacklotCharacter.NAVI -> drawNaviCharacter(state, pointRight, phase, breathe)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrameCharacter(
    state: CinePulseState,
    pointRight: Boolean,
    phase: Float,
    breathe: Float,
) {
    val u = size.minDimension / 100f
    val cx = size.width / 2f
    val dir = if (pointRight) 1f else -1f
    val wave = sin(phase * Math.PI * 2.0).toFloat()
    val walk = sin(phase * Math.PI * 2.0).toFloat()
    val bob = when (state) {
        CinePulseState.WALK -> abs(walk) * 1.4f * u
        CinePulseState.CELEBRATE, CinePulseState.SUCCESS -> abs(wave) * .8f * u
        else -> (breathe - 1f) * 8f * u
    }
    val headShiftY = if (state == CinePulseState.NOD) abs(wave) * 1.4f * u else 0f
    val headY = 28f * u + bob + headShiftY
    val headW = 61f * u
    val headH = 40f * u
    val headLeft = cx - headW / 2f
    val headTop = headY - headH / 2f
    val gold = MutedGold
    val face = Color(0xFF0A0B0D)
    val body = Color(0xFF121316)
    val outline = Color(0xFF25272B)
    val ivory = ProjectorIvory

    // Head shadow and unmistakable Backlot frame silhouette.
    drawRoundRect(
        color = Color(0xFF000000).copy(alpha = .30f),
        topLeft = Offset(headLeft + 2.5f * u, headTop + 3f * u),
        size = Size(headW, headH),
        cornerRadius = CornerRadius(10f * u, 10f * u),
    )
    drawRoundRect(
        color = face,
        topLeft = Offset(headLeft, headTop),
        size = Size(headW, headH),
        cornerRadius = CornerRadius(10f * u, 10f * u),
    )
    drawRoundRect(
        color = gold,
        topLeft = Offset(headLeft, headTop),
        size = Size(headW, headH),
        cornerRadius = CornerRadius(10f * u, 10f * u),
        style = Stroke(width = 4.2f * u),
    )

    val look = when (state) {
        CinePulseState.POINT, CinePulseState.PRESENT -> dir * 1.9f * u
        CinePulseState.LOOK -> wave * 2.4f * u
        CinePulseState.THINK -> dir * 1.1f * u
        CinePulseState.LISTEN -> -dir * .8f * u
        else -> 0f
    }
    val blink = phase > .965f && state !in setOf(CinePulseState.SUCCESS, CinePulseState.CELEBRATE)
    val eyeY = headY + .5f * u
    val eyeDX = 12.5f * u

    if (state == CinePulseState.SUCCESS || state == CinePulseState.CELEBRATE) {
        listOf(-eyeDX, eyeDX).forEach { dx ->
            drawArc(
                color = ivory,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(cx + dx - 4.5f * u + look, eyeY - 2.2f * u),
                size = Size(9f * u, 7f * u),
                style = Stroke(width = 2.1f * u, cap = StrokeCap.Round),
            )
        }
    } else {
        val eyeH = if (blink || state == CinePulseState.REST) 1.7f * u else 10.5f * u
        listOf(-eyeDX, eyeDX).forEach { dx ->
            drawRoundRect(
                color = ivory.copy(alpha = if (state == CinePulseState.REST) .68f else 1f),
                topLeft = Offset(cx + dx - 2.2f * u + look, eyeY - eyeH / 2f),
                size = Size(4.4f * u, eyeH),
                cornerRadius = CornerRadius(2.2f * u, 2.2f * u),
            )
        }
    }

    val bodyTop = 51f * u + bob
    drawRoundRect(
        color = body,
        topLeft = Offset(cx - 14f * u, bodyTop),
        size = Size(28f * u, 27f * u),
        cornerRadius = CornerRadius(9f * u, 9f * u),
    )
    drawRoundRect(
        color = outline,
        topLeft = Offset(cx - 14f * u, bodyTop),
        size = Size(28f * u, 27f * u),
        cornerRadius = CornerRadius(9f * u, 9f * u),
        style = Stroke(width = 1.2f * u),
    )
    // Small flat Backlot chest badge.
    drawRoundRect(
        color = gold,
        topLeft = Offset(cx - 3.3f * u, bodyTop + 8.5f * u),
        size = Size(6.6f * u, 7f * u),
        cornerRadius = CornerRadius(1.4f * u, 1.4f * u),
    )
    drawRect(face, Offset(cx - 1.2f * u, bodyTop + 9.7f * u), Size(1.5f * u, 4.6f * u))

    val shoulderY = bodyTop + 7f * u
    val leftShoulder = Offset(cx - 12f * u, shoulderY)
    val rightShoulder = Offset(cx + 12f * u, shoulderY)
    val leftHand: Offset
    val rightHand: Offset
    when (state) {
        CinePulseState.WAVE -> {
            leftHand = Offset(cx - 22f * u, bodyTop + 18f * u)
            rightHand = Offset(cx + (18f + wave * 3f) * u, bodyTop - 5f * u)
        }
        CinePulseState.POINT, CinePulseState.PRESENT -> {
            if (dir > 0) {
                leftHand = Offset(cx - 22f * u, bodyTop + 18f * u)
                rightHand = Offset(cx + 31f * u, bodyTop + 7f * u)
            } else {
                leftHand = Offset(cx - 31f * u, bodyTop + 7f * u)
                rightHand = Offset(cx + 22f * u, bodyTop + 18f * u)
            }
        }
        CinePulseState.THINK -> {
            leftHand = Offset(cx - 20f * u, bodyTop + 18f * u)
            rightHand = Offset(cx + 21f * u, headY + 11f * u)
        }
        CinePulseState.CELEBRATE, CinePulseState.SUCCESS -> {
            leftHand = Offset(cx - 23f * u, bodyTop - 7f * u)
            rightHand = Offset(cx + 23f * u, bodyTop - 7f * u)
        }
        else -> {
            leftHand = Offset(cx - 21f * u, bodyTop + 19f * u)
            rightHand = Offset(cx + 21f * u, bodyTop + 19f * u)
        }
    }
    fun arm(start: Offset, end: Offset) {
        drawLine(outline, start, end, 6.6f * u, StrokeCap.Round)
        drawLine(body, start, end, 4.8f * u, StrokeCap.Round)
        drawCircle(gold, 3.3f * u, end)
    }
    arm(leftShoulder, leftHand)
    arm(rightShoulder, rightHand)

    val hipY = bodyTop + 24f * u
    val step = if (state == CinePulseState.WALK) walk * 4f * u else 0f
    val leftFoot = Offset(cx - 9f * u - step, 91f * u + bob)
    val rightFoot = Offset(cx + 9f * u + step, 91f * u + bob)
    drawLine(body, Offset(cx - 7f * u, hipY), leftFoot, 7f * u, StrokeCap.Round)
    drawLine(body, Offset(cx + 7f * u, hipY), rightFoot, 7f * u, StrokeCap.Round)
    drawRoundRect(Color(0xFF0A0B0D), Offset(leftFoot.x - 7f * u, leftFoot.y - 2f * u), Size(12f * u, 5f * u), CornerRadius(2.5f * u))
    drawRoundRect(Color(0xFF0A0B0D), Offset(rightFoot.x - 5f * u, rightFoot.y - 2f * u), Size(12f * u, 5f * u), CornerRadius(2.5f * u))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNaviCharacter(
    state: CinePulseState,
    pointRight: Boolean,
    phase: Float,
    breathe: Float,
) {
    val u = size.minDimension / 100f
    val cx = size.width / 2f
    val dir = if (pointRight) 1f else -1f
    val wave = sin(phase * Math.PI * 2.0).toFloat()
    val walk = sin(phase * Math.PI * 2.0).toFloat()
    val bob = when (state) {
        CinePulseState.WALK -> abs(walk) * 1.2f * u
        CinePulseState.CELEBRATE, CinePulseState.SUCCESS -> abs(wave) * .7f * u
        else -> (breathe - 1f) * 7f * u
    }
    val skin = Color(0xFFF0C79E)
    val hair = Color(0xFF111317)
    val body = Color(0xFF15171B)
    val outline = Color(0xFF2B2E33)
    val shoe = Color(0xFFF2EEE5)
    val accent = MutedGold
    val headY = 31f * u + bob + if (state == CinePulseState.NOD) abs(wave) * 1.2f * u else 0f

    // Hair silhouette first: one stable hand-drawn-like shape, not a collection of generated assets.
    val hairPath = Path().apply {
        moveTo(cx - 23f * u, headY - 2f * u)
        cubicTo(cx - 26f * u, headY - 17f * u, cx - 15f * u, headY - 26f * u, cx - 3f * u, headY - 22f * u)
        cubicTo(cx + 5f * u, headY - 29f * u, cx + 20f * u, headY - 23f * u, cx + 22f * u, headY - 12f * u)
        cubicTo(cx + 31f * u, headY - 7f * u, cx + 26f * u, headY + 4f * u, cx + 19f * u, headY + 6f * u)
        cubicTo(cx + 13f * u, headY + 13f * u, cx + 7f * u, headY + 15f * u, cx + 2f * u, headY + 13f * u)
        lineTo(cx - 17f * u, headY + 11f * u)
        cubicTo(cx - 25f * u, headY + 9f * u, cx - 30f * u, headY + 2f * u, cx - 23f * u, headY - 2f * u)
        close()
    }
    drawPath(hairPath, hair)

    drawOval(
        color = skin,
        topLeft = Offset(cx - 17f * u, headY - 15f * u),
        size = Size(34f * u, 35f * u),
    )
    // Hair sweep over the forehead.
    val fringe = Path().apply {
        moveTo(cx - 18f * u, headY - 10f * u)
        cubicTo(cx - 10f * u, headY - 25f * u, cx + 10f * u, headY - 23f * u, cx + 18f * u, headY - 11f * u)
        cubicTo(cx + 6f * u, headY - 14f * u, cx + 1f * u, headY - 5f * u, cx - 4f * u, headY - 2f * u)
        cubicTo(cx - 7f * u, headY - 10f * u, cx - 11f * u, headY - 7f * u, cx - 18f * u, headY - 10f * u)
        close()
    }
    drawPath(fringe, hair)

    val look = when (state) {
        CinePulseState.POINT, CinePulseState.PRESENT -> dir * 1.5f * u
        CinePulseState.LOOK -> wave * 2f * u
        CinePulseState.THINK -> dir * .8f * u
        CinePulseState.LISTEN -> -dir * .7f * u
        else -> 0f
    }
    val blink = phase > .965f && state !in setOf(CinePulseState.SUCCESS, CinePulseState.CELEBRATE)
    val eyeY = headY + 2f * u
    val eyeDX = 7.7f * u
    if (state == CinePulseState.SUCCESS || state == CinePulseState.CELEBRATE) {
        listOf(-eyeDX, eyeDX).forEach { dx ->
            drawArc(
                color = hair,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(cx + dx - 3.5f * u + look, eyeY - 1.5f * u),
                size = Size(7f * u, 5.5f * u),
                style = Stroke(width = 1.8f * u, cap = StrokeCap.Round),
            )
        }
    } else {
        val eyeH = if (blink || state == CinePulseState.REST) 1.5f * u else 7.8f * u
        listOf(-eyeDX, eyeDX).forEach { dx ->
            drawRoundRect(
                hair,
                Offset(cx + dx - 1.7f * u + look, eyeY - eyeH / 2f),
                Size(3.4f * u, eyeH),
                CornerRadius(1.7f * u),
            )
        }
    }

    val bodyTop = 52f * u + bob
    val hoodie = Path().apply {
        moveTo(cx - 12f * u, bodyTop)
        cubicTo(cx - 19f * u, bodyTop + 7f * u, cx - 18f * u, bodyTop + 24f * u, cx - 12f * u, bodyTop + 29f * u)
        lineTo(cx + 12f * u, bodyTop + 29f * u)
        cubicTo(cx + 18f * u, bodyTop + 24f * u, cx + 19f * u, bodyTop + 7f * u, cx + 12f * u, bodyTop)
        close()
    }
    drawPath(hoodie, body)
    drawPath(hoodie, outline, style = Stroke(1.1f * u))
    drawCircle(accent, 3.4f * u, Offset(cx, bodyTop + 13f * u))
    drawRect(body, Offset(cx - .9f * u, bodyTop + 10f * u), Size(1.8f * u, 6f * u))

    val leftShoulder = Offset(cx - 13f * u, bodyTop + 7f * u)
    val rightShoulder = Offset(cx + 13f * u, bodyTop + 7f * u)
    val leftHand: Offset
    val rightHand: Offset
    when (state) {
        CinePulseState.WAVE -> {
            leftHand = Offset(cx - 20f * u, bodyTop + 20f * u)
            rightHand = Offset(cx + (18f + wave * 3f) * u, bodyTop - 6f * u)
        }
        CinePulseState.POINT, CinePulseState.PRESENT -> {
            if (dir > 0) {
                leftHand = Offset(cx - 20f * u, bodyTop + 20f * u)
                rightHand = Offset(cx + 31f * u, bodyTop + 8f * u)
            } else {
                leftHand = Offset(cx - 31f * u, bodyTop + 8f * u)
                rightHand = Offset(cx + 20f * u, bodyTop + 20f * u)
            }
        }
        CinePulseState.THINK -> {
            leftHand = Offset(cx - 19f * u, bodyTop + 20f * u)
            rightHand = Offset(cx + 18f * u, headY + 13f * u)
        }
        CinePulseState.CELEBRATE, CinePulseState.SUCCESS -> {
            leftHand = Offset(cx - 22f * u, bodyTop - 7f * u)
            rightHand = Offset(cx + 22f * u, bodyTop - 7f * u)
        }
        else -> {
            leftHand = Offset(cx - 19f * u, bodyTop + 20f * u)
            rightHand = Offset(cx + 19f * u, bodyTop + 20f * u)
        }
    }
    fun arm(start: Offset, end: Offset) {
        drawLine(body, start, end, 5.4f * u, StrokeCap.Round)
        drawCircle(skin, 3.2f * u, end)
    }
    arm(leftShoulder, leftHand)
    arm(rightShoulder, rightHand)

    val hipY = bodyTop + 27f * u
    val step = if (state == CinePulseState.WALK) walk * 3.2f * u else 0f
    val leftFoot = Offset(cx - 8f * u - step, 92f * u + bob)
    val rightFoot = Offset(cx + 8f * u + step, 92f * u + bob)
    drawLine(body, Offset(cx - 6f * u, hipY), leftFoot, 6.2f * u, StrokeCap.Round)
    drawLine(body, Offset(cx + 6f * u, hipY), rightFoot, 6.2f * u, StrokeCap.Round)
    drawRoundRect(shoe, Offset(leftFoot.x - 6.8f * u, leftFoot.y - 2.2f * u), Size(11f * u, 4.8f * u), CornerRadius(2.4f * u))
    drawRoundRect(shoe, Offset(rightFoot.x - 4.2f * u, rightFoot.y - 2.2f * u), Size(11f * u, 4.8f * u), CornerRadius(2.4f * u))
}
