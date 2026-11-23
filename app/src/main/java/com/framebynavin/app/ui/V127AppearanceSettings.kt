package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*

@Composable
internal fun V127AppearanceSettings(showGuidePicker: Boolean = true) {
    val selected = VisualExperiencePrefs.currentTheme

    Column(Modifier.fillMaxWidth()) {
        if (showGuidePicker) {
            V144GuidePicker()
            Spacer(Modifier.height(18.dp))
        }

        Text("CHOOSE YOUR THEME", color = RecRed, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(4.dp))
        Text("Same Backlot, two looks.", color = ProjectorIvory,
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text("Change colors without changing your work, reminders or navigation.",
            color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))

        V137ThemeOrder.forEach { theme ->
            V148AppearanceCard(
                theme = theme,
                active = theme == selected,
                onSelect = {
                    VisualExperiencePrefs.setTheme(theme)
                },
            )
            Spacer(Modifier.height(9.dp))
        }

        Spacer(Modifier.height(8.dp))
        V133AppIconPicker()


    }
}

@Composable
private fun V148AppearanceCard(
    theme: FrameTheme,
    active: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = RoundedCornerShape(BacklotCardRadius),
        color = CinemaSurface,
        border = BorderStroke(
            width = if (active) 1.5.dp else BacklotBorderWidth,
            color = if (active) RecRed else CinemaLine,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            V148AppearancePreview(theme, Modifier.fillMaxWidth().height(98.dp))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(theme.displayName, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Black)
                    Text(theme.tagline, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)
                }
                if (active) {
                    Surface(shape = RoundedCornerShape(99.dp), color = RecRed.copy(alpha = .12f)) {
                        Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(RecRed, CircleShape))
                            Spacer(Modifier.width(5.dp))
                            Text("ACTIVE", color = RecRed, fontSize = 7.4.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V148AppearancePreview(theme: FrameTheme, modifier: Modifier = Modifier) {
    // P9B: a small recognizable Creator Home composition rather than abstract lines.
    // This is a theme preview, not a screenshot of a running Home screen.
    val p = theme.palette
    val profile = theme.profile
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = p.background,
        border = BorderStroke(1.dp, p.line),
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(p.primary, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("BACKLOT", color = p.foreground, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("TODAY", color = p.muted, fontSize = 8.sp)
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().height(53.dp),
                shape = RoundedCornerShape(profile.cardRadius),
                color = p.surfaceRaised,
                border = BorderStroke(1.dp, p.line),
            ) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(28.dp)
                        .background(p.primary, RoundedCornerShape(99.dp)))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Up next", color = p.foreground, fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Projects · Ideas · Reminders", color = p.muted, fontSize = 8.sp)
                    }
                    Box(Modifier.size(21.dp).background(p.primary,
                        RoundedCornerShape(profile.buttonRadius)))
                }
            }
        }
    }
}
