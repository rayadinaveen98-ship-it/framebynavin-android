package com.framebynavin.app.personal.spaces

/**
 * A small offline suggestion for the user's OWN manual progress entry.
 * Never infer completion, streaks, health results, or create reminders.
 * Zero means no activity was logged, so an explicit positive number is required.
 */
internal data class V292SpaceMetricPreset(
    val name: String,
    val unit: String,
    val maxValue: Int,
) {
    /** Simple whole-number tracking; fail closed for malformed, huge or negative input. */
    fun parseOrNull(raw: String): Double? {
        val value = raw.trim()
        if (!value.matches(Regex("[0-9]{1,5}"))) return null
        val number = value.toIntOrNull() ?: return null
        return number.takeIf { it in 1..maxValue }?.toDouble()
    }
}

internal object V292SpaceMetricSuggestions {
    fun forType(type: V289SpaceType): V292SpaceMetricPreset? = when (type) {
        V289SpaceType.FITNESS -> V292SpaceMetricPreset("Workout time", "minutes", 1440)
        V289SpaceType.READING -> V292SpaceMetricPreset("Pages read", "pages", 10000)
        V289SpaceType.EDITING -> V292SpaceMetricPreset("Editing practice", "minutes", 1440)
        V289SpaceType.GAME_DEVELOPMENT -> V292SpaceMetricPreset("Building time", "minutes", 1440)
        V289SpaceType.GAMING -> V292SpaceMetricPreset("Playing time", "minutes", 1440)
        V289SpaceType.SKIN_CARE, V289SpaceType.CUSTOM -> null
    }

    /** Historic decimal values remain accurately displayable; no synthetic progress. */
    fun describe(name: String, value: Double, unit: String): String {
        require(value.isFinite()) { "Invalid recorded progress measurement" }
        val formatted = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        return "$name: $formatted $unit"
    }
}
