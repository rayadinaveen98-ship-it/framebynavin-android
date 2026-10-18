package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorOsSettings
import com.framebynavin.app.data.WeeklyScheduleSlot

/** Presentation-only settings guidance; never affects reminder scheduling or permissions. */
internal data class V169PermissionSummary(
    val title: String,
    val explanation: String,
    val advancedMissing: Int,
)

internal object V169SettingsClarityPolicy {
    fun permissions(
        notifications: Boolean,
        preciseTiming: Boolean,
        fullScreen: Boolean,
        batteryAccess: Boolean,
    ): V169PermissionSummary {
        val missingExtras = listOf(preciseTiming, fullScreen, batteryAccess).count { !it }
        return if (!notifications) V169PermissionSummary(
            "Notifications are off",
            "Allow notifications to see standard Backlot reminders. Other Android options are available below.",
            missingExtras,
        ) else if (missingExtras > 0) V169PermissionSummary(
            "Standard notifications allowed",
            "Basic alerts are permitted. Some alarm and background options still need your attention.",
            missingExtras,
        ) else V169PermissionSummary(
            "Reminder permissions allowed",
            "Android permissions are allowed. Delivery can still be affected by device settings and battery restrictions.",
            0,
        )
    }

    fun snoozeChoices(current: Int): List<Int> =
        (listOf(5, 10, 15, 20, 30) + current.takeIf { it in 1..240 }.orEmptyNumber())
            .distinct().sorted()

    fun alarmTimeoutChoices(current: Int): List<Int> =
        (listOf(30, 60, 120, 300) + current.takeIf { it in 1..3600 }.orEmptyNumber())
            .distinct().sorted()

    private fun Int?.orEmptyNumber(): List<Int> = if (this == null) emptyList() else listOf(this)

    fun quietHoursDescription(settings: CreatorOsSettings): String =
        if (settings.quietHoursEnabled)
            "Quiet hours silence standard notification sounds. Voice, alarms and later Smart stages can still sound."
        else "Off. Normal reminder sound rules apply."

    fun autoPlanExplanation(enabled: Boolean, eligibleCount: Int, slots: List<WeeklyScheduleSlot>): String =
        if (enabled)
            "On · ${slots.count { it.enabled }} active weekly templates. ${eligibleCount} upcoming projects may be added; nothing is published."
        else
            "Off · ${slots.count { it.enabled }} active templates remain saved. Existing projects are untouched."
}
