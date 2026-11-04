package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.reminders.ReminderMusicMode
import com.framebynavin.app.widget.V224WidgetUpdater
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * User-reviewed Diet & Fitness V1.0 template. It is never silently seeded.
 *
 * This uses the existing Personal store, alarm scheduling, permissions and backup;
 * it does not write CreatorTask, creator projects, YouTube data or health telemetry.
 * Stable IDs allow safe re-entry without replacing user edits or Snooze/Done leases.
 */
/** Distinguishes durable DataStore saves from best-effort Android scheduling. */
internal data class PersonalDietFitnessInstallResult(
    val added: Int,
    val deliverySchedulingAccepted: Boolean,
    val widgetsRefreshed: Boolean,
)

internal object PersonalDietFitnessPlanV1 {
    const val ID_PREFIX = "dietfit-v1-"

    private data class DayMenu(
        val day: DayOfWeek,
        val breakfast: String,
        val lunch: String,
        val snack: String,
        val dinner: String,
    )

    private val menus = listOf(
        DayMenu(DayOfWeek.MONDAY,
            "2 idlis + 1 cup sambar + 3 boiled eggs.",
            "150–200 g cooked rice + about 150 g cooked chicken curry + vegetables + 150–200 g curd.",
            "1 fruit + 30–40 g roasted chana.",
            "2 whole-wheat rotis (no added oil/ghee) + 1 cup dal + 1 egg + curd + vegetables."),
        DayMenu(DayOfWeek.TUESDAY,
            "2 pesarattu + 2 eggs.",
            "150–200 g cooked rice + 150–180 g fish curry + vegetables + curd.",
            "200–250 ml milk + a small handful of peanuts.",
            "2 whole-wheat rotis (no added oil/ghee) + 100–150 g paneer curry + 1 cup dal."),
        DayMenu(DayOfWeek.WEDNESDAY,
            "2 medium dosas + sambar + 2 eggs.",
            "150–200 g cooked rice + 3-egg curry + 1 cup dal + vegetables.",
            "1 fruit + 30–40 g roasted chana.",
            "2 whole-wheat rotis (no added oil/ghee) + about 150 g cooked chicken curry + vegetables."),
        DayMenu(DayOfWeek.THURSDAY,
            "1 cup vegetable upma + 3 eggs + 200–250 ml milk.",
            "150–200 g cooked rice + 1 cup dal + 100–150 g paneer curry + vegetables.",
            "150–200 g curd + 30–40 g roasted chana.",
            "2 whole-wheat rotis (no added oil/ghee) + 150–180 g fish curry + vegetables."),
        DayMenu(DayOfWeek.FRIDAY,
            "2 whole-wheat rotis (no added oil/ghee) + 2-egg bhurji + curd.",
            "150–200 g cooked rice + about 150 g cooked chicken curry + vegetables.",
            "1 banana + 200–250 ml milk.",
            "2 whole-wheat rotis (no added oil/ghee) + 100–150 g paneer curry + 1 cup dal + salad."),
        DayMenu(DayOfWeek.SATURDAY,
            "2 idlis + sambar + 2 eggs.",
            "150–200 g cooked rice + 150–180 g fish curry + 1 cup dal + vegetables.",
            "1 fruit + 200–250 ml milk + 30–40 g roasted chana.",
            "2 whole-wheat rotis (no added oil/ghee) + 3-egg curry + 1 cup dal."),
        DayMenu(DayOfWeek.SUNDAY,
            "Homemade breakfast: 2 dosas + sambar + 2 eggs.",
            "Homemade chicken biryani, moderate portion + raita + salad.",
            "1 fruit + 200–250 ml milk.",
            "2 whole-wheat rotis (no added oil/ghee) + 100–150 g paneer curry + 1 cup dal + vegetables."),
    )

