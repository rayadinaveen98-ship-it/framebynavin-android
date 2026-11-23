package com.framebynavin.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.reminders.*
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Founder-approved P1 gallery import and preview.
 * This page owns no alarm, navigation, auth, recording, playback-service, or cloud state.
 * P10D adds the three living built-in families beside existing private gallery media.
 * The previews simulate composition only; they do not post reminders or receipts.
 */
@Composable
internal fun P1GalleryBackgroundLaunchCard() {
    var open by rememberSaveable { mutableStateOf(false) }
    Surface(
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth().testTag("p1-gallery-settings-entry"),
        color = CinemaSurface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = RecRed)
            Column(Modifier.weight(1f)) {
                Text("Reminder backgrounds", color = ProjectorIvory,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Living Bloom, Pulse Studio, Rise Core · photos and videos",
                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MutedText)
        }
    }
    if (open) Dialog(
        onDismissRequest = { open = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            P1GalleryBackgroundStudio(onClose = { open = false })
        }
    }
}

@Composable
private fun P1GalleryBackgroundStudio(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeScope by rememberSaveable { mutableStateOf(P1ReminderScope.CREATOR.name) }
    val domain = P1ReminderScope.valueOf(activeScope)
    var importFor by remember { mutableStateOf(P1ReminderScope.PERSONAL) }
    var revision by remember { mutableIntStateOf(0) }
    var importing by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var fullPreview by remember { mutableStateOf(false) }
    var showFraming by rememberSaveable { mutableStateOf(false) }
    var showMediaDetails by rememberSaveable { mutableStateOf(false) }
    var confirmRemoveMedia by remember { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(ReminderCheckInKind.VOICE) }
    val selectedMedia = remember(domain, revision) { P1EntranceMediaStore.selected(context, domain) }
    val family = remember(domain, revision) { P10ReminderFamilyStore.selected(context, domain) }
    val backgroundSource = remember(domain, revision, selectedMedia?.file?.absolutePath) {
        P10ReminderBackgroundStore.selected(context, domain, selectedMedia != null)
    }
    var frame by remember(domain, revision) { mutableStateOf(P1EntranceMediaStore.framing(context, domain)) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && !importing) {
            val target = importFor
            scope.launch {
                importing = true
                feedback = null
                val result = withContext(Dispatchers.IO) {
                    P1EntranceMediaStore.importGallery(context, target, uri)
                }
                if (result.isSuccess) {
                    val activated = withContext(Dispatchers.IO) {
                        P1EntranceMediaStore.saveFraming(context, target, P1MediaFraming())
                        P10ReminderBackgroundStore.select(
                            context, target, P10ReminderBackgroundSource.GALLERY)
                    }
                    activeScope = target.name
                    revision++
                    feedback = if (activated) "Gallery background saved and selected for this reminder space."
                        else "Gallery media saved, but activation could not be confirmed. Select it again."
                } else feedback = result.exceptionOrNull()?.message
                    ?: "Could not import this gallery item."
                importing = false
            }
        }
    }
    fun useFamily(choice: P10ReminderFamily) {
        scope.launch {
            importing = true
            val changed = withContext(Dispatchers.IO) {
                val savedFamily = P10ReminderFamilyStore.select(context, domain, choice)
                val savedSource = P10ReminderBackgroundStore.select(
                    context, domain, P10ReminderBackgroundSource.BUILT_IN)
                savedFamily && savedSource
            }
            revision++
            feedback = if (changed) "${choice.label} selected. Your gallery media is kept."
                else "Could not fully save the new family choice. Try again."
            importing = false
        }
    }
    fun useGallery() {
        if (selectedMedia == null) return
        scope.launch {
            importing = true
            val changed = withContext(Dispatchers.IO) {
                P10ReminderBackgroundStore.select(
                    context, domain, P10ReminderBackgroundSource.GALLERY)
            }
            revision++
            feedback = if (changed) "Your saved gallery background is active."
                else "Could not activate gallery media."
            importing = false
        }
    }
    fun removeSavedGallery() {
        scope.launch {
            importing = true
            val removed = withContext(Dispatchers.IO) {
                val cleared = P1EntranceMediaStore.clear(context, domain)
                if (cleared) P10ReminderBackgroundStore.select(
                    context, domain, P10ReminderBackgroundSource.BUILT_IN)
                cleared
            }
            confirmRemoveMedia = false
            revision++
            feedback = if (removed) "Saved gallery background removed. ${family.label} stays selected."
                else "Could not remove the saved gallery background."
            importing = false
        }
    }
    fun persistFraming() {
        scope.launch {
            val snapshot = frame
            val saved = withContext(Dispatchers.IO) {
                P1EntranceMediaStore.saveFraming(context, domain, snapshot)
            }
            if (!saved) feedback = "Could not save the new crop position."
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("p1-gallery-studio"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory)
            }
            Column(Modifier.weight(1f)) {
                Text("REMINDER APPEARANCE", color = MutedGold, fontSize = 9.sp, letterSpacing = 1.sp)
                Text("Reminder backgrounds", color = ProjectorIvory,
                    fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("Choose your reminder space", color = ProjectorIvory, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                P1ReminderScope.CREATOR to "Creator",
                P1ReminderScope.PERSONAL to "Personal",
                P1ReminderScope.CHALLENGE_21 to "21-Day",
            ).forEach { (option, title) ->
                FilterChip(
                    selected = option == domain,
                    onClick = {
                        activeScope = option.name
                        feedback = null
                        showFraming = false
                    },
                    label = { Text(title, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).testTag("p1-scope-" + option.name),
                )
            }
        }
        P1EntrancePreview(
            domain = domain, media = selectedMedia, frame = frame, kind = mode,
            motion = true, modifier = Modifier.fillMaxWidth().height(230.dp),
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("PREVIEW TYPE", color = MutedText, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(ReminderCheckInKind.VOICE, ReminderCheckInKind.ALARM).forEach { option ->
                    FilterChip(
                        selected = mode == option, onClick = { mode = option },
                        label = { Text(if (option == ReminderCheckInKind.VOICE) "Voice" else "Alarm", fontSize = 10.sp) }
                    )
                }
            }
        }
        OutlinedButton(
            onClick = { fullPreview = true }, modifier = Modifier.fillMaxWidth()
                .testTag("p1-preview-full-screen"),
        ) {
            Icon(Icons.Outlined.Fullscreen, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("FULL-SCREEN PREVIEW")
        }
        // P9B: one background source panel, not parallel "Original" and "Gallery" cards.
        Surface(shape = RoundedCornerShape(16.dp), color = CinemaSurface) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("BACKGROUND SOURCE", color = MutedGold, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(if (selectedMedia == null) "Backlot Original" else "My Gallery",
                    color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (selectedMedia == null) "Original Backlot background is selected for this space."
                    else when (selectedMedia.kind) {
                        P1MediaKind.IMAGE -> "Your saved photo"
                        P1MediaKind.ANIMATED -> "Your saved animated image"
                        P1MediaKind.VIDEO -> "Your saved silent video"
                    },
                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                )
                Button(
                    onClick = {
                        importFor = domain
                        picker.launch(PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageAndVideo
                        ))
                    },
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth().testTag("p1-choose-gallery"),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (importing) "IMPORTING…" else
                        if (selectedMedia == null) "CHOOSE FROM GALLERY" else "REPLACE BACKGROUND")
                }
                if (selectedMedia != null) {
                    TextButton(
                        onClick = ::reset, enabled = !importing,
                        modifier = Modifier.testTag("p1-restore-original"),
                    ) { Text("USE BACKLOT ORIGINAL", color = RecRed) }
                    OutlinedButton(
                        onClick = { showFraming = !showFraming },
                        modifier = Modifier.fillMaxWidth().testTag("p1-adjust-crop"),
                    ) { Text(if (showFraming) "HIDE CROP CONTROLS" else "ADJUST CROP / POSITION") }
                    if (showFraming) {
                        Text("Zoom", color = ProjectorIvory, fontSize = 11.sp)
                        Slider(
                            value = frame.zoom, onValueChange = { frame = frame.copy(zoom = it) },
                            onValueChangeFinished = ::persistFraming, valueRange = 1f..2.5f,
                            modifier = Modifier.testTag("p1-crop-zoom"),
                        )
                        Text("Horizontal position", color = ProjectorIvory, fontSize = 11.sp)
                        Slider(
                            value = frame.horizontal,
                            onValueChange = { frame = frame.copy(horizontal = it) },
                            onValueChangeFinished = ::persistFraming, valueRange = -1f..1f,
                            modifier = Modifier.testTag("p1-crop-x"),
                        )
                        Text("Vertical position", color = ProjectorIvory, fontSize = 11.sp)
                        Slider(
                            value = frame.vertical,
                            onValueChange = { frame = frame.copy(vertical = it) },
                            onValueChangeFinished = ::persistFraming, valueRange = -1f..1f,
                            modifier = Modifier.testTag("p1-crop-y"),
                        )
                    }
                }
            }
        }
        if (domain != P1ReminderScope.CREATOR) {
            Text("Saved and previewable here. The new full-screen visual attachment for this reminder type is pending integration.",
                color = MutedGold, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.testTag("p1-host-compatibility-note"))
        }
        Text("Gallery backgrounds stay on this device. Previewing does not send an alert.",
            color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
        TextButton(
            onClick = { showMediaDetails = !showMediaDetails },
            modifier = Modifier.testTag("p9b-gallery-media-help"),
        ) { Text(if (showMediaDetails) "HIDE MEDIA DETAILS" else "MEDIA FORMATS & PRIVACY", color = MutedGold) }
        if (showMediaDetails) {
            Text("Photo, animated GIF/WebP or silent looping video (up to 15 seconds and 40 MB). Live Photos may import as still images; export motion to video when needed. Third-party live wallpapers cannot be imported directly.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
            Text("Local media only. Personal gallery content is hidden while Android is locked. No cloud uploads or changes to reminder delivery.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp)
        }
        if (!feedback.isNullOrBlank())
            Text(feedback.orEmpty(), color = MutedGold, fontSize = 11.sp,
                modifier = Modifier.testTag("p1-gallery-feedback"))
        Spacer(Modifier.height(12.dp))
    }
    if (fullPreview) Dialog(
        onDismissRequest = { fullPreview = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(CinemaBlack)) {
            P1EntrancePreview(
                domain, selectedMedia, frame, mode, motion = true,
                modifier = Modifier.fillMaxSize().testTag("p1-full-preview"),
            )
            IconButton(onClick = { fullPreview = false },
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)) {
                Icon(Icons.Outlined.Close, "Close preview", tint = Color.White)
            }
            Text("Preview only · No reminder was delivered",
                color = Color.White, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding().padding(22.dp))
        }
    }
}

@Composable
private fun P1EntrancePreview(
    domain: P1ReminderScope,
    media: P1Media?,
    frame: P1MediaFraming,
    kind: ReminderCheckInKind,
    motion: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF120910)).testTag("p1-entrance-preview")) {
        P1EntranceBackdrop(
            domain = domain, motionEnabled = motion, lowPower = false,
            modifier = Modifier.fillMaxSize(), overrideMedia = media,
            overrideFraming = frame,
        )
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("BACKLOT / " + when (domain) {
                P1ReminderScope.PERSONAL -> "PERSONAL"
                P1ReminderScope.CREATOR -> "CREATOR"
                P1ReminderScope.CHALLENGE_21 -> "21-DAY"
            }, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold,
                color = Color.White, textAlign = TextAlign.Center)
            V336DirectorSignalArt(
                kind = kind, motionEnabled = motion, lowPower = false,
                modifier = Modifier.size(width = 195.dp, height = 135.dp),
            )
            Text("DOUBLE TAP TO OPEN", fontSize = 12.sp,
                letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold,
                color = Color.White, textAlign = TextAlign.Center)
        }
    }
}
