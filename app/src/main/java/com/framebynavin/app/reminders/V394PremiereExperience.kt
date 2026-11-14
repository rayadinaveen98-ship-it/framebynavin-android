package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * V394: A complete opt-in reminder PRESENTATION, not another V391 background.
 * Delivery, audio, notification, lock-screen security and response callbacks
 * remain owned by the existing Creator/Personal controllers.
 *
 * A challenge example is deliberately preview-only. Android does not grant
 * ordinary challenge voice notifications an automatic full-screen launch.
 */
internal enum class V394PremiereMode { CREATOR, PERSONAL, CHALLENGE }

internal object V394PremierePresentationPolicy {
    fun creator(enabled: Boolean): Boolean = enabled
    fun personal(enabled: Boolean, locked: Boolean, validReminder: Boolean): Boolean =
        enabled && !locked && validReminder
    fun automaticChallengeScreen(): Boolean = false
}

internal object V394PremierePrefs {
    private const val FILE = "backlot_premiere_experience_v394"
    private var app: Context? = null
    var voice by mutableStateOf(false)
        private set
    var alarm by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        if (app != null) return
        app = context.applicationContext
        val saved = app!!.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        voice = saved.getBoolean("voice", false)
        alarm = saved.getBoolean("alarm", false)
    }

    fun enabled(kind: ReminderCheckInKind): Boolean = when (kind) {
        ReminderCheckInKind.VOICE -> voice
        ReminderCheckInKind.ALARM -> alarm
    }

    fun set(kind: ReminderCheckInKind, enabled: Boolean) {
        val context = app ?: return
        when (kind) {
            ReminderCheckInKind.VOICE -> voice = enabled
            ReminderCheckInKind.ALARM -> alarm = enabled
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(if (kind == ReminderCheckInKind.VOICE) "voice" else "alarm", enabled)
            .apply()
    }
}

private val PremiereGold = Color(0xFFF2C991)
private val PremiereIvory = Color(0xFFFFF3E6)
private val PremiereMuted = Color(0xFFBEAA9A)
private val PremiereBlack = Color(0xFF08080C)

private fun V394PremiereMode.accent(): Color = when (this) {
    V394PremiereMode.CREATOR -> PremiereGold
    V394PremiereMode.PERSONAL -> Color(0xFFFFC2AF)
    V394PremiereMode.CHALLENGE -> Color(0xFFC9B7FF)
}

private fun V394PremiereMode.eyebrow(): String = when (this) {
    V394PremiereMode.CREATOR -> "A SCENE WORTH MAKING"
    V394PremiereMode.PERSONAL -> "A MOMENT WORTH KEEPING"
    V394PremiereMode.CHALLENGE -> "YOUR STORY CONTINUES"
}

private fun V394PremiereMode.headline(): String = when (this) {
    V394PremiereMode.CREATOR -> "Lights. Camera. You."
    V394PremiereMode.PERSONAL -> "The moment is yours."
    V394PremiereMode.CHALLENGE -> "One day. One promise."
}

