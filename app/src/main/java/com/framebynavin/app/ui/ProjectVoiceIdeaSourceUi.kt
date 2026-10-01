package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorViewModel
import com.framebynavin.app.data.IdeaProjectBridge
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import androidx.compose.material3.Text

/** Keeps an Idea Vault voice recording reachable after the idea becomes a project. */
@Composable
internal fun ProjectVoiceIdeaSource(task: CreatorTask) {
    val creatorViewModel: CreatorViewModel = viewModel()
    val sourceIdea by remember(task.origin, task.sourceRefId, creatorViewModel) {
        derivedStateOf { IdeaProjectBridge.sourceVoiceIdea(task, creatorViewModel.ideas) }
    }
    val idea = sourceIdea ?: return

    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(14.dp))
        Text(
            "SOURCE VOICE IDEA",
            color = MutedGold,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            "Original recording stays linked to this project. Its latest transcript remains in sync with Idea Vault.",
            color = MutedText,
            fontSize = 8.7.sp,
            lineHeight = 12.5.sp,
        )
        Spacer(Modifier.height(7.dp))
        VoiceIdeaPlaybackControl(idea = idea)
        Spacer(Modifier.height(3.dp))
    }
}
