package com.framebynavin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.SuccessGreen

/**
 * Current reusable project-workflow detail panel.
 *
 * This used to live inside the removed V07 Studio screen. The current app still uses the
 * detail panel from its active Studio route, so it now lives independently rather than
 * keeping an entire obsolete app generation compiled just for one composable.
 */
@Composable
internal fun V071WorkflowInlineContent(
    task: CreatorTask,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    onFocus: () -> Unit,
) {
    val template = CreatorWorkflowEngine.templateFor(task)
    val currentIndex = CreatorWorkflowEngine.stageIndex(task)
    val progress = CreatorWorkflowEngine.progress(task)
    val currentStep = CreatorWorkflowEngine.currentStage(task)
    val done = task.status == TaskStatus.DONE

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(20.dp),
        color = CinemaSurfaceRaised,
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        template.label.uppercase(),
                        color = RecRed,
                        fontSize = 8.7.sp,
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(task.dueLabel, color = MutedGold, fontSize = 10.3.sp)
                }
                Text(
                    if (done) "COMPLETE" else "STEP ${currentIndex + 1} OF ${template.stages.size}",
                    color = if (done) SuccessGreen else MutedText,
                    fontSize = 8.7.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(11.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = if (done) SuccessGreen else RecRed,
                trackColor = Color(0xFF303030),
            )
            Spacer(Modifier.height(17.dp))

            template.stages.forEachIndexed { index, step ->
                val completed = done || index < currentIndex
                val current = !done && index == currentIndex
                val upcoming = !completed && !current

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(29.dp).background(
                            when {
                                completed -> SuccessGreen.copy(alpha = .16f)
                                current -> RecRed.copy(alpha = .18f)
                                else -> Color(0xFF141414)
                            },
                            CircleShape,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when {
                                completed -> Icons.Outlined.Check
                                current -> Icons.Outlined.RadioButtonChecked
                                else -> Icons.Outlined.Lock
                            },
                            contentDescription = null,
                            tint = when {
                                completed -> SuccessGreen
                                current -> RecRed
                                else -> Color(0xFF5C5852)
                            },
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            step.label,
                            color = if (upcoming) Color(0xFF77726C) else ProjectorIvory,
                            fontSize = 12.8.sp,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            when {
                                completed -> "Completed"
                                current -> step.action
                                else -> "Upcoming"
                            },
                            color = when {
                                completed -> SuccessGreen.copy(alpha = .75f)
                                current -> MutedGold
                                else -> Color(0xFF5F5B56)
                            },
                            fontSize = 9.3.sp,
                        )
                    }
                }

                if (index < template.stages.lastIndex) {
                    Box(
                        Modifier
                            .padding(start = 14.dp)
                            .width(1.dp)
                            .height(15.dp)
                            .background(if (completed) SuccessGreen.copy(alpha = .25f) else CinemaLine)
                    )
                }
            }

            if (!done) {
                Spacer(Modifier.height(18.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFF10100F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text("NEXT", color = MutedText, fontSize = 8.3.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            currentStep.action,
                            color = ProjectorIvory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(11.dp))
                Button(
                    onClick = onFocus,
                    modifier = Modifier.fillMaxWidth().height(49.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("WORK ON · ${currentStep.label.uppercase()}", fontWeight = FontWeight.Black)
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Previous step",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("BACK", color = ProjectorIvory, fontSize = 9.3.sp)
                    }
                    Button(
                        onClick = onAdvance,
                        modifier = Modifier.weight(1.45f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272727)),
                    ) {
                        Text(
                            if (currentIndex == template.stages.lastIndex) "MARK PUBLISHED" else "MARK STEP DONE",
                            color = ProjectorIvory,
                            fontSize = 9.3.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(13.dp))
                Text(
                    "✓ Project published",
                    color = SuccessGreen,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
