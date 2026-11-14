package com.framebynavin.app.reminders

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Visual profiles never own alarm scheduling, foreground audio, privacy or responses. */
internal enum class V391VisualMode { VOICE, ALARM }

internal enum class V391ReminderScene(val title: String, val detail: String) {
    DIRECTOR("Director's Cut", "The existing Backlot cinematic backdrop"),
    AURORA_SILK("Aurora Silk", "Luminous flowing silk ribbons"),
    LIQUID_CHROME("Liquid Chrome", "Silver reflections with subtle motion"),
    CELESTIAL_DRIFT("Celestial Drift", "A drifting star field and constellations"),
    CRIMSON_PULSE("Crimson Pulse", "Confident concentric alarm pulses"),
    GLASSWAVE("Glasswave", "Refracted cyan waves"),
    EMBER_FIELD("Ember Field", "Warm floating sparks");
}

/** Exactly two stable app-wide defaults; Personal and Creator share the selection. */
internal object V391ReminderScenePrefs {
    private const val PREFS = "backlot_reminder_scenes_v1"
    private const val VOICE = "voice_scene"
    private const val ALARM = "alarm_scene"
    private var loaded = false

    var voice by mutableStateOf(V391ReminderScene.DIRECTOR)
        private set
    var alarm by mutableStateOf(V391ReminderScene.DIRECTOR)
        private set

    @Synchronized fun initialize(context: Context) {
        if (loaded) return
        val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        voice = V391ReminderScene.entries.firstOrNull { it.name == preferences.getString(VOICE, null) }
            ?: V391ReminderScene.DIRECTOR
        alarm = V391ReminderScene.entries.firstOrNull { it.name == preferences.getString(ALARM, null) }
            ?: V391ReminderScene.DIRECTOR
        loaded = true
    }

    fun sceneFor(mode: V391VisualMode): V391ReminderScene =
        if (mode == V391VisualMode.VOICE) voice else alarm

    fun select(context: Context, mode: V391VisualMode, scene: V391ReminderScene) {
        initialize(context)
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(if (mode == V391VisualMode.VOICE) VOICE else ALARM, scene.name).apply()
        if (mode == V391VisualMode.VOICE) voice = scene else alarm = scene
    }
}

/**
 * Full-screen decoration, ALWAYS under action controls. A style cannot access
 * playback services or intercept taps. The old Director backdrop is the
 * default so existing users retain their approved visual until opting in.
 */
@Composable
internal fun V391ReminderVisualBackdrop(
    mode: V391VisualMode,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
) {
    V391ReminderScenePrefs.initialize(LocalContext.current)
    val scene = V391ReminderScenePrefs.sceneFor(mode)
    if (scene == V391ReminderScene.DIRECTOR) {
        V336DirectorBackdrop(motionEnabled, lowPower, modifier)
    } else {
        V391AnimatedScene(scene, motionEnabled, lowPower, modifier)
    }
}

