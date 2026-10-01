package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorPriorityEngine
import com.framebynavin.app.data.CreatorTask

/** Presentation-only helpers for Home's progressive Next Move disclosure. */
internal object NextMovePresentation {
    fun alternatives(
        rankedQueue: List<CreatorTask>,
        selectedProjectId: String,
        limit: Int = 2,
    ): List<CreatorTask> {
        if (limit <= 0) return emptyList()
        return rankedQueue
            .asSequence()
            .filter { it.id != selectedProjectId }
            .filter { CreatorPriorityEngine.score(it) != Int.MIN_VALUE }
            .take(limit)
            .toList()
    }
}
