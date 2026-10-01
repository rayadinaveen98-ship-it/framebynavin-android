package com.framebynavin.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

enum class CinePulseState {
    IDLE,
    WALK,
    POINT,
    PRESENT,
    LOOK,
    NOD,
    WAVE,
    THINK,
    LISTEN,
    SUCCESS,
    CELEBRATE,
    REST,
}

/**
 * Compatibility entry point retained for existing Backlot guide call sites.
 * V144 routes the same semantic states to Frame, Navi, Funny or Cute.
 */
@Composable
internal fun CinePulseGuide(
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    V144GuidePrefs.initialize(LocalContext.current)
    V144GuideCharacter(
        guide = V144GuidePrefs.currentGuide,
        state = state,
        modifier = modifier,
        pointRight = pointRight,
    )
}
