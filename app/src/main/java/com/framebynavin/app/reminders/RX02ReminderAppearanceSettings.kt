package com.framebynavin.app.reminders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One coherent approved appearance screen for Creator, Personal and Challenge.
 * Old Classic/Cinematic/Premiere selectors are intentionally absent. Historical
 * preference records remain readable for safe upgrades, never deleted here.
 * Android FSI and alarms remain owned by delivery settings.
 */
@Composable
internal fun RX02ReminderAppearanceSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentDomain by rememberSaveable { mutableStateOf(RX01ReminderDomain.PERSONAL) }
    var pendingImportDomain by remember { mutableStateOf(RX01ReminderDomain.PERSONAL) }
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf(false) }
    var previewMain by remember { mutableStateOf(false) }
    var previewMode by remember { mutableStateOf(RX01ReminderIconKind.VOICE) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val target = pendingImportDomain
            scope.launch {
                busy = true
                message = null
                val result = withContext(Dispatchers.IO) {
                    RX02EntranceMediaStore.import(context, target, uri)
                }
                if (result.isSuccess) {
                    val enabled = withContext(Dispatchers.IO) {
                        RX01ReminderScenePrefs.set(context, target, true)
                    }
                    message = if (enabled)
                        "Custom entrance background saved locally."
                    else "Media saved, but Fluid Entrance could not be enabled. Check the switch."
                    revision++
                } else {
                    message = result.exceptionOrNull()?.message
                        ?: "Android could not import this gallery item."
                }
                busy = false
            }
        }
    }

    // revision forces the truthful media badge after every successful save,
    // clear or replacement; no file path ever comes from user-visible input.
    val media = remember(currentDomain, revision) {
        RX02EntranceMediaStore.selected(context, currentDomain)
    }
    Column(
        modifier = Modifier.fillMaxWidth().testTag("rx02-reminder-appearance"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("REMINDER APPEARANCE", color = MutedGold, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Text("The entrance is yours.", color = ProjectorIvory,
            fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Choose one background per reminder type. Your double-tap text, " +
                "mic/alarm symbol and main reminder actions stay the same.",
            color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((domain, title) in listOf(
                RX01ReminderDomain.CREATOR to "Creator",
                RX01ReminderDomain.PERSONAL to "Personal",
                RX01ReminderDomain.CHALLENGE_21 to "21-Day",
            )) {
                FilterChip(
                    selected = currentDomain == domain,
                    onClick = { currentDomain = domain; message = null },
                    label = { Text(title, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).testTag(
                        "rx02-domain-" + domain.name.lowercase()
                    ),
                )
            }
        }
        if (currentDomain == RX01ReminderDomain.CHALLENGE_21 &&
            !RX01ReminderScenePrefs.enabled(context, RX01ReminderDomain.CHALLENGE_21)
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = CinemaSurface) {
                Column(Modifier.fillMaxWidth().padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("21-Day entrance is off on this device",
                        color = ProjectorIvory, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("You can enable the new entrance without granting extra Android alarm permissions. Delivery permissions remain in Notifications & Reminders.",
                        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
                    Button(
                        onClick = {
                            if (RX01ReminderScenePrefs.set(context,
                                    RX01ReminderDomain.CHALLENGE_21, true)) {
                                revision++
                                message = "Modern 21-Day entrance enabled."
                            } else message = "Could not save the 21-Day appearance choice."
                        },
                        modifier = Modifier.fillMaxWidth()
                            .testTag("backlot-enable-modern-challenge-visual"),
                    ) { Text("ENABLE MODERN 21-DAY ENTRANCE") }
                }
            }
        }

        Text("ENTRANCE BACKGROUND", color = MutedGold, fontSize = 11.sp,
            letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        // Built-in is the unmodified original RX01 palette for this domain.
        Surface(shape = RoundedCornerShape(16.dp), color = CinemaSurface) {
            Column(Modifier.fillMaxWidth().padding(11.dp)) {
                Box(Modifier.fillMaxWidth().height(116.dp)) {
                    RX01FluidReminderBackdrop(currentDomain, RX01SurfacePhase.ENTRANCE,
                        motionEnabled = false, lowPower = true,
                        modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(8.dp))
                Text("Backlot Original", color = ProjectorIvory,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    when (currentDomain) {
                        RX01ReminderDomain.CREATOR -> "Pulse Studio · cobalt and ultraviolet"
                        RX01ReminderDomain.PERSONAL -> "Living Bloom · rose and apricot"
                        RX01ReminderDomain.CHALLENGE_21 -> "Rise Core · crimson and warm amber"
                    },
                    color = MutedText, fontSize = 11.sp,
                )
                TextButton(
                    onClick = {
                        val cleared = RX02EntranceMediaStore.clear(context, currentDomain)
                        if (cleared) {
                            // Keep the approved modern entrance active after
                            // selecting the original; Challenge's OS permission
                            // controls remain separate in Delivery Settings.
                            RX01ReminderScenePrefs.set(context, currentDomain, true)
                            revision++
                            message = "Original Backlot entrance selected."
                        } else message = "Could not restore the original entrance."
                    },
                    enabled = !busy,
                    modifier = Modifier.testTag("rx02-use-original"),
                ) { Text(if (media == null) "Original selected" else "Use this original") }
            }
        }
        Surface(shape = RoundedCornerShape(16.dp), color = CinemaSurface) {
            Column(Modifier.fillMaxWidth().padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("My Gallery", color = ProjectorIvory,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Choose a photo, GIF/WebP animation, or a compatible video " +
                        "up to 15 seconds (40 MB). Video plays silently.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 17.sp)
                if (media != null) {
                    Text("Saved: " + when (media.kind) {
                        RX02MediaKind.IMAGE -> "Photo"
                        RX02MediaKind.ANIMATED -> "Animated image"
                        RX02MediaKind.VIDEO -> "Video · up to 15 seconds"
                    }, color = MutedGold, fontWeight = FontWeight.Bold,
                        fontSize = 11.sp, modifier = Modifier.testTag("rx02-media-saved"))
                }
                Button(
                    onClick = {
                        pendingImportDomain = currentDomain
                        picker.launch(PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        ))
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().testTag("rx02-pick-gallery"),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text(if (busy) "IMPORTING…" else if (media == null)
                    "CHOOSE FROM GALLERY" else "REPLACE GALLERY BACKGROUND") }
                if (media != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { preview = true },
                            modifier = Modifier.weight(1f).testTag("rx02-preview"),
                            enabled = !busy,
                        ) { Text("PREVIEW") }
                        OutlinedButton(
                            onClick = {
                                if (RX02EntranceMediaStore.clear(context, currentDomain)) {
                                    revision++
                                    message = "Gallery background removed. Original restored."
                                } else message = "Could not remove the gallery background."
                            },
                            modifier = Modifier.weight(1f).testTag("rx02-clear"),
                            enabled = !busy,
                        ) { Text("REMOVE") }
                    }
                }
                Text(
                    "Motion Photos may appear as still images. For iPhone Live " +
                        "Photos or system live wallpapers, export a compatible " +
                        "video first. Long videos must be trimmed in Gallery.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
                )
            }
        }
        if (message != null) {
            Text(message.orEmpty(), color = MutedGold, fontSize = 12.sp,
                modifier = Modifier.testTag("rx02-import-status"))
        }
        TextButton(
            onClick = { preview = true },
            modifier = Modifier.fillMaxWidth().testTag("rx02-preview-entrance"),
        ) { Text("PREVIEW FULL-SCREEN ENTRANCE") }
        Text(
            "Your gallery media is stored privately on this device and never " +
                "uploaded. It appears only on an unlocked entrance. The main " +
                "reminder screen, audio, alerts and responses are untouched.",
            color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
        )
        Spacer(Modifier.height(14.dp))
        // Move the existing optional live-alert companion control out of the
        // removed legacy collection. This preserves the same preference.
        V407ReminderCompanionPrefs.initialize(context)
        Surface(shape = RoundedCornerShape(16.dp), color = CinemaSurface,
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(13.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Companion on live reminders", color = ProjectorIvory,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Optional, brief Spark or Mira appearance without delaying reminder actions.",
                        color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Switch(
                    checked = V407ReminderCompanionPrefs.showOnLiveAlerts,
                    onCheckedChange = V407ReminderCompanionPrefs::set,
                    modifier = Modifier.testTag("backlot-reminder-companion-toggle"),
                )
            }
        }
        // Each domain has a distinct, existing RX01 MAIN palette rather than
        // exposing rejected V391/V394 collections or creating a new renderer.
        Text("MAIN REMINDER SCREEN", color = MutedGold,
            fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
        Surface(shape = RoundedCornerShape(16.dp), color = CinemaSurface,
            modifier = Modifier.fillMaxWidth().testTag("backlot-modern-main-background")) {
            Column(Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().height(132.dp)) {
                    RX01FluidReminderBackdrop(
                        domain = currentDomain,
                        phase = RX01SurfacePhase.MAIN,
                        motionEnabled = false, lowPower = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(when (currentDomain) {
                    RX01ReminderDomain.CREATOR -> "Studio Nocturne"
                    RX01ReminderDomain.PERSONAL -> "Velvet Sanctuary"
                    RX01ReminderDomain.CHALLENGE_21 -> "Ember Command"
                }, color = ProjectorIvory, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp)
                Text("A quieter, readable background for your real reminder text and actions.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
                OutlinedButton(
                    onClick = { previewMain = true },
                    modifier = Modifier.fillMaxWidth().testTag("backlot-preview-modern-main"),
                ) { Text("PREVIEW MAIN SCREEN") }
            }
        }

    }

    if (preview) {
        Dialog(onDismissRequest = { preview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black).testTag("rx02-entrance-preview")) {
                RX02EntranceBackdrop(
                    domain = currentDomain,
                    motionEnabled = true,
                    lowPower = false,
                    overrideMedia = media,
                )
                RX01ReminderEntranceHero(
                    domain = currentDomain,
                    mode = previewMode,
                    motionEnabled = true,
                    lowPower = false,
                )
                // Preview isn't a live alert and never registers a response.
                Row(
                    modifier = Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth().safeDrawingPadding().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(
                            selected = previewMode == RX01ReminderIconKind.VOICE,
                            onClick = { previewMode = RX01ReminderIconKind.VOICE },
                            label = { Text("Voice") },
                        )
                        FilterChip(
                            selected = previewMode == RX01ReminderIconKind.ALARM,
                            onClick = { previewMode = RX01ReminderIconKind.ALARM },
                            label = { Text("Alarm") },
                        )
                    }
                    TextButton(
                        onClick = { preview = false },
                        modifier = Modifier.testTag("rx02-close-preview"),
                    ) { Text("CLOSE", color = Color.White) }
                }
            }
        }
    }
    if (previewMain) {
        Dialog(onDismissRequest = { previewMain = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)
                .testTag("backlot-main-background-preview")) {
                RX01FluidReminderBackdrop(
                    domain = currentDomain, phase = RX01SurfacePhase.MAIN,
                    motionEnabled = true, lowPower = false,
                )
                Column(
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("MAIN REMINDER PREVIEW", color = ProjectorIvory,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Text("Your live reminder text and response controls appear here. This preview takes no action.",
                        color = ProjectorIvory, fontSize = 12.sp, lineHeight = 18.sp)
                }
                TextButton(
                    onClick = { previewMain = false },
                    modifier = Modifier.align(Alignment.TopEnd)
                        .safeDrawingPadding().padding(12.dp)
                        .testTag("backlot-close-main-preview"),
                ) { Text("CLOSE", color = Color.White) }
            }
        }
    }

}
