package com.framebynavin.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalDeliveryCoordinator
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V286PersonalReadinessPolicy
import com.framebynavin.app.personal.V286PersonalReadinessReader
import com.framebynavin.app.personal.V286ReadinessSeverity
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import kotlinx.coroutines.launch

/**
 * Explicitly acknowledges the OS constraint: data saved != scheduled != posted
 * != viewed. This card observes only platform settings and personal records.
 */
@Composable
internal fun V286PersonalReadinessCard(
    reminders: List<PersonalReminder>,
    permissionRevision: Int,
    enabled: Boolean,
    onRecheck: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val snapshot = remember(context, reminders, permissionRevision) {
        V286PersonalReadinessReader.read(context, reminders)
    }
    val signals = remember(snapshot) { V286PersonalReadinessPolicy.signals(snapshot) }

    fun openSettings(intent: Intent) {
        val appSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:" + context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }.onFailure {
            runCatching { context.startActivity(appSettings) }
                .onFailure { feedback = "Android settings could not be opened on this device." }
        }
    }

    Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, CinemaLine),
        modifier = Modifier.fillMaxWidth().testTag("v286-personal-readiness"),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("REMINDER DELIVERY HEALTH", color = MutedGold,
                fontWeight = FontWeight.Black, fontSize = 10.sp)
            Text(V286PersonalReadinessPolicy.summary(signals), color = ProjectorIvory,
                fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.testTag("v286-readiness-summary"))
            Text("Permissions are necessary, not a guarantee that Android displayed an alert.",
                color = MutedText, fontSize = 10.sp)
            signals.forEach { status ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        when (status.severity) {
                            V286ReadinessSeverity.OK -> "OK"
                            V286ReadinessSeverity.REVIEW -> "CHECK"
                            V286ReadinessSeverity.NOTE -> "INFO"
                        },
                        color = if (status.severity == V286ReadinessSeverity.REVIEW) RecRed else MutedGold,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    )
                    Column {
                        Text(status.label, color = ProjectorIvory, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold)
                        Text(status.detail, color = MutedText, fontSize = 10.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                    },
                    modifier = Modifier.testTag("v286-open-notification-settings"),
                ) { Text("NOTIFICATIONS", fontSize = 10.sp) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    snapshot.exactTimingAllowed != true) {
                    OutlinedButton(
                        onClick = {
                            openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:" + context.packageName)))
                        },
                        modifier = Modifier.testTag("v286-open-exact-settings"),
                    ) { Text("PRECISE ALARMS", fontSize = 10.sp) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    snapshot.activeAlarmCount > 0 && snapshot.fullScreenAlarmAllowed != true) {
                    OutlinedButton(
                        onClick = {
                            openSettings(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                                .setData(Uri.parse("package:" + context.packageName)))
                        },
                        modifier = Modifier.testTag("v286-open-fullscreen-settings"),
                    ) { Text("FULL-SCREEN ALARMS", fontSize = 10.sp) }
                }
            }
            if (reminders.isNotEmpty()) {
                Button(
                    enabled = enabled && !busy,
                    onClick = {
                        if (busy) return@Button
                        busy = true
                        scope.launch {
                            try {
                                PersonalDeliveryCoordinator(context).reconcile()
                                feedback = "Requested a reminder reschedule. Android " +
                                    "delivery may still be delayed or blocked; test on this phone."
                            } catch (_: Exception) {
                                feedback = "Rescheduling could not be completed. " +
                                    "Existing reminders were not deleted."
                            } finally {
                                busy = false
                                onRecheck()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    modifier = Modifier.fillMaxWidth().testTag("v286-recheck-scheduling"),
                ) { Text(if (busy) "RECHECKING..." else "RECHECK REMINDER SCHEDULING") }
            }
            feedback?.let {
                Text(it, color = MutedGold, fontSize = 11.sp,
                    modifier = Modifier.testTag("v286-scheduling-feedback"))
            }
        }
    }
}