    private val monWedFri = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    private val tueThu = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)

    /** Builds fixed recurring time slots in the device's local time zone on install. */
    fun templates(startDate: LocalDate, zoneId: ZoneId): List<PersonalReminder> = buildList {
        fun addSlot(
            key: String, title: String, notes: String,
            time: LocalTime, days: Set<DayOfWeek> = emptySet(),
            alert: PersonalAlert = PersonalAlert.NOTIFICATION,
            snooze: Int = 10,
            purpose: PersonalPurpose = PersonalPurpose.CUSTOM,
        ) {
            add(PersonalReminder(
                id = ID_PREFIX + key,
                title = title,
                notes = notes,
                startDate = startDate,
                time = time,
                zoneId = zoneId,
                repeat = if (days.isEmpty()) PersonalRepeat.DAILY else PersonalRepeat.WEEKLY,
                weeklyDays = days,
                alert = alert,
                snoozeMinutes = snooze,
                purpose = purpose,
                voiceMusicMode = ReminderMusicMode.PIANO,
                voiceRepeatCount = 3,
                voiceRepeatGapMillis = 2_500L,
                autoRetryEnabled = false,
            ).checked())
        }

        addSlot("wake", "Wake up & hydrate",
            "Wake around 7 AM. On weigh-in days, use the bathroom and weigh before food or water; then drink water. Work toward regular sleep.",
            LocalTime.of(7, 0), alert = PersonalAlert.ALARM)
        addSlot("weigh", "Morning weigh-in",
            "After the bathroom, before eating or drinking, record your weight. Compare weekly averages, not single readings.",
            LocalTime.of(7, 5), monWedFri + DayOfWeek.SUNDAY,
            purpose = PersonalPurpose.PERSONAL_GOAL)
        addSlot("workout", "Full-body strength workout",
            "30 minutes. Warm up; practise squats, incline push-ups, glute bridges, rows and short planks. Increase gradually.",
            LocalTime.of(7, 15), monWedFri, PersonalAlert.VOICE,
            purpose = PersonalPurpose.MOVEMENT)
        addSlot("mobility", "Mobility & recovery",
            "15–20 minutes of easy mobility and stretching, no intense training needed.",
            LocalTime.of(7, 15), tueThu, PersonalAlert.VOICE,
            purpose = PersonalPurpose.MOVEMENT)
        addSlot("walk-30", "Evening brisk walk · 30 min",
            "Walk at a comfortable brisk pace for around 30 minutes.",
            LocalTime.of(17, 0), monWedFri, PersonalAlert.VOICE,
            purpose = PersonalPurpose.MOVEMENT)
        addSlot("walk-35", "Evening brisk walk · 35 min",
            "Walk at a comfortable brisk pace for around 35 minutes.",
            LocalTime.of(17, 0), tueThu, PersonalAlert.VOICE,
            purpose = PersonalPurpose.MOVEMENT)
        addSlot("walk-40", "Evening walk · 40 min",
            "Saturday walk at a comfortable pace for around 40 minutes. Sunday walking is optional.",
            LocalTime.of(17, 0), setOf(DayOfWeek.SATURDAY), PersonalAlert.VOICE,
            purpose = PersonalPurpose.MOVEMENT)
        addSlot("fruit", "Fruit break",
            "One serving of seasonal fruit: guava, apple, orange or papaya. No special detox food required.",
            LocalTime.of(11, 0), snooze = 15)
        addSlot("sleep", "Prepare for sleep",
            "Start winding down: dim screens, avoid evening caffeine and plan to sleep around 10:45–11 PM. Keep wake-up time consistent.",
            LocalTime.of(22, 15), alert = PersonalAlert.VOICE,
            purpose = PersonalPurpose.SLEEP)
        addSlot("weekly-review", "Weekly fitness review",
            "Sunday check-in: note average morning weight, exercise consistency, hunger, energy and sleep. First milestone 75 kg; eventual goal around 70 kg.",
            LocalTime.of(18, 30), setOf(DayOfWeek.SUNDAY),
            purpose = PersonalPurpose.PERSONAL_GOAL)

        menus.forEach { menu ->
            val day = menu.day.name.lowercase()
                .replaceFirstChar { it.uppercaseChar() }
            val dayOnly = setOf(menu.day)
            addSlot("meal-${menu.day.name.lowercase()}-breakfast", "Breakfast · $day",
                menu.breakfast, LocalTime.of(8, 15), dayOnly, snooze = 15)
            addSlot("meal-${menu.day.name.lowercase()}-lunch", "Lunch · $day",
                menu.lunch, LocalTime.of(13, 30), dayOnly, snooze = 15)
            addSlot("meal-${menu.day.name.lowercase()}-snack", "Snack · $day",
                menu.snack + " Optional unsweetened green tea (avoid it late in the day).",
                LocalTime.of(16, 30), dayOnly, snooze = 15)
            addSlot("meal-${menu.day.name.lowercase()}-dinner", "Dinner · $day",
                menu.dinner, LocalTime.of(19, 30), dayOnly, snooze = 15)
        }
    }.also { items ->
        require(items.size == 38 && items.map { it.id }.toSet().size == items.size) {
            "Diet and fitness template configuration is incomplete"
        }
    }

    /** Append missing managed slots only; do not overwrite edited or snoozed records. */
    fun mergeMissing(
        existing: List<PersonalReminder>, templates: List<PersonalReminder>,
    ): List<PersonalReminder> {
        val existingIds = existing.mapTo(HashSet()) { it.id }
        return existing + templates.filter { it.id !in existingIds }
    }

    /**
     * Atomic commit of all missing reminders; one reconciliation after save.
     * A repeated install is a no-op and existing Personal/Creator entries survive.
     */
    suspend fun install(context: Context): PersonalDietFitnessInstallResult {
        val app = context.applicationContext
        val zone = ZoneId.systemDefault()
        val desired = templates(LocalDate.now(zone), zone)
        val store = PersonalReminderStore(app)
        var added = 0
        // This transaction is the only mutation. A scheduler failure AFTER it
        // commits must never be described to the user as a failed save.
        store.mutate { existing ->
            val updated = mergeMissing(existing, desired)
            added = updated.size - existing.size
            updated
        }
        return ensureDelivery(app, added)
    }

    /**
     * Explicit retry of Android scheduling without touching the saved plan.
     * Caller can offer this after a device setting, permission or OS failure.
     */
    suspend fun ensureDelivery(context: Context, added: Int = 0): PersonalDietFitnessInstallResult {
        val app = context.applicationContext
        val schedulingAccepted = runCatching {
            PersonalDeliveryCoordinator(app).reconcile()
        }.isSuccess
        val widgetRefresh = runCatching {
            V224WidgetUpdater.refresh(app)
        }.isSuccess
        return PersonalDietFitnessInstallResult(added, schedulingAccepted, widgetRefresh)
    }
}
