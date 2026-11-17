package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.CinePulseState
import com.framebynavin.app.ui.V144GuideCharacter
import com.framebynavin.app.ui.V144GuidePrefs
import kotlinx.coroutines.delay

/**
 * UX06E user-controlled, local-only preference. OFF by default; upgrading
 * V337 cannot cause an unexpected character to obscure live alert content.
 * This preference is separate from saved companion identity and scene IDs.
 */
internal object V407ReminderCompanionPrefs {
    private const val FILE = "backlot_reminder_companion_v407"
    private const val KEY = "show_on_live_alerts"
    private var app: Context? = null

    var showOnLiveAlerts by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        if (app != null) return
        app = context.applicationContext
        showOnLiveAlerts = app!!.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)
    }

    fun set(enabled: Boolean) {
        val context = app ?: return
        showOnLiveAlerts = enabled
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, enabled).apply()
    }
}

/**
 * A deliberately short, non-interactive appearance of the user's EXISTING
 * Spark/Mira artwork inside an existing decorative hero area. It never delays
 * Done, Snooze or any action and does not play a sound or claim a reward.
 *
 * The parent must place this inside its own hero Box, never over action
 * buttons or at the system/window level. A new occurrence can use a new key.
 */
@Composable
internal fun V407ReminderCompanionMoment(
    occurrenceKey: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    V407ReminderCompanionPrefs.initialize(context)
    if (!V407ReminderCompanionPrefs.showOnLiveAlerts) return

    V144GuidePrefs.initialize(context)
    val guide = V144GuidePrefs.currentGuide
    val reducedMotion = !ValueAnimator.areAnimatorsEnabled() ||
        remember(context) {
            context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        }
    var visible by remember(occurrenceKey) { mutableStateOf(true) }
    LaunchedEffect(occurrenceKey) {
        delay(3_000L)
        visible = false
    }
    // A future full companion celebration belongs to UX08. This lightweight
    // reminder greeting is only a decorative moment, not an achievement.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(if (reducedMotion) 0 else 180)),
        exit = fadeOut(tween(if (reducedMotion) 0 else 260)),
        modifier = modifier.testTag("ux06e-selected-companion-moment")
            .clearAndSetSemantics { },
    ) {
        Box(contentAlignment = Alignment.Center) {
            V144GuideCharacter(
                guide = guide,
                state = if (reducedMotion) CinePulseState.IDLE else CinePulseState.PRESENT,
                modifier = Modifier.size(width = 72.dp, height = 84.dp),
            )
        }
    }
}
