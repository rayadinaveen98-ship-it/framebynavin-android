package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Decorative scenes are separated from the delivery and response engines:
 * neither this file nor its persisted preferences may stop a service, handle
 * Done/Snooze, or create a notification.
 */
internal enum class V391ReminderScene(val label: String) {
    DIRECTOR("Director's Cut"),
    AURORA_SILK("Aurora Silk"),
    LIQUID_CHROME("Liquid Chrome"),
    CELESTIAL_DRIFT("Celestial Drift"),
    CRIMSON_PULSE("Crimson Pulse"),
    GLASSWAVE("Glasswave"),
    EMBER_FIELD("Ember Field");
}

internal object V391ReminderScenePrefs {
    private const val PREFS = "backlot_reminder_scenes_v391"
    private const val KEY_VOICE = "voice"
    private const val KEY_ALARM = "alarm"
    private var context: Context? = null

    // Default to the existing V336 scene on upgrade, never change users' art
    // silently. The two scene choices remain independent of Creator / Personal.
    var voice by mutableStateOf(V391ReminderScene.DIRECTOR)
        private set
    var alarm by mutableStateOf(V391ReminderScene.DIRECTOR)
        private set

    fun initialize(app: Context) {
        if (context != null) return
        context = app.applicationContext
        val saved = context!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        voice = runCatching { V391ReminderScene.valueOf(saved.getString(KEY_VOICE, "") ?: "") }
            .getOrDefault(V391ReminderScene.DIRECTOR)
        alarm = runCatching { V391ReminderScene.valueOf(saved.getString(KEY_ALARM, "") ?: "") }
            .getOrDefault(V391ReminderScene.DIRECTOR)
    }

    fun selected(kind: ReminderCheckInKind): V391ReminderScene = when (kind) {
        ReminderCheckInKind.VOICE -> voice
        ReminderCheckInKind.ALARM -> alarm
    }

