package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.framebynavin.app.personal.PersonalDietFitnessPlanV1
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * One-time opt-in to a private, user-approved schedule. No automatic seeding,
 * cloud request, CreatorTask creation or existing reminder replacement.
 */
@Composable
internal fun V284DietFitnessSetupCard(
    reminders: List<PersonalReminder>,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val templates = remember {
        val zone = ZoneId.systemDefault()
        PersonalDietFitnessPlanV1.templates(LocalDate.now(zone), zone)
    }
    val ids = remember(templates) { templates.mapTo(HashSet()) { it.id } }
    val installedCount = reminders.count { it.id in ids }
    val missingCount = templates.size - installedCount
    var showConfirmation by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
        modifier = Modifier.fillMaxWidth().testTag("v284-diet-fitness-setup"),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("DIET & FITNESS  ·  V1.0", color = MutedGold,
                fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text("Your weekly home-food routine", color = ProjectorIvory,
                fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Daily meals with weekday-specific recipes, morning exercise, 5 PM walks, " +
                    "sleep preparation and weight check-ins.",
                color = MutedText, fontSize = 12.sp,
            )
            Text("$installedCount of ${templates.size} reminder slots saved",
                color = MutedGold, fontSize = 11.sp,
                modifier = Modifier.testTag("v284-plan-progress"))
            if (missingCount > 0) {
                Button(
                    onClick = { showConfirmation = true },
                    enabled = enabled && !installing,
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    modifier = Modifier.fillMaxWidth().testTag("v284-preview-plan"),
                ) {
                    Text(if (installedCount == 0) "REVIEW & INSTALL ROUTINE"
                        else "REVIEW & ADD $missingCount MISSING REMINDERS")
                }
            } else {
                Text("Plan saved. Edit, pause or delete any reminder below.",
                    color = ProjectorIvory, fontSize = 11.sp)
            }
            message?.let {
                Text(it, color = MutedGold, fontSize = 11.sp,
                    modifier = Modifier.testTag("v284-install-feedback"))
            }
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!installing) showConfirmation = false },
            title = { Text("Install Diet & Fitness V1.0?") },
            text = {
                Column(
                    Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("Adds $missingCount new recurring Personal Reminders (not Creator projects).")
                    Text("Meals: 8:15 AM breakfast, 11 AM fruit, 1:30 PM lunch, " +
                        "4:30 PM snack and 7:30 PM dinner. The menus change by weekday.")
                    Text("Activity: 7:15 AM strength Mon/Wed/Fri, mobility Tue/Thu; " +
                        "5 PM walking Mon–Sat. Wake 7 AM, weigh-in Mon/Wed/Fri/Sun, " +
                        "sleep wind-down 10:15 PM and Sunday progress review.")
                    Text("Wake-up uses an alarm; activities and wind-down use voice " +
                        "(3 spoken cycles, default piano). Meals use normal notifications. " +
                        "Meals snooze 15 min; other reminders 10 min.")
                    Text("Full-screen alarm availability, speech and timing depend on Android " +
                        "permissions and device restrictions. Voice alerts do not automatically " +
                        "open full-screen. Automatic retry is OFF.")
                    Text("Existing reminders and any custom edits are preserved. " +
                        "You can change or pause each new reminder afterward.")
                }
            },
            confirmButton = {
                Button(
                    enabled = !installing && enabled,
                    onClick = {
                        if (installing) return@Button
                        installing = true
                        scope.launch {
                            try {
                                val added = PersonalDietFitnessPlanV1.install(context)
                                message = if (added == 0) "All plan slots were already saved."
                                    else "$added reminders saved. Enable notifications and " +
                                        "exact alarm access for reliable delivery."
                                showConfirmation = false
                            } catch (error: Exception) {
                                message = "Could not finish setup. Your existing reminders " +
                                    "were retained. Review the saved count and retry."
                                showConfirmation = false
                            } finally {
                                installing = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("v284-confirm-install"),
                ) {
                    if (installing) CircularProgressIndicator()
                    else Text("ADD REMINDERS")
                }
            },
            dismissButton = {
                TextButton(enabled = !installing, onClick = { showConfirmation = false }) {
                    Text("CANCEL")
                }
            },
            modifier = Modifier.testTag("v284-confirm-dialog"),
        )
    }
}
