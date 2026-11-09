package com.framebynavin.app.reminders

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Verified service lifecycle EVENT, not TTS/MediaPlayer audio amplitude. */
internal enum class FC13CreatorAudioStage { SILENT, ALARM_PLAYING, VOICE_SPEAKING }

internal data class FC13CreatorAudioReceipt(
    val taskId: String = "",
    val occurrenceId: String = "",
    val stage: FC13CreatorAudioStage = FC13CreatorAudioStage.SILENT,
) {
    fun forOccurrence(task: String, token: String): FC13CreatorAudioStage =
        if (task.isNotBlank() && token.isNotBlank() &&
            taskId == task && occurrenceId == token
        ) stage else FC13CreatorAudioStage.SILENT
}

internal object FC13CreatorPlaybackVisualState {
    private val mutable = MutableStateFlow(FC13CreatorAudioReceipt())
    val receipts: StateFlow<FC13CreatorAudioReceipt> = mutable.asStateFlow()

    fun show(taskId: String, token: String, stage: FC13CreatorAudioStage) {
        if (taskId.isBlank() || token.isBlank() || stage == FC13CreatorAudioStage.SILENT) return
        mutable.value = FC13CreatorAudioReceipt(taskId, token, stage)
    }

    fun clear(taskId: String, token: String) {
        if (mutable.value.taskId == taskId && mutable.value.occurrenceId == token &&
            taskId.isNotBlank() && token.isNotBlank()) {
            mutable.value = FC13CreatorAudioReceipt()
        }
    }
}
