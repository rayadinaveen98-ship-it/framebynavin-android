package com.framebynavin.app.ui

import com.framebynavin.app.data.ProjectAttentionPlan

/**
 * V174: A fast first view uses the same saved project and reminder validation as
 * the complete seven-step flow. Only presentation and the *new* Quick Create
 * default deadline differ. No persistence shortcuts or background automation.
 */
internal object V174QuickCreatePolicy {
    private const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000

    /** Quick Create gives new work a full day by default instead of silently using one hour. */
    fun defaultDueAt(nowMillis: Long): Long = nowMillis + ONE_DAY_MILLIS

    fun needsCustomReminderTime(plan: ProjectAttentionPlan): Boolean =
        plan == ProjectAttentionPlan.CUSTOM

    fun valid(
        title: String,
        archetypeId: String,
        platform: String,
        format: String,
        dueAtMillis: Long,
        reminderAtMillis: Long,
        attentionPlan: ProjectAttentionPlan,
        nowMillis: Long,
    ): Boolean = V153QuickCreatePolicy.canCreate(
        title = title,
        archetypeId = archetypeId,
        platform = platform,
        format = format,
        dueAtMillis = dueAtMillis,
        nowMillis = nowMillis,
        supportReady = v20NewProjectSupportReady(attentionPlan, reminderAtMillis, dueAtMillis, nowMillis),
    )
}
