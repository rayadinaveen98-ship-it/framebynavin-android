package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

enum class FrameGuidePose { IDLE, WALK, POINT, PRESENT, LOOK, NOD, WAVE, THINK, LISTEN, SUCCESS, CELEBRATE, REST }

/** Compatibility surface used by setup, guided tour and helper moments. */
@Composable
internal fun FrameGuideCompanion(
    pose: FrameGuidePose,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    val state = when (pose) {
        FrameGuidePose.IDLE -> CinePulseState.IDLE
        FrameGuidePose.WALK -> CinePulseState.WALK
        FrameGuidePose.POINT -> CinePulseState.POINT
        FrameGuidePose.PRESENT -> CinePulseState.PRESENT
        FrameGuidePose.LOOK -> CinePulseState.LOOK
        FrameGuidePose.NOD -> CinePulseState.NOD
        FrameGuidePose.WAVE -> CinePulseState.WAVE
        FrameGuidePose.THINK -> CinePulseState.THINK
        FrameGuidePose.LISTEN -> CinePulseState.LISTEN
        FrameGuidePose.SUCCESS -> CinePulseState.SUCCESS
        FrameGuidePose.CELEBRATE -> CinePulseState.CELEBRATE
        FrameGuidePose.REST -> CinePulseState.REST
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CinePulseGuide(
            state = state,
            modifier = Modifier.fillMaxSize(),
            pointRight = pointRight,
        )
    }
}

@Composable
internal fun V127SetupGuideStrip(page: Int) {
    val tips = listOf(
        "Choose your main creator mode.",
        "Choose where you publish.",
        "Choose how you usually create.",
        "Pick your priorities.",
        "Optional reminder permissions.",
    )
    val poses = listOf(FrameGuidePose.PRESENT, FrameGuidePose.LOOK, FrameGuidePose.POINT, FrameGuidePose.THINK, FrameGuidePose.SUCCESS)

    AnimatedContent(
        targetState = page.coerceIn(0, tips.lastIndex),
        transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
        label = "setupBacklotGuide",
    ) { index ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, RecRed.copy(alpha = .22f)),
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                FrameGuideCompanion(poses[index], Modifier.size(72.dp), pointRight = true)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(V144GuidePrefs.currentGuide.displayName.uppercase(), color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(tips[index], color = ProjectorIvory, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
