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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    FRAME("Frame", "Simple. Expressive. Always with you."),
    NAVI("Navi", "Curious. Calm. Creative."),
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
        selectedCharacter = saved?.let { value -> BacklotCharacter.entries.firstOrNull { it.name == value } }
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
                    V138CharacterChoiceCard(
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
private fun V138CharacterChoiceCard(
    character: BacklotCharacter,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val identity = if (character == BacklotCharacter.FRAME) FrameGold else NaviCoral
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = CinemaSurface,
        border = BorderStroke(1.2.dp, identity.copy(alpha = .78f)),
    ) {
        Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(158.dp),
                shape = RoundedCornerShape(18.dp),
                color = CharacterStage,
                border = BorderStroke(1.dp, identity.copy(alpha = .28f)),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BacklotGuideCharacter(
                        character = character,
                        state = CinePulseState.WAVE,
                        modifier = Modifier.size(width = 128.dp, height = 148.dp),
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(character.displayName, color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(character.description, color = MutedText, fontSize = 8.4.sp, lineHeight = 11.sp, textAlign = TextAlign.Center, minLines = 2)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = identity,
                    contentColor = if (character == BacklotCharacter.FRAME) Color(0xFF121212) else Color.White,
                ),
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
        Text("Frame and Navi keep their own identity in every theme.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BacklotCharacter.entries.forEach { character ->
                val active = character == selected
                val identity = if (character == BacklotCharacter.FRAME) FrameGold else NaviCoral
                Surface(
                    modifier = Modifier.weight(1f).clickable { BacklotCharacterPrefs.select(character) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) CinemaSurfaceRaised else CinemaSurface,
                    border = BorderStroke(1.2.dp, if (active) identity else CinemaLine),
                ) {
                    Column(Modifier.padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.height(108.dp).fillMaxWidth(),
                            color = CharacterStage,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                BacklotGuideCharacter(character, CinePulseState.IDLE, Modifier.size(width = 88.dp, height = 102.dp))
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(character.displayName, color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                        Text(if (active) "SELECTED" else "TAP TO USE", color = if (active) identity else MutedText, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private val FrameGold = Color(0xFFFFC857)
private val FrameGoldDeep = Color(0xFF9F6A10)
private val CharacterInk = Color(0xFF0A0C10)
private val CharacterBody = Color(0xFF171A20)
private val CharacterKeyline = Color(0xFF5A616D)
private val CharacterIvory = Color(0xFFFFFBF3)
private val CharacterStage = Color(0xFF15191F)
private val NaviSkin = Color(0xFFFFC998)
private val NaviSkinShade = Color(0xFFE8A978)
private val NaviHair = Color(0xFF0B1118)
private val NaviHairLine = Color(0xFF43566D)
private val NaviCoral = Color(0xFFF04F54)

/**
 * Deterministic flat-vector renderer. Identity colors never inherit the selected app theme,
 * so Frame and Navi remain recognizable against every Backlot visual world.
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
        // Solid floor mark separates the character from both dark and light themes without glow.
        drawOval(
            color = Color.Black.copy(alpha = .26f),
            topLeft = Offset(size.width * .25f, size.height * .88f),
            size = Size(size.width * .50f, size.height * .065f),
        )
        when (character) {
            BacklotCharacter.FRAME -> drawV138Frame(state, pointRight, phase, breathe)
            BacklotCharacter.NAVI -> drawV138Navi(state, pointRight, phase, breathe)
        }
    }
}

private fun DrawScope.drawV138Frame(state: CinePulseState, pointRight: Boolean, phase: Float, breathe: Float) {
    val u = size.minDimension / 100f
    val cx = size.width / 2f
    val dir = if (pointRight) 1f else -1f
    val wave = sin(phase * Math.PI * 2.0).toFloat()
    val walk = sin(phase * Math.PI * 2.0).toFloat()
    val bob = when (state) {
        CinePulseState.WALK -> abs(walk) * 1.4f * u
        CinePulseState.CELEBRATE, CinePulseState.SUCCESS -> abs(wave) * .9f * u
        else -> (breathe - 1f) * 8f * u
    }
    val headY = 28f * u + bob + if (state == CinePulseState.NOD) abs(wave) * 1.4f * u else 0f
    val headW = 64f * u
    val headH = 42f * u
    val headLeft = cx - headW / 2f
    val headTop = headY - headH / 2f

    drawRoundRect(Color.Black.copy(alpha = .38f), Offset(headLeft + 3f * u, headTop + 3.4f * u), Size(headW, headH), CornerRadius(11f * u))
    drawRoundRect(CharacterInk, Offset(headLeft, headTop), Size(headW, headH), CornerRadius(11f * u))
    drawRoundRect(FrameGoldDeep, Offset(headLeft - .8f * u, headTop - .8f * u), Size(headW + 1.6f * u, headH + 1.6f * u), CornerRadius(11.8f * u), style = Stroke(6.2f * u))
    drawRoundRect(FrameGold, Offset(headLeft, headTop), Size(headW, headH), CornerRadius(11f * u), style = Stroke(4.2f * u))

    val look = when (state) {
        CinePulseState.POINT, CinePulseState.PRESENT -> dir * 2f * u
        CinePulseState.LOOK -> wave * 2.5f * u
        CinePulseState.THINK -> dir * 1f * u
        CinePulseState.LISTEN -> -dir * .7f * u
        else -> 0f
    }
    val eyeY = headY + .5f * u
    val eyeDX = 13f * u
    val blink = phase > .965f

    when {
        state in setOf(CinePulseState.SUCCESS, CinePulseState.CELEBRATE) -> {
            listOf(-eyeDX, eyeDX).forEach { dx ->
                drawArc(CharacterIvory, 205f, 130f, false, Offset(cx + dx - 5f * u + look, eyeY - 2.2f * u), Size(10f * u, 7f * u), style = Stroke(2.3f * u, cap = StrokeCap.Round))
            }
        }
        state == CinePulseState.THINK -> {
            listOf(-eyeDX, eyeDX).forEachIndexed { index, dx ->
                val y = eyeY + if (index == 1) 1.6f * u else 0f
                drawRoundRect(CharacterIvory, Offset(cx + dx - 3.3f * u + look, y - 1.5f * u), Size(6.6f * u, 3f * u), CornerRadius(1.5f * u))
            }
        }
        state == CinePulseState.LISTEN -> {
            listOf(-eyeDX, eyeDX).forEach { dx -> drawOval(CharacterIvory, Offset(cx + dx - 4f * u + look, eyeY - 5f * u), Size(8f * u, 10f * u)) }
        }
        state == CinePulseState.REST || blink -> {
            listOf(-eyeDX, eyeDX).forEach { dx -> drawRoundRect(CharacterIvory.copy(alpha = .82f), Offset(cx + dx - 3.4f * u + look, eyeY - .9f * u), Size(6.8f * u, 1.8f * u), CornerRadius(.9f * u)) }
        }
        else -> {
            listOf(-eyeDX, eyeDX).forEach { dx -> drawRoundRect(CharacterIvory, Offset(cx + dx - 2.5f * u + look, eyeY - 5.5f * u), Size(5f * u, 11f * u), CornerRadius(2.5f * u)) }
        }
    }

    val bodyTop = 51f * u + bob
    drawRoundRect(CharacterBody, Offset(cx - 15f * u, bodyTop), Size(30f * u, 28f * u), CornerRadius(9f * u))
    drawRoundRect(CharacterKeyline, Offset(cx - 15f * u, bodyTop), Size(30f * u, 28f * u), CornerRadius(9f * u), style = Stroke(1.7f * u))
    drawRoundRect(FrameGold, Offset(cx - 3.6f * u, bodyTop + 8f * u), Size(7.2f * u, 8f * u), CornerRadius(1.5f * u))
    drawRect(CharacterInk, Offset(cx - 1.25f * u, bodyTop + 9.4f * u), Size(1.6f * u, 5.2f * u))

    val leftShoulder = Offset(cx - 13f * u, bodyTop + 7f * u)
    val rightShoulder = Offset(cx + 13f * u, bodyTop + 7f * u)
    val (leftHand, rightHand) = v138Hands(state, cx, bodyTop, headY, dir, wave, u, frame = true)
    fun arm(start: Offset, end: Offset) {
        drawLine(CharacterKeyline, start, end, 7.4f * u, StrokeCap.Round)
        drawLine(CharacterBody, start, end, 5.1f * u, StrokeCap.Round)
        drawCircle(FrameGold, 3.7f * u, end)
    }
    arm(leftShoulder, leftHand)
    arm(rightShoulder, rightHand)

    val hipY = bodyTop + 25f * u
    val step = if (state == CinePulseState.WALK) walk * 4f * u else 0f
    val leftFoot = Offset(cx - 9f * u - step, 91f * u + bob)
    val rightFoot = Offset(cx + 9f * u + step, 91f * u + bob)
    drawLine(CharacterKeyline, Offset(cx - 7f * u, hipY), leftFoot, 8f * u, StrokeCap.Round)
    drawLine(CharacterBody, Offset(cx - 7f * u, hipY), leftFoot, 5.7f * u, StrokeCap.Round)
    drawLine(CharacterKeyline, Offset(cx + 7f * u, hipY), rightFoot, 8f * u, StrokeCap.Round)
    drawLine(CharacterBody, Offset(cx + 7f * u, hipY), rightFoot, 5.7f * u, StrokeCap.Round)
    drawRoundRect(CharacterInk, Offset(leftFoot.x - 7f * u, leftFoot.y - 2f * u), Size(12f * u, 5.5f * u), CornerRadius(2.5f * u))
    drawRoundRect(CharacterInk, Offset(rightFoot.x - 5f * u, rightFoot.y - 2f * u), Size(12f * u, 5.5f * u), CornerRadius(2.5f * u))
}

private fun DrawScope.drawV138Navi(state: CinePulseState, pointRight: Boolean, phase: Float, breathe: Float) {
    val u = size.minDimension / 100f
    val cx = size.width / 2f
    val dir = if (pointRight) 1f else -1f
    val wave = sin(phase * Math.PI * 2.0).toFloat()
    val walk = sin(phase * Math.PI * 2.0).toFloat()
    val bob = when (state) {
        CinePulseState.WALK -> abs(walk) * 1.2f * u
        CinePulseState.CELEBRATE, CinePulseState.SUCCESS -> abs(wave) * .8f * u
        else -> (breathe - 1f) * 7f * u
    }
    val headY = 31f * u + bob + if (state == CinePulseState.NOD) abs(wave) * 1.2f * u else 0f

    // Strong blue-black outer hair keyline makes the silhouette readable on every theme.
    val outerHair = naviHairPath(cx, headY, u, 1.08f)
    drawPath(outerHair, NaviHairLine)
    drawPath(naviHairPath(cx, headY, u, 1f), NaviHair)

    drawOval(NaviSkinShade, Offset(cx - 17.8f * u, headY - 15.8f * u), Size(35.6f * u, 36.6f * u))
    drawOval(NaviSkin, Offset(cx - 17f * u, headY - 15f * u), Size(34f * u, 35f * u))
    drawPath(naviFringePath(cx, headY, u), NaviHair)

    val look = when (state) {
        CinePulseState.POINT, CinePulseState.PRESENT -> dir * 1.6f * u
        CinePulseState.LOOK -> wave * 2f * u
        CinePulseState.THINK -> dir * .8f * u
        CinePulseState.LISTEN -> -dir * .6f * u
        else -> 0f
    }
    val eyeY = headY + 1.5f * u
    val eyeDX = 7.8f * u
    val blink = phase > .965f

    when {
        state in setOf(CinePulseState.SUCCESS, CinePulseState.CELEBRATE) -> {
            listOf(-eyeDX, eyeDX).forEach { dx ->
                drawArc(NaviHair, 205f, 130f, false, Offset(cx + dx - 4f * u + look, eyeY - 2f * u), Size(8f * u, 6f * u), style = Stroke(2f * u, cap = StrokeCap.Round))
            }
            drawArc(NaviHair, 10f, 160f, false, Offset(cx - 4.5f * u, headY + 7f * u), Size(9f * u, 5.5f * u), style = Stroke(1.5f * u, cap = StrokeCap.Round))
        }
        state == CinePulseState.THINK -> {
            drawOval(NaviHair, Offset(cx - eyeDX - 2f * u + look, eyeY - 3.4f * u), Size(4f * u, 6.8f * u))
            drawRoundRect(NaviHair, Offset(cx + eyeDX - 2.5f * u + look, eyeY - .8f * u), Size(5f * u, 1.6f * u), CornerRadius(.8f * u))
            drawLine(NaviHair, Offset(cx + 4f * u, headY - 6f * u), Offset(cx + 10f * u, headY - 8f * u), 1.25f * u, StrokeCap.Round)
            drawCircle(NaviHair, 1.1f * u, Offset(cx + 1f * u, headY + 8f * u))
        }
        state == CinePulseState.LISTEN -> {
            listOf(-eyeDX, eyeDX).forEach { dx -> drawOval(NaviHair, Offset(cx + dx - 2.8f * u + look, eyeY - 4.5f * u), Size(5.6f * u, 9f * u)) }
            drawOval(NaviHair, Offset(cx - 2.3f * u, headY + 6.5f * u), Size(4.6f * u, 6f * u))
        }
        state == CinePulseState.REST || blink -> {
            listOf(-eyeDX, eyeDX).forEach { dx -> drawRoundRect(NaviHair, Offset(cx + dx - 2.5f * u + look, eyeY - .8f * u), Size(5f * u, 1.6f * u), CornerRadius(.8f * u)) }
        }
        else -> {
            listOf(-eyeDX, eyeDX).forEach { dx -> drawOval(NaviHair, Offset(cx + dx - 2.3f * u + look, eyeY - 4.2f * u), Size(4.6f * u, 8.4f * u)) }
            drawLine(NaviHair.copy(alpha = .76f), Offset(cx - 1.8f * u, headY + 8f * u), Offset(cx + 1.8f * u, headY + 8f * u), 1.1f * u, StrokeCap.Round)
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
    drawPath(hoodie, CharacterBody)
    drawPath(hoodie, NaviHairLine, style = Stroke(1.8f * u))
    drawRoundRect(FrameGold, Offset(cx - 3.2f * u, bodyTop + 9f * u), Size(6.4f * u, 7.2f * u), CornerRadius(1.4f * u))
    drawRect(CharacterInk, Offset(cx - 1f * u, bodyTop + 10.2f * u), Size(1.5f * u, 4.8f * u))

    val leftShoulder = Offset(cx - 13f * u, bodyTop + 7f * u)
    val rightShoulder = Offset(cx + 13f * u, bodyTop + 7f * u)
    val (leftHand, rightHand) = v138Hands(state, cx, bodyTop, headY, dir, wave, u, frame = false)
    fun arm(start: Offset, end: Offset) {
        drawLine(NaviHairLine, start, end, 6.7f * u, StrokeCap.Round)
        drawLine(CharacterBody, start, end, 4.7f * u, StrokeCap.Round)
        drawCircle(NaviSkinShade, 3.6f * u, end)
        drawCircle(NaviSkin, 3.05f * u, end)
    }
    arm(leftShoulder, leftHand)
    arm(rightShoulder, rightHand)

    val hipY = bodyTop + 27f * u
    val step = if (state == CinePulseState.WALK) walk * 3.2f * u else 0f
    val leftFoot = Offset(cx - 8f * u - step, 92f * u + bob)
    val rightFoot = Offset(cx + 8f * u + step, 92f * u + bob)
    drawLine(NaviHairLine, Offset(cx - 6f * u, hipY), leftFoot, 7.2f * u, StrokeCap.Round)
    drawLine(CharacterBody, Offset(cx - 6f * u, hipY), leftFoot, 5.3f * u, StrokeCap.Round)
    drawLine(NaviHairLine, Offset(cx + 6f * u, hipY), rightFoot, 7.2f * u, StrokeCap.Round)
    drawLine(CharacterBody, Offset(cx + 6f * u, hipY), rightFoot, 5.3f * u, StrokeCap.Round)
    drawRoundRect(CharacterIvory, Offset(leftFoot.x - 7f * u, leftFoot.y - 2.4f * u), Size(11.5f * u, 5.4f * u), CornerRadius(2.6f * u))
    drawRoundRect(CharacterIvory, Offset(rightFoot.x - 4.5f * u, rightFoot.y - 2.4f * u), Size(11.5f * u, 5.4f * u), CornerRadius(2.6f * u))
}

private fun v138Hands(
    state: CinePulseState,
    cx: Float,
    bodyTop: Float,
    headY: Float,
    dir: Float,
    wave: Float,
    u: Float,
    frame: Boolean,
): Pair<Offset, Offset> {
    val down = if (frame) 20f else 20f
    return when (state) {
        CinePulseState.WAVE -> Offset(cx - 20f * u, bodyTop + down * u) to Offset(cx + (19f + wave * 3f) * u, bodyTop - 8f * u)
        CinePulseState.POINT, CinePulseState.PRESENT -> if (dir > 0) {
            Offset(cx - 20f * u, bodyTop + down * u) to Offset(cx + 33f * u, bodyTop + 6f * u)
        } else {
            Offset(cx - 33f * u, bodyTop + 6f * u) to Offset(cx + 20f * u, bodyTop + down * u)
        }
        CinePulseState.THINK -> Offset(cx - 19f * u, bodyTop + down * u) to Offset(cx + 18f * u, headY + 12f * u)
        CinePulseState.CELEBRATE, CinePulseState.SUCCESS -> Offset(cx - 24f * u, bodyTop - 9f * u) to Offset(cx + 24f * u, bodyTop - 9f * u)
        else -> Offset(cx - 20f * u, bodyTop + down * u) to Offset(cx + 20f * u, bodyTop + down * u)
    }
}

private fun naviHairPath(cx: Float, headY: Float, u: Float, scale: Float): Path = Path().apply {
    val s = scale
    moveTo(cx - 23f * u * s, headY - 2f * u)
    cubicTo(cx - 28f * u * s, headY - 18f * u, cx - 16f * u * s, headY - 28f * u, cx - 4f * u, headY - 23f * u)
    cubicTo(cx + 5f * u, headY - 31f * u, cx + 21f * u * s, headY - 25f * u, cx + 23f * u * s, headY - 13f * u)
    cubicTo(cx + 32f * u * s, headY - 8f * u, cx + 28f * u * s, headY + 5f * u, cx + 20f * u * s, headY + 8f * u)
    cubicTo(cx + 12f * u, headY + 15f * u, cx + 4f * u, headY + 16f * u, cx - 3f * u, headY + 14f * u)
    lineTo(cx - 18f * u * s, headY + 12f * u)
    cubicTo(cx - 27f * u * s, headY + 10f * u, cx - 31f * u * s, headY + 2f * u, cx - 23f * u * s, headY - 2f * u)
    close()
}

private fun naviFringePath(cx: Float, headY: Float, u: Float): Path = Path().apply {
    moveTo(cx - 18f * u, headY - 10f * u)
    cubicTo(cx - 10f * u, headY - 26f * u, cx + 11f * u, headY - 24f * u, cx + 18f * u, headY - 11f * u)
    cubicTo(cx + 7f * u, headY - 14f * u, cx + 2f * u, headY - 5f * u, cx - 4f * u, headY - 2f * u)
    cubicTo(cx - 7f * u, headY - 10f * u, cx - 11f * u, headY - 7f * u, cx - 18f * u, headY - 10f * u)
    close()
}
