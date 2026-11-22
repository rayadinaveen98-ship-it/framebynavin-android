package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.BacklotError
import com.framebynavin.app.ui.theme.BacklotUX10Tokens
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * UX11-D: PRESENTATION ONLY, with three user-visible entry intents.
 *
 * SIGN_UP and LOG_IN invoke the SAME existing Google CredentialManager route:
 * Google / CloudCreatorAccountPolicy, not a UI selection, determines whether
 * this is a returning account or needs a new profile/username. Never invent
 * password login or a separate account-creation backend.
 *
 * Local continuation, recovery and successful account completion remain owned
 * by the pre-existing V23AccountOnboarding callers.
 */
internal enum class UX11AuthEntryMode {
    CHOOSE,
    SIGN_UP,
    LOG_IN,
}

internal object UX11AuthPresentationPolicy {
    fun title(mode: UX11AuthEntryMode): String = when (mode) {
        UX11AuthEntryMode.CHOOSE -> "A space for what you create."
        UX11AuthEntryMode.SIGN_UP -> "Start your Backlot."
        UX11AuthEntryMode.LOG_IN -> "Welcome back."
    }

    fun subtitle(mode: UX11AuthEntryMode): String = when (mode) {
        UX11AuthEntryMode.CHOOSE ->
            "Your projects, personal reminders and everyday ideas, together in one place."
        UX11AuthEntryMode.SIGN_UP ->
            "Connect with Google. If this is a new account, you'll choose your Backlot name next."
        UX11AuthEntryMode.LOG_IN ->
            "Continue securely with Google. Your existing account is recognized automatically."
    }

    fun googleAction(mode: UX11AuthEntryMode, busy: Boolean): String = when {
        busy -> "CONNECTING TO GOOGLE…"
        mode == UX11AuthEntryMode.SIGN_UP -> "SIGN UP WITH GOOGLE"
        else -> "LOG IN WITH GOOGLE"
    }
}

@Composable
internal fun UX11PremiumAccountEntry(
    mode: UX11AuthEntryMode,
    busy: Boolean,
    error: String?,
    onSelectSignUp: () -> Unit,
    onSelectLogin: () -> Unit,
    onBack: () -> Unit,
    onGoogle: () -> Unit,
    onContinueLocally: () -> Unit,
) {
    UX11AuthShell(
        title = UX11AuthPresentationPolicy.title(mode),
        subtitle = UX11AuthPresentationPolicy.subtitle(mode),
        eyebrow = if (mode == UX11AuthEntryMode.CHOOSE) "YOUR BACKLOT STARTS HERE" else
            if (mode == UX11AuthEntryMode.SIGN_UP) "CREATE ACCOUNT" else "SIGN IN",
        variant = mode,
        testTag = "ux11-auth-" + mode.name.lowercase(),
    ) {
        when (mode) {
            UX11AuthEntryMode.CHOOSE -> {
                UX11PrimaryAuthButton(
                    label = "CREATE ACCOUNT",
                    onClick = onSelectSignUp,
                    enabled = !busy,
                    modifier = Modifier.testTag("ux11-auth-create-account"),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onSelectLogin,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .testTag("ux11-auth-login"),
                    border = BorderStroke(1.dp, ProjectorIvory.copy(alpha = .30f)),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("LOG IN", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(19.dp))
                UX11AuthDividerLabel("OR STAY ON THIS DEVICE")
                Spacer(Modifier.height(10.dp))
                UX11LocalContinuation(onContinueLocally, busy)
            }
            UX11AuthEntryMode.SIGN_UP, UX11AuthEntryMode.LOG_IN -> {
                // UX12-D2: one user intent, one Google action. Alternate account
                // modes and local-only continuation belong ONLY to CHOOSE.
                // Back returns there without silently starting another auth route.
                androidx.compose.material3.TextButton(
                    onClick = onBack,
                    enabled = !busy,
                    modifier = Modifier.heightIn(min = BacklotUX10Tokens.minimumTouchTarget)
                        .testTag("ux11-auth-back"),
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = null,
                        modifier = Modifier.size(17.dp), tint = ProjectorIvory)
                    Text("  BACK", color = ProjectorIvory,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(17.dp))
                UX11PrimaryAuthButton(
                    label = UX11AuthPresentationPolicy.googleAction(mode, busy),
                    onClick = onGoogle,
                    enabled = !busy,
                    modifier = Modifier.testTag("ux11-auth-google"),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Google verifies your identity. Backlot recognizes existing " +
                        "accounts and guides new accounts through profile setup.",
                    color = MutedText, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier.testTag("ux12-d2-focused-auth-note"),
                )
            }
        }
        if (error != null) {
            Spacer(Modifier.height(16.dp))
            UX11AuthError(error)
        }
        if (mode == UX11AuthEntryMode.CHOOSE) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Local use doesn't require Google. Cloud backup/recovery covers " +
                    "supported data only; automatic two-device synchronization isn't available yet.",
                color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
                modifier = Modifier.testTag("ux11-auth-data-disclosure"),
            )
        }
    }
}