@Composable
internal fun V391AnimatedScene(
    scene: V391ReminderScene,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
) {
    val t = V336MotionTime(motionEnabled, lowPower)
    Canvas(modifier.fillMaxSize().testTag("v391-scene-" + scene.name.lowercase())) {
        val w = size.width
        val h = size.height
        val base = min(w, h)
        val count = if (lowPower) 10 else 36
        val dark = Color(0xFF09070E)
        val ruby = Color(0xFFFF668D)
        val pearl = Color(0xFFFCF0E8)
        when (scene) {
            V391ReminderScene.DIRECTOR -> drawRect(V336DirectorPalette.black)
            V391ReminderScene.AURORA_SILK -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF080817), Color(0xFF150D2C), dark)))
                for (i in 0 until if (lowPower) 3 else 6) {
                    val y = h * (.20f + i * .11f) + sin(t * .28f + i * 1.31f).toFloat() * h * .025f
                    val p = Path().apply {
                        moveTo(-w * .1f, y)
                        cubicTo(w * .20f, y - h * .12f, w * .45f, y + h * .18f, w * 1.1f, y - h * .06f)
                    }
                    val color = if (i % 2 == 0) Color(0xFF9369ED) else Color(0xFF55C4D9)
                    drawPath(p, color.copy(alpha = .07f), style = Stroke(width = base * .09f, cap = StrokeCap.Round))
                    drawPath(p, color.copy(alpha = .27f), style = Stroke(width = base * .026f, cap = StrokeCap.Round))
                    drawPath(p, pearl.copy(alpha = .27f), style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            V391ReminderScene.LIQUID_CHROME -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF11121B), Color(0xFF20232F), dark)))
                drawCircle(Brush.radialGradient(listOf(Color(0xFF8FA3BA).copy(alpha = .28f), Color.Transparent),
                    center = Offset(w * .54f, h * .33f), radius = w * .65f),
                    center = Offset(w * .54f, h * .33f), radius = w * .65f)
                for (i in 0 until if (lowPower) 3 else 7) {
                    val radius = base * (.22f + i * .092f)
                    val center = Offset(w * .50f + sin(t * .22f + i).toFloat() * w * .035f,
                        h * .35f + cos(t * .18f + i).toFloat() * h * .024f)
                    drawCircle(Color(0xFFDAE3F3).copy(alpha = .13f + (i % 3) * .035f),
                        radius, center, style = Stroke(width = (if (i % 2 == 0) 3.8f else 1.5f).dp.toPx()))
                }
            }
            V391ReminderScene.CELESTIAL_DRIFT -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF050A19), Color(0xFF122347), dark)))
                drawCircle(Brush.radialGradient(listOf(Color(0xFF3269BA).copy(alpha = .28f), Color.Transparent),
                    center = Offset(w * .7f, h * .29f), radius = w * .72f),
                    center = Offset(w * .7f, h * .29f), radius = w * .72f)
                for (i in 0 until count) {
                    val x = (((i * 97 + 31) % 257) / 257f) * w
                    val y = (((i * 151 + 43) % 271) / 271f) * h +
                        sin(t * .17f + i).toFloat() * 4.dp.toPx()
                    val glow = .28f + (.40f * (1f + sin(t * .53f + i).toFloat()) * .5f)
                    drawCircle(pearl.copy(alpha = glow), if (i % 9 == 0) 1.8.dp.toPx() else .9.dp.toPx(), Offset(x, y))
                }
                for (i in 0 until if (lowPower) 2 else 5) {
                    val x = (((i * 73 + 22) % 101) / 101f) * w
                    val y = (((i * 57 + 10) % 97) / 97f) * h
                    drawLine(Color(0xFF9DC6F6).copy(alpha = .23f), Offset(x,y),
                        Offset((x + w * .12f).coerceAtMost(w), (y + h * .038f).coerceAtMost(h)), .8.dp.toPx())
                }
            }
            V391ReminderScene.CRIMSON_PULSE -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF200613), Color(0xFF08020A), dark)))
                drawCircle(Brush.radialGradient(listOf(ruby.copy(alpha = .3f), Color.Transparent),
                    center = Offset(w * .5f, h * .38f), radius = w * .72f),
                    center = Offset(w * .5f, h * .38f), radius = w * .72f)
                for (i in 0 until if (lowPower) 3 else 6) {
                    val f = (i / 6f + t * .11f) % 1f
                    drawCircle(ruby.copy(alpha = (.45f * (1f - f)).coerceAtLeast(.04f)),
                        radius = base * (.16f + f * 1.1f),
                        center = Offset(w * .5f, h * .38f),
                        style = Stroke(width = (2.8f - f * 1.7f).dp.toPx()))
                }
            }
            V391ReminderScene.GLASSWAVE -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF06121A), Color(0xFF09273B), dark)))
                for (i in 0 until if (lowPower) 3 else 8) {
                    val y = h * (.19f + i * .077f) + sin(t * .32f + i * .7f).toFloat() * h * .022f
                    val p = Path().apply {
                        moveTo(-w * .1f, y)
                        cubicTo(w * .16f, y - h * .11f, w * .72f, y + h * .09f, w * 1.1f, y - h * .12f)
                    }
                    val col = if (i % 2 == 0) Color(0xFF43D4D0) else Color(0xFF5A8EEB)
                    drawPath(p, col.copy(alpha = .10f), style = Stroke(width = base * .058f, cap = StrokeCap.Round))
                    drawPath(p, col.copy(alpha = .55f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
                }
            }
            V391ReminderScene.EMBER_FIELD -> {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF190C13), Color(0xFF28101A), dark)))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFD47528).copy(alpha = .28f), Color.Transparent),
                    center = Offset(w * .52f, h * .75f), radius = w * .82f),
                    center = Offset(w * .52f, h * .75f), radius = w * .82f)
                for (i in 0 until count) {
                    val x = (((i * 83 + 7) % 251) / 251f) * w + sin(t * .3f + i).toFloat() * w * .025f
                    val y = ((((i * 137 + 11) % 229) / 229f + 1f - t * .018f) % 1f) * h
                    val alpha = .22f + .5f * ((i % 5) / 5f)
                    val pos = Offset(x.coerceIn(0f,w),y)
                    drawCircle(Color(0xFFFFBC64).copy(alpha=alpha*.25f), 5.dp.toPx(), pos)
                    drawCircle(Color(0xFFFFD899).copy(alpha=alpha), 1.dp.toPx(), pos)
                }
            }
        }
        // Every scene has the same bottom readability field.
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x33000000),
            Color(0xB9000000)), startY = h * .38f, endY = h))
    }
}

