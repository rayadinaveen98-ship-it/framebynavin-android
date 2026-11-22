package com.framebynavin.app.reminders

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.Phase1HabitAlertKind
import com.framebynavin.app.personal.Phase1HabitNotification
import com.framebynavin.app.personal.RX01ChallengePresentationPrefs
import com.framebynavin.app.personal.RX01ChallengePresentationPolicy

/**
 * Compatibility route for existing settings links. Do not resurrect the
 * old selector or allow switches to fall through to rejected Premiere /
 * Classic scenes. The approved RX02 screen now owns all appearance choices.
 * Challenge full-screen alarm permission controls remain separate below.
 */
@Composable
internal fun RX01ReminderSceneSettings() {
    RX02ReminderAppearanceSettings()
}

/**
 * The Challenge FSI decision is isolated from the scene toggle:
 * owner opt-in != actual Android entitlement. Only existing ALARM channel
 * delivery can request it. Voice / Quiet cannot auto-launch full screen.
 */
@Composable
internal fun RX01ChallengeFullScreenSettings() {
    val context = LocalContext.current
    var optedIn by remember {
        mutableStateOf(RX01ChallengePresentationPrefs.fullScreenAlarmOptIn(context))
    }
    var saveFailed by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var launchFailed by remember { mutableStateOf(false) }
    val manager = remember(context) {
        context.getSystemService(NotificationManager::class.java)
    }
    val fsiAllowed = remember(context, refresh) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            runCatching { manager?.canUseFullScreenIntent() == true }.getOrDefault(false)
    }
    val highChannel = remember(context, refresh) {
        runCatching {
            manager?.getNotificationChannel(
                Phase1HabitNotification.channel(Phase1HabitAlertKind.ALARM)
            )?.importance?.let { it >= NotificationManager.IMPORTANCE_HIGH } == true
        }.getOrDefault(false)
    }
    val notificationsAllowed = remember(context, refresh) {
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val fluidEnabled = RX01ReminderScenePrefs.enabled(context, RX01ReminderDomain.CHALLENGE_21)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("21-DAY FULL-SCREEN ALARM", color = V336DirectorPalette.ivory,
            fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            "Optional for 21-Day Alarm reminders only. When enabled, Backlot " +
                "may request a full-screen alert if Android grants access. " +
                "Android can still show a notification instead. Voice and Quiet " +
                "reminders never request automatic full-screen launch.",
            color = V336DirectorPalette.muted, fontSize = 12.sp, lineHeight = 17.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Request full-screen for Challenge alarms",
                    color = V336DirectorPalette.ivory, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    if (optedIn) "Requested when allowed by Android" else "Off by default",
                    color = V336DirectorPalette.muted, fontSize = 11.sp,
                )
            }
            Switch(
                checked = optedIn,
                onCheckedChange = { enabled ->
                    saveFailed = !RX01ChallengePresentationPrefs
                        .setFullScreenAlarmOptIn(context, enabled)
                    if (!saveFailed) optedIn = enabled
                },
                modifier = Modifier.testTag("rx01-challenge-fsi-opt-in")
                    .semantics {
                        contentDescription = "Request Android full screen for 21-day Challenge alarms"
                    },
            )
        }
        if (saveFailed) Text("Couldn't save this preference. Retry.",
            color = V336DirectorPalette.hot, fontSize = 11.sp)
        Text(
            "Android access: " + if (fsiAllowed) "Allowed" else "Not granted",
            color = V336DirectorPalette.ivory, fontSize = 11.sp,
        )
        Text(
            "Challenge Alarm notification channel: " +
                if (highChannel) "High importance" else "Disabled, lowered or not created",
            color = V336DirectorPalette.muted, fontSize = 11.sp,
        )
        Text(
            "Notifications: " + if (notificationsAllowed) "Allowed" else "Blocked",
            color = V336DirectorPalette.muted, fontSize = 11.sp,
        )
        if (optedIn && (!fsiAllowed || !highChannel || !notificationsAllowed || !fluidEnabled)) {
            Text(
                "Full-screen delivery is not currently eligible. Review Android " +
                    "access and keep the Rise Core entrance enabled. Normal " +
                    "notification fallback remains available when allowed.",
                color = V336DirectorPalette.hot,
                fontSize = 11.sp, lineHeight = 16.sp,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    launchFailed = !runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = Uri.parse("package:" + context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }.isSuccess
                },
            ) {
                Text("OPEN ANDROID FULL-SCREEN ACCESS", fontSize = 11.sp)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    launchFailed = !runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }.isSuccess
                },
            ) { Text("NOTIFICATION SETTINGS", fontSize = 11.sp) }
            Button(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) {
                Text("REFRESH ANDROID ACCESS STATUS", fontSize = 11.sp)
            }
        }
        if (launchFailed) Text(
            "Android settings couldn't be opened on this device.",
            color = V336DirectorPalette.hot,
            fontSize = 11.sp,
        )
    }
}
