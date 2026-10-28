package com.framebynavin.app.ui

/**
 * Do not echo untrusted filenames, file paths or remote provider diagnostics
 * into public UI. Tell the creator which safe recovery action is available.
 * Never silently bypass integrity checks or overwrite data on validation failure.
 */
internal object V208BackupErrorCopy {
    private fun reason(error: Throwable?): String {
        val msg = error?.message.orEmpty()
        return when {
            msg.contains("Could not verify the exported backup file", ignoreCase = true) ||
                msg.contains("Could not reopen", ignoreCase = true) ||
                msg.contains("Could not open the selected file", ignoreCase = true) ->
                "The selected file provider could not reopen or verify the export. It may have saved a partial file; try a local Files/Downloads destination."
            msg.contains("Voice Idea", ignoreCase = true) ||
                msg.contains("voice media", ignoreCase = true) ||
                msg.contains("voice recording", ignoreCase = true) ->
                "One or more Voice Idea recordings are missing, damaged or mismatched."
            msg.contains("integrity", ignoreCase = true) ||
                msg.contains("checksum", ignoreCase = true) ||
                msg.contains("manifest", ignoreCase = true) ||
                msg.contains("payloadSha256", ignoreCase = true) ->
                "The backup integrity verification did not match its saved data."
            msg.contains("schema", ignoreCase = true) ||
                msg.contains("version", ignoreCase = true) ->
                "This backup uses an unsupported or incomplete data version."
            else -> "The selected backup could not be verified."
        }
    }

    fun forExport(error: Throwable?): String =
        reason(error) + " No complete backup was confirmed. Check your saved recordings and available storage, then export again."

    fun forValidation(error: Throwable): String =
        reason(error) + " Nothing was restored. Export a fresh backup from the original device if possible; do not edit the backup file."

    fun forRestore(error: Throwable?): String =
        reason(error) + " Restoration was not confirmed. Check your existing projects before trying again; retained recovery copies remain available."
}