/** One picker for both Creator and Personal Voice/Alarm screens. */
@Composable
internal fun V391ReminderScenesPicker() {
    val context = LocalContext.current
    V391ReminderScenePrefs.initialize(context)
    var mode by remember { mutableStateOf(V391VisualMode.VOICE) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("REMINDER SCENES", color = V336DirectorPalette.ivory,
            fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Choose Voice and Alarm backgrounds independently. These styles do not change ringing, speech, Snooze or delivery.",
            color = V336DirectorPalette.muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (item in V391VisualMode.entries) {
                val active = mode == item
                OutlinedButton(onClick = { mode = item },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (active) V336DirectorPalette.hot else V336DirectorPalette.ivory),
                    modifier = Modifier.weight(1f)) {
                    Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        for (scene in V391ReminderScene.entries) {
            val active = V391ReminderScenePrefs.sceneFor(mode) == scene
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    V391ReminderScenePrefs.select(context, mode, scene)
                }.testTag("v391-choose-" + mode.name.lowercase() + "-" + scene.name.lowercase()),
                shape = RoundedCornerShape(14.dp),
                color = if (active) Color(0xFF251721) else Color(0xFF15121B),
                border = androidx.compose.foundation.BorderStroke(
                    if (active) 2.dp else 1.dp,
                    if (active) V336DirectorPalette.hot else Color(0xFF403241)),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(width=93.dp,height=75.dp).background(Color.Black,
                        RoundedCornerShape(10.dp))) {
                        if (scene == V391ReminderScene.DIRECTOR)
                            V336DirectorBackdrop(motionEnabled=false, lowPower=true)
                        else V391AnimatedScene(scene, motionEnabled=false, lowPower=true)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(scene.title, color = V336DirectorPalette.ivory,
                            fontSize=13.sp, fontWeight=FontWeight.Bold)
                        Text(scene.detail, color = V336DirectorPalette.muted, fontSize=11.sp,
                            lineHeight=14.sp)
                        if (active) Text("SELECTED", color=V336DirectorPalette.hot,
                            fontSize=10.sp, fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }
}
