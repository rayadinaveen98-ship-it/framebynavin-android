package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.widget.CreatorWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun V127AppearanceSettings(showGuidePicker: Boolean = true) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val selected = VisualExperiencePrefs.currentTheme

    fun refreshWidgets() {
        scope.launch(Dispatchers.IO) {
            CreatorWidgetUpdater.updateAll(context.applicationContext, TaskStore(context.applicationContext).load())
        }
    }

    Column(Modifier.fillMaxWidth()) {
        if (showGuidePicker) {
            V144GuidePicker()
            Spacer(Modifier.height(18.dp))
        }

        Text("APPEARANCE", color = MutedGold, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(4.dp))
        Text("Choose how Backlot looks.", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(
            "One Backlot design language, two complete appearances. Layout, typography and workflow stay consistent in both.",
            color = MutedText,
            fontSize = 9.sp,
            lineHeight = 13.sp,
        )
        Spacer(Modifier.height(12.dp))

        V137ThemeOrder.forEach { theme ->
            V148AppearanceCard(
                theme = theme,
                active = theme == selected,
                onSelect = {
                    VisualExperiencePrefs.setTheme(theme)
                    refreshWidgets()
                },
            )
            Spacer(Modifier.height(9.dp))
        }

        Spacer(Modifier.height(8.dp))
        V133AppIconPicker()

        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(BacklotCardRadius),
            color = CinemaSurface,
            border = BorderStroke(BacklotBorderWidth, CinemaLine),
        ) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), RoundedCornerShape(BacklotSmallRadius), RecRed.copy(alpha = .12f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.GraphicEq, null, tint = RecRed, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Launch sound", color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    Text("Short original sonic ident on normal cold launches.", color = MutedText, fontSize = 8.2.sp)
                }
                Switch(
                    checked = VisualExperiencePrefs.launchSoundEnabled,
                    onCheckedChange = VisualExperiencePrefs::updateLaunchSound,
                    colors = SwitchDefaults.colors(checkedThumbColor = ProjectorIvory, checkedTrackColor = RecRed),
                )
            }
        }
    }
}

@Composable
private fun V148AppearanceCard(
    theme: FrameTheme,
    active: Boolean,
    onSelect: () -> Unit,
) {
    val palette = theme.palette
    val profile = theme.profile
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
                    Text(theme.displayName, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Black)
                    Text(theme.tagline, color = MutedText, fontSize = 8.2.sp, lineHeight = 11.5.sp)
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
    val p = theme.palette
    val profile = theme.profile
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = p.background,
        border = BorderStroke(1.dp, p.line),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawRect(p.primary.copy(alpha = if (p.isLight) .05f else .08f), Offset(w * .055f, 0f), Size(w * .012f, h))
                drawLine(p.primary.copy(alpha = .55f), Offset(w * .055f, h * .15f), Offset(w * .055f, h * .54f), 2.2f)
                drawLine(p.secondary.copy(alpha = .55f), Offset(w * .70f, h * .16f), Offset(w * .92f, h * .16f), 1.5f)
                drawLine(p.primary.copy(alpha = .18f), Offset(w * .12f, h * .88f), Offset(w * .88f, h * .70f), 1.1f)
            }

            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(p.primary, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.width(52.dp).height(4.dp).background(p.foreground.copy(alpha = .78f), RoundedCornerShape(99.dp)))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.width(28.dp).height(4.dp).background(p.secondary.copy(alpha = .72f), RoundedCornerShape(99.dp)))
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Surface(
                        modifier = Modifier.width(116.dp).height(42.dp),
                        shape = RoundedCornerShape(profile.cardRadius),
                        color = p.surfaceRaised,
                        border = BorderStroke(1.dp, p.line),
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Box(Modifier.width(67.dp).height(4.dp).background(p.foreground.copy(alpha = .68f), RoundedCornerShape(99.dp)))
                            Spacer(Modifier.height(7.dp))
                            Box(Modifier.width(88.dp).height(3.dp).background(p.muted.copy(alpha = .55f), RoundedCornerShape(99.dp)))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.width(54.dp).height(28.dp)
                            .background(p.primary, RoundedCornerShape(profile.buttonRadius)),
                    )
                }
            }
        }
    }
}
