package com.framebynavin.app.reminders

/** Point 1: pure limits and crop policy, isolated from reminder delivery. */
internal object P1GalleryImportPolicy {
    const val MAX_VIDEO_MILLIS: Long = 15_000L
    const val MAX_BYTES: Long = 40L * 1024L * 1024L
    fun validBytes(bytes: Long): Boolean = bytes in 1..MAX_BYTES
    fun validVideoDuration(millis: Long): Boolean = millis in 1..MAX_VIDEO_MILLIS
    fun boundedFraming(frame: P1MediaFraming): P1MediaFraming = P1MediaFraming(
        zoom = frame.zoom.coerceIn(1f, 2.5f),
        horizontal = frame.horizontal.coerceIn(-1f, 1f),
        vertical = frame.vertical.coerceIn(-1f, 1f),
    )
}

/** V337 has a live Creator entrance only; never invent another alert route. */
internal object P1V337HostCompatibility {
    fun liveEntranceAvailable(scope: P1ReminderScope): Boolean =
        scope == P1ReminderScope.CREATOR
}
