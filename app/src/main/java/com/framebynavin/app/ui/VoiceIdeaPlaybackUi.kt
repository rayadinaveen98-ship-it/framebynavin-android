package com.framebynavin.app.ui

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.io.File

/** Compact local playback control for the original audio attached to a Voice Idea. */
@Composable
fun VoiceIdeaPlaybackControl(
    idea: CreatorIdea,
    modifier: Modifier = Modifier,
) {
    if (!idea.hasOriginalRecording) return

    val localPath = idea.audioLocalPath
    var player by remember(localPath) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(localPath) { mutableStateOf(false) }
    var unavailable by remember(localPath) { mutableStateOf(false) }

    DisposableEffect(localPath) {
        onDispose {
            runCatching { player?.stop() }
            runCatching { player?.release() }
            player = null
        }
    }

    fun pause() {
        runCatching { player?.pause() }
        playing = false
    }

    fun play() {
        unavailable = false
        val file = File(localPath)
        if (!file.exists() || file.length() <= 0L) {
            unavailable = true
            playing = false
            return
        }

        val active = player ?: runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener { completed ->
                    playing = false
                    runCatching { completed.seekTo(0) }
                }
            }
        }.getOrElse {
            unavailable = true
            playing = false
            return
        }.also { player = it }

        runCatching { active.start() }
            .onSuccess { playing = true }
            .onFailure {
                unavailable = true
                playing = false
                runCatching { active.release() }
                player = null
            }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "VOICE • ${formatVoiceDuration(idea.audioDurationMillis)}",
                color = ProjectorIvory,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(7.dp))
            Text(
                if (unavailable) "Audio unavailable" else "Original recording",
                color = if (unavailable) RecRed else MutedText,
                fontSize = 8.5.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { if (playing) pause() else play() }) {
                Text(
                    if (playing) "PAUSE" else "PLAY",
                    color = if (playing) RecRed else MaterialTheme.colorScheme.primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
