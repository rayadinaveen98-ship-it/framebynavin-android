package com.framebynavin.app.cloud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CloudSyncActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FrameByNavinTheme { CloudSyncScreen(onClose = ::finish) } }
    }
}

@Composable
private fun CloudSyncScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { CloudSyncManager(context.applicationContext) }
    var state by remember { mutableStateOf(manager.localState()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var restoreTarget by remember { mutableStateOf<CloudRestorePoint?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun reloadPoints() {
        state = manager.localState()
        if (state.session != null) {
            scope.launch {
                manager.restorePoints().onSuccess { state = manager.localState().copy(restorePoints = it) }
            }
        }
    }

    fun runOperation(block: suspend () -> CloudOperationResult) {
        if (busy) return
        busy = true
        scope.launch {
            val result = block()
            message = when (result) {
                is CloudOperationResult.Success -> result.message
                is CloudOperationResult.Skipped -> result.message
                is CloudOperationResult.Failure -> result.message
            }
            busy = false
            reloadPoints()
        }
    }

    val googleOptions = remember {
        if (CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) null
        else GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(CloudConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleClient = remember(googleOptions) { googleOptions?.let { GoogleSignIn.getClient(context, it) } }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val account = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
        }.getOrElse {
            message = if (it is ApiException && it.statusCode == 12501) "Google sign-in cancelled" else "Google sign-in failed"
            null
        }
        val idToken = account?.idToken
        if (!idToken.isNullOrBlank()) runOperation { manager.completeGoogleSignIn(idToken) }
        else if (account != null) message = "Google did not return an ID token"
    }

    LaunchedEffect(Unit) { reloadPoints() }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Column(Modifier.weight(1f)) {
                    Text("CLOUD", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text("Sync & Backup", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Your phone stays primary.", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                "FrameByNavin keeps working offline. Cloud Sync protects Creator OS data and makes a future phone restore possible.",
                color = MutedText,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(20.dp))

            if (state.session == null) {
                CloudCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                            Text("G", color = Color(0xFF202124), fontWeight = FontWeight.Black, fontSize = 17.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("FrameByNavin Account", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Google identity only · no Gmail inbox access", color = MutedText, fontSize = 8.7.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
                        Text(
                            "One-time Google Web OAuth client setup is still required before account sign-in can be enabled.",
                            color = MutedGold,
                            fontSize = 9.5.sp,
                            lineHeight = 14.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Button(
                        onClick = { googleClient?.let { googleLauncher.launch(it.signInIntent) } },
                        enabled = googleClient != null && !busy,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProjectorIvory, contentColor = CinemaBlack),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text("CONTINUE WITH GOOGLE", fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            } else {
                val session = state.session!!
                CloudCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).background(RecRed.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Person, null, tint = RecRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(session.displayName.ifBlank { "FrameByNavin" }, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(session.email, color = MutedText, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("CONNECTED", color = SuccessGreen, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(Modifier.height(12.dp))
                CloudCard {
                    CloudToggleRow(
                        title = "Cloud Sync",
                        subtitle = "Automatically protect your Creator OS data",
                        checked = state.settings.enabled,
                        onChecked = { manager.setEnabled(it); state = manager.localState() },
                    )
                    HorizontalDivider(color = CinemaLine, modifier = Modifier.padding(vertical = 12.dp))
                    CloudToggleRow(
                        title = "Wi-Fi only",
                        subtitle = "Wait for Wi-Fi before automatic uploads",
                        checked = state.settings.wifiOnly,
                        onChecked = { manager.setWifiOnly(it); state = manager.localState() },
                    )
                    Spacer(Modifier.height(15.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("LAST SUCCESSFUL SYNC", color = MutedText, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                            Text(cloudTime(state.settings.lastSyncAtMillis), color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { runOperation { manager.syncNow(force = true) } },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            shape = RoundedCornerShape(13.dp),
                        ) { Text(if (busy) "WORKING…" else "SYNC NOW", fontSize = 8.5.sp, fontWeight = FontWeight.Black) }
                    }
                    if (state.settings.lastError.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(state.settings.lastError, color = MutedGold, fontSize = 8.7.sp, lineHeight = 13.sp)
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text("RESTORE POINTS", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(8.dp))
                if (state.restorePoints.isEmpty()) {
                    CloudCard { Text("No cloud restore point yet. Tap Sync Now to create the first one.", color = MutedText, fontSize = 9.5.sp) }
                } else {
                    state.restorePoints.forEachIndexed { index, point ->
                        CloudRestoreRow(point = point, onClick = { restoreTarget = point })
                        if (index != state.restorePoints.lastIndex) Spacer(Modifier.height(7.dp))
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text("ACCOUNT & DATA", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Spacer(Modifier.height(8.dp))
                CloudCard {
                    CloudActionRow(Icons.Outlined.DeleteOutline, "Delete cloud data", "Keeps everything on this phone") { confirmDelete = true }
                    HorizontalDivider(color = CinemaLine, modifier = Modifier.padding(vertical = 9.dp))
                    CloudActionRow(Icons.Outlined.Logout, "Sign out", "Cloud Sync turns off; local data stays") { runOperation { manager.signOut() } }
                }
            }

            message?.let {
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = CinemaSurfaceRaised, border = BorderStroke(1.dp, CinemaLine)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, null, tint = MutedGold, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(9.dp))
                        Text(it, color = ProjectorIvory, fontSize = 9.3.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { message = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Close, null, tint = MutedText, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
        }
    }

    restoreTarget?.let { point ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Restore this backup?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This replaces Creator OS data on this phone with the ${cloudPointLabel(point)} restore point. A rollback copy is created locally first.", color = MutedText, fontSize = 10.sp) },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null; runOperation { manager.restore(point) } }) { Text("RESTORE", color = RecRed, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { restoreTarget = null }) { Text("CANCEL", color = MutedText) } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Delete cloud data?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("Cloud backups and device records will be removed. Nothing stored locally on this phone will be deleted.", color = MutedText, fontSize = 10.sp) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; runOperation { manager.deleteCloudData() } }) { Text("DELETE CLOUD DATA", color = RecRed, fontWeight = FontWeight.Black) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("CANCEL", color = MutedText) } },
        )
    }
}

@Composable
private fun CloudCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun CloudToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MutedText, fontSize = 8.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = ProjectorIvory, checkedTrackColor = RecRed),
        )
    }
}

@Composable
private fun CloudRestoreRow(point: CloudRestorePoint, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = CinemaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(MutedGold.copy(alpha = .10f), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CloudDone, null, tint = MutedGold, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(cloudPointLabel(point), color = ProjectorIvory, fontSize = 10.7.sp, fontWeight = FontWeight.Bold)
                Text("${point.projectCount} projects · ${point.ideaCount} ideas · ${point.activeReminderCount} active reminders", color = MutedText, fontSize = 8.3.sp)
            }
            Icon(Icons.Outlined.Restore, null, tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CloudActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MutedText, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ProjectorIvory, fontSize = 10.7.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MutedText, fontSize = 8.3.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = MutedText, modifier = Modifier.size(17.dp))
    }
}

private fun cloudPointLabel(point: CloudRestorePoint): String = when (point.kind) {
    "latest" -> "Latest · ${cloudTime(point.capturedAtMillis)}"
    "daily" -> "${point.snapshotDay.ifBlank { "Daily" }} · ${cloudTime(point.capturedAtMillis)}"
    else -> "Safety snapshot · ${cloudTime(point.capturedAtMillis)}"
}

private fun cloudTime(millis: Long): String = if (millis <= 0L) "Not synced yet"
else SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
