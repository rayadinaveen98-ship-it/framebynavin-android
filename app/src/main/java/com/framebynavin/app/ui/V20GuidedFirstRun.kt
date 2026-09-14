package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Contextual coach mark shown on top of the real product. It never replaces Today, Ideas,
 * Workspace or Insights with tutorial mockups, so the creator learns the actual interaction model.
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
    val top = step == CreatorGuidedTourStep.WORKSPACE || step == CreatorGuidedTourStep.CONTROL

    Box(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                .then(if (top) Modifier.statusBarsPadding().padding(top = 14.dp) else Modifier.navigationBarsPadding().padding(bottom = 92.dp))
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(22.dp),
            color = CinemaSurfaceRaised,
            border = BorderStroke(1.dp, MutedGold.copy(alpha = .42f)),
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = RecRed.copy(alpha = .12f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(copy.icon, null, tint = RecRed, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${copy.eyebrow} · ${step.ordinal + 1}/6",
                            color = MutedGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .8.sp,
                        )
                        Text(copy.title, color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                    TextButton(onClick = onSkip) {
                        Text("SKIP TOUR", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(copy.body, color = MutedText, fontSize = 12.5.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(copy.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }

                copy.secondary?.let { label ->
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                        Text(label, color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun guidedCopy(step: CreatorGuidedTourStep, hasProjects: Boolean): GuidedCoachCopy = when (step) {
    CreatorGuidedTourStep.TODAY -> GuidedCoachCopy(
        eyebrow = "TODAY",
        title = "Start with what matters now",
        body = "Today is your creator command center. It keeps active work, deadlines and the next useful action in one place instead of making you hunt through the app.",
        primary = "SHOW ME IDEAS",
        icon = Icons.Outlined.Home,
    )
    CreatorGuidedTourStep.IDEAS -> GuidedCoachCopy(
        eyebrow = "IDEA VAULT",
        title = "Keep ideas light until they are ready",
        body = "Ideas can stay rough here. Capture a thought without turning it into a full project, then promote it only when you actually want to make it.",
        primary = "CAPTURE AN IDEA",
        secondary = "CONTINUE WITHOUT CAPTURING",
        icon = Icons.Outlined.Lightbulb,
    )
    CreatorGuidedTourStep.PROJECT -> GuidedCoachCopy(
        eyebrow = "PROJECT",
        title = if (hasProjects) "Open a real project" else "Build your first project",
        body = if (hasProjects)
            "Projects turn creator intent into an actual workflow. We'll open one of your projects so the next step teaches the real workspace."
        else
            "The New Project wizard asks one decision at a time and uses your creator setup to recommend sensible defaults. Complete it normally — this is your real project, not demo data.",
        primary = if (hasProjects) "OPEN A PROJECT" else "CREATE FIRST PROJECT",
        icon = Icons.Outlined.AddCircleOutline,
    )
    CreatorGuidedTourStep.WORKSPACE -> GuidedCoachCopy(
        eyebrow = "WORKSPACE",
        title = "This is where the project moves",
        body = "Your project opens directly into its workspace. Use its stages and project tools to move from idea to finished work without losing context.",
        primary = "SHOW ME INSIGHTS",
        icon = Icons.Outlined.MovieEdit,
    )
    CreatorGuidedTourStep.INSIGHTS -> GuidedCoachCopy(
        eyebrow = "INSIGHTS",
        title = "This is the brain of FrameByNavin",
        body = "Insights tells you what is happening, why a signal matters and what deserves investigation next. Summary cards stay clean; tap into them when you want the evidence.",
        primary = "NEXT: CONTROL",
        icon = Icons.Outlined.Insights,
    )
    CreatorGuidedTourStep.CONTROL -> GuidedCoachCopy(
        eyebrow = "CONTROL",
        title = "Fast actions live one tap away",
        body = "Control is your toolbox for quick capture, new projects, planning, reminders and settings. Open it now. You can replay this tour anytime from Settings.",
        primary = "OPEN CONTROL",
        icon = Icons.Outlined.GridView,
    )
}
