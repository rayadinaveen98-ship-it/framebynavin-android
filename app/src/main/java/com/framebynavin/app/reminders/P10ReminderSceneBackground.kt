package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag

/**
 * P10D presentation-only background compositor for both reminder acts.
 * Uses existing private P1 media/decoder; owns no notification, response or gallery file.
 * Gallery source and built-in family are independent.
 */
@Composable
internal fun P10ReminderSceneBackground(
    scope: P1ReminderScope,
    family: P10ReminderFamily,
    source: P10ReminderBackgroundSource,
    act: P10ReminderAct,
    media: P1Media?,
    framing: P1MediaFraming? = null,
    locked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val animationsAllowed = remember { ValueAnimator.areAnimatorsEnabled() }
    val batterySaver = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val galleryVisible = P10ReminderBackgroundPolicy.showGallery(
        source = source,
        galleryAvailable = media != null,
        locked = locked,
    )
    val palette = family.pigments()
    Box(modifier.fillMaxSize().testTag(
        "p10-scene-" + act.name.lowercase() + "-" + scope.name.lowercase(),
    )) {
        if (galleryVisible) {
            // The private media stays behind both acts. No second media store.
            P1EntranceBackdrop(
                domain = scope,
                motionEnabled = animationsAllowed,
                lowPower = batterySaver || lowRam,
                modifier = Modifier.fillMaxSize(),
                overrideMedia = requireNotNull(media),
                overrideFraming = framing,
                fallbackAct = act,
            )
            // Family-colored atmosphere is a lightweight wash, not a new media decoder.
            P10LivingReminderBackdrop(
                family = family,
                act = act,
                animationsAllowed = animationsAllowed,
                batterySaver = batterySaver,
                lowRam = lowRam,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = if (act == P10ReminderAct.ENTRANCE) .16f else .10f
                },
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(
                    palette.dark.copy(alpha = .30f),
                    Color.Transparent,
                    palette.dark.copy(alpha = if (act == P10ReminderAct.MAIN) .75f else .59f),
                ),
            )))
        } else {
            // This is the only no-media and media-error visual fallback.
            P10LivingReminderBackdrop(
                family = family,
                act = act,
                animationsAllowed = animationsAllowed,
                batterySaver = batterySaver,
                lowRam = lowRam,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
