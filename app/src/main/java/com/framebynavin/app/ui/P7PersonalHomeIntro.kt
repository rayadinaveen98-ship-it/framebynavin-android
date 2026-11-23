package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.MutedText

/**
 * P7 Personal Home: one warm, honest editorial note above real reminder data.
 *
 * Backlot remains Creator OS. This is a care-oriented companion surface, not
 * a second setup, account, profile, habit engine, dashboard or fake progress.
 * Reuse only existing callback destinations and the approved companion sprite.
 */
@Composable
internal fun P7PersonalHomeIntro(
    actions: P7PersonalHomeActions,
    onCreateReminder: () -> Unit,
    onOpenSpaces: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenGroups: () -> Unit,
) {
    V144GuidePrefs.initialize(LocalContext.current)
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("p7-personal-care-intro"),
        color = Color(0xFF251E26),
        border = BorderStroke(1.dp, Color(0xFF4B3947)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = "PERSONAL · A SPACE FOR YOU",
                        color = Color(0xFFE5B9AD), fontSize = 10.sp,
                        letterSpacing = 0.9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "A little room for you.",
                        color = ProjectorIvory,
                        fontSize = 22.sp, lineHeight = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("p7-personal-care-title"),
                    )
                    Text(
                        text = "Your creative work matters. So does your everyday life.",
                        color = Color(0xFFD3C8D0),
                        fontSize = 12.sp, lineHeight = 18.sp,
                    )
                }
                // Decorative, undistorted, already-approved Spark/Mira art.
                // Not a second interactive avatar or notification affordance.
                Box(Modifier.size(width = 88.dp, height = 110.dp),
                    contentAlignment = Alignment.Center) {
                    V144GuideCharacter(
                        guide = V144GuidePrefs.currentGuide,
                        state = CinePulseState.LISTEN,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .testTag("p7-personal-quick-actions"),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                P7PersonalHomeAction(
                    "New reminder", enabled = actions.createReminder,
                    onClick = onCreateReminder,
                    modifier = Modifier.testTag("p7-create-personal-reminder"),
                )
                P7PersonalHomeAction(
                    "My Spaces", enabled = actions.openSpaces,
                    onClick = onOpenSpaces,
                    // Existing test-tag preserved from previous My Spaces row.
                    modifier = Modifier.testTag("v289-open-spaces"),
                )
                P7PersonalHomeAction(
                    "21-day challenges", enabled = actions.openChallenges,
                    onClick = onOpenChallenges,
                    modifier = Modifier.testTag("p7-open-personal-challenges"),
                )
                P7PersonalHomeAction(
                    "Groups", enabled = actions.openGroups,
                    onClick = onOpenGroups,
                    modifier = Modifier.testTag("p7-open-personal-groups"),
                )
            }
        }
    }
}

@Composable
private fun P7PersonalHomeAction(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
        border = BorderStroke(1.dp, Color(0xFF66545F)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ProjectorIvory,
            disabledContentColor = MutedText,
        ),
    ) {
        Text(title, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
    }
}
