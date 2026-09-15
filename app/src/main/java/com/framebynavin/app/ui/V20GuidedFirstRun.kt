package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/**
 * Premium contextual first-run journey. The actual app stays visible and usable; the Frame Guide
 * moves with the journey, points toward the current area and keeps copy intentionally short.
 */
@Composable
internal fun V20GuidedFirstRunCoach(
    step: CreatorGuidedTourStep,
    hasProjects: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onSkip: () -> Unit,
) {
    val copy = guidedCopy(step, hasProjects)
    val placeAtTop = step == CreatorGuidedTourStep.CONTROL
    val pointRight = step.ordinal % 2 == 0

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, CinemaBlack.copy(alpha = .18f), CinemaBlack.copy(alpha = .42f)),
                radius = 1150f,
            )
        ),
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally(tween(330, easing = FastOutSlowInEasing)) { if (targetState.ordinal >= initialState.ordinal) it / 3 else -it / 3 } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { if (targetState.ordinal >= initialState.ordinal) -it / 4 else it / 4 } + fadeOut(tween(150)))
            },
            modifier = Modifier
                .align(if (placeAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                .then(if (placeAtTop) Modifier.statusBarsPadding().padding(top = 8.dp) else Modifier.navigationBarsPadding().padding(bottom = 88.dp))
                .padding(horizontal = 12.dp)
                .widthIn(max = 520.dp),
            label = "frameGuideJourney",
        ) { current ->
            val currentCopy = guidedCopy(current, hasProjects)
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = if (pointRight) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    FrameGuideCompanion(
                        pose = currentCopy.pose,
                        modifier = Modifier.size(width = 80.dp, height = 96.dp),
                        pointRight = pointRight,
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = CinemaSurfaceRaised.copy(alpha = .97f),
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .34f)),
                    shadowElevation = 16.dp,
                ) {
                    Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(6) { index ->
                                Box(
                                    Modifier.weight(1f).height(if (index == current.ordinal) 4.dp else 2.dp)
                                        .background(
                                            if (index <= current.ordinal) RecRed else CinemaLine,
                                            RoundedCornerShape(100.dp),
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = RecRed.copy(alpha = .13f),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(currentCopy.icon, null, tint = RecRed, modifier = Modifier.size(19.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${currentCopy.eyebrow} · ${current.ordinal + 1}/6",
                                    color = MutedGold,
                                    fontSize = 7.8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = .8.sp,
                                )
                                Text(currentCopy.title, color = ProjectorIvory, fontSize = 15.5.sp, fontWeight = FontWeight.Black)
                            }
                            TextButton(onClick = onSkip, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)) {
                                Text("SKIP", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(currentCopy.body, color = ProjectorIvory.copy(alpha = .76f), fontSize = 10.2.sp, lineHeight = 14.sp)
                        Spacer(Modifier.height(11.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            currentCopy.secondary?.let { label ->
                                OutlinedButton(
                                    onClick = onSecondary,
                                    modifier = Modifier.weight(.82f).height(43.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, CinemaLine),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                ) {
                                    Text(label, color = ProjectorIvory.copy(alpha = .76f), fontSize = 8.1.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Button(
                                onClick = onPrimary,
                                modifier = Modifier.weight(1f).height(43.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            ) {
                                Text(currentCopy.primary, fontSize = 8.8.sp, fontWeight = FontWeight.Black)
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
    CreatorGuidedTourStep.TODAY -> GuidedCoachCopy(
        eyebrow = "TODAY",
        title = "This is your command center",
        body = "Deadlines, active work and the next useful action live here.",
        primary = "SHOW IDEAS",
        icon = Icons.Outlined.Home,
        pose = FrameGuidePose.POINT,
    )
    CreatorGuidedTourStep.IDEAS -> GuidedCoachCopy(
        eyebrow = "IDEA VAULT",
        title = "Capture first. Decide later.",
        body = "Save rough thoughts without forcing every idea into a project.",
        primary = "CAPTURE IDEA",
        secondary = "NEXT",
        icon = Icons.Outlined.Lightbulb,
        pose = FrameGuidePose.WALK,
    )
    CreatorGuidedTourStep.PROJECT -> GuidedCoachCopy(
        eyebrow = "PROJECT",
        title = if (hasProjects) "Open something real" else "Build your first project",
        body = if (hasProjects) "I’ll follow you into the real workspace." else "Create one now, or continue and come back whenever you are ready.",
        primary = if (hasProjects) "OPEN PROJECT" else "CREATE PROJECT",
        secondary = if (hasProjects) null else "NOT NOW",
        icon = Icons.Outlined.AddCircleOutline,
        pose = FrameGuidePose.POINT,
    )
    CreatorGuidedTourStep.WORKSPACE -> GuidedCoachCopy(
        eyebrow = "WORKSPACE",
        title = "Move the work, one stage at a time",
        body = "Your stages, tools and project context stay together here.",
        primary = "SHOW INSIGHTS",
        icon = Icons.Outlined.MovieEdit,
        pose = FrameGuidePose.POINT,
    )
    CreatorGuidedTourStep.INSIGHTS -> GuidedCoachCopy(
        eyebrow = "INSIGHTS",
        title = "This is your creator brain",
        body = "See patterns, evidence and what to improve next.",
        primary = "SHOW CONTROL",
        icon = Icons.Outlined.Insights,
        pose = FrameGuidePose.IDLE,
    )
    CreatorGuidedTourStep.CONTROL -> GuidedCoachCopy(
        eyebrow = "CONTROL",
        title = "Fast actions live here",
        body = "Create, capture, plan and manage the system without hunting through menus.",
        primary = "FINISH TOUR",
        icon = Icons.Outlined.GridView,
        pose = FrameGuidePose.CELEBRATE,
    )
}
