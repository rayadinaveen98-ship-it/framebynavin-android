package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorGuidedTourStep
import com.framebynavin.app.ui.theme.*

private data class GuidedCoachCopy(
    val eyebrow: String,
    val title: String,
    val body: String,
    val primary: String,
    val secondary: String? = null,
    val icon: ImageVector,
    val pose: FrameGuidePose = FrameGuidePose.POINT,
)

private data class SpotlightSpec(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val corner: Float = 34f,
)

private fun spotlightFor(step: CreatorGuidedTourStep): SpotlightSpec = when (step) {
    CreatorGuidedTourStep.TODAY -> SpotlightSpec(.045f, .23f, .91f, .38f)
    CreatorGuidedTourStep.IDEAS -> SpotlightSpec(.78f, .035f, .17f, .13f, 50f)
    CreatorGuidedTourStep.PROJECT -> SpotlightSpec(.05f, .18f, .90f, .30f)
    CreatorGuidedTourStep.WORKSPACE -> SpotlightSpec(.04f, .22f, .92f, .43f)
    CreatorGuidedTourStep.INSIGHTS -> SpotlightSpec(.045f, .17f, .91f, .27f)
    CreatorGuidedTourStep.CONTROL -> SpotlightSpec(.78f, .74f, .17f, .14f, 54f)
}

/** Premium spotlight journey: the real product remains visible while only the target stays clear. */
@Composable
internal fun V20GuidedFirstRunCoach(
    step: CreatorGuidedTourStep,
    hasProjects: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onSkip: () -> Unit,
) {
    val placeAtTop = step == CreatorGuidedTourStep.CONTROL
    val pointRight = step.ordinal % 2 == 0
    val spot = spotlightFor(step)
    val spotlightMotion = rememberInfiniteTransition(label = "spotlightBreath")
    val spotlightBreath by spotlightMotion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spotlightBreathValue",
    )
    val spotX by animateFloatAsState(spot.x, tween(360, easing = FastOutSlowInEasing), label = "spotX")
    val spotY by animateFloatAsState(spot.y, tween(360, easing = FastOutSlowInEasing), label = "spotY")
    val spotW by animateFloatAsState(spot.width, tween(360, easing = FastOutSlowInEasing), label = "spotW")
    val spotH by animateFloatAsState(spot.height, tween(360, easing = FastOutSlowInEasing), label = "spotH")

    Box(Modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        ) {
            drawRect(CinemaBlack.copy(alpha = .76f))
            val breathing = 2.5f + spotlightBreath * 3.5f
            val left = size.width * spotX - breathing
            val top = size.height * spotY - breathing
            val width = size.width * spotW + breathing * 2f
            val height = size.height * spotH + breathing * 2f
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(spot.corner + breathing, spot.corner + breathing),
                blendMode = BlendMode.Clear,
            )
            drawRoundRect(
                color = RecRed.copy(alpha = .34f + spotlightBreath * .28f),
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(spot.corner + breathing, spot.corner + breathing),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f + spotlightBreath * 1.2f),
            )
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(360, easing = FastOutSlowInEasing)) { if (targetState.ordinal >= initialState.ordinal) it / 4 else -it / 4 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { if (targetState.ordinal >= initialState.ordinal) -it / 5 else it / 5 } + fadeOut(tween(150)))
            },
            modifier = Modifier
                .align(if (placeAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                .then(if (placeAtTop) Modifier.statusBarsPadding().padding(top = 10.dp) else Modifier.navigationBarsPadding().padding(bottom = 88.dp))
                .padding(horizontal = 16.dp)
                .widthIn(max = 470.dp),
            label = "frameGuideSpotlightJourney",
        ) { current ->
            val copy = guidedCopy(current, hasProjects)
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = if (pointRight) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    FrameGuideCompanion(
                        pose = copy.pose,
                        modifier = Modifier.size(64.dp),
                        pointRight = pointRight,
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = CinemaSurfaceRaised.copy(alpha = if (VisualExperiencePrefs.isGlass) .82f else .96f),
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .38f)),
                    shadowElevation = 18.dp,
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(6) { index ->
                                Box(
                                    Modifier.weight(1f).height(if (index == current.ordinal) 3.dp else 2.dp)
                                        .background(if (index <= current.ordinal) RecRed else CinemaLine, RoundedCornerShape(100.dp))
                                )
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(34.dp), RoundedCornerShape(11.dp), RecRed.copy(alpha = .13f)) {
                                Box(contentAlignment = Alignment.Center) { Icon(copy.icon, null, tint = RecRed, modifier = Modifier.size(18.dp)) }
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${copy.eyebrow} · ${current.ordinal + 1}/6", color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                                Text(copy.title, color = ProjectorIvory, fontSize = 14.5.sp, fontWeight = FontWeight.Black)
                            }
                            TextButton(onClick = onSkip, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)) {
                                Text("SKIP", color = MutedText, fontSize = 7.7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(copy.body, color = ProjectorIvory.copy(alpha = .72f), fontSize = 9.7.sp, lineHeight = 13.5.sp)
                        Spacer(Modifier.height(9.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            copy.secondary?.let { label ->
                                OutlinedButton(
                                    onClick = onSecondary,
                                    modifier = Modifier.weight(.78f).height(41.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    border = BorderStroke(1.dp, CinemaLine),
                                ) { Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black) }
                            }
                            Button(
                                onClick = onPrimary,
                                modifier = Modifier.weight(1f).height(41.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                                shape = RoundedCornerShape(13.dp),
                            ) {
                                Text(copy.primary, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(5.dp))
                                Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun guidedCopy(step: CreatorGuidedTourStep, hasProjects: Boolean): GuidedCoachCopy = when (step) {
    CreatorGuidedTourStep.TODAY -> GuidedCoachCopy("TODAY", "Your command center", "Today shows the next thing worth your attention.", "SHOW IDEAS", icon = Icons.Outlined.Home, pose = FrameGuidePose.PRESENT)
    CreatorGuidedTourStep.IDEAS -> GuidedCoachCopy("IDEA VAULT", "Capture before it disappears", "Use + for a quick thought. Organize it later.", "CAPTURE IDEA", "NEXT", Icons.Outlined.Lightbulb, FrameGuidePose.POINT)
    CreatorGuidedTourStep.PROJECT -> GuidedCoachCopy("PROJECT", if (hasProjects) "Open a project" else "Build your first project", if (hasProjects) "Open one and I’ll follow you into the workspace." else "Create one now, or continue without one.", if (hasProjects) "OPEN PROJECT" else "CREATE PROJECT", if (hasProjects) null else "NOT NOW", Icons.Outlined.AddCircleOutline, FrameGuidePose.WALK)
    CreatorGuidedTourStep.WORKSPACE -> GuidedCoachCopy("WORKSPACE", "Move work stage by stage", "Your project tools and progress stay together here.", "SHOW INSIGHTS", icon = Icons.Outlined.MovieEdit, pose = FrameGuidePose.WAVE)
    CreatorGuidedTourStep.INSIGHTS -> GuidedCoachCopy("INSIGHTS", "Your creator brain", "Patterns and evidence help you decide what to improve next.", "SHOW CONTROL", icon = Icons.Outlined.Insights, pose = FrameGuidePose.THINK)
    CreatorGuidedTourStep.CONTROL -> GuidedCoachCopy("CONTROL", "Fast actions live here", "Create, capture and manage the system from one place.", "FINISH TOUR", icon = Icons.Outlined.GridView, pose = FrameGuidePose.CELEBRATE)
}
