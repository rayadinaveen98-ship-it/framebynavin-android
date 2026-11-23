package com.framebynavin.app.ui

import android.content.MutableContextWrapper
import androidx.activity.compose.BackHandler
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.R
import com.framebynavin.app.cloud.*
import com.framebynavin.app.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

/**
 * Point 4: explicit initial choice and independent Sign in / Sign up surfaces.
 *
 * The existing verified Google CredentialManager + CloudSyncManager functions
 * are reused intact. New screens NEVER sign out, erase local work, modify
 * backup status, assume sign-in created a Creator ID or auto-run setup.
 * Existing Google/Supabase authentication may establish a Google identity even
 * if no Backlot Creator profile exists; we do not misrepresent that as account
 * creation. In Sign in it leads to a separate review screen.
 */
@Composable
internal fun V23AccountOnboarding(
    onComplete: (String) -> Unit,
    onContinueLocally: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(context) { CloudSyncManager(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialContext = remember(context) { MutableContextWrapper(context) }

    var session by remember { mutableStateOf(manager.localState().session) }
    var profile by remember { mutableStateOf(manager.cachedCreatorProfile()) }
    var checkingExisting by remember { mutableStateOf(session != null) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var screenName by rememberSaveable { mutableStateOf(P4AccountEntryStep.CHOICE.name) }
    var displayName by rememberSaveable {
        mutableStateOf(profile?.displayName.orEmpty().ifBlank { session?.displayName.orEmpty() })
    }
    var username by rememberSaveable { mutableStateOf(profile?.username.orEmpty()) }
    val screen = P4AccountEntryStep.valueOf(screenName)

    fun reload() {
        session = manager.localState().session
        profile = manager.cachedCreatorProfile()
        if (displayName.isBlank()) {
            displayName = profile?.displayName.orEmpty()
                .ifBlank { session?.displayName.orEmpty() }
        }
        if (username.isBlank()) username = profile?.username.orEmpty()
    }

    fun finishReturning(): Boolean {
        val existingSession = manager.localState().session
        val existingProfile = manager.cachedCreatorProfile()
        if (CloudCreatorAccountPolicy.route(existingSession, existingProfile) !=
            CloudCreatorAccountRoute.RETURNING_CREATOR) return false
        onComplete(existingProfile!!.displayName.ifBlank { existingSession!!.displayName })
        return true
    }

    fun enter(next: P4AccountEntryStep) {
        screenName = next.name
        error = null
    }

    fun startGoogleAuthentication(intent: P4AccountIntent) {
        if (busy) return
        if (CloudConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            error = "Google sign-in isn't configured on this installation."
            return
        }
        busy = true
        error = null
        scope.launch {
            try {
                val option = GetSignInWithGoogleOption.Builder(
                    CloudConfig.GOOGLE_WEB_CLIENT_ID
                ).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option).build()
                val result = credentialManager.getCredential(
                    context = credentialContext,
                    request = request,
                )
                val credential = result.credential
                val token = if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                } else null
                if (token.isNullOrBlank()) {
                    error = "Google couldn't complete authentication."
                } else when (val resultOp = manager.completeGoogleSignIn(token)) {
                    is CloudOperationResult.Success -> {
                        reload()
                        if (!finishReturning()) {
                            // A backend-authenticated identity is not proof of an
                            // existing Backlot Creator ID. Refresh before routing.
                            val refresh = manager.refreshCreatorIdentity()
                            if (refresh.isFailure) {
                                error = "Google connected, but we couldn't check your " +
                                    "Backlot profile. Retry when the connection is available."
                            } else {
                                reload()
                                if (!finishReturning()) {
                                    val route = CloudCreatorAccountPolicy.route(session, profile)
                                    val destination = P4AccountEntryPolicy
                                        .destinationAfterGoogle(intent, route)
                                    if (destination == null) {
                                        // The real RETURNING_CREATOR path above owns completion.
                                        error = "Couldn't finish checking your account. Retry."
                                    } else enter(destination)
                                }
                            }
                        }
                    }
                    is CloudOperationResult.Skipped -> error = resultOp.message
                    is CloudOperationResult.Failure -> error = resultOp.message
                }
            } catch (_: GetCredentialCancellationException) {
                error = "Google authentication cancelled."
            } catch (_: NoCredentialException) {
                error = "No Google account is available on this device."
            } catch (_: GoogleIdTokenParsingException) {
                error = "Google couldn't verify the sign-in response."
            } catch (_: GetCredentialException) {
                error = "Google authentication failed. Please retry."
            } catch (failure: Throwable) {
                error = failure.message ?: "Google authentication failed."
            } finally {
                busy = false
            }
        }
    }

    fun claimCreatorId() {
        if (busy || session == null ||
            !P4AccountEntryPolicy.usernameAllowed(username) ||
            displayName.trim().isBlank()) return
        busy = true
        error = null
        scope.launch {
            try {
                when (val result = manager.claimUsername(username, displayName.trim())) {
                    is CloudOperationResult.Success -> {
                        reload()
                        onComplete(displayName.ifBlank { session?.displayName.orEmpty() })
                    }
                    is CloudOperationResult.Skipped -> error = result.message
                    is CloudOperationResult.Failure -> error = result.message
                }
            } catch (failure: Throwable) {
                error = failure.message ?: "Couldn't create the Creator ID."
            } finally { busy = false }
        }
    }

    // An authenticated returning user never needs the new one-time choice.
    // Failed/partial lookup leaves data and credentials untouched and shows
    // the ordinary entry choice so the owner can choose deliberately.
    LaunchedEffect(Unit) {
        if (session != null) {
            try {
                if (!finishReturning()) {
                    val refresh = manager.refreshCreatorIdentity()
                    if (refresh.isSuccess) {
                        reload()
                        finishReturning()
                    } else {
                        error = "Couldn't verify your existing account. " +
                            "You can retry Google sign-in without losing local work."
                    }
                }
            } finally {
                checkingExisting = false
            }
        }
    }

    BackHandler(enabled = screen != P4AccountEntryStep.CHOICE && !busy) {
        enter(P4AccountEntryPolicy.backFrom(screen))
    }

    Surface(Modifier.fillMaxSize().testTag("p4-account-entry"), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding().navigationBarsPadding().imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (checkingExisting) {
                Spacer(Modifier.height(70.dp))
                Text("Checking your account…", color = ProjectorIvory,
                    fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                CircularProgressIndicator(Modifier.size(22.dp),
                    color = MutedGold, strokeWidth = 2.dp)
            } else {
                if (screen != P4AccountEntryStep.CHOICE) {
                    TextButton(
                        onClick = { enter(P4AccountEntryPolicy.backFrom(screen)) },
                        enabled = !busy,
                        modifier = Modifier.testTag("p4-back"),
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(Modifier.height(40.dp))
                }

                if (screen == P4AccountEntryStep.CHOICE) {
                    Image(
                        painter = painterResource(R.drawable.backlot_v198_brand_mark),
                        contentDescription = "Backlot logo",
                        modifier = Modifier.size(80.dp).testTag("p4-brand-mark"),
                    )
                    Spacer(Modifier.height(28.dp))
                    Text("Your creative space awaits.", color = ProjectorIvory,
                        fontSize = 29.sp, lineHeight = 35.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("Choose how you'd like to begin.", color = MutedText,
                        fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = { enter(P4AccountEntryStep.SIGN_UP) },
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .testTag("p4-open-sign-up"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProjectorIvory, contentColor = CinemaBlack),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("CREATE ACCOUNT", fontWeight = FontWeight.SemiBold) }
                    OutlinedButton(
                        onClick = { enter(P4AccountEntryStep.SIGN_IN) },
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .testTag("p4-open-sign-in"),
                        border = BorderStroke(1.dp, CinemaLine),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("SIGN IN", color = ProjectorIvory,
                        fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.height(9.dp))
                    TextButton(
                        onClick = onContinueLocally,
                        modifier = Modifier.fillMaxWidth()
                            .testTag("p4-continue-without-account"),
                    ) { Text("Continue without account", color = MutedText) }
                }

                if (screen == P4AccountEntryStep.SIGN_IN) {
                    Spacer(Modifier.height(27.dp))
                    Text("Welcome back.", color = ProjectorIvory,
                        fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    Text("Sign in with Google to return to your Backlot account.",
                        color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { startGoogleAuthentication(P4AccountIntent.SIGN_IN) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .testTag("p4-sign-in-google"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProjectorIvory, contentColor = CinemaBlack),
                    ) { Text(if (busy) "CONNECTING…" else "SIGN IN WITH GOOGLE",
                        fontWeight = FontWeight.SemiBold) }
                }

                if (screen == P4AccountEntryStep.SIGN_UP) {
                    Spacer(Modifier.height(27.dp))
                    Text("Create your account.", color = ProjectorIvory,
                        fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    Text("Connect Google to create your Backlot identity. " +
                        "If this account already exists, we'll take you to it.",
                        color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { startGoogleAuthentication(P4AccountIntent.SIGN_UP) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .testTag("p4-sign-up-google"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProjectorIvory, contentColor = CinemaBlack),
                    ) { Text(if (busy) "CONNECTING…" else "CONTINUE WITH GOOGLE",
                        fontWeight = FontWeight.SemiBold) }
                }

                if (screen == P4AccountEntryStep.ACCOUNT_REVIEW) {
                    Spacer(Modifier.height(27.dp))
                    Text("Account needs setup.", color = ProjectorIvory,
                        fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    Text("Google is connected, but we couldn't find a completed " +
                        "Backlot Creator ID. You can explicitly create one, or return.",
                        color = MutedText, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { enter(P4AccountEntryStep.CREATOR_ID) },
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .testTag("p4-review-create-account"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProjectorIvory, contentColor = CinemaBlack),
                    ) { Text("CREATE ACCOUNT", fontWeight = FontWeight.SemiBold) }
                }

                if (screen == P4AccountEntryStep.CREATOR_ID) {
                    Spacer(Modifier.height(20.dp))
                    Text("Finish creating your account.", color = ProjectorIvory,
                        fontSize = 27.sp, lineHeight = 33.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("Choose the Creator ID shown on your Backlot profile.",
                        color = MutedText, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(40) },
                        modifier = Modifier.fillMaxWidth().testTag("p4-display-name"),
                        label = { Text("Display name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { raw ->
                            username = raw.lowercase().filter { ch ->
                                ch in 'a'..'z' || ch in '0'..'9' || ch == '_'
                            }.take(24)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("p4-username"),
                        prefix = { Text("@") },
                        label = { Text("Username") },
                        supportingText = { Text("3–24 letters, numbers or underscores") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = ::claimCreatorId,
                        enabled = !busy && session != null &&
                            displayName.trim().isNotBlank() &&
                            P4AccountEntryPolicy.usernameAllowed(username),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                            .testTag("p4-create-account"),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    ) { Text(if (busy) "CREATING…" else "CREATE ACCOUNT",
                        fontWeight = FontWeight.SemiBold) }
                }
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(9.dp))
                    Text(error.orEmpty(), color = ProjectorIvory,
                        fontSize = 12.sp, lineHeight = 18.sp,
                        modifier = Modifier.testTag("p4-account-error"))
                }
            }
        }
    }
}
