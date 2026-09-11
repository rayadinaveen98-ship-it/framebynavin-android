package com.framebynavin.app.ui

import android.accounts.Account
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.*
import com.framebynavin.app.ui.theme.*
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.Scopes
import kotlinx.coroutines.launch

/** Fresh-install gate: existing Drive history is checked before creator setup is allowed to run. */
@Composable
internal fun V20DriveRecoveryGate(
    session: CloudSession,
    onRecovered: () -> Unit,
    onStartNew: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember(context) { Identity.getAuthorizationClient(context) }
    val vault = remember { DriveVaultManager(context.applicationContext) }
    val local = remember { DriveVaultLocalStore(context.applicationContext) }
    var token by remember { mutableStateOf(DriveVaultTokenMemory.get(session.email)) }
    var points by remember { mutableStateOf<List<DriveVaultRestorePoint>>(emptyList()) }
    var checked by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun friendlyError(t: Throwable): String {
        val raw = t.message.orEmpty()
        return when {
            raw.contains("has not been used in project", ignoreCase = true) ||
                raw.contains("accessNotConfigured", ignoreCase = true) ->
                "Google Drive backup is not available for this app configuration yet. Your phone data is safe. Enable the Drive API, then retry."
            raw.contains("401") || raw.contains("invalid authentication", ignoreCase = true) ->
                "Google Drive authorization expired. Reconnect the private Drive vault and retry."
            raw.contains("403") ->
                "Google Drive access was denied. Check the Drive permission for this Google account and retry."
            else -> "Could not inspect your Google Drive vault. Your phone data is safe."
        }
    }

    fun load(access: String) {
        busy = true
        error = null
        scope.launch {
            runCatching { vault.list(access) }
                .onSuccess { points = it; checked = true; error = null }
                .onFailure { checked = false; error = friendlyError(it) }
            busy = false
        }
    }

    val resolution = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            runCatching { client.getAuthorizationResultFromIntent(result.data!!) }
                .onSuccess { auth ->
                    val value = auth.accessToken
                    if (value.isNullOrBlank()) error = "Google Drive permission was not granted."
                    else { token = value; DriveVaultTokenMemory.put(session.email, value); load(value) }
                }
                .onFailure { error = "Google Drive authorization failed. Please retry." }
        } else error = "Google Drive authorization was cancelled."
    }

    fun authorize() {
        busy = true
        error = null
        val request = AuthorizationRequest.builder()
            .setAccount(Account(session.email, "com.google"))
            .setRequestedScopes(listOf(Scope(Scopes.DRIVE_APPFOLDER)))
            .build()
        client.authorize(request).addOnSuccessListener { auth ->
            busy = false
            if (auth.hasResolution()) {
                val pending = auth.pendingIntent
                if (pending == null) error = "Google Drive authorization needs attention."
                else resolution.launch(IntentSenderRequest.Builder(pending.intentSender).build())
            } else {
                val value = auth.accessToken
                if (value.isNullOrBlank()) error = "Google Drive permission was not granted."
                else { token = value; DriveVaultTokenMemory.put(session.email, value); load(value) }
            }
        }.addOnFailureListener {
            busy = false
            error = "Google Drive authorization failed. Please retry."
        }
    }

    LaunchedEffect(token) { token?.let(::load) }
    val recommended = DriveVaultPolicy.recommended(points)

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.CloudDone, null, tint = MutedGold, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(16.dp))
            Text("Check your creator vault", color = ProjectorIvory, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(session.email, color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "Before FrameByNavin treats this phone as a new workspace, it checks this Google account for an existing private snapshot.",
                color = MutedText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(22.dp))

            if (token == null) {
                Button(
                    onClick = ::authorize,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text(if (busy) "CONNECTING…" else "CONNECT PRIVATE DRIVE VAULT", fontWeight = FontWeight.Black) }
            } else if (!checked && error == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = RecRed)
                Spacer(Modifier.height(8.dp))
                Text("Looking for your existing FrameByNavin workspace…", color = MutedText)
            } else if (!checked && error != null) {
                Surface(
                    Modifier.fillMaxWidth(),
                    color = CinemaSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Drive vault needs attention", color = ProjectorIvory, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(5.dp))
                        Text(error.orEmpty(), color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { token?.let(::load) ?: authorize() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text(if (busy) "RETRYING…" else "RETRY", fontWeight = FontWeight.Black) }
            } else if (points.isEmpty()) {
                Surface(
                    Modifier.fillMaxWidth(),
                    color = CinemaSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No previous workspace found", color = ProjectorIvory, fontWeight = FontWeight.Black)
                        Text(
                            "This Google account has no FrameByNavin Drive snapshots yet. Creator setup can start as new.",
                            color = MutedText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        local.markRecoveryReviewed(session.email)
                        onStartNew()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("SET UP NEW WORKSPACE", fontWeight = FontWeight.Black) }
            } else if (recommended != null) {
                Surface(
                    Modifier.fillMaxWidth(),
                    color = CinemaSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MutedGold.copy(alpha = .5f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("WORKSPACE FOUND", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text("${recommended.projectCount} projects · ${recommended.ideaCount} ideas", color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(
                            "The newest meaningful snapshot is recommended. Newer empty snapshots are never preferred automatically.",
                            color = MutedText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val access = token ?: return@Button
                        busy = true
                        scope.launch {
                            runCatching { vault.restore(access, recommended) }
                                .onSuccess {
                                    local.markRecoveryReviewed(session.email)
                                    onRecovered()
                                }
                                .onFailure { error = "Could not restore this workspace. Your current phone data was not replaced." }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text(if (busy) "RESTORING…" else "RESTORE MY WORKSPACE", fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        local.markRecoveryReviewed(session.email)
                        onStartNew()
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) { Text("START FRESH INSTEAD", color = MutedText) }
            }
        }
    }
}
