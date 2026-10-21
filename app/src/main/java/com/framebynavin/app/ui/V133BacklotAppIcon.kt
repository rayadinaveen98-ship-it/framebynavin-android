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
    CINEMATIC("Backlot", "The approved Backlot creator emblem.", R.drawable.backlot_v198_approved_icon, "BacklotCinematicAlias"),
    MINIMAL("Backlot", "The approved Backlot creator emblem.", R.drawable.backlot_v198_approved_icon, "BacklotMinimalAlias"),
    LIGHT("Backlot", "The approved Backlot creator emblem.", R.drawable.backlot_v198_approved_icon, "BacklotLightAlias"),
    HORIZONTAL("Backlot", "The approved Backlot creator emblem.", R.drawable.backlot_v198_approved_icon, "BacklotHorizontalAlias"),
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

/**
 * One canonical approved B mark. Existing icon aliases/prefs remain compatible
 * with upgrades, but all alias drawables and previews now use the same new logo.
 */
@Composable
internal fun V133AppIconPicker() {
    Column(Modifier.fillMaxWidth()) {
        Text("APP ICON", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.backlot_v198_approved_icon),
                    contentDescription = "Backlot app icon",
                    modifier = Modifier.size(62.dp).clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.size(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Backlot", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text("Built for Creators", color = MutedText, fontSize = 11.sp)
                }
            }
        }
    }
}
