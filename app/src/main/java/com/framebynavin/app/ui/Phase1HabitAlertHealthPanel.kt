package com.framebynavin.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.framebynavin.app.personal.Phase1HabitAlertReadinessInspector
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitDeliveryEvidencePolicy
import com.framebynavin.app.personal.Phase1HabitDeliveryHealth
import com.framebynavin.app.personal.Phase1HabitDeliveryLedger
import com.framebynavin.app.personal.Phase1HabitDeliveryStage
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import com.framebynavin.app.ui.theme.BacklotSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Small disclosure in the live challenge screen: read-only health, real
 * journal evidence and explicit repair. No simulated delivery or Done state.
 */
@Composable
internal fun Phase1HabitAlertHealthPanel(
    challenge: Phase1HabitChallenge,
    now: Instant,
    onRepair: () -> Unit,
) {
    val kind = challenge.alertKind ?: return
    val context = LocalContext.current
    var expanded by remember(challenge.id) { mutableStateOf(false) }
    var refresh by remember(challenge.id) { mutableStateOf(0) }
    val readiness = remember(challenge.id, kind, now.epochSecond / 30, refresh) {
        runCatching { Phase1HabitAlertReadinessInspector.inspect(context, kind) }.getOrNull()
    }
    val evidence = remember(challenge.id, now.epochSecond / 30, refresh) {
        runCatching { Phase1HabitDeliveryLedger(context).recent() }
    }
    val entries = evidence.getOrNull().orEmpty().filter { it.challengeId == challenge.id }
    val occurrence = entries.lastOrNull {
        it.stage != Phase1HabitDeliveryStage.ARMED_EXACT &&
            it.stage != Phase1HabitDeliveryStage.ARMED_INEXACT
    }?.scheduledAtMillis ?: entries.maxOfOrNull { it.scheduledAtMillis }
    val health = occurrence?.let {
        Phase1HabitDeliveryEvidencePolicy.health(entries, challenge.id, it,
            now.toEpochMilli())
    }
    val shortStatus = when {
        readiness == null -> "Android permissions could not be read."
        readiness.needsAttention -> "Android alert settings need attention."
        health == Phase1HabitDeliveryHealth.PERMISSION_BLOCKED ->
            "A previous alert was blocked by notification settings."
        health == Phase1HabitDeliveryHealth.NOTIFICATION_FAILED ||
            health == Phase1HabitDeliveryHealth.VOICE_UNAVAILABLE ->
            "A previous alert reported a delivery or voice problem."
        else -> "Tap to inspect Android access and delivery evidence."
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 3.dp),
        color = BacklotSurface,
        border = BorderStroke(1.dp, BacklotBorder),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ALERT HEALTH", color = BacklotPrimaryText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 10.dp))
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "CLOSE" else "DETAILS", color = BacklotAccent)
                }
            }
            Text(shortStatus, color = BacklotSecondaryText,
                style = MaterialTheme.typography.bodySmall)
            if (expanded) {
                if (readiness != null) {
                    Text(
                        "App notifications: " +
                            if (readiness.appNotificationsAllowed) "Allowed" else "Blocked",
                        color = BacklotPrimaryText)
                    Text(
                        "21-day notification channel: " +
                            if (readiness.channelAllowed) "Allowed" else "Blocked",
                        color = BacklotPrimaryText)
                    Text(
                        "Exact alarms: " +
                            if (readiness.exactAlarmAllowed) "Allowed" else
                                "Not allowed (Android may delay an inexact alarm)",
                        color = BacklotPrimaryText)
                    if (readiness.batteryOptimizationExempt == false) {
                        Text("Battery optimization is active; background timing may vary.",
                            color = BacklotSecondaryText,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                val description = when (health) {
                    null, Phase1HabitDeliveryHealth.NOT_RECORDED ->
                        "No delivery evidence recorded yet."
                    Phase1HabitDeliveryHealth.ARMED ->
                        "Scheduled with Android; not yet due."
                    Phase1HabitDeliveryHealth.OS_DELIVERY_UNCONFIRMED ->
                        "Scheduled time has passed, but no OS-fire event is recorded. " +
                            "This does not prove the alert was missed."
                    Phase1HabitDeliveryHealth.FIRED_PRESENTATION_UNCONFIRMED ->
                        "Android fired the receiver; presentation is not confirmed."
                    Phase1HabitDeliveryHealth.NOTIFICATION_POSTED ->
                        "Notification posted. This does not prove you saw it."
                    Phase1HabitDeliveryHealth.PERMISSION_BLOCKED ->
                        "Android notification permission or the alert channel was blocked."
                    Phase1HabitDeliveryHealth.NOTIFICATION_FAILED ->
                        "Notification posting failed; check permissions and re-arm."
                    Phase1HabitDeliveryHealth.VOICE_REQUESTED ->
                        "Voice service was requested; speech not confirmed."
                    Phase1HabitDeliveryHealth.VOICE_SPEAKING ->
                        "Text-to-Speech accepted an announcement; audibility is not verified."
                    Phase1HabitDeliveryHealth.VOICE_UNAVAILABLE ->
                        "Android restricted voice or Text-to-Speech failed."
                    Phase1HabitDeliveryHealth.STALE_IGNORED ->
                        "An outdated scheduled alert was safely ignored."
                }
                val whenText = occurrence?.let {
                    DateTimeFormatter.ofPattern("d MMM, h:mm a")
                        .format(Instant.ofEpochMilli(it).atZone(ZoneId.of(challenge.zoneId)))
                } ?: "None"
                Text("Most recent tracked alert: $whenText",
                    color = BacklotSecondaryText,
                    style = MaterialTheme.typography.bodySmall)
                Text(description, color = BacklotPrimaryText,
                    style = MaterialTheme.typography.bodySmall)
                if (evidence.isFailure) {
                    Text("Local diagnostic history cannot be read; your challenge is unchanged.",
                        color = BacklotSecondaryText)
                }
                TextButton(onClick = {
                    refresh++
                    onRepair()
                }) { Text("RECHECK & RE-ARM", color = BacklotAccent) }
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + context.packageName)))
                    }
                }) { Text("ANDROID APP SETTINGS", color = BacklotAccent) }
            }
        }
    }
}