    fun set(kind: ReminderCheckInKind, scene: V391ReminderScene) {
        val app = context ?: return
        when (kind) {
            ReminderCheckInKind.VOICE -> voice = scene
            ReminderCheckInKind.ALARM -> alarm = scene
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(if (kind == ReminderCheckInKind.VOICE) KEY_VOICE else KEY_ALARM, scene.name)
            .apply()
    }
}

/** Rendered *behind* all existing actions. Canvas has no pointer handlers. */
@Composable
internal fun V391ReminderBackdrop(
    kind: ReminderCheckInKind,
    motionEnabled: Boolean,
    lowPower: Boolean,
    modifier: Modifier = Modifier,
    previewScene: V391ReminderScene? = null,
) {
    V391ReminderScenePrefs.initialize(LocalContext.current)
    val scene = previewScene ?: V391ReminderScenePrefs.selected(kind)
    if (scene == V391ReminderScene.DIRECTOR) {
        V336DirectorBackdrop(motionEnabled, lowPower, modifier)
        return
    }
    val t = V336MotionTime(motionEnabled, lowPower)
    Canvas(modifier.fillMaxSize().testTag("v391-scene-${scene.name}")) {
        val base = when (scene) {
            V391ReminderScene.AURORA_SILK -> listOf(Color(0xFF070812), Color(0xFF1C1133), Color(0xFF150B20), Color(0xFF070812))
            V391ReminderScene.LIQUID_CHROME -> listOf(Color(0xFF070A13), Color(0xFF19232D), Color(0xFF10131E), Color(0xFF070A13))
            V391ReminderScene.CELESTIAL_DRIFT -> listOf(Color(0xFF060717), Color(0xFF14172F), Color(0xFF0D0D20), Color(0xFF050712))
            V391ReminderScene.CRIMSON_PULSE -> listOf(Color(0xFF100508), Color(0xFF2A0A18), Color(0xFF18060C), Color(0xFF090408))
            V391ReminderScene.GLASSWAVE -> listOf(Color(0xFF040F16), Color(0xFF0B2730), Color(0xFF071D25), Color(0xFF030B11))
            V391ReminderScene.EMBER_FIELD -> listOf(Color(0xFF0D0807), Color(0xFF21100C), Color(0xFF1D0B0A), Color(0xFF0B0708))
            V391ReminderScene.DIRECTOR -> error("Handled before Canvas")
        }
        drawRect(Brush.verticalGradient(base))
        val w = size.width
        val h = size.height
        when (scene) {
            V391ReminderScene.AURORA_SILK -> {
                v391Glow(.37f, .27f, .68f, Color(0xFF9A53EE), .20f)
                v391Glow(.74f, .68f, .60f, Color(0xFFEE7A9F), .10f)
                repeat(if (lowPower) 2 else 5) { i ->
                    v391Ribbon(
                        y = h * (.22f + i * .085f), t = t * (.25f + i * .02f),
                        amplitude = h * (.045f + i * .004f), color =
                            if (i % 2 == 0) Color(0xFFA68FFF) else Color(0xFFF49EC2),
                        alpha = .17f, stroke = (1.8f + i * .4f).dp.toPx(),
                    )
                }
            }
            V391ReminderScene.LIQUID_CHROME -> {
                v391Glow(.54f, .28f, .61f, Color(0xFF99B8C8), .18f)
                repeat(if (lowPower) 2 else 5) { i ->
                    val phase = (i + 1) * .67f + t * .18f
                    val center = Offset(w * (.49f + sin(phase).toFloat() * .15f),
                        h * (.28f + cos(phase * .71f).toFloat() * .07f))
                    val radius = w * (.17f + i * .12f)
                    drawCircle(Color(0xFFC9DBE4).copy(alpha = .17f / (1f + i * .22f)),
                        radius, center, style = Stroke(width = (2.8f - i * .28f).coerceAtLeast(.7f).dp.toPx()))
                    drawCircle(Color(0xFFEAACD7).copy(alpha = .07f), radius * .97f,
                        center, style = Stroke(width = 6.dp.toPx()))
                }
            }
            V391ReminderScene.CELESTIAL_DRIFT -> {
                v391Glow(.52f, .20f, .62f, Color(0xFF6367DE), .14f)
                val count = if (lowPower) 28 else 83
                repeat(count) { i ->
                    val px = (((i * 67 + 41) % 257) / 257f) * w
                    val py = (((i * 139 + 17) % 269) / 269f) * h
                    val pulse = .48f + .30f * sin(t * .45f + i * .47f).toFloat()
                    drawCircle(Color(0xFFE7DEFF).copy(alpha = pulse),
                        (if (i % 11 == 0) 1.7f else .75f).dp.toPx(), Offset(px, py))
                    if (i % 12 == 0 && i < count - 1) {
                        val nx = (((i * 67 + 108) % 257) / 257f) * w
                        val ny = (((i * 139 + 156) % 269) / 269f) * h
                        drawLine(Color(0xFFAFBBFF).copy(alpha = .08f), Offset(px, py),
                            Offset(nx, ny), strokeWidth = .65.dp.toPx())
                    }
                }
            }
            V391ReminderScene.CRIMSON_PULSE -> {
                v391Glow(.50f, .32f, .67f, Color(0xFFD84E68), .21f)
                repeat(if (lowPower) 3 else 6) { i ->
                    val phase = (i.toFloat() / 6f + t * .065f) % 1f
                    val radius = w * (.16f + 1.02f * phase)
                    drawCircle(Color(0xFFFF6A85).copy(alpha = (.20f * (1f - phase))),
                        radius, Offset(w * .5f, h * .31f),
                        style = Stroke(width = (1.9f + phase * 2f).dp.toPx()))
                }
            }
            V391ReminderScene.GLASSWAVE -> {
                v391Glow(.53f, .33f, .69f, Color(0xFF38AEBE), .16f)
                repeat(if (lowPower) 2 else 6) { i ->
                    v391Ribbon(
                        y = h * (.26f + i * .07f), t = t * (.23f + i * .012f),
                        amplitude = h * (.026f + i * .004f),
                        color = if (i % 2 == 0) Color(0xFF79F3ED) else Color(0xFF9AC8FA),
                        alpha = .16f, stroke = (1.0f + i * .4f).dp.toPx(),
                    )
                }
            }
            V391ReminderScene.EMBER_FIELD -> {
                v391Glow(.48f, .73f, .70f, Color(0xFFE57A32), .14f)
                repeat(if (lowPower) 17 else 46) { i ->
                    val drift = (i * 37 % 101) / 101f
                    val rising = (drift + t * (.012f + (i % 7) * .003f)) % 1f
                    val x = ((i * 97 + 31) % 283) / 283f * w +
                        sin(t * .43f + i).toFloat() * w * .008f
                    val y = h * (1f - rising)
                    val alpha = (.09f + .37f * sin(rising * 3.14159f).toFloat()).coerceIn(0f, .48f)
                    drawCircle(if (i % 4 == 0) Color(0xFFFFD7A0).copy(alpha = alpha)
                               else Color(0xFFFFA362).copy(alpha = alpha * .75f),
                        (if (i % 7 == 0) 2.1f else 1.1f).dp.toPx(), Offset(x, y))
                }
            }
            V391ReminderScene.DIRECTOR -> Unit
        }
    }
}

private fun DrawScope.v391Glow(
    x: Float, y: Float, relativeRadius: Float, color: Color, alpha: Float,
) {
    val c = Offset(size.width * x, size.height * y)
    val radius = size.width * relativeRadius
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent),
            center = c, radius = radius),
        radius = radius, center = c,
    )
}

