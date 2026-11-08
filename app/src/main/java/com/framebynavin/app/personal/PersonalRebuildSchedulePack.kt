package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.widget.V224WidgetUpdater
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Explicit owner-selected Personal schedule pack. NEVER installed at startup.
 *
 * The repository deliberately contains no intimate journal entries, health
 * diagnoses, private photographs or sexual-behaviour tracking. The user may
 * store those privately in their own Personal Spaces if desired.
 *
 * Stable IDs, additive writes and one DataStore transaction preserve existing
 * reminders, previous edits, response receipts, alarms and cloud/creator data.
 */
internal object PersonalRebuildSchedulePack {
    const val PREFIX = "rebuild-owner-20261109-"
    const val CHALLENGE_ID = "rebuild-walk-21d-20261109"

    internal data class InstallResult(
        val remindersAdded: Int,
        val challengeCreated: Boolean,
        val schedulingAccepted: Boolean,
        val widgetsRefreshed: Boolean,
    )

    fun reminders(startDate: LocalDate, zone: ZoneId): List<PersonalReminder> = buildList {
        fun slot(
            key: String,
            title: String,
            notes: String,
            h: Int,
            m: Int,
            days: Set<DayOfWeek> = emptySet(),
            purpose: PersonalPurpose = PersonalPurpose.CUSTOM,
            alert: PersonalAlert = PersonalAlert.NOTIFICATION,
        ) {
            add(PersonalReminder(
                id = PREFIX + key,
                title = title,
                notes = notes,
                startDate = startDate,
                time = LocalTime.of(h, m),
                zoneId = zone,
                repeat = if (days.isEmpty()) PersonalRepeat.DAILY else PersonalRepeat.WEEKLY,
                weeklyDays = days,
                purpose = purpose,
                alert = alert,
                snoozeMinutes = 10,
                voiceRepeatCount = 1,
                alarmDurationSeconds = 30,
                autoRetryEnabled = false,
            ).checked())
        }

        slot("wake", "Wake up & morning routine",
            "Drink water, freshen up, brush with fluoride toothpaste, prepare two eggs and a banana. Get ready for the walk and apply outdoor sun protection.",
            7, 30, alert = PersonalAlert.ALARM)
        slot("morning-walk", "Morning walk",
            "Walk comfortably for 20–30 minutes. A short gentle walk is enough on recovery days.",
            8, 20, purpose = PersonalPurpose.MOVEMENT)
        slot("morning-skin", "Shower & morning skincare",
            "Green tea and cool down, then shower. Gentle cleanser, moisturizer, SPF 50 sunscreen on exposed skin, and lip balm if needed.",
            9, 5)
        slot("clarity", "Quiet thinking & idea journal",
            "Check in with your mood, read or think quietly, write down ideas and choose one manageable task. No pressure to produce.",
            10, 0, purpose = PersonalPurpose.PERSONAL_GOAL)
        slot("evening-walk", "Evening walk",
            "Comfortable outdoor movement for 20–40 minutes. Shorten or skip if recovery or illness requires it.",
            17, 15, purpose = PersonalPurpose.MOVEMENT)
        slot("night-skin", "Night skincare & grooming",
            "If showering, use lukewarm water. Gentle facewash, moisturizer and lip balm as needed; brush and floss. Introduce active serum cautiously if tolerated.",
            21, 0)
        slot("novel", "Read your novel",
            "Enjoy one novel for around 20–30 minutes. A few pages still count.",
            21, 30, purpose = PersonalPurpose.PERSONAL_GOAL)
        slot("wind-down", "Screen off & wind down",
            "Finish Friends earlier, turn off autoplay, dim lights and prepare for a steady bedtime.",
            22, 15, purpose = PersonalPurpose.SLEEP)

        slot("app-work", "Focused Backlot & app work",
            "One defined milestone for up to 70 minutes; finish with a short progress note. Keep the evening work session optional.",
            10, 30,
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                         DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            purpose = PersonalPurpose.PERSONAL_GOAL)

        slot("hobby-mon", "Strength & roses",
            "Short beginner strength session and ten minutes with your rose plants. Adjust to recovery.",
            12, 0, days = setOf(DayOfWeek.MONDAY), purpose = PersonalPurpose.MOVEMENT)
        slot("hobby-tue", "Kickboxing basics & Ramayana",
            "Gentle stance/footwork practice, then read a short section of the Ramayana. No sparring.",
            12, 0, days = setOf(DayOfWeek.TUESDAY))
        slot("hobby-wed", "Yoga & drawing",
            "Gentle beginner yoga or stretching, then draw a simple object for enjoyment.",
            12, 0, days = setOf(DayOfWeek.WEDNESDAY), purpose = PersonalPurpose.MOVEMENT)
        slot("hobby-thu", "Strength & rose gardening",
            "Short strength training and a quick check of rose leaves, soil and flowers.",
            12, 0, days = setOf(DayOfWeek.THURSDAY), purpose = PersonalPurpose.MOVEMENT)
        slot("hobby-fri", "Guitar & creative writing",
            "If you have a guitar, practice basics; then write one short fictional scene.",
            12, 0, days = setOf(DayOfWeek.FRIDAY))
        slot("hobby-sat", "Kickboxing & storytelling",
            "Easy footwork/balance practice, then develop a short story or script without publishing pressure.",
            12, 0, days = setOf(DayOfWeek.SATURDAY))
        slot("hobby-sun", "Mahabharata & free exploration",
            "Read a short section of the Mahabharata or enjoy an unstructured hobby. Rest is allowed.",
            12, 0, days = setOf(DayOfWeek.SUNDAY))
    }.also { records ->
        require(records.size == 16 && records.map { it.id }.toSet().size == records.size)
    }

    /**
     * Install only after a visible confirmation. Never replace older records.
     * Re-entering the installer is safe, and manually created matching titles
     * at the same time are not duplicated.
     */
    suspend fun install(context: Context): InstallResult {
        val app = context.applicationContext
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = maxOf(LocalDate.of(2026, 11, 9), today)
        val templates = reminders(start, zone)
        var added = 0
        PersonalReminderStore(app).mutate { current ->
            val ids = current.mapTo(HashSet()) { it.id }
            val missing = templates.filter { template ->
                template.id !in ids && current.none {
                    it.title.equals(template.title, ignoreCase = true) &&
                        it.time == template.time && it.repeat == template.repeat
                }
            }
            added = missing.size
            current + missing
        }

        val habits = Phase1HabitStore(app)
        var createdChallenge = false
        // This is a one-activity challenge with no duplicate notification:
        // its 08:20 walking reminder already belongs to the Personal pack.
        if (habits.load().none { it.id == CHALLENGE_ID }) {
            val challenge = Phase1HabitChallenge(
                id = CHALLENGE_ID,
                activity = "Morning walking",
                startDate = start,
                zoneId = zone.id,
            ).checked()
            try {
                habits.create(challenge)
                createdChallenge = true
            } catch (error: Exception) {
                throw IllegalStateException(
                    "$added reminders were saved. The walking challenge needs review; " +
                        "opening this installer again will not replace or duplicate them.",
                    error,
                )
            }
        }

        val scheduled = runCatching {
            PersonalDeliveryCoordinator(app).reconcile()
            Phase1HabitAlarmScheduler(app).reconcile()
        }.isSuccess
        val widgets = runCatching { V224WidgetUpdater.refresh(app) }.isSuccess
        return InstallResult(added, createdChallenge, scheduled, widgets)
    }
}
