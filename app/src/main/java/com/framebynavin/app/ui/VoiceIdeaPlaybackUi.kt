package com.framebynavin.app.ui

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorViewModel
import com.framebynavin.app.data.IdeaTranscriptionState
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.voice.IdeaVoiceLanguage
import com.framebynavin.app.voice.SavedVoiceIdeaTranscriber
import com.framebynavin.app.voice.SavedVoiceIdeaTranscriberListener
import java.io.File

/**
 * Local playback + optional transcription for the original audio attached to a Voice Idea.
 *
 * Transcript writes go through the Activity-scoped CreatorViewModel so they share Backlot's
 * normal optimistic save queue and conflict handling instead of writing around it.
 */
@Composable
fun VoiceIdeaPlaybackControl(
    idea: CreatorIdea,
    modifier: Modifier = Modifier,
) {
    if (!idea.hasOriginalRecording) return

    val context = LocalContext.current
    val creatorViewModel: CreatorViewModel = viewModel()
    val latestIdea by rememberUpdatedState(idea)
    val localPath = idea.audioLocalPath

    var player by remember(localPath) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(localPath) { mutableStateOf(false) }
    var unavailable by remember(localPath) { mutableStateOf(false) }
    var transcriptionState by remember(idea.id) { mutableStateOf(idea.transcriptionState) }
    var transcript by remember(idea.id) { mutableStateOf(idea.transcript) }
    var transcriptionError by remember(idea.id) { mutableStateOf(idea.transcriptionError) }

    LaunchedEffect(idea.transcriptionState, idea.transcript, idea.transcriptionError) {
        if (transcriptionState != IdeaTranscriptionState.PENDING) {
            transcriptionState = idea.transcriptionState
            transcript = idea.transcript
            transcriptionError = idea.transcriptionError
        }
    }

    val transcriber = remember(idea.id) {
        SavedVoiceIdeaTranscriber(
            context = context,
            listener = object : SavedVoiceIdeaTranscriberListener {
                override fun onTranscriptionState(state: IdeaTranscriptionState, error: String) {
                    transcriptionState = state
                    transcriptionError = error
                    when (state) {
                        IdeaTranscriptionState.FAILED,
                        IdeaTranscriptionState.UNAVAILABLE -> {
                            creatorViewModel.saveIdea(
                                latestIdea.copy(
                                    transcriptionState = state,
                                    transcriptionError = error,
                                )
                            )
                        }
                        IdeaTranscriptionState.NOT_REQUESTED,
                        IdeaTranscriptionState.PENDING,
                        IdeaTranscriptionState.COMPLETED -> Unit
                    }
                }

                override fun onTranscriptReady(text: String, confidence: Float?) {
                    val clean = text.trim()
                    if (clean.isBlank()) return
                    transcript = clean
                    transcriptionState = IdeaTranscriptionState.COMPLETED
                    transcriptionError = ""
                    creatorViewModel.saveIdea(
                        latestIdea.copy(
                            transcript = clean,
                            transcriptionState = IdeaTranscriptionState.COMPLETED,
                            transcriptionError = "",
                        )
                    )
                }
            },
        )
    }

    DisposableEffect(localPath, transcriber) {
        onDispose {
            runCatching { player?.stop() }
            runCatching { player?.release() }
            player = null
            transcriber.destroy()
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

    fun transcribe() {
        if (playing) pause()
        transcriptionError = ""
        transcriptionState = IdeaTranscriptionState.PENDING
        transcriber.start(localPath, IdeaVoiceLanguage.AUTO)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    transcriptionStatusLabel(transcriptionState, transcript),
                    color = when (transcriptionState) {
                        IdeaTranscriptionState.COMPLETED -> MutedGold
                        IdeaTranscriptionState.FAILED,
                        IdeaTranscriptionState.UNAVAILABLE -> RecRed
                        else -> MutedText
                    },
                    fontSize = 8.5.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = ::transcribe,
                    enabled = transcriptionState != IdeaTranscriptionState.PENDING,
                ) {
                    Text(
                        when (transcriptionState) {
                            IdeaTranscriptionState.PENDING -> "TRANSCRIBING…"
                            IdeaTranscriptionState.COMPLETED -> "RETRANSCRIBE"
                            IdeaTranscriptionState.FAILED,
                            IdeaTranscriptionState.UNAVAILABLE -> "TRY AGAIN"
                            IdeaTranscriptionState.NOT_REQUESTED -> "TRANSCRIBE"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            if (transcript.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    transcript,
                    color = ProjectorIvory,
                    fontSize = 9.sp,
                    lineHeight = 12.5.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (transcriptionError.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    transcriptionError,
                    color = MutedText,
                    fontSize = 8.3.sp,
                    lineHeight = 11.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun transcriptionStatusLabel(
    state: IdeaTranscriptionState,
    transcript: String,
): String = when (state) {
    IdeaTranscriptionState.NOT_REQUESTED -> "Transcript optional"
    IdeaTranscriptionState.PENDING -> "Preparing saved recording…"
    IdeaTranscriptionState.COMPLETED -> if (transcript.isBlank()) "Transcript ready" else "Transcript saved"
    IdeaTranscriptionState.FAILED -> "Transcription failed"
    IdeaTranscriptionState.UNAVAILABLE -> "Saved-audio transcription unavailable"
}
