package com.framebynavin.app.personal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ephemeral, per-process presentation state sourced ONLY from the actual Personal
 * playback callbacks. No microphones, AudioPlaybackCapture, remote data, or
 * persistence. This cannot be interpreted as measured audio amplitude.
 *
 * Token scoping prevents a retired alarm/voice occurrence from lighting another
 * reminder, while service termination always returns the visual to silence.
 */
internal enum class FC13PlaybackStage { SILENT, ALARM_RINGING, VOICE_SPEAKING }

internal data class FC13PlaybackVisualReceipt(
    val reminderId: String = "",
    val occurrenceToken: String = "",
    val stage: FC13PlaybackStage = FC13PlaybackStage.SILENT,
) {
    fun stageFor(id: String, token: String): FC13PlaybackStage =
        if (id.isNotBlank() && token.isNotBlank() &&
            reminderId == id && occurrenceToken == token
        ) stage else FC13PlaybackStage.SILENT
}

internal object FC13PersonalPlaybackVisualState {
    private val mutable = MutableStateFlow(FC13PlaybackVisualReceipt())
    val receipts: StateFlow<FC13PlaybackVisualReceipt> = mutable.asStateFlow()

    /** Call on the playback service's main thread only. */
    fun show(reminderId: String, token: String, stage: FC13PlaybackStage) {
        if (reminderId.isBlank() || token.isBlank() || stage == FC13PlaybackStage.SILENT) return
        mutable.value = FC13PlaybackVisualReceipt(reminderId, token, stage)
    }

    /** Only the matching occurrence can clear an in-flight visual. */
    fun clear(reminderId: String, token: String) {
        if (reminderId.isNotBlank() && token.isNotBlank() &&
            mutable.value.reminderId == reminderId &&
            mutable.value.occurrenceToken == token
        ) mutable.value = FC13PlaybackVisualReceipt()
    }
}
