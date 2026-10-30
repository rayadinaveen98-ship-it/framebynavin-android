package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorRewardEventType
import com.framebynavin.app.data.CreatorRewardLedgerEntry

/**
 * Only a committed stage-completion reward may trigger a celebration.
 * Other creator awards never pretend that a production step was finished.
 */
internal object V2284StageCelebrationPolicy {
    fun confirmedStageKey(entry: CreatorRewardLedgerEntry?): String? =
        entry?.takeIf {
            it.type == CreatorRewardEventType.PROJECT_STAGE_COMPLETED &&
                it.eventKey.startsWith("stage:") && it.projectId.isNotBlank()
        }?.eventKey
}
