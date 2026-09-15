package com.framebynavin.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.R
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory

internal enum class BacklotAppIcon(
    val displayName: String,
    val description: String,
    @DrawableRes val previewRes: Int,
    val aliasSuffix: String,
) {
    CINEMATIC("Cinematic", "The signature illuminated Backlot gateway.", R.drawable.backlot_icon_cinematic, "BacklotCinematicAlias"),
    MINIMAL("Minimal", "Reduced gold mark for a quieter home screen.", R.drawable.backlot_icon_minimal, "BacklotMinimalAlias"),
    LIGHT("Light", "Ivory edition with the dark Backlot mark.", R.drawable.backlot_icon_light, "BacklotLightAlias"),
    HORIZONTAL("Horizontal", "A compact wide-lockup inspired edition.", R.drawable.backlot_icon_horizontal, "BacklotHorizontalAlias"),
    ;

    fun componentName(packageName: String): String = "$packageName.launcher.$aliasSuffix"
}

internal object BacklotAppIconManager {
    private const val PREFS = "backlot_launcher_identity"
    private const val KEY = "selected_icon"

    fun current(context: Context): BacklotAppIcon {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return BacklotAppIcon.entries.firstOrNull { it.name == stored } ?: BacklotAppIcon.CINEMATIC
    }

    fun set(context: Context, icon: BacklotAppIcon) {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager

        // Enable the destination first so there is never a moment with no launcher entry.
        packageManager.setComponentEnabledSetting(
            ComponentName(appContext, icon.componentName(appContext.packageName)),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        BacklotAppIcon.entries.filterNot { it == icon }.forEach { candidate ->
            packageManager.setComponentEnabledSetting(
                ComponentName(appContext, candidate.componentName(appContext.packageName)),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, icon.name).apply()
    }
}

@Composable
internal fun V133AppIconPicker() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(BacklotAppIconManager.current(context)) }

    Column(Modifier.fillMaxWidth()) {
        Text("APP ICON", color = MutedGold, fontSize = 8.6.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(4.dp))
        Text("Choose how Backlot appears on your home screen.", color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Text("Your launcher may take a moment to refresh after switching.", color = MutedText, fontSize = 8.3.sp)
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BacklotAppIcon.entries.chunked(2).forEach { rowIcons ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowIcons.forEach { icon ->
                        val active = icon == selected
                        Surface(
                            modifier = Modifier.weight(1f).clickable {
                                runCatching { BacklotAppIconManager.set(context, icon) }
                                    .onSuccess {
                                        selected = icon
                                        Toast.makeText(context, "Backlot icon: ${icon.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(context, "Could not change icon on this launcher.", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, if (active) MutedGold else CinemaLine),
                        ) {
                            Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                    Image(painterResource(icon.previewRes), contentDescription = "${icon.displayName} Backlot icon", modifier = Modifier.size(54.dp))
                                }
                                Spacer(Modifier.height(7.dp))
                                Text(icon.displayName, color = ProjectorIvory, fontSize = 9.7.sp, fontWeight = FontWeight.Bold)
                                Text(icon.description, color = MutedText, fontSize = 7.2.sp, lineHeight = 9.2.sp, maxLines = 2)
                            }
                        }
                    }
                    if (rowIcons.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