@Composable
internal fun UX11PremiumAccountIdentity(
    displayName: String,
    username: String,
    busy: Boolean,
    error: String?,
    onDisplayNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onCreateAccount: () -> Unit,
) {
    UX11AuthShell(
        title = "Make it yours.",
        subtitle = "Your Google account is connected. Choose the name and unique " +
            "username that will identify your Backlot profile.",
        eyebrow = "FINISH CREATING YOUR ACCOUNT",
        variant = UX11AuthEntryMode.SIGN_UP,
        testTag = "ux11-auth-profile",
    ) {
        Surface(
            color = CinemaSurface.copy(alpha = .90f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MutedGold.copy(alpha = .25f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                        tint = MutedGold, modifier = Modifier.size(20.dp))
                    Text("  GOOGLE CONNECTED",
                        color = MutedGold, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = .8.sp)
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ux11-auth-display-name"),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    prefix = { Text("@") },
                    label = { Text("Unique username") },
                    supportingText = {
                        Text("3–24 letters, numbers or underscores. Username availability " +
                            "is confirmed when you create the account.")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ux11-auth-username"),
                )
            }
        }
        Spacer(Modifier.height(15.dp))
        UX11PrimaryAuthButton(
            label = if (busy) "CREATING YOUR ACCOUNT…" else "FINISH ACCOUNT SETUP",
            onClick = onCreateAccount,
            enabled = !busy && username.length in 3..24,
            modifier = Modifier.testTag("ux11-auth-claim"),
        )
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            UX11AuthError(error)
        }
        Spacer(Modifier.height(13.dp))
        Text(
            "Only the existing account service can confirm a username or finish " +
                "registration. Your profile isn't created until it confirms success.",
            fontSize = 11.sp, lineHeight = 17.sp, color = MutedText,
        )
    }
}

