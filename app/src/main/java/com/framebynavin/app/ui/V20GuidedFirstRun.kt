package com.framebynavin.app.ui

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
)

/**
 * Lightweight contextual coach mark. The real product remains visible beneath a very light scrim,
 * and the coach itself is translucent so the tour feels like guidance, not another screen.
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

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = .13f)),
    ) {
        Surface(
            modifier = Modifier
                .align(if (placeAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                .then(
                    if (placeAtTop) Modifier.statusBarsPadding().padding(top = 12.dp)
                    else Modifier.navigationBarsPadding().padding(bottom = 92.dp)
                )
                .padding(horizontal = 14.dp)
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xD018181B),
            border = BorderStroke(1.dp, ProjectorIvory.copy(alpha = .14f)),
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                // Small progress rail instead of a large tutorial header.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(6) { index ->
                        Box(
                            Modifier.weight(1f).height(if (index == step.ordinal) 3.dp else 2.dp)
                                .background(
                                    if (index <= step.ordinal) RecRed else ProjectorIvory.copy(alpha = .12f),
                                    RoundedCornerShape(100.dp),
                                )
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(11.dp),
                        color = RecRed.copy(alpha = .13f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(copy.icon, null, tint = RecRed, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${copy.eyebrow}  ·  ${step.ordinal + 1}/6",
                            color = MutedGold,
                            fontSize = 7.8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .7.sp,
                        )
                        Text(copy.title, color = ProjectorIvory, fontSize = 14.5.sp, fontWeight = FontWeight.Black)
                    }
                    TextButton(
                        onClick = onSkip,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Text("SKIP", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(copy.body, color = ProjectorIvory.copy(alpha = .72f), fontSize = 10.5.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    copy.secondary?.let { label ->
                        OutlinedButton(
                            onClick = onSecondary,
                            modifier = Modifier.weight(.82f).height(42.dp),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, ProjectorIvory.copy(alpha = .16f)),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(label, color = ProjectorIvory.copy(alpha = .72f), fontSize = 8.2.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Button(
                        onClick = onPrimary,
                        modifier = Modifier.weight(1f).height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(13.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Text(copy.primary, fontSize = 8.8.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

private fun guidedCopy(step: CreatorGuidedTourStep, hasProjects: Boolean): GuidedCoachCopy = when (step) {
    CreatorGuidedTourStep.TODAY -> GuidedCoachCopy(
        eyebrow = "TODAY",
        title = "Your command center",
        body = "Active work, deadlines and the next useful action stay here. The real screen remains usable underneath this guide.",
        primary = "IDEAS",
        icon = Icons.Outlined.Home,
    )
    CreatorGuidedTourStep.IDEAS -> GuidedCoachCopy(
        eyebrow = "IDEA VAULT",
        title = "Capture first. Decide later.",
        body = "Keep rough thoughts here without turning every idea into a project.",
        primary = "CAPTURE IDEA",
        secondary = "SKIP · NEXT",
        icon = Icons.Outlined.Lightbulb,
    )
    CreatorGuidedTourStep.PROJECT -> GuidedCoachCopy(
        eyebrow = "PROJECT",
        title = if (hasProjects) "Open a real project" else "Build your first project",
        body = if (hasProjects)
            "Open one of your real projects and we’ll continue inside its workspace."
        else
            "Create one now to learn Workspace. After Create & Open, the tour resumes there automatically — or continue without making anything.",
        primary = if (hasProjects) "OPEN PROJECT" else "CREATE PROJECT",
        secondary = if (hasProjects) null else "NOT NOW · NEXT",
        icon = Icons.Outlined.AddCircleOutline,
    )
    CreatorGuidedTourStep.WORKSPACE -> GuidedCoachCopy(
        eyebrow = "WORKSPACE",
        title = "Move the project here",
        body = "Stages and project tools live together, so you can move from idea to finished work without losing context.",
        primary = "INSIGHTS",
        icon = Icons.Outlined.MovieEdit,
    )
    CreatorGuidedTourStep.INSIGHTS -> GuidedCoachCopy(
        eyebrow = "INSIGHTS",
        title = "Your creator brain",
        body = "See what changed, why it matters and tap deeper only when you want the evidence.",
        primary = "CONTROL",
        icon = Icons.Outlined.Insights,
    )
    CreatorGuidedTourStep.CONTROL -> GuidedCoachCopy(
        eyebrow = "CONTROL",
        title = "Fast actions, one tap away",
        body = "Capture, create, plan, reminders and settings live here. You can replay this guide anytime from Settings.",
        primary = "OPEN CONTROL",
        icon = Icons.Outlined.GridView,
    )
}
