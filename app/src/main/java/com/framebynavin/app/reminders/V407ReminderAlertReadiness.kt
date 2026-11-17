package com.framebynavin.app.reminders

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat

/**
 * Read-only, non-guarantee Android capability information.
 *
 * A channel that has not been created is UNKNOWN, not "Allowed". Android may
 * deny precise timing, full-screen presentation or actual sound regardless
 * of permissions. This snapshot never proves delivered, viewed or heard.
 */
internal enum class V407ReminderOwner { CREATOR, PERSONAL }
internal enum class V407Access { ALLOWED, DEGRADED, BLOCKED, UNKNOWN }

internal data class V407ReminderAccessSnapshot(
    val notifications: V407Access,
    val channel: V407Access,
    val preciseTiming: V407Access,
    val fullScreen: V407Access,
) {
    val needsAttention: Boolean get() = listOf(notifications, channel, preciseTiming, fullScreen)
        .any { it == V407Access.BLOCKED || it == V407Access.DEGRADED }

    val unknown: Boolean get() = listOf(notifications, channel, preciseTiming, fullScreen)
        .any { it == V407Access.UNKNOWN }
}

internal object V407ReminderAccessInspector {
    fun inspect(
        context: Context, owner: V407ReminderOwner, kind: ReminderCheckInKind,
    ): V407ReminderAccessSnapshot {
        val app = context.applicationContext
        val notif = runCatching {
            if (NotificationManagerCompat.from(app).areNotificationsEnabled())
                V407Access.ALLOWED else V407Access.BLOCKED
        }.getOrDefault(V407Access.UNKNOWN)
        val channel = runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) V407Access.ALLOWED
            else {
                val manager = app.getSystemService(NotificationManager::class.java)
                    ?: return@runCatching V407Access.UNKNOWN
                val channelId = when (owner) {
                    V407ReminderOwner.CREATOR -> if (kind == ReminderCheckInKind.VOICE)
                        ReminderConstants.VOICE_CHANNEL_ID
                    else ReminderConstants.ALARM_CHANNEL_ID
                    V407ReminderOwner.PERSONAL -> "backlot_personal_reminders_v1"
                }
                val c = manager.getNotificationChannel(channelId)
                    ?: return@runCatching V407Access.UNKNOWN
                when {
                    c.importance == NotificationManager.IMPORTANCE_NONE -> V407Access.BLOCKED
                    c.importance < NotificationManager.IMPORTANCE_HIGH -> V407Access.DEGRADED
                    else -> V407Access.ALLOWED
                }
            }
        }.getOrDefault(V407Access.UNKNOWN)
        val precise = runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                app.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
            ) V407Access.ALLOWED else V407Access.DEGRADED
        }.getOrDefault(V407Access.UNKNOWN)
        val full = runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                app.getSystemService(NotificationManager::class.java)
                    ?.canUseFullScreenIntent() == true
            ) V407Access.ALLOWED else V407Access.DEGRADED
        }.getOrDefault(V407Access.UNKNOWN)
        return V407ReminderAccessSnapshot(notif, channel, precise, full)
    }
}

/**
 * User-initiated expansion only, after existing Creator or Personal response
 * actions. Nothing here modifies a reminder, posts a notification, requests
 * broad permissions, schedules an alarm or stops foreground playback.
 */
@Composable
internal fun V407ReminderAccessDisclosure(
    owner: V407ReminderOwner,
    kind: ReminderCheckInKind,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var expanded by rememberSaveable(owner, kind) { mutableStateOf(false) }
    var refresh by remember(owner, kind) { mutableIntStateOf(0) }
    Column(
        modifier.testTag("ux06e-readiness-${owner.name.lowercase()}"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("ux06e-readiness-disclosure"),
        ) {
            Text(
                if (expanded) "HIDE ANDROID ALERT ACCESS" else "CHECK ANDROID ALERT ACCESS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (expanded) {
            val snapshot = remember(owner, kind, refresh) {
                runCatching {
                    V407ReminderAccessInspector.inspect(context, owner, kind)
                }.getOrNull()
            }
            Surface(
                color = V336DirectorPalette.panel,
                border = BorderStroke(1.dp, V336DirectorPalette.line),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
                    .testTag("ux06e-readiness-details"),
            ) {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        when {
                            snapshot == null -> "Android access could not be checked."
                            snapshot.needsAttention ->
                                "Some Android settings may delay or limit this alert."
                            snapshot.unknown ->
                                "Some access details are not available yet."
                            else -> "Access appears permitted. Actual delivery is not guaranteed."
                        },
                        color = V336DirectorPalette.ivory, fontSize = 12.sp,
                    )
                    if (snapshot != null) {
                        V407AccessRow("App notifications", snapshot.notifications)
                        V407AccessRow("This alert channel", snapshot.channel)
                        V407AccessRow("Precise timing", snapshot.preciseTiming)
                        V407AccessRow("Full-screen presentation", snapshot.fullScreen)
                    }
                    Text(
                        "A posted alert is not proof it was seen. An audio service " +
                            "request is not proof speech or sound was heard. " +
                            "Android may show a heads-up notification instead of full-screen.",
                        color = V336DirectorPalette.muted, fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { refresh++ }) {
                            Text("RECHECK", fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            V196ReminderLaunchReadiness.openNotificationSettings(context)
                        }) {
                            Text("NOTIFICATION SETTINGS", fontSize = 11.sp)
                        }
                    }
                    if (snapshot?.fullScreen != V407Access.ALLOWED &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        OutlinedButton(
                            onClick = {
                                V196ReminderLaunchReadiness.openFullScreenSettings(context)
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("FULL-SCREEN ACCESS SETTINGS", fontSize = 11.sp)
                        }
                    }
                    if (snapshot?.preciseTiming != V407Access.ALLOWED &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                        Uri.parse("package:" + context.packageName),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }.onFailure {
                                    // Only open Settings after the user deliberately taps.
                                    runCatching {
                                        context.startActivity(Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:" + context.packageName),
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("PRECISE ALARM SETTINGS", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V407AccessRow(label: String, value: V407Access) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = V336DirectorPalette.muted, fontSize = 11.sp)
        Text(
            when (value) {
                V407Access.ALLOWED -> "Allowed"
                V407Access.DEGRADED -> "Limited"
                V407Access.BLOCKED -> "Blocked"
                V407Access.UNKNOWN -> "Not known"
            },
            color = V336DirectorPalette.ivory,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
