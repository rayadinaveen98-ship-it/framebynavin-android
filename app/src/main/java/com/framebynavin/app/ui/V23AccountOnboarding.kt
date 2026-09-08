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

    fun reload() {
        session = manager.localState().session
        cloudProfile = manager.cachedCreatorProfile()
        if (displayName.isBlank()) displayName = cloudProfile?.displayName.orEmpty().ifBlank { session?.displayName.orEmpty() }
        if (username.isBlank()) username = cloudProfile?.username.orEmpty()
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
                            manager.refreshCreatorProfile()
                            reload()
                        }
                        is CloudOperationResult.Skipped -> error = resultState.message
                        is CloudOperationResult.Failure -> error = resultState.message
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
            manager.refreshCreatorProfile()
            reload()
        }
    }

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

            if (session == null) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = RecRed.copy(alpha = .13f),
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.PersonOutline, null, tint = RecRed, modifier = Modifier.size(28.dp)) } }
                Spacer(Modifier.height(18.dp))
                Text("Your creator identity starts here.", color = ProjectorIvory, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(9.dp))
                Text(
                    "Sign in with Google to create your FrameByNavin identity and unlock cloud backup. Your projects still live locally and keep working offline after sign-in.",
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
            } else {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = MutedGold.copy(alpha = .13f),
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AlternateEmail, null, tint = MutedGold, modifier = Modifier.size(28.dp)) } }
                Spacer(Modifier.height(18.dp))
                Text("Choose your creator identity.", color = ProjectorIvory, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Your username is unique across FrameByNavin. It does not make your projects public.", color = MutedText, fontSize = 11.5.sp, lineHeight = 17.sp)
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
}
