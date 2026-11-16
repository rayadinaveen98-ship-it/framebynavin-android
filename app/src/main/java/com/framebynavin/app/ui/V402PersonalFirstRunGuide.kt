package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * UX02: A deliberately short, optional guide for Personal-first users.
 * Does not invent a CreatorProfile, enable permissions or mutate a reminder.
 * Existing Personal hub remains the first actionable destination.
 */
@Composable
internal fun V402PersonalFirstRunGuide(onFinish: () -> Unit) {
    val lessons = listOf(
        Triple(
            "Personal Reminders",
            "Make room for what matters",
            "Create reminders for your everyday life. Set the time, voice and alerts you prefer.",
        ),
        Triple(
            "21-Day Challenges",
            "Build one habit at a time",
            "Choose an activity, check in when you complete it, and see your progress over time.",
        ),
        Triple(
            "My Spaces",
            "Keep the details together",
            "Connect reminders with meaningful Spaces, notes and your private photo journal.",
        ),
    )
    var page by rememberSaveable { mutableIntStateOf(0) }
    var voiceGuideEnabled by rememberSaveable { mutableStateOf(false) }
    val lesson = lessons[page.coerceIn(0, lessons.lastIndex)]
    V402TourNarration(enabled = voiceGuideEnabled, spokenCopy = lesson.third)

    Surface(Modifier.fillMaxSize().testTag("v402-personal-first-run"), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("BACKLOT / PERSONAL", color = MutedGold, fontSize = 10.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Text((page + 1).toString() + " / " + lessons.size, color = MutedText, fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            V144GuideCharacter(
                guide = V144GuidePrefs.currentGuide,
                state = CinePulseState.WAVE,
                modifier = Modifier.align(Alignment.CenterHorizontally).size(150.dp),
            )
            Text(lesson.first, color = ProjectorIvory, fontSize = 28.sp,
                fontWeight = FontWeight.Black, lineHeight = 34.sp,
                modifier = Modifier.testTag("v402-personal-guide-heading"))
            Text(lesson.second, color = MutedGold, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold)
            Text(lesson.third, color = MutedText, fontSize = 13.sp, lineHeight = 20.sp)
            TextButton(onClick = { voiceGuideEnabled = !voiceGuideEnabled },
                modifier = Modifier.testTag("v402-personal-voice-toggle")) {
                Text(if (voiceGuideEnabled) "Voice guide on" else "Voice guide off",
                    color = MutedGold, fontSize = 12.sp)
            }
            Text(
                "Your Personal space works without a Creator profile. " +
                    "All Creator tools remain available whenever you need them.",
                color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (page == lessons.lastIndex) onFinish() else page++ },
                modifier = Modifier.fillMaxWidth().height(52.dp)
                    .testTag("v402-personal-guide-next"),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
            ) {
                Text(if (page == lessons.lastIndex) "OPEN PERSONAL REMINDERS" else "CONTINUE",
                    fontWeight = FontWeight.Black)
            }
            TextButton(onClick = onFinish,
                modifier = Modifier.align(Alignment.CenterHorizontally)
                    .testTag("v402-personal-guide-skip")) {
                Text("Skip introduction", color = MutedText, fontSize = 12.sp)
            }
        }
    }
}
