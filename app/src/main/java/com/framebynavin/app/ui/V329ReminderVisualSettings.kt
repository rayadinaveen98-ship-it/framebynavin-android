package com.framebynavin.app.ui

import android.animation.ValueAnimator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.reminders.V329ReminderAppearance
import com.framebynavin.app.reminders.V329ReminderChannel
import com.framebynavin.app.reminders.V329ReminderDomain
import com.framebynavin.app.reminders.V330ReminderPrototypeScene
import com.framebynavin.app.reminders.V329ReminderVisualStore
import com.framebynavin.app.reminders.V329ReminderWorld

/**
 * Visual-only settings. The current choices are saved immediately, independently
 * for Alarm and Voice inside each Personal / Creator domain.
 */
@Composable
internal fun V329ReminderVisualSettings(domain: V329ReminderDomain) {
    val context = LocalContext.current
    val deviceWidthDp = LocalConfiguration.current.screenWidthDp
    val store = remember(context) { V329ReminderVisualStore(context) }
    var selectedChannel by rememberSaveable(domain.name) {
        mutableStateOf(V329ReminderChannel.ALARM.name)
    }
    val channel = V329ReminderChannel.entries.firstOrNull { it.name == selectedChannel }
        ?: V329ReminderChannel.ALARM
    var appearance by remember(domain, channel) {
        mutableStateOf(store.get(domain, channel))
    }
    fun save(next: V329ReminderAppearance) {
        val safe = next.normalized()
        appearance = safe
        store.set(domain, channel, safe)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("v329-visual-settings-${domain.name.lowercase()}"),
        color = Color(0xFF111014),
        border = BorderStroke(1.dp, Color(0xFF4C2731)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("REMINDER MOTION", color = Color(0xFFFF687D),
                fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(
                "Choose how your ${domain.name.lowercase()} reminders appear",
                color = Color(0xFFF8F2F4), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Visual effects only. All reminder options, buttons, sound, timing and actions stay unchanged.",
                color = Color(0xFFB9ADB3), fontSize = 12.sp, lineHeight = 17.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V329ReminderChannel.entries.forEach { choice ->
                    FilterChip(
                        selected = channel == choice,
                        onClick = { selectedChannel = choice.name },
                        label = { Text(if (choice == V329ReminderChannel.ALARM) "Alarm" else "Voice") },
                        modifier = Modifier.testTag("v329-channel-${choice.name.lowercase()}"),
                    )
                }
            }
            Text("VISUAL DIRECTION", color = Color(0xFFCABAC0),
                fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                V329ReminderWorld.entries.forEach { world ->
                    FilterChip(
                        selected = appearance.world == world,
                        onClick = { save(appearance.copy(world = world)) },
                        label = { Text(world.title, maxLines = 1) },
                        modifier = Modifier.testTag("v329-world-${world.name.lowercase()}"),
                    )
                }
            }
            Text(appearance.world.description, color = Color(0xFFD5C7CD),
                fontSize = 12.sp)
            Surface(
                color = Color(0xFF050408),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(if (deviceWidthDp >= 600) 306.dp else 270.dp)
                    .testTag("v329-live-preview"),
            ) {
                V330ReminderPrototypeScene(
                    domain = domain,
                    channel = channel,
                    appearance = appearance,
                    onReveal = {},
                    modifier = Modifier.fillMaxWidth(),
                    interactive = false,
                )
            }
            if (!ValueAnimator.areAnimatorsEnabled()) {
                Text("Android system animations are disabled. Reminder entry effects will be skipped by the accessibility setting.",
                    color = Color(0xFFFFB4C0), fontSize = 12.sp, lineHeight = 17.sp)
            }
            Text("Live preview · The real reminder opens after a double-tap.",
                color = Color(0xFFB3A5AE), fontSize = 11.sp)
            V329MotionSlider("Glow intensity", appearance.intensity) {
                save(appearance.copy(intensity = it))
            }
            V329MotionSlider("Particle density", appearance.density) {
                save(appearance.copy(density = it))
            }
            V329MotionSlider("Motion speed", appearance.speed) {
                save(appearance.copy(speed = it))
            }
            Spacer(Modifier.height(1.dp))
            Text("Saved automatically on this device · Battery saver reduces effects.",
                color = Color(0xFFA596A0), fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun V329MotionSlider(
    title: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(title, fontSize = 12.sp, color = Color(0xFFE9DBE1),
            fontWeight = FontWeight.Medium)
        Slider(
            value = value,
            valueRange = .15f..1f,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().testTag("v329-slider-${title.lowercase().replace(' ', '-')}"),
        )
    }
}
