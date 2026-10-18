package com.framebynavin.app.reminders

import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.ProjectPulseResponse

/** Clear but non-guaranteed timing copy for full-screen check-in actions. */
object ReminderResponseCopy {
    fun followUp(
        plan: ProjectAttentionPlan,
        response: ProjectPulseResponse,
        stageCheckIn: Boolean,
    ): String {
        if (!stageCheckIn) return when (response) {
            ProjectPulseResponse.WORKING -> "The project remains open. This alert won't repeat automatically."
            ProjectPulseResponse.DISMISSED -> "This one-time reminder will be dismissed."
            else -> "This action only changes the reminder."
        }
        val millis = ProjectPulseEngine.followUpDelayMillis(plan, response)
        if (millis <= 0L) return "Reminders are switched off for this project."
        val minutes = millis / 60_000L
        val display = if (minutes % 60L == 0L) "${minutes / 60}h"
            else if (minutes >= 60L) "${minutes / 60}h ${minutes % 60}m"
            else "${minutes}m"
        return "Next check-in is usually within $display, or sooner at your deadline."
    }
}
