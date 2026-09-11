package com.framebynavin.app.ui

import android.content.MutableContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.cloud.*
import com.framebynavin.app.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

@Composable
internal fun V23AccountOnboarding(
    onComplete: (String) -> Unit,
    onContinueLocally: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { CloudSyncManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialContext = remember(context) { MutableContextWrapper(context) }

    var session by remember { mutableStateOf(manager.localState().session) }
    var cloudProfile by remember { mutableStateOf(manager.cachedCreatorProfile()) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var displayName by rememberSaveable { mutableStateOf(session?.displayName.orEmpty()) }
    var username by rememberSaveable { mutableStateOf(cloudProfile?.username.orEmpty()) }
    var recoveryPoints by remember { mutableStateOf<List<CloudRestorePoint>?>(null) }
    var recoveryLoading by rememberSaveable { mutableStateOf(false) }
    var recoveryError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmUseThisPhone by rememberSaveable { mutableStateOf(false) }

    fun reload() {
        session = manager.localState().session
        cloudProfile = manager.cachedCreatorProfile()
        if (displayName.isBlank()) displayName = cloudProfile?.displayName.orEmpty().ifBlank { session?.displayName.orEmpty() }
        if (username.isBlank()) username = cloudProfile?.username.orEmpty()
    }

    fun returningIdentity(): Pair<CloudSession, CloudCreatorProfile>? {
        val connectedSession = manager.localState().session ?: return null
        val connectedProfile = manager.cachedCreatorProfile() ?: return null
        if (CloudCreatorAccountPolicy.route(connectedSession, connectedProfile) != CloudCreatorAccountRoute.RETURNING_CREATOR) {
            return null
        }
        return connectedSession to connectedProfile
    }

    suspend fun prepareReturningCreatorRecovery(): Boolean {
        val identity = returningIdentity() ?: return false
        recoveryLoading = true
        recoveryError = null
        val result = manager.restorePoints()
        recoveryLoading = false
        if (result.isFailure) {
            recoveryPoints = null
            recoveryError = result.exceptionOrNull()?.message ?: "Couldn't load your cloud restore points. Your cloud data was not changed."
            return true
        }

        val points = result.getOrThrow()
        if (points.isEmpty()) {
            when (val keep = manager.keepLocalData()) {
                is CloudOperationResult.Success -> onComplete(identity.second.displayName.ifBlank { identity.first.displayName })
                is CloudOperationResult.Skipped -> recoveryError = keep.message
                is CloudOperationResult.Failure -> recoveryError = keep.message
            }
        } else {
            recoveryPoints = points
        }
        return true
    }

    fun retryReturningRecovery() {
        if (busy || recoveryLoading) return
        scope.launch { prepareReturningCreatorRecovery() }
    }

    fun restoreReturningCreator(point: CloudRestorePoint) {
        if (busy) return
        busy = true
        recoveryError = null
        scope.launch {
            try {
                when (val result = manager.restore(point)) {
                    is CloudOperationResult.Success -> {
                        recoveryPoints = null
                        // The backup includes CreatorOsSettings. Passing a blank name prevents the
                        // parent account callback from overwriting restored category/platform/goal.
                        onComplete("")
                    }
                    is CloudOperationResult.Skipped -> recoveryError = result.message
                    is CloudOperationResult.Failure -> recoveryError = result.message
                }
            } finally {
                busy = false
            }
        }
    }

    fun useThisPhoneInstead() {
        if (busy) return
        val identity = returningIdentity() ?: return
        busy = true
        recoveryError = null
        scope.launch {
            try {
                when (val result = manager.keepLocalData()) {
                    is CloudOperationResult.Success ->
                        onComplete(identity.second.displayName.ifBlank { identity.first.displayName })
                    is CloudOperationResult.Skipped -> recoveryError = result.message
                    is CloudOperationResult.Failure -> recoveryError = result.message
                }
            } finally {
                busy = false
            }
        }
    }

    fun startGoogleSignIn() {
        if (busy || CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) return
        busy = true
        error = null
        scope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(CloudConfig.GOOGLE_WEB_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = credentialManager.getCredential(
                    context = credentialContext,
                    request = request,
                )
                val credential = result.credential
                val idToken = if (
                    credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                } else null

                if (idToken.isNullOrBlank()) {
                    error = "Google couldn't complete sign-in. Try again."
                } else {
                    when (val resultState = manager.completeGoogleSignIn(idToken)) {
                        is CloudOperationResult.Success -> {
                            reload()
                            if (!prepareReturningCreatorRecovery()) {
                                manager.refreshCreatorIdentity()
                                reload()
                                prepareReturningCreatorRecovery()
                            }
                        }
                        is CloudOperationResult.Skipped -> {
                            reload()
                            if (!prepareReturningCreatorRecovery()) error = resultState.message
                        }
                        is CloudOperationResult.Failure -> {
                            // Authentication and creator identity are independent from backup health.
                            // A returning creator stays in recovery rather than falling into new setup.
                            reload()
                            if (!prepareReturningCreatorRecovery()) error = resultState.message
                        }
                    }
                }
            } catch (_: GetCredentialCancellationException) {
                error = "Google sign-in cancelled"
            } catch (_: NoCredentialException) {
                error = "No Google account is available on this device."
            } catch (_: GoogleIdTokenParsingException) {
                error = "Google couldn't verify the sign-in response. Try again."
            } catch (_: GetCredentialException) {
                error = "Google sign-in failed. Try again."
            } catch (_: Throwable) {
                error = "Google sign-in failed. Try again."
            } finally {
                busy = false
            }
        }
    }

    fun claimUsername() {
        if (busy) return
        val normalized = username.trim().lowercase()
        busy = true
        error = null
        scope.launch {
            when (val result = manager.claimUsername(normalized, displayName.trim())) {
                is CloudOperationResult.Success -> {
                    reload()
                    busy = false
                    onComplete(displayName.trim().ifBlank { session?.displayName.orEmpty() })
                }
                is CloudOperationResult.Skipped -> {
                    busy = false
                    error = result.message
                }
                is CloudOperationResult.Failure -> {
                    busy = false
                    error = result.message
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (session != null) {
            if (!prepareReturningCreatorRecovery()) {
                manager.refreshCreatorIdentity()
                reload()
                prepareReturningCreatorRecovery()
            }
        }
    }

    val accountRoute = CloudCreatorAccountPolicy.route(session, cloudProfile)

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 22.dp),
        ) {
            Text("FRAMEBYNAVIN", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(34.dp))

            if (accountRoute == CloudCreatorAccountRoute.SIGN_IN_REQUIRED) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = RecRed.copy(alpha = .13f),
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonOutline, null, tint = RecRed, modifier = Modifier.size(28.dp)) } }
                Spacer(Modifier.height(18.dp))
                Text("Your creator identity starts here.", color = ProjectorIvory, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(9.dp))
                Text(
                    "Sign in with Google to connect your FrameByNavin identity and cloud backup. If this account already exists, we'll restore that identity instead of asking you to create it again.",
                    color = MutedText,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = ::startGoogleSignIn,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProjectorIvory, contentColor = CinemaBlack),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = CinemaBlack)
                    else {
                        Icon(Icons.Outlined.AccountCircle, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CONTINUE WITH GOOGLE", fontWeight = FontWeight.Black, fontSize = 10.5.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onContinueLocally,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Text("USE LOCALLY FOR NOW", color = MutedText, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(14.dp))
                Text("You can connect an account later from Profile & Account.", color = MutedText, fontSize = 8.8.sp)
            } else if (accountRoute == CloudCreatorAccountRoute.RETURNING_CREATOR) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = SuccessGreen.copy(alpha = .13f),
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CloudDone, null, tint = SuccessGreen, modifier = Modifier.size(28.dp)) } }
                Spacer(Modifier.height(18.dp))
                Text("Welcome back.", color = ProjectorIvory, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    "@${cloudProfile?.username.orEmpty()} is your existing FrameByNavin identity. Before setup continues, choose the creator-data copy you want on this phone.",
                    color = MutedText,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(22.dp))

                val points = recoveryPoints.orEmpty()
                val recommended = CloudRecoveryPolicy.recommended(points)
                when {
                    recoveryLoading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MutedGold)
                            Spacer(Modifier.width(10.dp))
                            Text("Loading your cloud restore history…", color = MutedText, fontSize = 10.sp)
                        }
                    }
                    recoveryError != null -> {
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("RESTORE HISTORY NEEDS A RETRY", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(7.dp))
                                Text(recoveryError.orEmpty(), color = ProjectorIvory, fontSize = 10.sp, lineHeight = 15.sp)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = ::retryReturningRecovery, enabled = !busy) { Text("RETRY", fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                    recommended != null -> {
                        Text("We found ${points.size} cloud restore point${if (points.size == 1) "" else "s"}.", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = CinemaSurface,
                            border = BorderStroke(1.dp, MutedGold.copy(alpha = .45f)),
                        ) {
                            Column(Modifier.padding(15.dp)) {
                                Text("RECOMMENDED RECOVERY", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(Modifier.height(7.dp))
                                Text("${recommended.projectCount} projects · ${recommended.ideaCount} ideas", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(4.dp))
                                Text("${recommended.appVersion} · ${recommended.snapshotDay}", color = MutedText, fontSize = 9.5.sp)
                                Spacer(Modifier.height(9.dp))
                                Text("Chosen as the newest backup that still contains creator work; an empty newer snapshot is never preferred automatically.", color = MutedText, fontSize = 9.sp, lineHeight = 14.sp)
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = { restoreReturningCreator(recommended) },
                                    enabled = !busy,
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                                    shape = RoundedCornerShape(15.dp),
                                ) { Text(if (busy) "RESTORING…" else "RESTORE THIS COPY", fontWeight = FontWeight.Black) }
                            }
                        }
                        if (points.size > 1) {
                            Spacer(Modifier.height(9.dp))
                            Text("Other restore points remain preserved and can be selected later in Cloud Backup.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { confirmUseThisPhone = true },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            border = BorderStroke(1.dp, CinemaLine),
                            shape = RoundedCornerShape(15.dp),
                        ) { Text("USE THIS PHONE INSTEAD", color = MutedText, fontSize = 9.5.sp, fontWeight = FontWeight.Bold) }
                    }
                    else -> {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MutedGold)
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = MutedGold.copy(alpha = .13f),
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AlternateEmail, null, tint = MutedGold, modifier = Modifier.size(28.dp)) } }
                Spacer(Modifier.height(18.dp))
                Text("Choose your creator identity.", color = ProjectorIvory, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("This Google account does not have a FrameByNavin creator ID yet. Your username is unique and does not make your projects public.", color = MutedText, fontSize = 11.5.sp, lineHeight = 17.sp)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Display name") },
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { value ->
                        username = value.lowercase().filter { it.isLetterOrDigit() || it == '_' }.take(24)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("@", color = MutedGold) },
                    label = { Text("Username") },
                    supportingText = { Text("3–24 characters · letters, numbers, underscore") },
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(9.dp))
                        Text("Projects, ideas and creator history remain private to your account.", color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = ::claimUsername,
                    enabled = !busy && username.length in 3..24,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = ProjectorIvory)
                    else {
                        Text("CREATE CREATOR ID", fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            error?.let {
                Spacer(Modifier.height(16.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), color = Color(0xFF1A1110), border = BorderStroke(1.dp, RecRed.copy(alpha = .35f))) {
                    Text(it, color = ProjectorIvory, fontSize = 9.5.sp, lineHeight = 14.sp, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }

    if (confirmUseThisPhone) {
        AlertDialog(
            onDismissRequest = { confirmUseThisPhone = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Use this phone's current data?", color = ProjectorIvory, fontWeight = FontWeight.Black) },
            text = { Text("This skips restoring an older cloud copy on this phone. Your existing cloud restore points stay preserved and are not overwritten. You may need creator setup if this installation is new.", color = MutedText, fontSize = 13.sp, lineHeight = 19.sp) },
            confirmButton = {
                TextButton(onClick = { confirmUseThisPhone = false; useThisPhoneInstead() }) {
                    Text("USE THIS PHONE", color = RecRed, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { confirmUseThisPhone = false }) { Text("GO BACK", color = MutedText) } },
        )
    }
}
