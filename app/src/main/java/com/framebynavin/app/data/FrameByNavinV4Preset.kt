package com.framebynavin.app.data

/** Build-visible metadata for the creator rhythm shipped with Backlot v144. */
object FrameByNavinV5Preset {
    const val LABEL = "FrameByNavin V5"
    const val MASTER_PROJECTS_PER_WEEK = 28
    const val PLATFORMS = "Instagram · YouTube · X"
}

/** Compatibility alias retained for older UI/tests that still reference the V4 symbol. */
object FrameByNavinV4Preset {
    const val LABEL = FrameByNavinV5Preset.LABEL
    const val MASTER_PROJECTS_PER_WEEK = FrameByNavinV5Preset.MASTER_PROJECTS_PER_WEEK
    const val PLATFORMS = FrameByNavinV5Preset.PLATFORMS
}