/** Safe, immediate actions and scrollable details; the artwork never handles touch. */
@Composable
internal fun V394PremiereReminderScreen(
    mode: V394PremiereMode,
    kind: ReminderCheckInKind,
    title: String,
    timeLabel: String,
    contextLabel: String,
    supportingText: String,
    primaryLabel: String,
    snoozeLabel: String,
    onPrimary: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onWorking: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    onReschedule: (() -> Unit)? = null,
    onReplay: (() -> Unit)? = null,
    onClosePreview: (() -> Unit)? = null,
    error: String? = null,
    busy: Boolean = false,
) {
    val context = LocalContext.current
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val lowPower = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    val compact = LocalConfiguration.current.screenHeightDp < 720
    val accent = mode.accent()
    var optionsOpen by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(motionEnabled) { entered = true }
    val reveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(if (motionEnabled && !lowPower) 850 else 0),
        label = "premiere-entrance",
    )

    Box(
        Modifier.fillMaxSize().background(PremiereBlack)
            .testTag("v394-premiere-screen"),
    ) {
        V394PremiereAtmosphere(mode, motionEnabled, lowPower, Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 23.dp, vertical = if (compact) 10.dp else 19.dp)
                .testTag("v394-premiere-controls"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("BACKLOT", color = PremiereIvory, fontSize = 12.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 3.1.sp)
                Text("P R E M I E R E  /  0 1", color = accent,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(if (compact) 14.dp else 24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(mode.name + " / " + kind.name, color = PremiereMuted,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.6.sp)
                Text(timeLabel, color = PremiereIvory, fontSize = 11.sp,
                    fontWeight = FontWeight.Medium)
            }
            Box(
                Modifier.fillMaxWidth()
                    .height(if (compact) 178.dp else 238.dp)
                    .graphicsLayer {
                        alpha = .6f + reveal * .4f
                        translationY = (1f - reveal) * 18.dp.toPx()
                    },
                contentAlignment = Alignment.Center,
            ) {
                V394PremiereHero(mode, kind, motionEnabled, lowPower,
                    Modifier.fillMaxSize())
                if (mode == V394PremiereMode.CHALLENGE) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("08", color = PremiereIvory, fontSize = 44.sp,
                            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal)
                        Text("OF 21 DAYS", color = accent, fontSize = 9.sp,
                            letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(mode.eyebrow(), color = accent, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
            Spacer(Modifier.height(9.dp))
            Text(mode.headline(), color = PremiereIvory,
                fontFamily = FontFamily.Serif, fontSize = if (compact) 27.sp else 32.sp,
                lineHeight = if (compact) 31.sp else 38.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text(title, color = PremiereIvory, fontSize = if (compact) 19.sp else 22.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = if (compact) 25.sp else 29.sp,
                textAlign = TextAlign.Center, maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().testTag(
                    if (mode == V394PremiereMode.PERSONAL)
                        "v2233-personal-alert-title" else "v200-reminder-title"))
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp)
                .background(accent.copy(alpha = .4f)))
            if (contextLabel.isNotBlank()) {
                Text(contextLabel, color = PremiereMuted, fontSize = 11.sp,
                    lineHeight = 17.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            }
            if (supportingText.isNotBlank()) {
                Text(supportingText, color = PremiereMuted, fontSize = 12.sp,
                    lineHeight = 18.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 13.dp))
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onPrimary, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .testTag(if (mode == V394PremiereMode.PERSONAL)
                        "v2233-personal-done" else "v200-primary-reminder-action"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent, contentColor = PremiereBlack),
            ) {
                Icon(Icons.Outlined.Check, null, Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text(primaryLabel, fontSize = 13.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(
                    onClick = onSnooze, enabled = !busy,
                    modifier = Modifier.weight(1f).heightIn(min = 51.dp)
                        .testTag(if (mode == V394PremiereMode.PERSONAL)
                            "v2233-personal-snooze" else "v394-premiere-snooze"),
                    border = BorderStroke(1.dp, accent.copy(alpha = .75f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Snooze, null, Modifier.size(17.dp),
                        tint = PremiereIvory)
                    Spacer(Modifier.width(5.dp))
                    Text(snoozeLabel, color = PremiereIvory,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1)
                }
                OutlinedButton(
                    onClick = { optionsOpen = !optionsOpen },
                    modifier = Modifier.heightIn(min = 51.dp),
                    border = BorderStroke(1.dp, Color(0xFF625246)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (optionsOpen) "LESS" else "OPTIONS",
                        color = PremiereIvory, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            // Personal dismiss remains immediately discoverable, as in V2233.
            if (mode == V394PremiereMode.PERSONAL) {
                TextButton(onClick = onDismiss, enabled = !busy,
                    modifier = Modifier.testTag("v2233-personal-dismiss")) {
                    Text("DISMISS WITHOUT COMPLETING", color = PremiereMuted,
                        fontSize = 11.sp)
                }
            }
            AnimatedVisibility(visible = optionsOpen) {
                Column(Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (onWorking != null) V394Option("I'M WORKING", onWorking, busy)
                    if (onPause != null) V394Option("PAUSE CHECK-INS", onPause, busy)
                    if (onReschedule != null) V394Option("RESCHEDULE", onReschedule, busy)
                    if (onReplay != null) V394Option("REPLAY VOICE", onReplay, busy)
                    if (mode != V394PremiereMode.PERSONAL) {
                        V394Option("DISMISS FOR NOW", onDismiss, busy)
                    }
                }
            }
            if (error != null) {
                Text(error, color = Color(0xFFFF8F91), fontSize = 12.sp,
                    modifier = Modifier.padding(top = 9.dp))
            }
            if (onClosePreview != null) {
                Spacer(Modifier.height(9.dp))
                TextButton(onClick = onClosePreview,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.Close, null, Modifier.size(16.dp),
                        tint = PremiereMuted)
                    Spacer(Modifier.width(6.dp))
                    Text("EXIT PREVIEW", color = PremiereMuted,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun V394Option(label: String, onClick: () -> Unit, busy: Boolean) {
    TextButton(onClick = onClick, enabled = !busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, color = PremiereIvory, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = .8.sp)
    }
}

/** Original, bounded native 2D stage artwork: no remote assets, GL or shaders. */
@Composable
private fun V394PremiereAtmosphere(
    mode: V394PremiereMode, motionEnabled: Boolean,
    lowPower: Boolean, modifier: Modifier = Modifier,
) {
    val t = V336MotionTime(motionEnabled, lowPower)
    val accent = mode.accent()
    Canvas(modifier.testTag("v394-premiere-atmosphere")) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(
            Color(0xFF08080C), Color(0xFF171017), Color(0xFF09080E))))
        val halo = Offset(w * .5f, h * .31f)
        drawCircle(Brush.radialGradient(
            listOf(accent.copy(alpha = .24f), accent.copy(alpha = .04f),
                Color.Transparent), center = halo, radius = w * .82f),
            radius = w * .82f, center = halo)
        val beamShift = if (motionEnabled && !lowPower)
            sin(t * .17f).toFloat() * w * .035f else 0f
        val beam = Path().apply {
            moveTo(w * .5f + beamShift, h * .035f)
            lineTo(w * .90f + beamShift, h * .66f)
            lineTo(w * .12f + beamShift, h * .66f)
            close()
        }
        drawPath(beam, Brush.verticalGradient(listOf(
            accent.copy(alpha = .065f), accent.copy(alpha = .008f))))
        // Film-gate edge perforations: visual frame, not UI chrome.
        val edge = 14.dp.toPx()
        drawLine(accent.copy(alpha = .18f), Offset(edge, h * .05f),
            Offset(edge, h * .95f), strokeWidth = 1.dp.toPx())
        drawLine(accent.copy(alpha = .18f), Offset(w - edge, h * .05f),
            Offset(w - edge, h * .95f), strokeWidth = 1.dp.toPx())
        repeat(13) { i ->
            val y = h * (.085f + i * .067f)
            for (x in listOf(edge * .48f, w - edge * .94f)) {
                drawRoundRect(accent.copy(alpha = .16f), Offset(x, y),
                    Size(edge * .46f, 9.dp.toPx()),
                    androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
        }
        if (!lowPower) repeat(26) { i ->
            val x = (((i * 71 + 19) % 257) / 257f) * w
            val y = (((i * 97 + 11) % 263) / 263f) * h
            val shimmer = if (motionEnabled)
                (.38f + .28f * sin(t * .35f + i * 1.3f).toFloat()) else .4f
            drawCircle(accent.copy(alpha = shimmer.coerceIn(0f, .7f)),
                radius = (if (i % 6 == 0) 1.2f else .65f).dp.toPx(),
                center = Offset(x, y))
        }
    }
}

@Composable
private fun V394PremiereHero(
    mode: V394PremiereMode, kind: ReminderCheckInKind,
    motionEnabled: Boolean, lowPower: Boolean, modifier: Modifier,
) {
    val t = V336MotionTime(motionEnabled, lowPower)
    val accent = mode.accent()
    Canvas(modifier.testTag("v394-premiere-hero")) {
        val w = size.width
        val h = size.height
        val c = Offset(w * .5f, h * .51f)
        val r = min(w * .32f, h * .42f)
        drawCircle(Brush.radialGradient(listOf(
            accent.copy(alpha = .19f), Color.Transparent),
            center = c, radius = r * 1.55f), r * 1.55f, c)
        // Moving projector halos, with static geometry in battery saver.
        repeat(3) { i ->
            val rotation = if (motionEnabled && !lowPower) t * (4f + i * 2f) else 0f
            drawArc(accent.copy(alpha = .25f - i * .045f),
                startAngle = -70f + rotation + i * 74f,
                sweepAngle = 115f - i * 13f,
                useCenter = false,
                topLeft = Offset(c.x - r * (1.03f + i * .15f),
                    c.y - r * (1.03f + i * .15f)),
                size = Size(r * 2f * (1.03f + i * .15f),
                    r * 2f * (1.03f + i * .15f)),
                style = Stroke(width = (1.5f - i * .2f).dp.toPx(),
                    cap = StrokeCap.Round))
        }
        // Frame corners are shared; the centre has a separate authored motif.
        val halfX = r * 1.42f
        val halfY = r * 1.13f
        val line = 1.4.dp.toPx()
        for (sx in listOf(-1f, 1f)) for (sy in listOf(-1f, 1f)) {
            val x = c.x + sx * halfX
            val y = c.y + sy * halfY
            drawLine(accent.copy(alpha = .55f), Offset(x, y),
                Offset(x - sx * r * .22f, y), line)
            drawLine(accent.copy(alpha = .55f), Offset(x, y),
                Offset(x, y - sy * r * .22f), line)
        }
        when (mode) {
            V394PremiereMode.CREATOR -> v394Aperture(c, r * .80f, accent, t,
                motionEnabled && !lowPower)
            V394PremiereMode.PERSONAL -> v394Sunrise(c, r * .84f, accent, t,
                motionEnabled && !lowPower)
            V394PremiereMode.CHALLENGE -> v394Progress(c, r * .84f, accent)
        }
        // An audio/clock identity cue, independent of the full scene.
        val cue = if (kind == ReminderCheckInKind.VOICE) 5 else 3
        repeat(cue) { i ->
            val x = c.x + (i - (cue - 1) / 2f) * 10.dp.toPx()
            val amplitude = if (motionEnabled && !lowPower)
                (.45f + .55f * kotlin.math.abs(sin(t * 1.8f + i))) else .65f
            val len = (3f + 8f * amplitude).dp.toPx()
            drawLine(accent.copy(alpha = .7f), Offset(x, h * .94f - len / 2f),
                Offset(x, h * .94f + len / 2f), 1.5.dp.toPx(),
                cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.v394Aperture(
    center: Offset, radius: Float, accent: Color, t: Float, animate: Boolean,
) {
    drawCircle(Brush.radialGradient(listOf(
        Color(0xFF5D3E32), Color(0xFF0A0A10)),
        center = center, radius = radius), radius, center)
    drawCircle(accent.copy(alpha = .75f), radius, center,
        style = Stroke(width = 2.dp.toPx()))
    drawCircle(accent.copy(alpha = .25f), radius * .82f, center,
        style = Stroke(width = 1.dp.toPx()))
    val spin = if (animate) sin(t * .27f).toFloat() * .12f else 0f
    repeat(6) { i ->
        val a = (i * PI / 3.0).toFloat() + spin
        val b = a + (PI / 3.0).toFloat()
        val inner = radius * .37f
        val outer = radius * .85f
        val p = Path().apply {
            moveTo(center.x + cos(a) * inner, center.y + sin(a) * inner)
            lineTo(center.x + cos(a) * outer, center.y + sin(a) * outer)
            lineTo(center.x + cos(b) * outer, center.y + sin(b) * outer)
            lineTo(center.x + cos(b + .19f) * inner,
                center.y + sin(b + .19f) * inner)
            close()
        }
        drawPath(p, if (i % 2 == 0) Color(0xFF67483C) else Color(0xFF332A2B))
        drawPath(p, accent.copy(alpha = .31f),
            style = Stroke(width = .7.dp.toPx()))
    }
    drawCircle(Brush.radialGradient(listOf(
        Color(0xFF9D7959), Color(0xFF332B36), Color(0xFF090B15)),
        center = Offset(center.x - radius * .12f, center.y - radius * .15f),
        radius = radius * .55f), radius * .38f, center)
    drawCircle(accent.copy(alpha = .68f), radius * .38f, center,
        style = Stroke(width = 1.dp.toPx()))
    drawCircle(Color.White.copy(alpha = .35f), radius * .07f,
        Offset(center.x - radius * .12f, center.y - radius * .15f))
}

private fun DrawScope.v394Sunrise(
    center: Offset, radius: Float, accent: Color, t: Float, animate: Boolean,
) {
    val rise = if (animate) sin(t * .28f).toFloat() * radius * .025f else 0f
    val sun = Offset(center.x, center.y + radius * .06f + rise)
    drawCircle(Brush.radialGradient(listOf(
        Color(0xFFFFE6C5), accent, Color(0xFF92535E)),
        center = Offset(sun.x - radius * .15f, sun.y - radius * .2f),
        radius = radius), radius * .67f, sun)
    drawCircle(accent.copy(alpha = .26f), radius * .83f, sun,
        style = Stroke(width = 1.dp.toPx()))
    repeat(13) { i ->
        val y = center.y + radius * (.42f + i * .04f)
        val half = radius * (.74f + i * .018f)
        drawLine(Color(0xFF110D16).copy(alpha = .6f),
            Offset(center.x - half, y), Offset(center.x + half, y),
            strokeWidth = (2f + i * .35f).dp.toPx())
    }
    drawLine(accent.copy(alpha = .88f),
        Offset(center.x - radius * 1.12f, center.y + radius * .40f),
        Offset(center.x + radius * 1.12f, center.y + radius * .40f),
        strokeWidth = 1.3.dp.toPx())
}

private fun DrawScope.v394Progress(center: Offset, radius: Float, accent: Color) {
    drawCircle(Color(0xFF201A2B), radius * .83f, center)
    drawCircle(accent.copy(alpha = .21f), radius * .80f, center,
        style = Stroke(width = 1.dp.toPx()))
    repeat(21) { i ->
        val a = ((i / 21.0) * PI * 2 - PI / 2).toFloat()
        val point = Offset(center.x + cos(a) * radius,
            center.y + sin(a) * radius)
        drawCircle(if (i < 8) accent else accent.copy(alpha = .21f),
            radius = (if (i == 7) 4.5f else 2.8f).dp.toPx(),
            center = point)
    }
    drawArc(accent.copy(alpha = .64f), -90f, 360f * 8f / 21f, false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
}

/** Separate opt-in: old V391 scene choices are never rewritten or migrated. */
@Composable
internal fun V394PremiereSettingsSection() {
    val context = LocalContext.current
    V394PremierePrefs.initialize(context)
    var preview by remember { mutableStateOf<Pair<V394PremiereMode, ReminderCheckInKind>?>(null) }
    Column(Modifier.fillMaxWidth().testTag("v394-premiere-settings"),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("PREMIERE  /  FULL-SCREEN EXPERIENCE",
            color = PremiereGold, fontSize = 12.sp,
            fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Text("Experimental cinematic layout, hero animation, typography and actions. " +
            "Off by default. Existing scene backgrounds stay saved for the classic layout.",
            color = PremiereMuted, fontSize = 12.sp, lineHeight = 18.sp)
        for (kind in listOf(ReminderCheckInKind.VOICE, ReminderCheckInKind.ALARM)) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(kind.name + " · USE PREMIERE", color = PremiereIvory,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Creator and Personal", color = PremiereMuted, fontSize = 11.sp)
                }
                Switch(checked = V394PremierePrefs.enabled(kind),
                    onCheckedChange = { V394PremierePrefs.set(kind, it) })
            }
        }
        Text("PREVIEW WITHOUT CHANGING A REMINDER",
            color = PremiereGold, fontSize = 10.sp, letterSpacing = 1.2.sp)
        for (mode in V394PremiereMode.entries) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (kind in if (mode == V394PremiereMode.CHALLENGE)
                    listOf(ReminderCheckInKind.VOICE)
                    else listOf(ReminderCheckInKind.VOICE, ReminderCheckInKind.ALARM)) {
                    OutlinedButton(
                        onClick = { preview = mode to kind },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PremiereGold.copy(alpha = .45f)),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, null, Modifier.size(15.dp),
                            tint = PremiereGold)
                        Spacer(Modifier.width(4.dp))
                        Text(mode.name + " " + kind.name, color = PremiereIvory,
                            fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
        Text("21-day Challenge is a visual preview only. Its existing voice " +
            "notification and response flow are unchanged; no automatic full-screen launch.",
            color = PremiereMuted, fontSize = 11.sp, lineHeight = 17.sp)
    }
    val selected = preview
    if (selected != null) {
        Dialog(onDismissRequest = { preview = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize()) {
                var sampleAction by remember(selected) { mutableStateOf<String?>(null) }
                V394PremiereReminderScreen(
                    mode = selected.first, kind = selected.second,
                    title = when (selected.first) {
                        V394PremiereMode.CREATOR -> "Bring the next scene to life"
                        V394PremiereMode.PERSONAL -> "Take a moment for yourself"
                        V394PremiereMode.CHALLENGE -> "Keep your daily promise"
                    },
                    timeLabel = "7:30 AM",
                    contextLabel = when (selected.first) {
                        V394PremiereMode.CREATOR -> "SCENE 04  /  YOUR NEXT TAKE"
                        V394PremiereMode.PERSONAL -> "A GENTLE PERSONAL REMINDER"
                        V394PremiereMode.CHALLENGE -> "DAY 08 OF 21  /  SAMPLE PROGRESS"
                    },
                    supportingText = "A little momentum changes everything.",
                    primaryLabel = if (selected.first == V394PremiereMode.PERSONAL)
                        "DONE" else "I'M READY",
                    snoozeLabel = "SNOOZE 10 MIN",
                    onPrimary = { sampleAction = "Preview only: no action taken" },
                    onSnooze = { sampleAction = "Preview only: no snooze scheduled" },
                    onDismiss = { sampleAction = "Preview only: no reminder dismissed" },
                    onClosePreview = { preview = null },
                    error = sampleAction,
                )
            }
        }
    }
}