private fun DrawScope.v391Ribbon(
    y: Float, t: Float, amplitude: Float, color: Color, alpha: Float, stroke: Float,
) {
    val w = size.width
    val wave = sin(t).toFloat() * amplitude * .34f
    val p = Path().apply {
        moveTo(-w * .08f, y + wave)
        cubicTo(w * .14f, y - amplitude, w * .38f, y + amplitude * .92f,
            w * .61f, y - amplitude * .5f)
        cubicTo(w * .78f, y - amplitude * 1.3f, w * .89f, y + amplitude * .7f,
            w * 1.08f, y)
    }
    drawPath(p, color.copy(alpha = alpha * .30f),
        style = Stroke(width = stroke * 9f, cap = StrokeCap.Round))
    drawPath(p, color.copy(alpha = alpha),
        style = Stroke(width = stroke, cap = StrokeCap.Round))
}

/** Two independent user-selectable voice and alarm scene defaults. */
@Composable
internal fun V391ReminderSceneSettings() {
    val context = LocalContext.current
    V391ReminderScenePrefs.initialize(context)
    Column(Modifier.fillMaxWidth().testTag("v391-scene-settings"),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("REMINDER SCENES", color = V336DirectorPalette.gold,
            fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        Text("Animated backgrounds for both Creator and Personal reminders. " +
            "Your reminder actions and sound settings stay the same.",
            color = V336DirectorPalette.muted, fontSize = 12.sp, lineHeight = 17.sp)
        for (kind in listOf(ReminderCheckInKind.VOICE, ReminderCheckInKind.ALARM)) {
            val selected = V391ReminderScenePrefs.selected(kind)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (kind == ReminderCheckInKind.VOICE) "VOICE BACKGROUND" else "ALARM BACKGROUND",
                    color = V336DirectorPalette.ivory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(18.dp),
                    color = V336DirectorPalette.black) {
                    Box(Modifier.fillMaxWidth().height(136.dp)
                        .clip(RoundedCornerShape(18.dp))) {
                        V391ReminderBackdrop(kind, motionEnabled = ValueAnimator.areAnimatorsEnabled(),
                            lowPower = true, previewScene = selected)
                        Text(selected.label, color = V336DirectorPalette.ivory,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(17.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    for (scene in V391ReminderScene.entries) {
                        OutlinedButton(
                            onClick = { V391ReminderScenePrefs.set(kind, scene) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (scene == selected)
                                    V336DirectorPalette.wine else Color.Transparent,
                                contentColor = V336DirectorPalette.ivory,
                            ),
                            modifier = Modifier.testTag("v391-pick-${kind.name}-${scene.name}"),
                        ) { Text(scene.label, fontSize = 11.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Scenes respect reduced-motion preferences and battery saver. " +
            "Animations never hide or intercept reminder controls.",
            color = V336DirectorPalette.muted, fontSize = 11.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(12.dp))
        V394PremiereSettingsSection()
    }
}
