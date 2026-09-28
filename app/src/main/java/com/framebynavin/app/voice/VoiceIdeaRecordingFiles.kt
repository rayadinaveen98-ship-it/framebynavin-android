package com.framebynavin.app.voice

import java.io.File

/** File ownership helpers for unsaved Voice Idea takes. */
object VoiceIdeaRecordingFiles {
    fun remove(
        recording: VoiceIdeaRecording?,
        deletePath: (String) -> Boolean = ::deletePath,
    ): Boolean {
        val path = recording?.localPath.orEmpty()
        return path.isBlank() || deletePath(path)
    }

    /**
     * Atomically hands ownership from an older unsaved take to a newly finalized take.
     *
     * If the previous take cannot be removed, the replacement is discarded and false is returned,
     * leaving the caller free to keep referencing the previous take.
     */
    fun replace(
        previous: VoiceIdeaRecording?,
        replacement: VoiceIdeaRecording,
        deletePath: (String) -> Boolean = ::deletePath,
    ): Boolean {
        val previousPath = previous?.localPath.orEmpty()
        if (previousPath.isBlank() || previousPath == replacement.localPath) return true
        if (deletePath(previousPath)) return true

        // Do not create an untracked second finalized file when ownership transfer failed.
        deletePath(replacement.localPath)
        return false
    }

    private fun deletePath(path: String): Boolean = runCatching {
        val file = File(path)
        !file.exists() || file.delete()
    }.getOrDefault(false)
}
