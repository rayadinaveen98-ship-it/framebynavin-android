package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
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
        Text("APPEARANCE", color = MutedGold, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(4.dp))
        Text("Choose your visual atmosphere.", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
        Text("The whole app, guide, voice orb, motion and widgets follow this theme.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
        Spacer(Modifier.height(11.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            FrameTheme.entries.forEach { theme ->
                val active = theme == selected
                val palette = theme.palette
                Surface(
                    modifier = Modifier.weight(1f).widthIn(min = 145.dp).height(112.dp).clickable {
                        VisualExperiencePrefs.setTheme(theme)
                        refreshWidgets()
                    },
                    shape = RoundedCornerShape(17.dp),
                    color = palette.surface,
                    border = BorderStroke(1.dp, if (active) palette.primary else palette.line),
                ) {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(palette.primary, palette.secondary, palette.foreground).forEach { color ->
                                    Box(Modifier.size(9.dp).background(color, CircleShape))
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            if (active) Box(Modifier.size(8.dp).background(palette.primary, CircleShape))
                        }
                        Spacer(Modifier.weight(1f))
                        if (theme == FrameTheme.LUMEN_FLOW) {
                            Box(Modifier.fillMaxWidth().height(14.dp).background(Brush.horizontalGradient(listOf(palette.primary, palette.tertiary, palette.secondary)), RoundedCornerShape(100.dp)))
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(theme.displayName, color = palette.foreground, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(theme.tagline, color = palette.muted, fontSize = 7.6.sp, lineHeight = 10.sp, maxLines = 2)
                    }
                }
            }
        }

        Spacer(Modifier.height(11.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), RoundedCornerShape(11.dp), RecRed.copy(alpha = .12f)) {
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
