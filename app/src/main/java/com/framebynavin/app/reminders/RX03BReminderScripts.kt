package com.framebynavin.app.reminders

import com.framebynavin.app.data.VoicePersona

/**
 * RX03B: domain-aware, concise and deterministic voice-pack copy.
 *
 * These are SCRIPT identities, not eight guaranteed distinct TTS speakers.
 * Personal and Challenge only opt into these scripts when their user explicitly
 * selects a pack. Creator uses these v2 scripts with the saved task persona.
 *
 * Never claims a reminder was completed, snoozed, viewed or even delivered.
 * No time-of-day or streak is invented. Audio repeat continues to reuse the
 * exact same frozen text for a single occurrence.
 */
internal object RX03BReminderScripts {
    private fun clean(value: String): String =
        value.trim().replace(Regex("\\s+"), " ")

    private fun sentence(value: String): String {
        val normalized = clean(value)
        return if (normalized.endsWith(".") || normalized.endsWith("!") ||
            normalized.endsWith("?")) normalized else "$normalized."
    }

    fun creator(
        persona: VoicePersona,
        title: String,
        urgency: String,
        stage: String,
        notes: String,
        includeNotes: Boolean,
    ): String {
        val task = "Task: " + sentence(title)
        val phase = "Stage: " + sentence(stage)
        val priority = urgency.lowercase()
        // Critical reminders are informative first, not comedic, across every
        // voice style. Style still determines the selected TTS speaker/timbre.
        val script = if ("critical" in priority) {
            "Critical creator deadline. $task $phase This needs your attention now."
        } else {
            val lead = if ("important" in priority)
                "Important creator reminder. " else ""
            val body = when (persona) {
                VoicePersona.WARM ->
                    "A small creative nudge. $task $phase One step at a time."
                VoicePersona.WOMAN ->
                    "Creator check-in. $task $phase Ready for your attention."
                VoicePersona.MAN ->
                    "Focus time. $task $phase Choose your next move."
                VoicePersona.YOUNG ->
                    "Ready for the next scene? $task $phase Let's make progress."
                VoicePersona.FUNNY ->
                    "Plot twist! Your next scene is on the task list. $task $phase Cue the action."
                VoicePersona.CARTOON ->
                    "Lights, camera, action! Creative mission: " +
                        sentence(title) + " $phase Time to begin."
                VoicePersona.ALIEN ->
                    "Backlot transmission. Mission: " +
                        sentence(title) + " Current phase: " +
                        sentence(stage) + " Awaiting your signal."
                VoicePersona.ROBOT ->
                    "Backlot task alert. $task Workflow stage: " +
                        sentence(stage) + " Action pending."
            }
            lead + body
        }
        val additional = clean(notes)
        return if (includeNotes && additional.isNotBlank())
            "$script Notes: $additional" else script
    }

    fun personal(persona: VoicePersona, title: String): String {
        val task = sentence(title)
        return when (persona) {
            VoicePersona.WARM ->
                "A gentle personal reminder. $task Take a moment for this."
            VoicePersona.WOMAN ->
                "Your personal reminder is here. $task It's on your schedule."
            VoicePersona.MAN ->
                "A quick personal check-in. $task Time to give this your attention."
            VoicePersona.YOUNG ->
                "Hey! Here's your reminder. $task Let's make it happen."
            VoicePersona.FUNNY ->
                "A little plot twist in your day: $task Consider this your friendly nudge."
            VoicePersona.CARTOON ->
                "Attention, superstar! Your reminder has arrived: $task Action time!"
            VoicePersona.ALIEN ->
                "Incoming personal transmission. Subject: $task Signal received."
            VoicePersona.ROBOT ->
                "Personal reminder notification. Task: $task Action pending."
        }
    }

    fun challenge(persona: VoicePersona, activity: String): String {
        val task = sentence(activity)
        return when (persona) {
            VoicePersona.WARM ->
                "Your 21-day challenge check-in. Today's activity: $task One step at a time."
            VoicePersona.WOMAN ->
                "21-day challenge reminder. Today's activity: $task " +
                    "Open Backlot when you're ready to record your progress."
            VoicePersona.MAN ->
                "Your challenge is calling. Today's focus: $task Give it your attention."
            VoicePersona.YOUNG ->
                "Let's go! Your 21-day challenge is ready. Today's activity: $task You can do this."
            VoicePersona.FUNNY ->
                "Today's plot twist: a mini mission! Activity: $task Small steps, big story."
            VoicePersona.CARTOON ->
                "Lights, camera, challenge! Today's mission: $task Ready for the next move?"
            VoicePersona.ALIEN ->
                "Challenge transmission received. Today's mission: $task Awaiting your response."
            VoicePersona.ROBOT ->
                "21-day challenge alert. Activity: $task Awaiting your action."
        }
    }

    /** Domain-specific realistic examples: changing a script never schedules a reminder. */
    fun creatorPreview(persona: VoicePersona): String = creator(
        persona,
        title = "Finish short-film edit",
        urgency = "reminder",
        stage = "Editing",
        notes = "",
        includeNotes = false,
    )

    fun personalPreview(persona: VoicePersona): String =
        personal(persona, title = "Drink water")

    fun challengePreview(persona: VoicePersona): String =
        challenge(persona, activity = "Morning walk")
}
