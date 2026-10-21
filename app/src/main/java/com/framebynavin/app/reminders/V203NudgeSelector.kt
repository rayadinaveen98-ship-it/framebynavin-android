package com.framebynavin.app.reminders

import com.framebynavin.app.data.CreatorContextNudge
import com.framebynavin.app.data.CreatorContextNudgeLevel
import java.time.ZoneId

/**
 * Chooses the highest-priority *eligible* active project instead of repeatedly
 * selecting a suppressed key. History is per key (not merely the last sent key).
 * V201's global 24-hour and daytime/quiet-hours checks remain authoritative.
 */
internal object V203NudgeSelector {
    fun choose(
        candidates: List<CreatorContextNudge>,
        nowMillis: Long,
        lastGlobalShownAtMillis: Long,
        lastGlobalKey: String,
        lastShownAtForKey: (String) -> Long,
        quietHoursEnabled: Boolean,
        quietStartMinute: Int,
        quietEndMinute: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): CreatorContextNudge? {
        for (nudge in candidates) {
            if (nudge.level == CreatorContextNudgeLevel.READY ||
                nudge.taskId.isBlank() ||
                nudge.key.isBlank()
            ) continue

            if (!V201ContextNudgePolicy.mayShow(
                    nowMillis = nowMillis,
                    lastShownMillis = lastGlobalShownAtMillis,
                    lastKey = lastGlobalKey,
                    candidateKey = nudge.key,
                    quietHoursEnabled = quietHoursEnabled,
                    quietStartMinute = quietStartMinute,
                    quietEndMinute = quietEndMinute,
                    zone = zone,
                )
            ) continue

            // A-B-A rotation must NOT bypass the 72-hour repeat-key limit.
            val previousForKey = maxOf(
                lastShownAtForKey(nudge.key),
                if (nudge.key == lastGlobalKey) lastGlobalShownAtMillis else 0L,
            )
            if (previousForKey > 0L &&
                nowMillis - previousForKey < V201ContextNudgePolicy.SAME_KEY_COOLDOWN_MS
            ) continue

            return nudge
        }
        return null
    }
}
