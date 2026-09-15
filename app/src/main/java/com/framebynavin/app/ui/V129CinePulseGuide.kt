package com.framebynavin.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
 * The selected v137 character is now the single renderer used everywhere.
 */
@Composable
internal fun CinePulseGuide(
    state: CinePulseState,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    BacklotGuideCharacter(
        character = BacklotCharacterPrefs.currentCharacter,
        state = state,
        modifier = modifier,
        pointRight = pointRight,
    )
}
