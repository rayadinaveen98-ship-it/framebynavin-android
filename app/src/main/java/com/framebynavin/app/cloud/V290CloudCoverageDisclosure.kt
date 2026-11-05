package com.framebynavin.app.cloud

/**
 * Precise, release-scoped description of the cloud surfaces that are actually
 * wired to authenticated sync in the V290 candidate.
 *
 * A green Creator/Personal sync receipt is NOT evidence that every app-private
 * Space and photo has survived on a second device.
 */
internal object V290CloudCoverageDisclosure {
    const val TITLE = "Creator + Personal cloud sync"

    const val INCLUDED =
        "Sync Now covers Creator projects, ideas, schedules and Personal Reminders " +
            "with completion history. Supported Voice Idea recordings sync separately."

    const val EXCLUDED =
        "NOT yet in this cloud backup: Personal Spaces, progress/check-in notes, " +
            "private photo journals, 21-day challenges, Reminder Groups, original Best Frame photos and custom music files. " +
            "You can export Spaces metadata separately; private photo bytes stay on " +
            "this device. Android reminder permissions must be set up on each phone."

    const val SUCCESS_SCOPE =
        "A successful cloud sync confirms supported Creator and Personal records only. " +
            "It does not confirm a complete Backlot backup or photo recovery."
}
