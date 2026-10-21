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
    val speech: String,
    val primary: String,
    val secondary: String? = null,
    val icon: ImageVector,
    val pose: FrameGuidePose = FrameGuidePose.POINT,
)

@Composable
internal fun V20GuidedFirstRunCoach(
    step: CreatorGuidedTourStep,
    hasProjects: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onSkip: () -> Unit,
) {
    val placeAtTop = true // Navigation highlights sit below; never cover the actual target.
    val pointRight = step.ordinal % 2 == 0
    // Actual spotlight geometry is measured from laid-out navigation items by
    // V137GuideSharpWindow. The coach must never draw a second guessed hole.
    Box(Modifier.fillMaxSize()) {
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
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = if (pointRight) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (pointRight) {
                        FrameGuideCompanion(
                            pose = copy.pose,
                            modifier = Modifier.size(width = 75.dp, height = 90.dp),
                            pointRight = true,
                        )
                        Spacer(Modifier.width(7.dp))
                        V145GuideSpeechBubble(copy.speech)
                    } else {
                        V145GuideSpeechBubble(copy.speech)
                        Spacer(Modifier.width(7.dp))
                        FrameGuideCompanion(
                            pose = copy.pose,
                            modifier = Modifier.size(width = 75.dp, height = 90.dp),
                            pointRight = false,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = CinemaSurfaceRaised.copy(alpha = if (VisualExperiencePrefs.isGlass) .90f else .98f),
                    border = BorderStroke(1.2.dp, RecRed.copy(alpha = .58f)),
                    shadowElevation = 20.dp,
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(V207GuidedTourPolicy.lessons.size) { index ->
                                Box(
                                    Modifier.weight(1f).height(if (index == V207GuidedTourPolicy.indexOf(current)) 4.dp else 2.dp)
                                        .background(if (index <= V207GuidedTourPolicy.indexOf(current)) RecRed else CinemaLine, RoundedCornerShape(100.dp))
                                )
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(34.dp), RoundedCornerShape(11.dp), RecRed.copy(alpha = .16f)) {
                                Box(contentAlignment = Alignment.Center) { Icon(copy.icon, null, tint = RecRed, modifier = Modifier.size(18.dp)) }
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${copy.eyebrow} · ${V207GuidedTourPolicy.indexOf(current) + 1}/${V207GuidedTourPolicy.lessons.size}", color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                                Text(copy.title, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            }
                            TextButton(onClick = onSkip, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)) {
                                Text("EXIT GUIDE", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(copy.body, color = ProjectorIvory.copy(alpha = .76f), fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(9.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            copy.secondary?.let { label ->
                                OutlinedButton(
                                    onClick = onSecondary,
                                    modifier = Modifier.weight(.78f).heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    border = BorderStroke(1.dp, CinemaLine),
                                ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black) }
                            }
                            Button(
                                onClick = onPrimary,
                                modifier = Modifier.weight(1f).height(41.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                                shape = RoundedCornerShape(13.dp),
                            ) {
                                Text(copy.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
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

@Composable
private fun V145GuideSpeechBubble(text: String) {
    Surface(
        modifier = Modifier.widthIn(min = 150.dp, max = 255.dp),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurfaceRaised,
        border = BorderStroke(1.dp, CinemaLine),
        shadowElevation = 10.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            color = ProjectorIvory,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun guidedCopy(step: CreatorGuidedTourStep, hasProjects: Boolean): GuidedCoachCopy = when (step) {
    CreatorGuidedTourStep.TODAY -> GuidedCoachCopy(
        "HOME",
        "Your command center",
        "Home shows the next thing worth your attention.",
        "See what's important today, all in one place.",
        "NEXT",
        icon = Icons.Outlined.Home,
        pose = FrameGuidePose.PRESENT,
    )
    CreatorGuidedTourStep.IDEAS -> GuidedCoachCopy(
        "IDEA VAULT",
        "Capture before it disappears",
        "Use + for a quick thought. Organize it later.",
        "Save ideas now. Turn them into projects later.",
        "NEXT",
        "TRY CAPTURE",
        Icons.Outlined.Lightbulb,
        FrameGuidePose.POINT,
    )
    CreatorGuidedTourStep.PROJECT -> v149GuidedProjectReminderCopy(hasProjects).let { projectCopy ->
        GuidedCoachCopy(
            "PROJECT + REMINDERS",
            projectCopy.title,
            projectCopy.body,
            projectCopy.speech,
            "NEXT",
            if (hasProjects) "OPEN PROJECT" else "CREATE PROJECT",
            Icons.Outlined.NotificationsActive,
            FrameGuidePose.WALK,
        )
    }
    CreatorGuidedTourStep.WORKSPACE -> GuidedCoachCopy(
        "WORKSPACE",
        "Move work stage by stage",
        "Your project tools and progress stay together here.",
        "See your next project step and keep moving.",
        "SHOW INSIGHTS",
        icon = Icons.Outlined.MovieEdit,
        pose = FrameGuidePose.WAVE,
    )
    CreatorGuidedTourStep.INSIGHTS -> GuidedCoachCopy(
        "INSIGHTS",
        "Understand your progress",
        "Insights helps you learn. Quick Actions is always one tap away.",
        "Connect YouTube to see what's working.",
        "FINISH TOUR",
        "QUICK ACTIONS",
        icon = Icons.Outlined.Insights,
        pose = FrameGuidePose.THINK,
    )
    CreatorGuidedTourStep.CONTROL -> GuidedCoachCopy(
        "CONTROL",
        "Fast actions live here",
        "Create, capture and manage the system from one place.",
        "Quickly start a project or save an idea.",
        "FINISH TOUR",
        icon = Icons.Outlined.GridView,
        pose = FrameGuidePose.CELEBRATE,
    )
}
