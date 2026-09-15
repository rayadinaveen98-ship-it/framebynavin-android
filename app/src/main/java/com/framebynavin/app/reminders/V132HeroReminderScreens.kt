package com.framebynavin.app.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.ui.V132HeroOrb
import com.framebynavin.app.ui.V132HeroOrbState
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val HeroPink = Color(0xFFFF3C8E)
private val HeroCoral = Color(0xFFFF684D)
private val HeroOrange = Color(0xFFFFA154)
private val HeroCyan = Color(0xFF55E7F1)
private val HeroViolet = Color(0xFF8C5DFF)
private val HeroGlass = Color(0xFF0D0D14)

@Composable
internal fun V132AlarmHeroScreen(
    title: String,
    dueLabel: String,
    stageLabel: String,
    notes: String,
    snoozeMinutes: Int,
    stageCheckIn: Boolean,
    onStageDone: () -> Unit,
    onDismiss: () -> Unit,
    onWorking: () -> Unit,
    onSnooze: () -> Unit,
    onPause: () -> Unit,
    onReschedule: () -> Unit,
) {
    BackHandler(enabled = true) { }
    HeroReminderShell(mode = HeroReminderMode.ALARM) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (stageCheckIn) "CINE PULSE · STAGE CHECK-IN" else "CINE PULSE · CREATIVE ALARM",
                color = HeroOrange,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()),
                color = ProjectorIvory,
                fontSize = 58.sp,
                lineHeight = 62.sp,
                fontWeight = FontWeight.Light,
            )
            Text(
                SimpleDateFormat("a", Locale.getDefault()).format(Date()),
                color = MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )

            V132HeroOrb(
                state = V132HeroOrbState.ALARM,
                signal = .52f,
                modifier = Modifier.size(220.dp),
            )

            Text(
                if (stageCheckIn) "Time to check your progress." else "Your creative time is here.",
                color = MutedGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                title,
                color = ProjectorIvory,
                fontSize = 27.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                HeroPill(dueLabel, HeroPink)
                if (stageCheckIn) HeroPill(stageLabel, HeroCyan)
            }

            if (notes.isNotBlank()) {
                Spacer(Modifier.height(13.dp))
                HeroGlassCard {
                    Text(
                        notes,
                        color = ProjectorIvory.copy(alpha = .76f),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            if (stageCheckIn) {
                HeroPrimaryButton(
                    label = "STAGE DONE",
                    onClick = onStageDone,
                    container = SuccessGreen,
                    icon = { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp)) },
                )
                Spacer(Modifier.height(8.dp))
            }
            HeroPrimaryButton(
                label = if (stageCheckIn) "KEEP WORKING" else "START NOW",
                onClick = onWorking,
                container = HeroCoral,
                icon = { Icon(Icons.Outlined.Work, null, modifier = Modifier.size(18.dp)) },
            )
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroSecondaryButton(
                    label = "SNOOZE ${snoozeMinutes}M",
                    modifier = Modifier.weight(1f),
                    onClick = onSnooze,
                    icon = { Icon(Icons.Outlined.Snooze, null, modifier = Modifier.size(16.dp)) },
                )
                HeroSecondaryButton(
                    label = if (stageCheckIn) "PAUSE 2H" else "RESCHEDULE",
                    modifier = Modifier.weight(1f),
                    onClick = if (stageCheckIn) onPause else onReschedule,
                    icon = {
                        Icon(
                            if (stageCheckIn) Icons.Outlined.PauseCircle else Icons.Outlined.Schedule,
                            null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
            if (stageCheckIn) {
                TextButton(onClick = onReschedule) {
                    Text("CHOOSE ANOTHER TIME", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onDismiss) {
                Text(
                    if (stageCheckIn) "DISMISS FOR NOW" else "DISMISS ALARM",
                    color = MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun V132VoiceHeroScreen(
    task: CreatorTask,
    snoozeMinutes: Int,
    stageCheckIn: Boolean,
    onStageDone: () -> Unit,
    onWorking: () -> Unit,
    onRepeat: () -> Unit,
    onSnooze: () -> Unit,
    onPause: () -> Unit,
    onReschedule: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = true) { }
    HeroReminderShell(mode = HeroReminderMode.VOICE) {
        Column(
            Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 17.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (stageCheckIn) "CINE PULSE · VOICE CHECK-IN" else "CINE PULSE · VOICE REMINDER",
                color = HeroCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.55.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("A reminder with presence.", color = MutedText, fontSize = 9.5.sp)

            V132HeroOrb(
                state = V132HeroOrbState.SPEAKING,
                signal = .42f,
                modifier = Modifier.size(245.dp),
            )

            Text(
                task.title,
                color = ProjectorIvory,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            HeroPill(
                if (stageCheckIn) "${CreatorWorkflowEngine.currentStage(task).label} · CHECK-IN" else task.dueLabel,
                if (stageCheckIn) HeroCyan else HeroPink,
            )

            if (task.notes.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                HeroGlassCard {
                    Text(
                        task.notes,
                        color = ProjectorIvory.copy(alpha = .76f),
                        fontSize = 10.5.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            if (stageCheckIn) {
                HeroPrimaryButton(
                    label = "STAGE DONE",
                    onClick = onStageDone,
                    container = SuccessGreen,
                    icon = { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(18.dp)) },
                )
                Spacer(Modifier.height(8.dp))
            }
            HeroPrimaryButton(
                label = "START NOW",
                onClick = onWorking,
                container = HeroCoral,
                icon = { Icon(Icons.Outlined.Work, null, modifier = Modifier.size(18.dp)) },
            )
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroSecondaryButton(
                    label = "REPLAY VOICE",
                    modifier = Modifier.weight(1f),
                    onClick = onRepeat,
                    icon = { Icon(Icons.Outlined.Replay, null, modifier = Modifier.size(16.dp)) },
                )
                HeroSecondaryButton(
                    label = "SNOOZE ${snoozeMinutes}M",
                    modifier = Modifier.weight(1f),
                    onClick = onSnooze,
                    icon = { Icon(Icons.Outlined.Snooze, null, modifier = Modifier.size(16.dp)) },
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stageCheckIn) {
                    HeroSecondaryButton(
                        label = "PAUSE 2H",
                        modifier = Modifier.weight(1f),
                        onClick = onPause,
                        icon = { Icon(Icons.Outlined.PauseCircle, null, modifier = Modifier.size(16.dp)) },
                    )
                }
                HeroSecondaryButton(
                    label = "CHOOSE TIME",
                    modifier = Modifier.weight(1f),
                    onClick = onReschedule,
                    icon = { Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp)) },
                )
            }
            TextButton(onClick = onDismiss) {
                Text(
                    if (stageCheckIn) "DISMISS FOR NOW" else "DISMISS REMINDER",
                    color = MutedText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private enum class HeroReminderMode { ALARM, VOICE }

@Composable
private fun HeroReminderShell(mode: HeroReminderMode, content: @Composable BoxScope.() -> Unit) {
    val motion = rememberInfiniteTransition(label = "heroReminderAtmosphere")
    val drift by motion.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(14_700, easing = LinearEasing)),
        label = "heroReminderDrift",
    )
    val breathe by motion.animateFloat(
        .82f,
        1f,
        infiniteRepeatable(tween(4_100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroReminderGlow",
    )

    Box(Modifier.fillMaxSize().background(CinemaBlack)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val accent = if (mode == HeroReminderMode.ALARM) HeroCoral else HeroCyan
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = .115f * breathe),
                        HeroViolet.copy(alpha = .075f * breathe),
                        Color.Transparent,
                    ),
                    center = Offset(w * .50f, h * .36f),
                    radius = w * 1.08f,
                ),
            )
            val phase = drift * 360f
            repeat(3) { index ->
                val radius = w * (.55f + index * .16f)
                drawArc(
                    color = listOf(HeroPink, HeroCyan, HeroOrange)[index].copy(alpha = .10f + index * .015f),
                    startAngle = phase * (if (index % 2 == 0) 1f else -1f) + index * 94f,
                    sweepAngle = 70f + index * 21f,
                    useCenter = false,
                    topLeft = Offset(w / 2f - radius, h * .34f - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = w * (.010f + index * .003f), cap = StrokeCap.Round),
                )
            }
            repeat(20) { index ->
                val theta = Math.toRadians((phase * .17f + index * 137.5f).toDouble())
                val rx = w * (.15f + (index % 7) * .055f)
                val ry = h * (.12f + (index % 5) * .048f)
                val p = Offset(
                    w * .5f + cos(theta).toFloat() * rx,
                    h * .34f + sin(theta * 1.19).toFloat() * ry,
                )
                drawCircle(
                    color = if (index % 3 == 0) HeroOrange.copy(alpha = .28f) else ProjectorIvory.copy(alpha = .14f),
                    radius = 1.2f + (index % 3) * .5f,
                    center = p,
                )
            }
        }
        content()
    }
}

@Composable
private fun HeroGlassCard(content: @Composable BoxScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = HeroGlass.copy(alpha = .88f),
        border = BorderStroke(1.dp, HeroCyan.copy(alpha = .17f)),
        content = { Box(Modifier.fillMaxWidth(), content = content) },
    )
}

@Composable
private fun HeroPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = color.copy(alpha = .10f),
        border = BorderStroke(1.dp, color.copy(alpha = .32f)),
    ) {
        Text(
            label.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = color,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = .6.sp,
        )
    }
}

@Composable
private fun HeroPrimaryButton(
    label: String,
    onClick: () -> Unit,
    container: Color,
    icon: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(55.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container),
        shape = RoundedCornerShape(18.dp),
    ) {
        icon()
        Spacer(Modifier.width(7.dp))
        Text(label, fontWeight = FontWeight.Black, letterSpacing = .25.sp)
    }
}

@Composable
private fun HeroSecondaryButton(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        border = BorderStroke(1.dp, HeroCyan.copy(alpha = .20f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ProjectorIvory),
        shape = RoundedCornerShape(16.dp),
    ) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 8.3.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
