package com.framebynavin.app.reminders

import android.content.Context

/**
 * P10A FOUNDATION ONLY. Not connected to production reminder rendering.
 *
 * Exactly three founder-approved built-in looks. These are independent of the
 * app-wide Director's Cut / Backlot Light themes. No V391 or V394 fallbacks.
 */
internal enum class P10ReminderFamily(val label: String) {
    LIVING_BLOOM("Living Bloom"),
    PULSE_STUDIO("Pulse Studio"),
    RISE_CORE("Rise Core"),
}

internal enum class P10ReminderAct {
    ENTRANCE,
    MAIN,
}

/** A predictable new visual fallback, not the retired Director/Premiere scene. */
internal object P10ReminderVisualPolicy {
    fun suggestedFamily(scope: P1ReminderScope): P10ReminderFamily = when (scope) {
        P1ReminderScope.CREATOR -> P10ReminderFamily.PULSE_STUDIO
        P1ReminderScope.PERSONAL -> P10ReminderFamily.LIVING_BLOOM
        P1ReminderScope.CHALLENGE_21 -> P10ReminderFamily.RISE_CORE
    }

    fun resolve(savedName: String?, scope: P1ReminderScope): P10ReminderFamily =
        P10ReminderFamily.entries.firstOrNull { it.name == savedName }
            ?: suggestedFamily(scope)

    /** Existing accessibility contract: with Android animations disabled, controls appear immediately. */
    fun startRevealed(animationsAllowed: Boolean): Boolean = !animationsAllowed

    /** Motion does not affect scheduling, notification delivery or response gates. */
    fun tuning(
        animationsAllowed: Boolean,
        batterySaver: Boolean,
        lowRam: Boolean,
        act: P10ReminderAct,
    ): P10MotionTuning {
        if (!animationsAllowed) return P10MotionTuning(
            animated = false,
            frameIntervalNanos = 0L,
            activeMotes = 0,
            depthLayers = 1,
            amplitude = 0f,
        )
        val reduced = batterySaver || lowRam
        return P10MotionTuning(
            animated = true,
            frameIntervalNanos = if (reduced) 50_000_000L else 33_333_333L,
            activeMotes = if (reduced) 6 else if (act == P10ReminderAct.ENTRANCE) 18 else 10,
            depthLayers = if (reduced) 2 else 4,
            // Intentional nonzero motion in both acts; quieter beneath controls.
            amplitude = if (reduced) 0.28f else if (act == P10ReminderAct.ENTRANCE) 1f else 0.54f,
        )
    }

    fun visibleMedia(locked: Boolean, hasSelectedMedia: Boolean): Boolean =
        !locked && hasSelectedMedia

    /** Accessible explicit activation is also permitted; never require touch double-tap from TalkBack. */
    fun shouldReveal(doubleTapped: Boolean, accessibleOpen: Boolean, unlocked: Boolean): Boolean =
        unlocked && (doubleTapped || accessibleOpen)
}

internal data class P10MotionTuning(
    val animated: Boolean,
    val frameIntervalNanos: Long,
    val activeMotes: Int,
    val depthLayers: Int,
    val amplitude: Float,
)

/**
 * Local-only family choices, separate for Creator, Personal and 21-Day.
 * Does not overwrite the existing P1 private gallery URI/crop store.
 * Never consumes old V391/V394 preference values or deletes them.
 */
internal object P10ReminderFamilyStore {
    private const val PREFS = "backlot_p10_reminder_visual_family_v1"

    fun selected(context: Context, scope: P1ReminderScope): P10ReminderFamily =
        P10ReminderVisualPolicy.resolve(
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(scope.name, null),
            scope,
        )

    fun select(context: Context, scope: P1ReminderScope, family: P10ReminderFamily): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(scope.name, family.name).commit()
}

/**
 * Selection of a built-in family and the saved private gallery are independent.
 * The gallery remains on disk when the owner switches to a built-in family.
 * A missing/corrupt gallery item always renders the selected P10 family.
 */
internal enum class P10ReminderBackgroundSource {
    BUILT_IN, GALLERY,
}

internal object P10ReminderBackgroundPolicy {
    fun resolved(
        saved: String?,
        galleryAvailable: Boolean,
    ): P10ReminderBackgroundSource = when (saved) {
        P10ReminderBackgroundSource.BUILT_IN.name -> P10ReminderBackgroundSource.BUILT_IN
        P10ReminderBackgroundSource.GALLERY.name ->
            if (galleryAvailable) P10ReminderBackgroundSource.GALLERY
            else P10ReminderBackgroundSource.BUILT_IN
        // Upgrade-safe: P1 gallery owners should not lose their existing selection.
        else -> if (galleryAvailable) P10ReminderBackgroundSource.GALLERY
            else P10ReminderBackgroundSource.BUILT_IN
    }

    fun showGallery(
        source: P10ReminderBackgroundSource,
        galleryAvailable: Boolean,
        locked: Boolean,
    ): Boolean = source == P10ReminderBackgroundSource.GALLERY &&
        galleryAvailable && !locked
}

internal object P10ReminderBackgroundStore {
    private const val PREFS = "backlot_p10_reminder_background_source_v1"

    fun selected(
        context: Context,
        scope: P1ReminderScope,
        galleryAvailable: Boolean,
    ): P10ReminderBackgroundSource = P10ReminderBackgroundPolicy.resolved(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(scope.name, null),
        galleryAvailable,
    )

    fun select(
        context: Context,
        scope: P1ReminderScope,
        source: P10ReminderBackgroundSource,
    ): Boolean = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(scope.name, source.name).commit()
}
