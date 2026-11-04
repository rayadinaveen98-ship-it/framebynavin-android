package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.reminders.ReminderMusicMode
import com.framebynavin.app.widget.V224WidgetUpdater
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * V285: a 21-day *plan* and a separate, opt-in routine reminder installer.
 *
 * The daily YouTube goals are proposals displayed locally, not completed/published
 * Creator projects. No video upload, project creation, cloud write, or automatic
 * reminder installation takes place when this object is read.
 */
internal object V285PersonalLifePlan {
    const val ID_PREFIX = "life21-v1-"
    val firstDay: LocalDate = LocalDate.of(2026, 11, 5)
    val lastDay: LocalDate = firstDay.plusDays(20)

    internal enum class Category(val title: String) {
        SKINCARE("Skin care"),
        CREATOR_TIME("Creative focus"),
        REST_AND_READING("Rest & reading"),
        GAME_DEVELOPMENT("Game development"),
    }

    internal data class Day(
        val date: LocalDate,
        val primary: String,
        val secondary: String,
        val publishing: String? = null,
    )

    private fun day(offset: Long, main: String, extra: String, publish: String? = null) =
        Day(firstDay.plusDays(offset), main, extra, publish)

    val days = listOf(
        day(0, "Choose the Nov 7 filmmaking argument, outline the original script and visual evidence.",
            "Organize footage, editing folders and the 21-day checklist."),
        day(1, "Record the relaunch voiceover; finish editing, thumbnail and upload checks.",
            "Prepare social launch materials. Keep the first channel upload for Nov 7."),
        day(2, "Publish the first original-voiceover FrameByNavin comeback video.",
            "Share a social teaser and reply to comments.", "NOV 7 · FIRST UPLOAD"),
        day(3, "Review launch analytics and audience feedback; choose the next breakdown.",
            "Weekly reset, rest and light game-design brainstorming."),
        day(4, "Research the Nov 15 flagship scene breakdown: intention, shots and sound.",
            "Learn one game-development concept."),
        day(5, "Write the flagship script; match each argument with evidence.",
            "Work on a small game-development prototype."),
        day(6, "Record flagship voiceover and assemble the rough cut.",
            "Refine audio and original explanatory visuals."),
        day(7, "Write, record and rough-edit the Friday Filmmaker POV review.",
            "Game-development session if review progress permits."),
        day(8, "Publish the weekly Filmmaker POV Movie Review.",
            "Post one short analysis-led reel and respond to comments.", "FRIDAY · REVIEW"),
        day(9, "Finalize the Nov 15 flagship edit, sound, thumbnail and metadata.",
            "Watch and note one filmmaking technique."),
        day(10, "Publish the flagship filmmaking breakdown.",
            "Weekly review and rest.", "SUNDAY · FLAGSHIP"),
        day(11, "Research and outline the next Friday film review.",
            "Learn one game-development concept."),
        day(12, "Draft the review and select visual references for each point.",
            "Game-development prototype session."),
        day(13, "Record the review voiceover and assemble the first complete edit.",
            "Storyboard the Nov 22 flagship analysis."),
        day(14, "Polish the review, subtitles, thumbnail and publication checks.",
            "Optional short game-development session."),
        day(15, "Publish the Filmmaker POV Movie Review.",
            "Post a short Reel with original commentary.", "FRIDAY · REVIEW"),
        day(16, "Finish the Nov 22 flagship video and technical QA.",
            "Keep the evening light to protect tomorrow's upload."),
        day(17, "Publish the flagship filmmaking breakdown.",
            "Review channel progress and rest.", "SUNDAY · FLAGSHIP"),
        day(18, "Research and outline the next Friday review (Nov 27).",
            "Game-development learning."),
        day(19, "Record the next review and assemble its rough cut.",
            "Game-development prototype session."),
        day(20, "Refine the review audio, edit and storytelling structure.",
            "21-day reflection and planning for the next cycle."),
    )

    internal data class Routine(
        val key: String,
        val category: Category,
        val title: String,
        val notes: String,
        val time: LocalTime,
        val weeklyDays: Set<DayOfWeek> = emptySet(),
        val snoozeMinutes: Int = 10,
    ) {
        val id: String get() = ID_PREFIX + key
    }

