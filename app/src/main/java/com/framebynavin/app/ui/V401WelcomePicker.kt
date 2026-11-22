package com.framebynavin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.V401WelcomeStyle
import com.framebynavin.app.ui.theme.VisualExperiencePrefs

/**
 * UX01: A preview never writes the saved style. Only the explicit Choose action does.
 * Leave the existing app-icon, theme, guide and sound controls untouched.
 */
@Composable
internal fun V401WelcomePicker() {
    val selected = VisualExperiencePrefs.welcomeStyle
    var preview by remember { mutableStateOf<V401WelcomeStyle?>(null) }
    val premiumStyles = listOf(
        V401WelcomeStyle.CINEMATIC_EMBER,
        V401WelcomeStyle.APERTURE_LIGHT_BLOOM,
        V401WelcomeStyle.VELVET_GLASS,
    )
    val classicStyles = V401WelcomeStyle.entries.filterNot { it in premiumStyles }
    // An existing classic choice stays visible when Settings first opens.
    // A preview is always transient; only Choose persists a new style.
    var showClassics by rememberSaveable { mutableStateOf(selected in classicStyles) }

    Column(Modifier.fillMaxWidth().testTag("v401-welcome-picker")) {
        Text(
            text = "WELCOME ANIMATION",
            color = MutedGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.05.sp,
        )
        Text(
            text = "Choose your opening",
            color = ProjectorIvory,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "Explore three cinematic openings or reveal the four original styles. " +
                "Preview is silent; only Choose saves your opening.",
            color = MutedText,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
        )

        Text(
            text = "NEW PREMIUM COLLECTION",
            color = MutedGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
        )
        val shownStyles = if (showClassics) premiumStyles + classicStyles else premiumStyles
        shownStyles.forEachIndexed { index, style ->
            if (index == premiumStyles.size) {
                Text(
                    text = "CLASSIC COLLECTION",
                    color = MutedGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 18.dp, bottom = 2.dp),
                )
            }
            val active = style == selected
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            style.displayName,
                            color = ProjectorIvory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            style.description,
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                    if (active) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = "Selected",
                            tint = RecRed,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 3.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { preview = style },
                        modifier = Modifier.testTag("v401-preview-" + style.name),
                    ) {
                        Text("Preview", color = ProjectorIvory)
                    }
                    Button(
                        onClick = { VisualExperiencePrefs.updateWelcomeStyle(style) },
                        enabled = !active,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RecRed,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.testTag("v401-select-" + style.name),
                    ) {
                        Text(if (active) "Selected" else "Choose")
                    }
                }
            }
            if (index != shownStyles.lastIndex) {
                HorizontalDivider(color = CinemaLine.copy(alpha = .52f))
            }
        }
        TextButton(
            onClick = { showClassics = !showClassics },
            modifier = Modifier.fillMaxWidth().testTag("ux12-d6-toggle-classics"),
        ) {
            Text(
                if (showClassics) "Hide original openings" else "See all four original openings",
                color = MutedGold,
            )
        }
    }

    preview?.let { style ->
        Dialog(
            onDismissRequest = { preview = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                Modifier.fillMaxSize().background(CinemaBlack)
                    .testTag("v401-preview-dialog"),
            ) {
                V401WelcomeExperience(
                    style = style,
                    preview = true,
                    onFinished = { preview = null },
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(top = 12.dp, end = 14.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    TextButton(onClick = { preview = null }) {
                        Text("Close preview", color = ProjectorIvory)
                    }
                }
            }
        }
    }
}
