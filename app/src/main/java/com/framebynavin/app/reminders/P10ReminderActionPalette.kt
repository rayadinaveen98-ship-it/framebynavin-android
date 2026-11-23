package com.framebynavin.app.reminders

import androidx.compose.ui.graphics.Color

/**
 * Act II surfaces belong to the same P10 family as the entrance.
 * No retired Director, Scene or Premiere palette is referenced.
 * This is presentation-only; no action, service or scheduling semantics.
 */
internal data class P10ReminderActionPalette(
    val black: Color,
    val hot: Color,
    val gold: Color,
    val ivory: Color,
    val muted: Color,
    val panel: Color,
    val line: Color,
)

internal fun P10ReminderFamily.actionPalette(): P10ReminderActionPalette {
    val p = pigments()
    return P10ReminderActionPalette(
        black = p.dark,
        hot = p.light,
        gold = p.warm,
        ivory = Color(0xFFF8F5F1),
        muted = Color(0xFFD4CAD0),
        panel = Color(0xD8161420),
        line = p.secondary.copy(alpha = .37f),
    )
}