    private val monSat = DayOfWeek.entries.filterNot { it == DayOfWeek.SUNDAY }.toSet()
    private val tueThuSat = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY)

    /** Only gentle notifications; existing V284 meal, workout and wake alarms are NOT duplicated. */
    val routines: List<Routine> = listOf(
        Routine("skin-am", Category.SKINCARE, "Morning skincare",
            "After workout/shower: gentle face wash, moisturizer, broad-spectrum SPF 30+ sunscreen. Leave on; reapply sunscreen if outdoors.", LocalTime.of(8, 0)),
        Routine("skin-sun", Category.SKINCARE, "Sun protection before outdoor walk",
            "If walking outdoors in daylight, reapply sunscreen; wash your face first only if very sweaty or dirty.", LocalTime.of(16, 45), monSat),
        Routine("skin-pm", Category.SKINCARE, "Evening skincare",
            "Gentle face wash, Minimalist niacinamide serum if tolerated and appropriate for its strength, then moisturizer. Start serum slowly.", LocalTime.of(21, 30)),
        Routine("write", Category.CREATOR_TIME, "Scriptwriting & filmmaking research",
            "Focus block until 11 AM. Original analysis, strong arguments, scene evidence and voiceover scripts; phone distractions off.", LocalTime.of(9, 0), monSat),
        Routine("edit", Category.CREATOR_TIME, "Editing & voiceover focus",
            "Deep editing or original voice recording until 1:15 PM. Check progress against the selected day's goal.", LocalTime.of(11, 15), monSat),
        Routine("polish", Category.CREATOR_TIME, "Production finish block",
            "From 3:15 to 4:25 PM: sound, edit, thumbnails or publication readiness. Protect your afternoon snack and walk.", LocalTime.of(15, 15), monSat),
        Routine("evening", Category.CREATOR_TIME, "Light creative work & tomorrow's top priority",
            "From 8 PM to 9:15 PM: calm planning, feedback and finishing work. No overnight editing.", LocalTime.of(20, 0), monSat),
        Routine("nap", Category.REST_AND_READING, "Afternoon nap",
            "Rest from 2 PM to 3 PM. Optional adjustment to a shorter or ~90-minute nap if 60 minutes leaves you groggy; don't compromise night sleep.", LocalTime.of(14, 0)),
        Routine("novel", Category.REST_AND_READING, "Read your novel",
            "Protected 10 PM reading time for around 30–40 minutes. Existing 10:15 PM wind-down voice reminder may need moving to 10:40 PM.", LocalTime.of(22, 0)),
        Routine("game", Category.GAME_DEVELOPMENT, "Game development practice",
            "Tue/Thu/Sat after the walk: 5:45–7:15 PM prototype, code, test or learn. Skip when a committed YouTube deadline needs the buffer.", LocalTime.of(17, 45), tueThuSat),
    )

    fun focusOn(date: LocalDate): Day? = days.firstOrNull { it.date == date }

    fun templates(startDate: LocalDate, zone: ZoneId, categories: Set<Category> = Category.entries.toSet()): List<PersonalReminder> =
        routines.filter { it.category in categories }.map { routine ->
            PersonalReminder(
                id = routine.id,
                title = routine.title,
                notes = routine.notes,
                startDate = startDate,
                time = routine.time,
                zoneId = zone,
                repeat = if (routine.weeklyDays.isEmpty()) PersonalRepeat.DAILY else PersonalRepeat.WEEKLY,
                weeklyDays = routine.weeklyDays,
                alert = PersonalAlert.NOTIFICATION,
                snoozeMinutes = routine.snoozeMinutes,
                voiceMusicMode = ReminderMusicMode.PIANO,
                voiceRepeatCount = 3,
                autoRetryEnabled = false,
            ).checked()
        }

    /**
     * One optional reminder for each *future* planned day (8:50 AM local time).
     * A late install never generates stale, backdated alarms; IDs remain stable.
     */
    fun focusTemplates(nowMillis: Long, zone: ZoneId): List<PersonalReminder> =
        days.filter { day ->
            day.date.atTime(8, 50).atZone(zone).toInstant().toEpochMilli() > nowMillis
        }.map { day ->
            PersonalReminder(
                id = ID_PREFIX + "focus-" + day.date.toString(),
                title = "Today's filmmaking focus",
                notes = day.primary + " " + day.secondary,
                startDate = day.date,
                time = LocalTime.of(8, 50),
                zoneId = zone,
                repeat = PersonalRepeat.ONCE,
                alert = PersonalAlert.NOTIFICATION,
                snoozeMinutes = 10,
                autoRetryEnabled = false,
            ).checked()
        }

    fun mergeMissing(existing: List<PersonalReminder>, additions: List<PersonalReminder>): List<PersonalReminder> {
        val ids = existing.mapTo(HashSet()) { it.id }
        return existing + additions.filter { it.id !in ids }
    }

    internal data class InstallResult(
        val added: Int,
        val schedulingAccepted: Boolean,
        val widgetsRefreshed: Boolean,
    )

    /** One atomic Personal-only save, followed by independently reported delivery reconciliation. */
    suspend fun install(
        context: Context,
        categories: Set<Category>,
        includeDailyFocus: Boolean = false,
    ): InstallResult {
        require(categories.isNotEmpty() || includeDailyFocus) {
            "Choose at least one routine or daily focus reminder."
        }
        val app = context.applicationContext
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val desired = templates(LocalDate.now(zone), zone, categories) +
            if (includeDailyFocus) focusTemplates(now, zone) else emptyList()
        var added = 0
        PersonalReminderStore(app).mutate { current ->
            val updated = mergeMissing(current, desired)
            added = updated.size - current.size
            updated
        }
        val deliveryOk = runCatching { PersonalDeliveryCoordinator(app).reconcile() }.isSuccess
        val widgetOk = runCatching { V224WidgetUpdater.refresh(app) }.isSuccess
        return InstallResult(added, deliveryOk, widgetOk)
    }

    fun hasReadingConflict(existing: List<PersonalReminder>): Boolean =
        existing.any { it.id == PersonalDietFitnessPlanV1.ID_PREFIX + "sleep" &&
            !it.paused && it.time.isAfter(LocalTime.of(22, 0)) &&
            it.time.isBefore(LocalTime.of(22, 40)) }
}
