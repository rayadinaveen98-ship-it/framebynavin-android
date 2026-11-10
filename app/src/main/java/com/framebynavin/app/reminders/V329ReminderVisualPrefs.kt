package com.framebynavin.app.reminders

import android.content.Context

/**
 * Presentation preferences only. This store never reads or writes a reminder,
 * an occurrence, its schedule, sound, delivery, or acknowledgement state.
 */
internal enum class V329ReminderWorld(val title: String, val description: String) {
    CRIMSON("Crimson Singularity", "Gravitational embers and dramatic plasma"),
    LIQUID("Liquid Frequency", "Reflective ribbons and liquid light"),
    OBSIDIAN("Obsidian Bloom", "Ruby crystal fragments in black glass"),
    SIGNAL("Signal Architecture", "Precision grids and holographic scans"),
    NOCTURNE("Nocturne Constellation", "Velvet night and drifting constellations"),
}

internal enum class V329ReminderDomain { CREATOR, PERSONAL }
internal enum class V329ReminderChannel { ALARM, VOICE }

internal data class V329ReminderAppearance(
    val world: V329ReminderWorld,
    val intensity: Float = .72f,
    val density: Float = .74f,
    val speed: Float = .58f,
) {
    fun normalized() = copy(
        intensity = intensity.coerceIn(.15f, 1f),
        density = density.coerceIn(.15f, 1f),
        speed = speed.coerceIn(.15f, 1f),
    )
}

/** Independent, device-local options for all four Creator/Personal voice/alarm surfaces. */
internal class V329ReminderVisualStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "backlot_reminder_motion_v329", Context.MODE_PRIVATE,
    )

    private fun prefix(domain: V329ReminderDomain, channel: V329ReminderChannel) =
        "${domain.name.lowercase()}_${channel.name.lowercase()}"

    fun get(domain: V329ReminderDomain, channel: V329ReminderChannel): V329ReminderAppearance {
        val prefix = prefix(domain, channel)
        val defaultWorld = if (channel == V329ReminderChannel.ALARM)
            V329ReminderWorld.CRIMSON else V329ReminderWorld.LIQUID
        val world = V329ReminderWorld.entries.firstOrNull {
            it.name == prefs.getString("${prefix}_world", null)
        } ?: defaultWorld
        return V329ReminderAppearance(
            world = world,
            intensity = prefs.getFloat("${prefix}_intensity", .72f),
            density = prefs.getFloat("${prefix}_density", .74f),
            speed = prefs.getFloat("${prefix}_speed", .58f),
        ).normalized()
    }

    fun set(
        domain: V329ReminderDomain,
        channel: V329ReminderChannel,
        appearance: V329ReminderAppearance,
    ) {
        val prefix = prefix(domain, channel)
        val safe = appearance.normalized()
        prefs.edit()
            .putString("${prefix}_world", safe.world.name)
            .putFloat("${prefix}_intensity", safe.intensity)
            .putFloat("${prefix}_density", safe.density)
            .putFloat("${prefix}_speed", safe.speed)
            .apply()
    }
}