@Composable
internal fun UX11PremiumAccountReturning(error: String?) {
    UX11AuthShell(
        title = "Welcome back.",
        subtitle = "Checking your existing Backlot account and available saved work.",
        eyebrow = "RETURNING TO YOUR SPACE",
        variant = UX11AuthEntryMode.LOG_IN,
        testTag = "ux11-auth-returning",
    ) {
        Surface(
            color = CinemaSurface.copy(alpha = .90f),
            border = BorderStroke(1.dp, CinemaLine),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MutedGold,
                    strokeWidth = 2.dp,
                )
                Text(
                    "Checking account details…",
                    color = ProjectorIvory, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (error != null) {
            Spacer(Modifier.height(14.dp))
            UX11AuthError(error)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Saved work may require a separate review before any recovery or " +
                "replacement. Backlot will not silently overwrite your local work.",
            color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
        )
    }
}

@Composable
private fun UX11AuthShell(
    title: String,
    subtitle: String,
    eyebrow: String,
    variant: UX11AuthEntryMode,
    testTag: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(CinemaBlack)
            .testTag(testTag),
    ) {
        UX11AuthAtmosphere(variant)
        Column(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = BacklotUX10Tokens.pageGutter,
                    vertical = 19.dp,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = RecRed.copy(alpha = .19f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .35f)),
                ) {
                    Icon(
                        Icons.Outlined.MovieCreation,
                        contentDescription = null,
                        tint = RecRed,
                        modifier = Modifier.padding(10.dp).size(24.dp),
                    )
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(
                        "BACKLOT",
                        color = ProjectorIvory,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.2.sp,
                    )
                    Text(
                        "BUILT FOR CREATORS",
                        color = MutedGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Spacer(Modifier.height(51.dp))
            Text(
                eyebrow,
                color = MutedGold,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(11.dp))
            Text(
                title,
                color = ProjectorIvory,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                lineHeight = 39.sp,
                modifier = Modifier.testTag("ux11-auth-hero-title"),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                subtitle,
                color = ProjectorIvory.copy(alpha = .76f),
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(37.dp))
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun UX11AuthAtmosphere(variant: UX11AuthEntryMode) {
    Canvas(
        modifier = Modifier.fillMaxSize()
            .testTag("ux11-auth-original-background"),
    ) {
        if (size.minDimension <= 0f) return@Canvas
        val primary = when (variant) {
            UX11AuthEntryMode.CHOOSE -> Color(0xFFAE343C)
            UX11AuthEntryMode.SIGN_UP -> Color(0xFFB95349)
            UX11AuthEntryMode.LOG_IN -> Color(0xFF65578C)
        }
        val secondary = when (variant) {
            UX11AuthEntryMode.CHOOSE -> Color(0xFFE3A26C)
            UX11AuthEntryMode.SIGN_UP -> Color(0xFFF2A76E)
            UX11AuthEntryMode.LOG_IN -> Color(0xFFAA7893)
        }
        drawRect(Brush.verticalGradient(listOf(
            Color(0xFF100E14), CinemaBlack, Color(0xFF100A0E),
        )))
        drawCircle(
            brush = Brush.radialGradient(
                listOf(primary.copy(alpha = .23f), primary.copy(alpha = .06f),
                    Color.Transparent),
                center = Offset(size.width * .93f, size.height * .07f),
                radius = size.width * .90f,
            ),
            center = Offset(size.width * .93f, size.height * .07f),
            radius = size.width * .90f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(secondary.copy(alpha = .11f), Color.Transparent),
                center = Offset(size.width * .03f, size.height * .90f),
                radius = size.width * 1.05f,
            ),
            center = Offset(size.width * .03f, size.height * .90f),
            radius = size.width * 1.05f,
        )
        // Restrained cinematographic arcs, not stock gradients or animated
        // particles. Remains stationary so fields, TalkBack, and Google auth
        // don't compete with an always-running animation.
        val r = size.width * .65f
        drawArc(
            color = secondary.copy(alpha = .18f),
            startAngle = 185f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = Offset(size.width * .53f, -r * .75f),
            size = Size(r * 2f, r * 2f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
        )
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent,
                    secondary.copy(alpha = .17f), Color.Transparent),
            ),
            start = Offset(size.width * .1f, size.height * .44f),
            end = Offset(size.width * .9f, size.height * .44f),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

@Composable
private fun UX11PrimaryAuthButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RecRed,
            contentColor = Color.White,
        ),
    ) {
        Text(
            label, fontSize = 13.sp, fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        if (enabled) {
            Icon(
                Icons.Outlined.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 9.dp).size(18.dp),
            )
        }
    }
}

@Composable
private fun UX11LocalContinuation(onContinueLocally: () -> Unit, busy: Boolean) {
    OutlinedButton(
        onClick = onContinueLocally,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("ux11-auth-continue-locally"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Icon(
            Icons.Outlined.PersonOutline,
            contentDescription = null, tint = MutedGold,
            modifier = Modifier.size(19.dp),
        )
        Text(
            "  USE WITHOUT ACCOUNT",
            color = ProjectorIvory,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun UX11AuthDividerLabel(label: String) {
    Text(
        label, color = MutedText,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = .75.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun UX11AuthError(message: String) {
    Surface(
        color = BacklotError.copy(alpha = .13f),
        border = BorderStroke(1.dp, BacklotError.copy(alpha = .35f)),
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.fillMaxWidth()
            .testTag("ux11-auth-error"),
    ) {
        Text(
            message,
            color = ProjectorIvory,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(14.dp),
        )
    }
}
