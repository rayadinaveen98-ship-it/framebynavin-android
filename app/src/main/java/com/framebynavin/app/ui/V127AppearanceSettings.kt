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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.widget.CreatorWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V127AppearanceSettings() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val selected = VisualExperiencePrefs.currentTheme

    fun refreshWidgets() {
        scope.launch(Dispatchers.IO) {
            CreatorWidgetUpdater.updateAll(context.applicationContext, TaskStore(context.applicationContext).load())
        }
    }

    Column(Modifier.fillMaxWidth()) {
        V137CharacterPicker()
        Spacer(Modifier.height(18.dp))

        Text("APPEARANCE", color = MutedGold, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(4.dp))
        Text("Choose your Backlot world.", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text(
            "Ten complete visual languages — layout rhythm, surfaces, shape, typography, navigation and atmosphere change together.",
            color = MutedText,
            fontSize = 9.sp,
            lineHeight = 13.sp,
        )
        Spacer(Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            maxItemsInEachRow = 2,
        ) {
            V137ThemeOrder.forEach { theme ->
                val active = theme == selected
                val palette = theme.palette
                val profile = theme.profile
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 150.dp)
                        .height(178.dp)
                        .clickable {
                            VisualExperiencePrefs.setTheme(theme)
                            refreshWidgets()
                        },
                    shape = RoundedCornerShape(profile.cardRadius),
                    color = palette.surface,
                    border = BorderStroke(profile.borderWidth, if (active) palette.primary else palette.line),
                ) {
                    Column(Modifier.fillMaxSize().padding(10.dp)) {
                        V139ThemePreview(theme, Modifier.fillMaxWidth().height(92.dp))
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(theme.displayName, color = palette.foreground, fontSize = 10.7.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                            if (active) {
                                Box(Modifier.size(8.dp).background(palette.primary, CircleShape))
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(theme.tagline, color = palette.muted, fontSize = 7.4.sp, lineHeight = 9.5.sp, maxLines = 2)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
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
private fun V139ThemePreview(theme: FrameTheme, modifier: Modifier = Modifier) {
    val p = theme.palette
    val profile = theme.profile
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(profile.smallRadius),
        color = p.background,
        border = BorderStroke(profile.borderWidth, p.line.copy(alpha = .8f)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                when (profile.visualLanguage) {
                    FrameVisualLanguage.CINEMATIC -> {
                        drawRect(p.primary.copy(alpha = .75f), Offset(w * .06f, h * .10f), Size(w * .018f, h * .58f))
                        drawLine(p.secondary.copy(alpha = .75f), Offset(w * .68f, h * .16f), Offset(w * .92f, h * .16f), 2f)
                        drawLine(p.primary.copy(alpha = .28f), Offset(w * .12f, h * .82f), Offset(w * .88f, h * .66f), 1.2f)
                    }
                    FrameVisualLanguage.LUXE -> {
                        drawLine(p.primary.copy(alpha = .72f), Offset(w * .08f, h * .13f), Offset(w * .34f, h * .13f), 1.7f)
                        drawLine(p.primary.copy(alpha = .72f), Offset(w * .08f, h * .13f), Offset(w * .08f, h * .36f), 1.7f)
                        drawLine(p.secondary.copy(alpha = .38f), Offset(w * .60f, h * .78f), Offset(w * .91f, h * .78f), 1.2f)
                    }
                    FrameVisualLanguage.MONO -> {
                        repeat(3) { i -> drawLine(p.foreground.copy(alpha = .13f), Offset(w * (.18f + i * .27f), 0f), Offset(w * (.18f + i * .27f), h), 1f) }
                        drawLine(p.foreground.copy(alpha = .15f), Offset(0f, h * .64f), Offset(w, h * .64f), 1f)
                    }
                    FrameVisualLanguage.GALLERY -> {
                        drawLine(p.foreground.copy(alpha = .20f), Offset(w * .10f, h * .22f), Offset(w * .90f, h * .22f), 1f)
                        drawRect(p.secondary.copy(alpha = .20f), Offset(w * .68f, h * .10f), Size(w * .20f, h * .08f))
                    }
                    FrameVisualLanguage.PAPER -> {
                        repeat(5) { i -> drawLine(p.foreground.copy(alpha = .12f), Offset(w * .08f, h * (.18f + i * .15f)), Offset(w * .94f, h * (.18f + i * .15f)), 1f) }
                        drawLine(p.primary.copy(alpha = .55f), Offset(w * .17f, h * .08f), Offset(w * .17f, h * .90f), 1.4f)
                    }
                    FrameVisualLanguage.ORGANIC -> {
                        drawOval(p.primary.copy(alpha = .22f), Offset(w * .67f, h * .13f), Size(w * .22f, h * .16f), style = Stroke(1.4f))
                        drawOval(p.secondary.copy(alpha = .20f), Offset(w * .54f, h * .44f), Size(w * .28f, h * .18f), style = Stroke(1.2f))
                    }
                    FrameVisualLanguage.POSTER -> {
                        drawRect(p.primary.copy(alpha = .34f), Offset(w * .07f, h * .12f), Size(w * .27f, h * .24f))
                        drawRect(p.secondary.copy(alpha = .20f), Offset(w * .62f, h * .55f), Size(w * .30f, h * .25f))
                    }
                    FrameVisualLanguage.BLOOM -> {
                        drawOval(p.primary.copy(alpha = .28f), Offset(w * .64f, h * .14f), Size(w * .25f, h * .18f), style = Stroke(1.5f))
                        drawOval(p.secondary.copy(alpha = .24f), Offset(w * .52f, h * .38f), Size(w * .25f, h * .16f), style = Stroke(1.2f))
                    }
                    FrameVisualLanguage.HORIZON -> {
                        drawRect(p.primary.copy(alpha = .12f), Offset(0f, h * .22f), Size(w, h * .14f))
                        drawLine(p.primary.copy(alpha = .54f), Offset(w * .05f, h * .40f), Offset(w * .95f, h * .40f), 1.5f)
                        drawLine(p.foreground.copy(alpha = .14f), Offset(w * .18f, h * .70f), Offset(w * .83f, h * .70f), 1f)
                    }
                    FrameVisualLanguage.STORYBOARD -> {
                        repeat(2) { row ->
                            repeat(2) { col ->
                                drawRect(
                                    p.foreground.copy(alpha = .24f),
                                    Offset(w * (.08f + col * .46f), h * (.10f + row * .36f)),
                                    Size(w * .38f, h * .27f),
                                    style = Stroke(1.1f),
                                )
                            }
                        }
                    }
                }
            }

            // Every preview also demonstrates the theme's surface and control geometry.
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).width(70.dp).height(25.dp),
                shape = RoundedCornerShape(profile.cardRadius),
                color = p.surfaceRaised,
                border = BorderStroke(profile.borderWidth, p.line),
            ) {
                Row(Modifier.padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(p.primary, RoundedCornerShape(profile.smallRadius)))
                    Spacer(Modifier.width(5.dp))
                    Box(Modifier.height(3.dp).weight(1f).background(p.foreground.copy(alpha = .55f), RoundedCornerShape(profile.smallRadius)))
                }
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(8.dp).width(34.dp).height(16.dp)
                    .background(p.primary, RoundedCornerShape(profile.buttonRadius)),
            )
        }
    }
}
