package com.framebynavin.app.data

/**
 * Historical v0 demo, verified against source commit
 * 15aa6faf64525ff043a1b184598580cdde5c360a.
 *
 * Never drop an actual creator project merely because it carries the old
 * starter ID. Comparison to the full default-value record is intentionally
 * conservative: any changed content, workflow, reminder or archive data
 * preserves the project. A touched seed indistinguishable from the original
 * record cannot be detected from this old format; prefer preservation for
 * any non-identical record.
 */
internal object V230LegacyStarterProjectPolicy {
    private val originalSeed = CreatorTask(
        id = "starter-frame-breakdown",
        title = "Frame Breakdown",
        platform = "Instagram",
        contentType = "Reel",
        dueLabel = "Today · 7:00 PM",
        status = TaskStatus.WORKING,
        progress = 72,
    )

    fun isUntouchedDemo(task: CreatorTask): Boolean = task == originalSeed

    /** Pure, stable-order cleanup; only an EXACT original demo record is removed. */
    fun withoutUntouchedDemos(tasks: List<CreatorTask>): List<CreatorTask> =
        tasks.filterNot(::isUntouchedDemo)
}
